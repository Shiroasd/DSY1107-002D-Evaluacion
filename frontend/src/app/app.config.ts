import { ApplicationConfig, APP_INITIALIZER, Injectable, inject, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse, HttpInterceptor } from '@angular/common/http';

registerLocaleData(localeEsCl, 'es-CL');
import {
  IPublicClientApplication,
  PublicClientApplication,
  BrowserCacheLocation,
  LogLevel
} from '@azure/msal-browser';
import {
  MSAL_INSTANCE,
  MsalService,
  MsalBroadcastService
} from '@azure/msal-angular';
import { switchMap } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { AuthService } from './services/auth.service';

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

@Injectable()
export class MsalInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const api = new URL(environment.apiConfig.baseUrl, window.location.origin);
    const target = new URL(req.url, window.location.origin);
    const path = api.pathname.replace(/\/$/, '');
    if (target.origin !== api.origin ||
        (target.pathname !== path && !target.pathname.startsWith(path + '/'))) {
      return next.handle(req);
    }

    return this.authService.acquireToken().pipe(
      switchMap(token => token
        ? next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }))
        : throwError(() => new HttpErrorResponse({
            status: 401, statusText: 'Se requiere una sesión de CloudStock', url: req.url
          })))
    );
  }
}

export function MSALInitializerFactory(authService: AuthService) {
  return () => authService.handleRedirectObservable();
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
      provide: APP_INITIALIZER,
      useFactory: MSALInitializerFactory,
      deps: [AuthService],
      multi: true
    },
    MsalService,
    MsalBroadcastService,
    {
      provide: LOCALE_ID,
      useValue: 'es-CL'
    }
  ]
};
