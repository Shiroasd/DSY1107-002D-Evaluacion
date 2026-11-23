import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent {
  public authService = inject(AuthService);

  get isLoggingIn(): boolean {
    return this.authService.isInteractionInProgress;
  }

  login(): void {
    this.authService.login();
  }
}
