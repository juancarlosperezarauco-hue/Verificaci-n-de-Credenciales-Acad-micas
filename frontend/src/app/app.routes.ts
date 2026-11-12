import { Routes } from '@angular/router';
import { studentGuard, permissionGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./features/landing/landing.page').then((m) => m.LandingPage) },
  { path:'student/register', data:{registering:true}, loadComponent:()=>import('./features/student-account/student-access.page').then(m=>m.StudentAccessPage) },
  { path:'student/login', loadComponent:()=>import('./features/student-account/student-access.page').then(m=>m.StudentAccessPage) },
  { path:'student/account', canActivate:[studentGuard], loadComponent:()=>import('./features/student-account/student-account.page').then(m=>m.StudentAccountPage) },
  { path: 'login', loadComponent: () => import('./features/organization/access-login.page').then(m=>m.AccessLoginPage) },
  { path: 'superadmin/login', data:{platform:true}, loadComponent: () => import('./features/organization/access-login.page').then(m=>m.AccessLoginPage) },
  { path: 'register', loadComponent: () => import('./features/organization/organization-register.page').then(m=>m.OrganizationRegisterPage) },
  { path: 'org/register', redirectTo:'register', pathMatch:'full' },
  { path: 'org/admin', redirectTo:'organization', pathMatch:'full' },
  { path: 'organization', canActivate:[permissionGuard], data:{permission:'ORGANIZATION_VIEW'}, loadComponent:()=>import('./features/organization/organization-profile.page').then(m=>m.OrganizationProfilePage) },
  { path: 'organization/team', canActivate:[permissionGuard], data:{roles:['ADMIN','TENANT_ADMIN']}, loadComponent:()=>import('./features/organization/organization-team.page').then(m=>m.OrganizationTeamPage) },
  { path: 'superadmin/organizations', canActivate:[permissionGuard], data:{roles:['SUPER_ADMIN']}, loadComponent:()=>import('./features/organization/platform-organizations.page').then(m=>m.PlatformOrganizationsPage) },
  { path: 'superadmin/organizations/:id', canActivate:[permissionGuard], data:{roles:['SUPER_ADMIN']}, loadComponent:()=>import('./features/organization/platform-detail.page').then(m=>m.PlatformDetailPage) },
  { path: 'access-denied', loadComponent:()=>import('./features/organization/access-denied.page').then(m=>m.AccessDeniedPage) },
  { path: 'verify', loadComponent: () => import('./features/verify/verify.page').then((m) => m.VerifyPage) },
  { path: 'verify/:code', loadComponent: () => import('./features/verify/verify.page').then((m) => m.VerifyPage) },
  { path: 'dashboard', canActivate: [permissionGuard], data:{permission:'ORGANIZATION_VIEW'}, loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage) },
  { path: 'students', canActivate: [permissionGuard], data:{permission:'STUDENT_VIEW'}, loadComponent: () => import('./features/students/students.page').then((m) => m.StudentsPage) },
  { path: 'credentials', canActivate: [permissionGuard], data:{permission:'CREDENTIAL_VIEW'}, loadComponent: () => import('./features/credentials/credentials.page').then((m) => m.CredentialsPage) },
  { path: 'institution', canActivate: [permissionGuard], data:{permission:'ORGANIZATION_VIEW'}, loadComponent: () => import('./features/institution/institution.page').then((m) => m.InstitutionPage) },
  { path: '**', redirectTo: '' },
];
