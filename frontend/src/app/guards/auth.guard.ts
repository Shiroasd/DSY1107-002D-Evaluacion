import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard de seguridad para proteger las rutas del inventario y categorías.
 * Si el usuario cuenta con una sesión activa en Microsoft Entra ID, permite el acceso.
 * Si no está autenticado, redirige automáticamente a la página de inicio (Landing)
 * evitando pantallas en blanco o bloqueos de navegación por redirects no autorizados en el navegador.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.getActiveAccount()) {
    return true;
  }

  const accounts = authService.getAllAccounts();
  if (accounts && accounts.length > 0) {
    authService.setActiveAccount(accounts[0]);
    authService.updateUserState();
    return true;
  }

  return router.createUrlTree(['/']);
};
