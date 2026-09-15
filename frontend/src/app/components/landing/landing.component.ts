import { Component, OnInit, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MsalService, MSAL_GUARD_CONFIG, MsalGuardConfiguration } from '@azure/msal-angular';
import { RedirectRequest, PopupRequest, InteractionType } from '@azure/msal-browser';
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
  private msalService = inject(MsalService);
  private router = inject(Router);

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
  }

  onLogin(): void {
    this.login();
  }

  login(): void {
    console.log('Iniciando flujo de login...');
    try {
      // Limpiar posibles estados residuales de interacción bloqueada en almacenamiento local
      sessionStorage.removeItem('msal.interaction.status');

      const request = this.msalGuardConfig?.authRequest
        ? { ...this.msalGuardConfig.authRequest }
        : { scopes: ['openid', 'profile', 'email'] };

      if (this.msalGuardConfig?.interactionType === InteractionType.Popup) {
        this.msalService.loginPopup({ ...request } as PopupRequest).subscribe({
          next: (res) => {
            console.log('[LandingComponent] Login popup exitoso:', res.account?.username);
            this.msalService.instance.setActiveAccount(res.account);
            const token = res.idToken || res.accessToken;
            if (token) {
              this.authService.setToken(token);
            }
            this.authService.updateUserState();
            this.router.navigate(['/inventory']);
          },
          error: (err) => {
            console.error('[LandingComponent] Error durante loginPopup:', err);
          }
        });
      } else {
        this.msalService.loginRedirect({ ...request } as RedirectRequest).subscribe({
          next: () => {
            console.log('[LandingComponent] Redirección hacia Microsoft Entra ID iniciada con éxito.');
          },
          error: (err) => {
            console.error('[LandingComponent] Error durante loginRedirect:', err);
          }
        });
      }
    } catch (error) {
      console.error('[LandingComponent] Excepción al ejecutar login:', error);
    }
  }
}
