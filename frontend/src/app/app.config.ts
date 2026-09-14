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

export function getRedirectUri(): string {
  if (typeof window !== 'undefined') {
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
      return 'http://localhost:4200';
    }
    return window.location.origin.endsWith('/') ? window.location.origin : `${window.location.origin}/`;
  }
  return environment.msalConfig.auth.redirectUri;
}

export function MSALInstanceFactory(): IPublicClientApplication {
  const redirectUri = getRedirectUri();
  return new PublicClientApplication({
    auth: {
      clientId: environment.msalConfig.auth.clientId,
      authority: environment.msalConfig.auth.authority,
      redirectUri: redirectUri,
      postLogoutRedirectUri: redirectUri,
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
  protectedResourceMap.set('http://32.192.168.114:8080/api/v1/*', environment.apiConfig.protectedResourceScopes);
  protectedResourceMap.set('http://localhost:8080/api/v1/*', environment.apiConfig.protectedResourceScopes);

  return {
    interactionType: InteractionType.Popup,
    protectedResourceMap: environment.apiConfig.protectedResourceMap || protectedResourceMap
  };
}

/**
 * Interceptor HTTP para Microsoft Entra ID (MSAL):
 * Inyecta automáticamente el Bearer Token en cada solicitud al Resource Server (Spring Boot).
 * Prioriza el token JWT oficial de la sesión activa autenticada (ID Token validado por Spring Boot).
 * Esto previene de raíz cualquier bucle de recarga (redirect loop), bloqueos por CORB en iframes
 * ocultos de Me.htm?v=3 y peticiones no autorizadas.
 */
@Injectable()
export class MsalInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const isApiRequest = req.url.includes('/api/v1') ||
                         req.url.startsWith(environment.apiConfig.baseUrl) ||
                         req.url.includes('32.192.168.114') ||
                         req.url.includes('localhost:8080');
    if (!isApiRequest) {
      return next.handle(req);
    }

    if (req.headers.has('Authorization')) {
      return next.handle(req);
    }

    // 1. Obtener el token JWT oficial autenticado de la sesión activa
    const storedToken = this.authService.getStoredToken();
    if (storedToken) {
      const cloned = req.clone({
        setHeaders: {
          Authorization: `Bearer ${storedToken}`
        }
      });
      return next.handle(cloned);
    }

    // Si aún no se resuelve el token pero hay cuenta activa, continuar la petición
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
