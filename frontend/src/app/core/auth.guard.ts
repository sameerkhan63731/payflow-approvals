import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './models';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? true : inject(Router).createUrlTree(['/login']);
};

export function roleGuard(role: Role): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (!auth.isAuthenticated()) {
      return inject(Router).createUrlTree(['/login']);
    }
    return auth.hasRole(role) ? true : inject(Router).createUrlTree(['/payments']);
  };
}
