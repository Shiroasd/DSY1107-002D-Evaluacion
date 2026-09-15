import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MsalService, MSAL_GUARD_CONFIG, MsalGuardConfiguration } from '@azure/msal-angular';
import { RedirectRequest, PopupRequest, InteractionType } from '@azure/msal-browser';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  public authService = inject(AuthService);
  private msalService = inject(MsalService);
  private router = inject(Router);

  constructor(
    @Inject(MSAL_GUARD_CONFIG) private msalGuardConfig: MsalGuardConfiguration
  ) {}

  onLogin(): void {
    this.login();
  }

  login(): void {
    console.log('Iniciando flujo de login...');
    try {
      sessionStorage.removeItem('msal.interaction.status');

      const request = this.msalGuardConfig?.authRequest
        ? { ...this.msalGuardConfig.authRequest }
        : { scopes: ['openid', 'profile', 'email'] };

      if (this.msalGuardConfig?.interactionType === InteractionType.Popup) {
        this.msalService.loginPopup({ ...request } as PopupRequest).subscribe({
          next: (res) => {
            console.log('[NavbarComponent] Login popup exitoso:', res.account?.username);
            this.msalService.instance.setActiveAccount(res.account);
            const token = res.idToken || res.accessToken;
            if (token) {
              this.authService.setToken(token);
            }
            this.authService.updateUserState();
            this.router.navigate(['/inventory']);
          },
          error: (err) => {
            console.error('[NavbarComponent] Error durante loginPopup:', err);
          }
        });
      } else {
        this.msalService.loginRedirect({ ...request } as RedirectRequest).subscribe({
          next: () => {
            console.log('[NavbarComponent] Redirección hacia Microsoft Entra ID iniciada con éxito.');
          },
          error: (err) => {
            console.error('[NavbarComponent] Error durante loginRedirect:', err);
          }
        });
      }
    } catch (error) {
      console.error('[NavbarComponent] Excepción al ejecutar login:', error);
    }
  }

  onLogout(): void {
    this.authService.logout();
  }
}
