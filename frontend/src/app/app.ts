import { Component, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { BrandingService } from './core/branding.service';
import { OrganizationApi } from './features/organization/organization.api';
import { AccountSecurityApi } from './core/account-security.api';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div [class]="isLanding() ? 'landing-shell min-h-screen bg-white text-slate-900' : 'app-surface min-h-screen bg-slate-950 text-slate-100'">
      <header [class]="isLanding() ? 'relative z-50 border-b border-slate-200 bg-white/90 backdrop-blur' : 'relative z-50 border-b border-white/10 bg-slate-950/90 backdrop-blur'">
        <div class="mx-auto flex max-w-7xl flex-wrap items-center gap-x-5 gap-y-3 px-6 py-4 sm:flex-nowrap">
          <a routerLink="/" class="flex shrink-0 items-center gap-3 font-semibold">
            @if (branding.profile()?.logoUrl) {
              <img [src]="branding.profile()?.logoUrl" alt="Logo institucional" class="size-11 rounded-xl bg-white object-contain p-1">
            } @else {
              <span class="brand-bg grid size-10 place-items-center rounded-xl font-black text-slate-950">VC</span>
            }
            <span class="hidden lg:inline">{{ branding.profile()?.name ?? 'Credenciales Académicas' }}</span>
          </a>
          <nav [class]="isLanding() ? 'module-bar order-2 flex w-full min-w-0 items-center justify-start gap-1 overflow-x-auto whitespace-nowrap text-sm text-slate-700 sm:order-none sm:ml-auto sm:w-auto sm:justify-end' : 'module-bar order-2 flex w-full min-w-0 items-center justify-start gap-1 overflow-x-auto whitespace-nowrap text-sm sm:order-none sm:ml-auto sm:w-auto sm:justify-end'">
            @if (auth.isAuthenticated()) {
              @if (auth.current()?.role === 'STUDENT') {
                <a routerLink="/student/account" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Mi expediente</a>
                <a routerLink="/student/security" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Seguridad</a>
                <a routerLink="/verify" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Verificar</a>
              } @else if(auth.current()?.role === 'SUPER_ADMIN') {
                <a routerLink="/superadmin/organizations" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Organizaciones</a>
                <a routerLink="/superadmin/audit" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Auditoría</a>
                <a routerLink="/superadmin/mail" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Correos</a>
                <a routerLink="/verify" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Verificar</a>
              } @else {
                @if(access.can('ORGANIZATION_VIEW')&&!access.access()?.onboarding){<a routerLink="/dashboard" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Panel</a>}
                @if(access.can('STUDENT_VIEW')){<a routerLink="/students" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Estudiantes</a>}
                @if(access.can('CREDENTIAL_VIEW')){<a routerLink="/credentials" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Credenciales</a>}
                @if(access.can('ORGANIZATION_VIEW')){<a routerLink="/organization" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Organización</a><a routerLink="/institution" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Imagen institucional</a>}
                @if(isAdmin()){<a routerLink="/organization/team" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Equipo y roles</a>}
                @if(access.can('AUDIT_VIEW')&&!access.access()?.onboarding){<a routerLink="/organization/audit" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Auditoría</a>}
                <a routerLink="/verify" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Verificar</a>
              }
              <a routerLink="/account/security" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Mis sesiones</a>
              <button (click)="logout()" class="ml-1 rounded-lg border border-white/15 px-3 py-2">Salir</button>
            } @else {
              <a routerLink="/student/login" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Estudiantes</a>
              <a routerLink="/register" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Instituciones</a>
              <a routerLink="/verify" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Verificación</a>
              <a routerLink="/login" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Portal académico</a>
              <a routerLink="/login" class="brand-bg ml-1 rounded-lg px-4 py-2 font-semibold text-slate-950">Ingresar</a>
            }
          </nav>
        </div>
      </header>
      <main><router-outlet /></main>
    </div>
  `,
  styles: `
    .landing-shell .brand-nav-active { background: #f1f5f9; color: #0f172a; }
    .app-surface .brand-nav-active { background: rgb(255 255 255 / 10%); }
    .module-bar { scrollbar-width: none; }
    .module-bar::-webkit-scrollbar { display: none; }
    .landing-shell .module-bar a:not(.brand-bg):hover { background: #f1f5f9; color: #0f172a; }
    .app-surface .module-bar a:not(.brand-bg):hover { background: rgb(255 255 255 / 8%); }
  `,
})
export class App {
  readonly auth = inject(AuthService);
  readonly branding = inject(BrandingService);
  readonly access = inject(OrganizationApi);
  private readonly security=inject(AccountSecurityApi);
  private readonly router = inject(Router);
  readonly isLanding = signal(this.lightSurface(this.router.url));
  private lightSurface(url:string){return url==='/'||/^\/(register|org|organization|superadmin|login|access-denied|account)(\/|$|\?)/.test(url)||/^\/student\/(security|recover|confirm-email)(\/|$|\?)/.test(url);}
  isAdmin(){return !this.access.access()?.onboarding&&['ADMIN','TENANT_ADMIN'].includes(this.auth.current()?.role??'');}

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) this.isLanding.set(this.lightSurface(event.urlAfterRedirects));
    });
    effect(() => {
      const session = this.auth.current();
      this.access.access.set(null);
      this.branding.clear();
      if (session?.token && session.role !== 'STUDENT') this.access.me().subscribe({
        next:p=>{if(p.permissions.includes('ORGANIZATION_VIEW'))this.branding.load();},
        error:()=>this.access.access.set(null)
      });
    });
  }

  logout(): void {
    const role=this.auth.current()?.role;
    const finish=(unconfirmed:boolean)=>{this.auth.logout();this.access.access.set(null);void this.router.navigate([role==='STUDENT'?'/student/login':role==='SUPER_ADMIN'?'/superadmin/login':'/login'],{queryParams:unconfirmed?{logoutUnconfirmed:true}:{}});};
    this.security.logout().subscribe({next:()=>finish(false),error:()=>finish(true)});
  }
}
