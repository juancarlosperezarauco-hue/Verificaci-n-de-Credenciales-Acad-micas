import { Component, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { BrandingService } from './core/branding.service';
import { OrganizationApi } from './features/organization/organization.api';
import { AccountSecurityApi } from './core/account-security.api';
import { NavigationComponent } from './core/navigation/navigation.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, NavigationComponent],
  template: `
    <div [class]="isLanding() ? 'landing-shell min-h-screen bg-white text-slate-900' : 'app-surface min-h-screen bg-slate-950 text-slate-100'">
      <header [class]="isLanding() ? 'relative z-50 border-b border-slate-200 bg-white/90 backdrop-blur' : 'relative z-50 border-b border-white/10 bg-slate-950/90 backdrop-blur'">
        <div class="header-content">
          <app-navigation class="header-navigation" [light]="isLanding()" (logout)="logout()" />
          <a routerLink="/" class="header-brand">
            @if (branding.profile()?.logoUrl) {
              <img [src]="branding.profile()?.logoUrl" alt="Logo institucional" class="header-logo rounded-xl bg-white object-contain p-1">
            } @else {
              <span class="header-logo brand-bg grid place-items-center rounded-xl font-black text-slate-950">VC</span>
            }
            <span class="header-name" [title]="branding.profile()?.name ?? 'Credenciales Académicas'">{{ branding.profile()?.name ?? 'Credenciales Académicas' }}</span>
          </a>
        </div>
      </header>
      <main><router-outlet /></main>
    </div>
  `,
  styles: `
    .header-content{display:flex;align-items:center;justify-content:flex-start;gap:20px;width:100%;max-width:1600px;margin:0 auto;padding:16px 24px;}
    .header-brand{display:flex;align-items:center;gap:12px;min-width:0;max-width:600px;flex:0 1 600px;font-weight:650;text-decoration:none;color:inherit;}
    .header-logo{width:44px;height:44px;flex-shrink:0;}
    .header-name{display:block;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
    .header-navigation{flex-shrink:0;}
    .header-brand:focus-visible{outline:2px solid var(--brand-primary);outline-offset:5px;border-radius:10px;}
    @media(max-width:1099px){.header-content{padding:14px 16px;gap:14px;}.header-brand{flex:1 1 auto;max-width:calc(100% - 60px);}.header-logo{width:40px;height:40px;}.header-name{font-size:.9rem;}}
  `,
})
export class App {
  readonly auth = inject(AuthService);
  readonly branding = inject(BrandingService);
  readonly access = inject(OrganizationApi);
  private readonly security=inject(AccountSecurityApi);
  private readonly router = inject(Router);
  readonly isLanding = signal(this.lightSurface(this.router.url));
  private lightSurface(url:string){return url==='/'||/^\/(verify|credentials|register|org|organization|superadmin|login|access-denied|account)(\/|$|\?)/.test(url)||/^\/student\/(academic|requests|identity-center|identity|security|recover|confirm-email)(\/|$|\?)/.test(url);}

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
