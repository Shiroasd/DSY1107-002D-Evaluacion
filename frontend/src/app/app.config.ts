import { ApplicationConfig, APP_INITIALIZER, Injectable, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType,
  BrowserCacheLocation,
  LogLevel
} from '@azure/msal-browser';
import {
  MsalInterceptor,
  MsalInterceptorConfiguration,
  MsalGuardConfiguration,
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MsalService,
  MsalGuard,
  MsalBroadcastService
} from '@azure/msal-angular';
import { concatMap, tap, catchError } from 'rxjs/operators';
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
      navigateToLoginRequestUrl: true
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
  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap: new Map([
      ['http://localhost:8080/api/v1', ['openid', 'profile', 'email']]
    ])
  };
}

/**
 * Interceptor HTTP que inyecta automáticamente el token Bearer oficial de Microsoft Entra ID
 * en cada solicitud realizada hacia el Resource Server (Spring Boot / API Gateway).
 */
@Injectable()
export class TokenHeaderInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.url.includes('localhost:8080/api/v1')) {
      const token = this.authService.getStoredToken();
      if (token && !req.headers.has('Authorization')) {
        req = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
      }
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
    const currentHash = typeof window !== 'undefined' ? window.location.hash : '';
    return msalService.initialize().pipe(
      concatMap(() => msalService.handleRedirectObservable(currentHash || undefined)),
      tap((result) => {
        if (result) {
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
      }),
      catchError((err) => {
        console.warn('[MSALInitializer] Nota en procesamiento de redirección:', err);
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
      useClass: TokenHeaderInterceptor,
      multi: true
    },
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
    MsalBroadcastService
  ]
};
