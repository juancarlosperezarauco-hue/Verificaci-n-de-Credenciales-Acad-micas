import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'verify' },
  { path: 'login', loadComponent: () => import('./features/login/login.page').then((m) => m.LoginPage) },
  { path: 'verify', loadComponent: () => import('./features/verify/verify.page').then((m) => m.VerifyPage) },
  { path: 'verify/:code', loadComponent: () => import('./features/verify/verify.page').then((m) => m.VerifyPage) },
  { path: 'dashboard', canActivate: [authGuard], loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage) },
  { path: 'credentials', canActivate: [authGuard], loadComponent: () => import('./features/credentials/credentials.page').then((m) => m.CredentialsPage) },
  { path: '**', redirectTo: 'verify' },
];
