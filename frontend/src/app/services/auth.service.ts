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
        if (result.payload && (result.payload as any).account) {
          this.msalService.instance.setActiveAccount((result.payload as any).account);
        }
        this.updateUserState();
      });

    // Inicializar estado inmediato con cuentas en caché local
    this.updateUserState();
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

        const claims = activeAccount.idTokenClaims as any;
        const roles: string[] = claims?.roles || [];
        
        // Si la cuenta académica autenticada no tiene roles explícitos asignados en Azure Portal,
        // como creador y dueño del sistema se le otorgan automáticamente privilegios de Administrador.
        const hasExplicitRoles = roles.length > 0;
        const isAdmin = !hasExplicitRoles || roles.some(r => r.trim().toLowerCase() === 'admin' || r.trim().toLowerCase() === 'administrador');

        const profile: UserProfile = {
          name: activeAccount.name || claims?.name || activeAccount.username,
          username: activeAccount.username || claims?.preferred_username || claims?.email || 'Usuario',
          roles: hasExplicitRoles ? roles : ['Admin (Dueño del Sistema)'],
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
      scopes: ['openid', 'profile', 'email'],
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
    const logoutRequest = {
      postLogoutRedirectUri: window.location.origin
    };

    this.msalService.logoutRedirect(logoutRequest).subscribe({
      error: (err) => console.warn('[AuthService] Error en logoutRedirect:', err)
    });
  }

  private cachedToken: string | null = null;

  public setToken(token: string | null): void {
    if (token) {
      this.cachedToken = token;
      try {
        sessionStorage.setItem('cloudstock_jwt_token', token);
      } catch {}
    }
  }

  public getStoredToken(): string | null {
    // 1. Token en memoria
    if (this.cachedToken && this.cachedToken.startsWith('ey')) {
      return this.cachedToken;
    }

    // 2. Token guardado en sessionStorage
    try {
      const saved = sessionStorage.getItem('cloudstock_jwt_token');
      if (saved && saved.startsWith('ey')) {
        this.cachedToken = saved;
        return saved;
      }
    } catch {}

    // 3. Buscar en el almacenamiento de MSAL (sessionStorage y localStorage)
    try {
      const storages = [sessionStorage, localStorage];
      for (const storage of storages) {
        for (let i = 0; i < storage.length; i++) {
          const key = storage.key(i);
          if (key && (key.includes('idtoken') || key.includes('accesstoken') || key.includes('token'))) {
            const val = storage.getItem(key);
            if (val && val.includes('secret')) {
              try {
                const parsed = JSON.parse(val);
                if (parsed.secret && typeof parsed.secret === 'string' && parsed.secret.startsWith('ey')) {
                  this.cachedToken = parsed.secret;
                  try {
                    sessionStorage.setItem('cloudstock_jwt_token', parsed.secret);
                  } catch {}
                  return parsed.secret;
                }
              } catch {}
            }
          }
        }
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
