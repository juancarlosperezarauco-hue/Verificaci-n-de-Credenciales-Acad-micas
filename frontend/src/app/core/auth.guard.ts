import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router=inject(Router);
  if(!auth.isAuthenticated()) return router.createUrlTree(['/login']);
  return auth.current()?.role !== 'STUDENT' || router.createUrlTree(['/student/account']);
};

export const studentGuard: CanActivateFn = () => {
  const auth=inject(AuthService); const router=inject(Router);
  if(!auth.isAuthenticated())return router.createUrlTree(['/student/login']);
  return auth.current()?.role === 'STUDENT' || router.createUrlTree(['/dashboard']);
};
