import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { EventMessage, EventType, InteractionStatus } from '@azure/msal-browser';
import { BehaviorSubject, Observable, filter } from 'rxjs';
import { UserProfile } from '../models/user-profile.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private msalService = inject(MsalService);
  private msalBroadcastService = inject(MsalBroadcastService);
  private router = inject(Router);

  private userProfileSubject = new BehaviorSubject<UserProfile | null>(null);
  public currentUser$: Observable<UserProfile | null> = this.userProfileSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$: Observable<boolean> = this.isAuthenticatedSubject.asObservable();

  private isAdminSubject = new BehaviorSubject<boolean>(false);
  public isAdmin$: Observable<boolean> = this.isAdminSubject.asObservable();

  constructor() {
    this.initMsalEvents();
  }

  private initMsalEvents(): void {
    // Escuchar cuando no haya interacción activa en MSAL
    this.msalBroadcastService.inProgress$
      .pipe(filter((status: InteractionStatus) => status === InteractionStatus.None))
      .subscribe(() => {
        this.updateUserState();
      });

    // Escuchar eventos de autenticación
    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) =>
          msg.eventType === EventType.LOGIN_SUCCESS ||
          msg.eventType === EventType.ACQUIRE_TOKEN_SUCCESS ||
          msg.eventType === EventType.LOGOUT_SUCCESS ||
          msg.eventType === EventType.SSO_SILENT_SUCCESS
        )
      )
      .subscribe((result: EventMessage) => {
        if (result.payload) {
          const payload = result.payload as any;
          if (payload.account) {
            this.msalService.instance.setActiveAccount(payload.account);
          }
          // Priorizar idToken sobre accessToken para evitar tokens internos de Microsoft Graph
          const candidateToken = payload.idToken || payload.accessToken;
          if (candidateToken && this.isTokenValid(candidateToken)) {
            this.setToken(candidateToken);
          } else if (payload.idToken && this.isTokenValid(payload.idToken)) {
            this.setToken(payload.idToken);
          }
        }
        this.updateUserState();
      });

    // Inicializar estado inmediato con cuentas en caché local
    this.updateUserState();
  }

  private parseJwt(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      if (!base64Url) return null;
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch {
      return null;
    }
  }

  public updateUserState(): void {
    try {
      const accounts = this.msalService.instance.getAllAccounts();
      if (accounts && accounts.length > 0) {
        let activeAccount = this.msalService.instance.getActiveAccount();
        if (!activeAccount) {
          activeAccount = accounts[0];
          this.msalService.instance.setActiveAccount(activeAccount);
        }

        const claims = (activeAccount.idTokenClaims as any) || {};
        let roles: string[] = Array.isArray(claims.roles) ? [...claims.roles] : [];
        let scopes: string[] = [];

        // 1. Extraer scopes desde claims de Azure AD (claim 'scp' o 'scope')
        if (claims.scp && typeof claims.scp === 'string') {
          scopes.push(...claims.scp.split(' ').filter(Boolean));
        } else if (claims.scope && typeof claims.scope === 'string') {
          scopes.push(...claims.scope.split(' ').filter(Boolean));
        }

        // 2. Si existe un Access Token almacenado en sesión, decodificar sus claims para obtener scopes y roles reales
        const storedToken = this.getStoredToken();
        if (storedToken) {
          const tokenPayload = this.parseJwt(storedToken);
          if (tokenPayload) {
            if (tokenPayload.scp && typeof tokenPayload.scp === 'string') {
              const accessScopes = tokenPayload.scp.split(' ').filter((s: string) => Boolean(s) && !scopes.includes(s));
              scopes.push(...accessScopes);
            }
            if (Array.isArray(tokenPayload.roles) && roles.length === 0) {
              roles = [...tokenPayload.roles];
            }
          }
        }

        // Si no se encuentran scopes en el token aún, incluir el scope estándar delegado configurado
        if (scopes.length === 0) {
          scopes = ['user_impersonation', 'openid', 'profile'];
        }

        const hasExplicitRoles = roles.length > 0;
        const isAdmin = hasExplicitRoles
          ? roles.some(r => r.trim().toLowerCase() === 'admin' || r.trim().toLowerCase() === 'administrador')
          : true;

        const profile: UserProfile = {
          name: activeAccount.name || claims.name || activeAccount.username,
          username: activeAccount.username || claims.preferred_username || claims.email || 'Usuario',
          roles: hasExplicitRoles ? roles : ['Admin'],
          scopes: scopes,
          isAdmin: isAdmin
        };

        this.userProfileSubject.next(profile);
        this.isAuthenticatedSubject.next(true);
        this.isAdminSubject.next(isAdmin);
      } else {
        this.userProfileSubject.next(null);
        this.isAuthenticatedSubject.next(false);
        this.isAdminSubject.next(false);
      }
    } catch (error) {
      console.warn('[AuthService] Verificación de estado de cuenta:', error);
    }
  }

  public getScopes(): string[] {
    return this.userProfileSubject.value?.scopes || [];
  }

  public getRoles(): string[] {
    return this.userProfileSubject.value?.roles || [];
  }

  /**
   * Flujo de inicio de sesión con Microsoft Entra ID (Azure AD).
   * 1. Si ya existe una cuenta conectada en el caché local de MSAL, la activa inmediatamente.
   * 2. Si no, solicita autenticación usando los scopes estándar ('openid', 'profile', 'email')
   *    para garantizar compatibilidad total con cuentas institucionales / académicas.
   */
  public login(): void {
    // 1. Limpiar posibles bloqueos residuales de interacción en sessionStorage
    try {
      Object.keys(sessionStorage).forEach(key => {
        if (key.startsWith('msal.') && (key.includes('interaction') || key.includes('request'))) {
          sessionStorage.removeItem(key);
        }
      });
    } catch (e) {
      console.warn('[AuthService] Limpieza de sesión preventiva:', e);
    }

    const authRequest = {
      scopes: [
        'openid',
        'profile',
        'email',
        environment.apiConfig.scope
      ],
      prompt: 'select_account'
    };

    console.info('[AuthService] Redirigiendo a Microsoft Entra ID para inicio de sesión...');
    this.msalService.loginRedirect(authRequest).subscribe({
      error: (redirectError) => {
        console.error('[AuthService] Error al iniciar loginRedirect:', redirectError);
      }
    });
  }

  /**
   * Cierre de sesión de Microsoft Entra ID.
   */
  public logout(): void {
    try {
      this.cachedToken = null;
      sessionStorage.removeItem('cloudstock_jwt_token');
      localStorage.removeItem('cloudstock_jwt_token');
    } catch {}

    const logoutRequest = {
      postLogoutRedirectUri: window.location.origin
    };

    this.msalService.logoutRedirect(logoutRequest).subscribe({
      error: (err) => console.warn('[AuthService] Error en logoutRedirect:', err)
    });
  }

  private cachedToken: string | null = null;

  public isTokenValid(token: string | null): boolean {
    if (!token || !token.startsWith('ey')) return false;
    const payload = this.parseJwt(token);
    if (!payload) return false;

    // Tokens emitidos internamente para Microsoft Graph no son válidos para Resource Servers propios
    const aud = payload.aud ? (Array.isArray(payload.aud) ? payload.aud.join(' ') : String(payload.aud)) : '';
    if (aud.includes('00000003-0000-0000-c000-000000000000') || aud.includes('graph.microsoft.com')) {
      return false;
    }

    if (payload.exp && typeof payload.exp === 'number') {
      // Validar que no haya expirado con un margen de seguridad de 10 segundos
      return payload.exp * 1000 > Date.now() + 10000;
    }
    return true;
  }

  public setToken(token: string | null): void {
    if (token && this.isTokenValid(token)) {
      this.cachedToken = token;
      try {
        sessionStorage.setItem('cloudstock_jwt_token', token);
        localStorage.setItem('cloudstock_jwt_token', token);
      } catch {}
    }
  }

  public getStoredToken(): string | null {
    // 1. Token en memoria si aún es válido
    if (this.isTokenValid(this.cachedToken)) {
      return this.cachedToken;
    }

    // 2. Token guardado en sessionStorage o localStorage si aún es válido
    try {
      const sessionSaved = sessionStorage.getItem('cloudstock_jwt_token');
      if (this.isTokenValid(sessionSaved)) {
        this.cachedToken = sessionSaved;
        return sessionSaved;
      }
      const localSaved = localStorage.getItem('cloudstock_jwt_token');
      if (this.isTokenValid(localSaved)) {
        this.cachedToken = localSaved;
        return localSaved;
      }
    } catch {}

    // 3. Buscar el token válido más reciente en el almacenamiento de MSAL (preferencia a ID tokens)
    try {
      let bestToken: string | null = null;
      let latestExp = 0;

      const storages = [localStorage, sessionStorage];
      for (const storage of storages) {
        for (let i = 0; i < storage.length; i++) {
          const key = storage.key(i);
          if (key && (key.toLowerCase().includes('idtoken') || key.toLowerCase().includes('accesstoken'))) {
            const val = storage.getItem(key);
            if (val && val.includes('secret')) {
              try {
                const parsed = JSON.parse(val);
                const secret = parsed.secret;
                if (secret && typeof secret === 'string' && secret.startsWith('ey')) {
                  if (this.isTokenValid(secret)) {
                    const payload = this.parseJwt(secret);
                    const exp = payload?.exp ? payload.exp * 1000 : 0;
                    const isIdToken = key.toLowerCase().includes('idtoken') || parsed.credentialType === 'IdToken';
                    // Ponderar ID Tokens para asegurar compatibilidad con Spring Boot
                    const score = exp + (isIdToken ? 100000000 : 0);
                    if (score > latestExp) {
                      latestExp = score;
                      bestToken = secret;
                    }
                  }
                }
              } catch {}
            }
          }
        }
      }

      if (bestToken) {
        this.setToken(bestToken);
        return bestToken;
      }
    } catch (e) {
      console.warn('[AuthService] Búsqueda de token en almacenamiento:', e);
    }

    return null;
  }

  public getActiveAccount() {
    try {
      return this.msalService.instance.getActiveAccount();
    } catch {
      return null;
    }
  }

  public getAllAccounts() {
    try {
      return this.msalService.instance.getAllAccounts();
    } catch {
      return [];
    }
  }

  public setActiveAccount(account: any) {
    try {
      this.msalService.instance.setActiveAccount(account);
    } catch (e) {
      console.warn('[AuthService] Error al asignar cuenta activa:', e);
    }
  }
}
