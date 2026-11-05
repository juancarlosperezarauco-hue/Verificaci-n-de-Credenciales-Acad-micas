import { Routes } from '@angular/router';
import { authGuard, studentGuard } from './core/auth.guard';

export const routes: Routes = [
  { path:'student/register', data:{registering:true}, loadComponent:()=>import('./features/student-account/student-access.page').then(m=>m.StudentAccessPage) },
  { path:'student/login', loadComponent:()=>import('./features/student-account/student-access.page').then(m=>m.StudentAccessPage) },
  { path:'student/account', canActivate:[studentGuard], loadComponent:()=>import('./features/student-account/student-account.page').then(m=>m.StudentAccountPage) },
  { path: '', pathMatch: 'full', redirectTo: 'verify' },
  { path: 'login', loadComponent: () => import('./features/login/login.page').then((m) => m.LoginPage) },
  { path: 'register', loadComponent: () => import('./features/register/register.page').then((m) => m.RegisterPage) },
  { path: 'verify', loadComponent: () => import('./features/verify/verify.page').then((m) => m.VerifyPage) },
  { path: 'verify/:code', loadComponent: () => import('./features/verify/verify.page').then((m) => m.VerifyPage) },
  { path: 'dashboard', canActivate: [authGuard], loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage) },
  { path: 'students', canActivate: [authGuard], loadComponent: () => import('./features/students/students.page').then((m) => m.StudentsPage) },
  { path: 'credentials', canActivate: [authGuard], loadComponent: () => import('./features/credentials/credentials.page').then((m) => m.CredentialsPage) },
  { path: 'institution', canActivate: [authGuard], loadComponent: () => import('./features/institution/institution.page').then((m) => m.InstitutionPage) },
  { path: '**', redirectTo: 'verify' },
];
