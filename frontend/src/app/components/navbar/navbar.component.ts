import { Component, OnInit, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MsalService, MsalBroadcastService, MSAL_GUARD_CONFIG, MsalGuardConfiguration } from '@azure/msal-angular';
import { RedirectRequest, PopupRequest, InteractionType, InteractionStatus } from '@azure/msal-browser';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  public authService = inject(AuthService);
  private msalAuthService = inject(MsalService);
  private msalBroadcastService = inject(MsalBroadcastService);
  private router = inject(Router);

  public isLoggingIn = false;

  constructor(
    @Inject(MSAL_GUARD_CONFIG) private msalGuardConfig: MsalGuardConfiguration
  ) {}

  ngOnInit(): void {
    // Desbloquear cuando finalice cualquier interacción previa
    this.msalBroadcastService.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None)
      )
      .subscribe(() => {
        this.isLoggingIn = false;
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
      sessionStorage.removeItem('msal.interaction.status');

      const request = this.msalGuardConfig?.authRequest
        ? { ...this.msalGuardConfig.authRequest }
        : { scopes: ['openid', 'profile', 'email'] };

      if (this.msalGuardConfig?.interactionType === InteractionType.Popup) {
        this.msalAuthService.loginPopup({ ...request } as PopupRequest).subscribe({
          next: (res) => {
            console.log('[NavbarComponent] Login popup exitoso:', res.account?.username);
            this.msalAuthService.instance.setActiveAccount(res.account);
            const token = res.idToken || res.accessToken;
            if (token) {
              this.authService.setToken(token);
            }
            this.authService.updateUserState();
            this.router.navigate(['/inventory']);
          },
          error: (err) => {
            console.error('[NavbarComponent] Error durante loginPopup:', err);
            this.isLoggingIn = false;
          }
        });
      } else {
        this.msalAuthService.loginRedirect({ ...request } as RedirectRequest).subscribe({
          next: () => {
            console.log('[NavbarComponent] Redirección hacia Microsoft Entra ID iniciada con éxito.');
          },
          error: (err) => {
            console.error('[NavbarComponent] Error durante loginRedirect:', err);
            this.isLoggingIn = false;
          }
        });
      }
    } catch (error) {
      console.error('[NavbarComponent] Excepción al ejecutar login:', error);
      this.isLoggingIn = false;
    }
  }

  onLogout(): void {
    this.authService.logout();
  }
}
