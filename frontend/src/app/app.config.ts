import { ApplicationConfig, APP_INITIALIZER, Injectable, inject, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';

registerLocaleData(localeEsCl, 'es-CL');
import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType,
  BrowserCacheLocation,
  LogLevel,
  AuthenticationResult
} from '@azure/msal-browser';
import {
  MsalInterceptorConfiguration,
  MsalGuardConfiguration,
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MsalService,
  MsalGuard,
  MsalBroadcastService
} from '@azure/msal-angular';
import { concatMap, tap, catchError, switchMap } from 'rxjs/operators';
import { of, Observable } from 'rxjs';
import { routes } from './app.routes';
import { environment } from '../environments/environment';

export function loggerCallback(logLevel: LogLevel, message: string) {
  if (logLevel <= LogLevel.Warning) {
    console.warn('[MSAL]', message);
  }
}

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.msalConfig.auth.clientId,
      authority: environment.msalConfig.auth.authority,
      redirectUri: environment.msalConfig.auth.redirectUri,
      postLogoutRedirectUri: environment.msalConfig.auth.redirectUri,
      navigateToLoginRequestUrl: false
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage,
      storeAuthStateInCookie: true
    },
    system: {
      loggerOptions: {
        loggerCallback,
        logLevel: LogLevel.Warning,
        piiLoggingEnabled: false
      }
    }
  });
}

export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: ['openid', 'profile', 'email']
    },
    loginFailedRoute: '/'
  };
}

import { AuthService } from './services/auth.service';

export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>();
  // Mapeo explícito de la IP del backend en AWS con los scopes protegidos de Azure
  protectedResourceMap.set('https://32.192.168.114/*', environment.apiConfig.protectedResourceScopes);
  protectedResourceMap.set('https://32.192.168.114/api/v1/*', environment.apiConfig.protectedResourceScopes);

  return {
    interactionType: InteractionType.Popup,
    protectedResourceMap: environment.apiConfig.protectedResourceMap || protectedResourceMap
  };
}

/**
 * Interceptor HTTP para Microsoft Entra ID (MSAL):
 * Inyecta automáticamente el Bearer Token en cada solicitud al Resource Server (https://32.192.168.114/*).
 * Intenta primero la adquisición silenciosa con MSAL para los scopes protegidos registrados en Azure.
 * Si acquireTokenSilent no puede obtener el token debido a restricciones de consentimiento en el tenant
 * de Azure AD, previene de forma segura cualquier bucle de recarga (redirect loop) y utiliza como
 * fallback el token JWT oficial de la sesión activa autenticada (ID Token validado por Spring Boot).
 */
@Injectable()
export class MsalInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);
  private msalService = inject(MsalService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const isApiRequest = req.url.includes('/api/v1') || req.url.startsWith(environment.apiConfig.baseUrl) || req.url.includes('32.192.168.114');
    if (!isApiRequest) {
      return next.handle(req);
    }

    if (req.headers.has('Authorization')) {
      return next.handle(req);
    }

    const account = this.authService.getActiveAccount();
    const scopes = environment.apiConfig.protectedResourceScopes;

    if (account && scopes && scopes.length > 0) {
      return this.msalService.acquireTokenSilent({
        account: account,
        scopes: scopes
      }).pipe(
        switchMap((result: AuthenticationResult) => {
          const token = result.accessToken || result.idToken;
          console.info('[MsalInterceptor] Bearer Token adjuntado vía MSAL acquireTokenSilent a:', req.url);
          const cloned = req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          });
          return next.handle(cloned);
        }),
        catchError((err) => {
          console.warn('[MsalInterceptor] acquireTokenSilent no pudo adquirir token para scope delegado, aplicando token de sesión activa:', err?.message || err);
          const fallbackToken = this.authService.getStoredToken();
          if (fallbackToken) {
            console.info('[MsalInterceptor] Adjuntando Bearer Token de sesión activa a:', req.url);
            const cloned = req.clone({
              setHeaders: {
                Authorization: `Bearer ${fallbackToken}`
              }
            });
            return next.handle(cloned);
          }
          return next.handle(req);
        })
      );
    }

    const fallbackToken = this.authService.getStoredToken();
    if (fallbackToken) {
      console.info('[MsalInterceptor] Adjuntando Bearer Token almacenado a:', req.url);
      const cloned = req.clone({
        setHeaders: {
          Authorization: `Bearer ${fallbackToken}`
        }
      });
      return next.handle(cloned);
    }

    return next.handle(req);
  }
}

/**
 * Inicializador de MSAL para Angular Standalone:
 * Ejecuta initialize() y procesa handleRedirectObservable() ANTES de que el Router
 * de Angular inicie la navegación y limpie el hash de autenticación (#code=...) de la URL.
 */
export function MSALInitializerFactory(msalService: MsalService, authService: AuthService) {
  return () => {
    const responseString = typeof window !== 'undefined'
      ? (window.location.hash || window.location.search || undefined)
      : undefined;
    return msalService.initialize().pipe(
      concatMap(() => msalService.handleRedirectObservable(responseString)),
      tap((result) => {
        if (result) {
          console.info('[MSALInitializer] Autenticación completada exitosamente:', result.account?.username);
          const token = result.idToken || result.accessToken;
          if (token) {
            authService.setToken(token);
          }
          if (result.account) {
            msalService.instance.setActiveAccount(result.account);
          }
        } else {
          const accounts = msalService.instance.getAllAccounts();
          if (accounts && accounts.length > 0) {
            msalService.instance.setActiveAccount(accounts[0]);
          }
        }
        authService.updateUserState();
      }),
      catchError((err) => {
        console.error('[MSALInitializer] Error o nota en procesamiento de redirección:', err);
        return of(null);
      })
    );
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true
    },
    {
      provide: MSAL_INSTANCE,
      useFactory: MSALInstanceFactory
    },
    {
      provide: MSAL_GUARD_CONFIG,
      useFactory: MSALGuardConfigFactory
    },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: MSALInterceptorConfigFactory
    },
    {
      provide: APP_INITIALIZER,
      useFactory: MSALInitializerFactory,
      deps: [MsalService, AuthService],
      multi: true
    },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
    {
      provide: LOCALE_ID,
      useValue: 'es-CL'
    }
  ]
};
