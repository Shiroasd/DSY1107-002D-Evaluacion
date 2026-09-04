import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
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
  private router = inject(Router);

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
    this.authService.login();
  }
}
