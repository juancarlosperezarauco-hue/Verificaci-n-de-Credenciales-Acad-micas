import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { OrganizationApi } from '../features/organization/organization.api';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router=inject(Router);
  if(!auth.isAuthenticated()) return router.createUrlTree(['/login']);
  if(auth.current()?.role === 'SUPER_ADMIN') return router.createUrlTree(['/superadmin/organizations']);
  return auth.current()?.role !== 'STUDENT' || router.createUrlTree(['/student/account']);
};

export const permissionGuard: CanActivateFn = route => {
  const auth=inject(AuthService);const api=inject(OrganizationApi);const router=inject(Router);
  const login=route.data['roles']?.includes('SUPER_ADMIN')?'/superadmin/login':'/login';
  if(!auth.isAuthenticated())return router.createUrlTree([login]);
  return api.me().pipe(map(p=>{
    if(p.onboarding && !route.data['allowOnboarding'])return router.createUrlTree(['/organization']);
    const permission=route.data['permission'] as string | undefined;
    const roles=route.data['roles'] as string[] | undefined;
    return (!permission||p.permissions.includes(permission))&&(!roles||roles.includes(p.role))
      ? true : router.createUrlTree(['/access-denied']);
  }),catchError(()=>{auth.logout();api.access.set(null);return of(router.createUrlTree([login]));}));
};

export const studentGuard: CanActivateFn = () => {
  const auth=inject(AuthService); const router=inject(Router);
  if(!auth.isAuthenticated())return router.createUrlTree(['/student/login']);
  return auth.current()?.role === 'STUDENT' || router.createUrlTree(['/dashboard']);
};
