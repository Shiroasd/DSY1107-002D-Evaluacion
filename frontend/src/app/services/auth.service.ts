import { Injectable, inject } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import {
  AccountInfo, AuthenticationResult, InteractionRequiredAuthError, InteractionStatus
} from '@azure/msal-browser';
import { BehaviorSubject, Observable, catchError, defer, finalize, map, of, shareReplay, tap } from 'rxjs';
import { UserProfile } from '../models/user-profile.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);
  private readonly apiScope = environment.apiConfig.scope;
  private readonly apiResource = this.apiScope.slice(0, this.apiScope.lastIndexOf('/'));
  private readonly profile = new BehaviorSubject<UserProfile | null>(null);
  private readonly needsInteraction = new BehaviorSubject(false);
  private cachedToken?: { accountKey: string; value: string };
  private pending?: { accountKey: string; token$: Observable<string | null> };
  private readonly redirect$ = defer(() => this.msal.handleRedirectObservable()).pipe(
    tap(result => {
      if (result?.account) {
        this.msal.instance.setActiveAccount(result.account);
        this.rememberToken(result);
      }
      this.updateUserState();
    }),
    catchError(() => {
      console.error('[AuthService] No se pudo completar la autenticación.');
      return of(null);
    }),
    shareReplay(1)
  );

  readonly currentUser$ = this.profile.asObservable();
  readonly isAuthenticated$ = this.currentUser$.pipe(map(user => user !== null));
  readonly isAdmin$ = this.currentUser$.pipe(map(user => user?.isAdmin ?? false));
  readonly needsInteraction$ = this.needsInteraction.asObservable();
  isInteractionInProgress = true;

  constructor() {
    this.broadcast.inProgress$.subscribe(status => {
      this.isInteractionInProgress = status !== InteractionStatus.None;
      if (!this.isInteractionInProgress) {
        this.updateUserState();
      }
    });
  }

  handleRedirectObservable(): Observable<AuthenticationResult | null> {
    return this.redirect$;
  }

  login(): void {
    if (this.isInteractionInProgress) {
      return;
    }

    this.isInteractionInProgress = true;
    const account = this.getActiveAccount();
    const redirect$ = account && this.needsInteraction.value
      ? this.msal.acquireTokenRedirect({ account, scopes: [this.apiScope] })
      : this.msal.loginRedirect({ scopes: [this.apiScope], prompt: 'select_account' });

    redirect$.subscribe({
      error: () => {
        this.isInteractionInProgress = false;
        console.error('[AuthService] No se pudo iniciar la autenticación.');
      }
    });
  }

  logout(): void {
    const account = this.getActiveAccount();
    this.cachedToken = undefined;
    this.pending = undefined;
    this.needsInteraction.next(false);
    this.profile.next(null);
    this.msal.logoutRedirect({ account, postLogoutRedirectUri: window.location.origin }).subscribe({
      error: () => this.updateUserState()
    });
  }

  acquireToken(): Observable<string | null> {
    return defer(() => {
      const account = this.getActiveAccount();
      if (!account) {
        return of(null);
      }
      const accountKey = this.accountKey(account);
      const cached = this.getStoredToken();
      if (cached) {
        return of(cached);
      }
      const pending = this.pending;
      if (pending?.accountKey === accountKey) {
        return pending.token$;
      }

      const token$ = this.msal.acquireTokenSilent({ account, scopes: [this.apiScope] }).pipe(
        map(result => {
          if (this.pending?.token$ !== token$ ||
              this.accountKey(this.getActiveAccount()) !== accountKey ||
              this.accountKey(result.account) !== accountKey) {
            return null;
          }
          this.rememberToken(result);
          this.updateUserState();
          return this.getStoredToken();
        }),
        catchError(error => {
          if (error instanceof InteractionRequiredAuthError &&
              this.pending?.token$ === token$ &&
              this.accountKey(this.getActiveAccount()) === accountKey) {
            this.needsInteraction.next(true);
          }
          return of(null);
        }),
        finalize(() => {
          if (this.pending?.token$ === token$) {
            this.pending = undefined;
          }
        }),
        shareReplay(1)
      );
      this.pending = { accountKey, token$ };
      return token$;
    });
  }

  getStoredToken(): string | null {
    const token = this.cachedToken;
    return token && token.accountKey === this.accountKey(this.getActiveAccount()) && this.isTokenValid(token.value)
      ? token.value : null;
  }

  getActiveAccount(): AccountInfo | null {
    const active = this.msal.instance.getActiveAccount();
    if (active) {
      return active;
    }
    const account = this.msal.instance.getAllAccounts()[0];
    if (account) {
      this.msal.instance.setActiveAccount(account);
    }
    return account ?? null;
  }

  private accountKey(account: AccountInfo | null): string {
    return account ? [account.homeAccountId, account.environment, account.tenantId].join(':') : '';
  }

  private rememberToken(result: AuthenticationResult): void {
    this.cachedToken = undefined;
    const accountKey = this.accountKey(result.account);
    if (accountKey && this.isTokenValid(result.accessToken)) {
      this.cachedToken = { accountKey, value: result.accessToken };
      this.needsInteraction.next(false);
    }
  }

  private isTokenValid(token: string): boolean {
    const claims = this.parseJwt(token);
    const audiences = Array.isArray(claims?.aud) ? claims.aud : [claims?.aud];
    return audiences.length > 0
      && audiences.every(aud => aud === this.apiResource || aud === this.apiResource.replace(/^api:\/\//, ''))
      && typeof claims?.exp === 'number' && claims.exp * 1000 > Date.now() + 30000
      && ((typeof claims.scp === 'string' && !!claims.scp.trim()) ||
          (Array.isArray(claims.roles) && claims.roles.length > 0));
  }

  private parseJwt(token: string): { aud?: string | string[]; exp?: number; scp?: string; roles?: string[] } | null {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    try {
      const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const bytes = Uint8Array.from(atob(payload), character => character.charCodeAt(0));
      return JSON.parse(new TextDecoder().decode(bytes));
    } catch {
      return null;
    }
  }

  private updateUserState(): void {
    const account = this.getActiveAccount();
    if (!account) {
      this.profile.next(null);
      return;
    }
    const token = this.getStoredToken();
    const claims = token ? this.parseJwt(token) : null;
    const roles = Array.isArray(claims?.roles) ? claims.roles : [];
    this.profile.next({
      name: account.name || account.username || 'Usuario',
      username: account.username,
      roles,
      scopes: typeof claims?.scp === 'string' ? claims.scp.split(/\s+/).filter(Boolean) : [],
      isAdmin: roles.some(role => ['Admin', 'ADMIN', 'ROLE_Admin', 'ROLE_ADMIN'].includes(role))
    });
  }
}
