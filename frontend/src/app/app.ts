import { Component, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { BrandingService } from './core/branding.service';

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
                <a routerLink="/verify" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Verificar</a>
              } @else {
                <a routerLink="/dashboard" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Panel</a>
                <a routerLink="/students" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Estudiantes</a>
                <a routerLink="/credentials" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Credenciales</a>
                <a routerLink="/institution" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Institución</a>
                <a routerLink="/verify" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Verificar</a>
              }
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
  private readonly router = inject(Router);
  readonly isLanding = signal(this.router.url === '/');

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) this.isLanding.set(event.urlAfterRedirects === '/');
    });
    effect(() => {
      const session = this.auth.current();
      if (session?.token && session.role !== 'STUDENT') this.branding.load();
      else this.branding.clear();
    });
  }

  logout(): void {
    const student = this.auth.current()?.role === 'STUDENT';
    this.auth.logout();
    void this.router.navigate([student ? '/student/login' : '/login']);
  }
}
