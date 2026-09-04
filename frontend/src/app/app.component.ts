import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AlertComponent } from './components/alert/alert.component';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, AlertComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private msalService = inject(MsalService);
  public authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit(): void {
    // Procesar respuesta de redirección tras autenticación con Microsoft Entra ID
    this.msalService.handleRedirectObservable().subscribe({
      next: (result) => {
        if (result) {
          const token = result.idToken || result.accessToken;
          if (token) {
            this.authService.setToken(token);
          }
          if (result.account) {
            this.msalService.instance.setActiveAccount(result.account);
          }
        }
        this.authService.updateUserState();
        if (this.authService.getActiveAccount() && (window.location.pathname === '/' || window.location.pathname === '')) {
          this.router.navigate(['/inventory']);
        }
      },
      error: (err) => {
        console.warn('[AppComponent] handleRedirectObservable completado:', err);
        this.authService.updateUserState();
        if (this.authService.getActiveAccount() && (window.location.pathname === '/' || window.location.pathname === '')) {
          this.router.navigate(['/inventory']);
        }
      }
    });

    // Verificación inmediata al arranque
    this.authService.updateUserState();
    if (this.authService.getActiveAccount() && (window.location.pathname === '/' || window.location.pathname === '')) {
      this.router.navigate(['/inventory']);
    }
  }
}
