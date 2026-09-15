import { Component, OnInit, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MsalService, MsalBroadcastService, MSAL_GUARD_CONFIG, MsalGuardConfiguration } from '@azure/msal-angular';
import { RedirectRequest, PopupRequest, InteractionType, InteractionStatus } from '@azure/msal-browser';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent implements OnInit {
  public authService = inject(AuthService);
  private msalAuthService = inject(MsalService);
  private msalBroadcastService = inject(MsalBroadcastService);
  private router = inject(Router);

  public isLoggingIn = false;

  constructor(
    @Inject(MSAL_GUARD_CONFIG) private msalGuardConfig: MsalGuardConfiguration
  ) {}

  ngOnInit(): void {
    // Si ya existe una cuenta activa verificada al cargar la vista, navegar a inventario
    if (this.authService.getActiveAccount()) {
      this.router.navigate(['/inventory']);
      return;
    }

    // Escuchar cambios reactivos de autenticación tras inicio de sesión o redirección
    this.authService.isAuthenticated$.subscribe((isAuth) => {
      if (isAuth) {
        this.router.navigate(['/inventory']);
      }
    });

    // Habilitar el botón únicamente cuando el estado sea InteractionStatus.None
    this.msalBroadcastService.inProgress$
      .subscribe((status: InteractionStatus) => {
        this.isLoggingIn = status !== InteractionStatus.None;
      });
  }

  onLogin(): void {
    this.login();
  }

  login(): void {
    if (this.isLoggingIn) return;

    this.isLoggingIn = true;
    console.log('Iniciando flujo de login...');
    try {
      // Limpiar posibles estados residuales de interacción bloqueada en almacenamiento local
      sessionStorage.removeItem('msal.interaction.status');

      const request = this.msalGuardConfig?.authRequest
        ? { ...this.msalGuardConfig.authRequest }
        : { scopes: ['openid', 'profile', 'email'] };

      if (this.msalGuardConfig?.interactionType === InteractionType.Popup) {
        this.msalAuthService.loginPopup({ ...request } as PopupRequest).subscribe({
          next: (res) => {
            console.log('[LandingComponent] Login popup exitoso:', res.account?.username);
            this.msalAuthService.instance.setActiveAccount(res.account);
            const token = res.idToken || res.accessToken;
            if (token) {
              this.authService.setToken(token);
            }
            this.authService.updateUserState();
            this.router.navigate(['/inventory']);
          },
          error: (err) => {
            console.error('[LandingComponent] Error durante loginPopup:', err);
            this.isLoggingIn = false;
          }
        });
      } else {
        this.msalAuthService.loginRedirect({ ...request } as RedirectRequest).subscribe({
          next: () => {
            console.log('[LandingComponent] Redirección hacia Microsoft Entra ID iniciada con éxito.');
          },
          error: (err) => {
            console.error('[LandingComponent] Error durante loginRedirect:', err);
            this.isLoggingIn = false;
          }
        });
      }
    } catch (error) {
      console.error('[LandingComponent] Excepción al ejecutar login:', error);
      this.isLoggingIn = false;
    }
  }
}
