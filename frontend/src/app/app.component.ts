import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
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
  private router = inject(Router);
  public apiBaseUrl = environment.apiConfig.baseUrl;

  ngOnInit(): void {
    this.authService.handleRedirectObservable().subscribe(result => {
      if (result || (this.authService.getActiveAccount() && window.location.pathname === '/')) {
        this.router.navigate(['/inventory']);
      }
    });
  }
}
