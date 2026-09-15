import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { AuthenticationResult } from '@azure/msal-browser';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AlertComponent } from './components/alert/alert.component';
import { AuthService } from './services/auth.service';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, AlertComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  public authService = inject(AuthService);
  private msalService = inject(MsalService);
  private router = inject(Router);
  public apiBaseUrl = environment.apiConfig.baseUrl;

  ngOnInit(): void {
    console.log('[AppComponent] Inicializando y escuchando handleRedirectObservable...');
    this.authService.handleRedirectObservable().subscribe({
      next: (result: AuthenticationResult | null) => {
        if (result) {
          console.log('[AppComponent] Autenticación completada exitosamente vía redirección:', result.account?.username);
          this.msalService.instance.setActiveAccount(result.account);
          const token = result.idToken || result.accessToken;
          if (token) {
            this.authService.setToken(token);
          }
          this.authService.updateUserState();
          this.router.navigate(['/inventory']);
        } else {
          // Verificar si ya existe sesión activa previamente establecida en el caché del navegador
          const accounts = this.msalService.instance.getAllAccounts();
          if (accounts && accounts.length > 0) {
            const active = this.msalService.instance.getActiveAccount() || accounts[0];
            this.msalService.instance.setActiveAccount(active);
            this.authService.updateUserState();

            const path = window.location.pathname;
            if (path === '/' || path === '' || path === '/index.html') {
              this.router.navigate(['/inventory']);
            }
          }
        }
      },
      error: (error) => {
        console.error('[AppComponent] Error en handleRedirectObservable:', error);
      }
    });
  }
}
