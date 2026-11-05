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
        <div class="mx-auto flex flex-wrap gap-4 max-w-7xl items-center justify-between px-6 py-4">
          <a routerLink="/" class="flex items-center gap-3 font-semibold">
            @if (branding.profile()?.logoUrl) {
              <img [src]="branding.profile()?.logoUrl" alt="Logo institucional" class="size-11 rounded-xl bg-white object-contain p-1">
            } @else {
              <span class="brand-bg grid size-10 place-items-center rounded-xl font-black text-slate-950">VC</span>
            }
            <span>{{ branding.profile()?.name ?? 'Credenciales Académicas' }}</span>
          </a>
          <nav [class]="isLanding() ? 'flex flex-wrap items-center gap-2 text-sm text-slate-700' : 'flex flex-wrap items-center gap-2 text-sm'">
            <details class="module-menu relative">
              <summary class="flex cursor-pointer list-none items-center gap-2 rounded-lg px-3 py-2 font-semibold">
                Módulos <span aria-hidden="true" class="text-xs opacity-60">▾</span>
              </summary>
              <div class="module-panel absolute left-0 top-[calc(100%+.65rem)] z-50 w-[min(20rem,calc(100vw-2rem))] rounded-2xl border p-2 shadow-2xl">
                @if (auth.isAuthenticated()) {
                  @if (auth.current()?.role === 'STUDENT') {
                    <a routerLink="/student/account"><strong>Mi expediente</strong><small>Credenciales y código personal</small></a>
                    <a routerLink="/verify"><strong>Verificación pública</strong><small>Consultar autenticidad y estado</small></a>
                  } @else {
                    <a routerLink="/dashboard"><strong>Panel académico</strong><small>Avisos, fechas y actividad</small></a>
                    <a routerLink="/students"><strong>Estudiantes</strong><small>Vinculación mediante código personal</small></a>
                    <a routerLink="/credentials"><strong>Credenciales</strong><small>Diplomas, certificados y emisión</small></a>
                    <a routerLink="/institution"><strong>Institución</strong><small>Logo, colores y documentos</small></a>
                  }
                } @else {
                  <a routerLink="/student/login"><strong>Identidad estudiantil</strong><small>Cuenta, expediente y código personal</small></a>
                  <a routerLink="/register"><strong>Gestión institucional</strong><small>Universidades, colegios e institutos</small></a>
                  <a routerLink="/verify"><strong>Credenciales verificables</strong><small>Diplomas, certificados y códigos QR</small></a>
                  <a routerLink="/login"><strong>Portal académico</strong><small>Avisos, fechas y administración</small></a>
                }
              </div>
            </details>
            <a routerLink="/verify" routerLinkActive="brand-nav-active" class="rounded-lg px-3 py-2">Verificar</a>
            @if (auth.isAuthenticated()) {
              <button (click)="logout()" class="rounded-lg border border-white/15 px-3 py-2">Salir</button>
            } @else {
              <a routerLink="/login" class="brand-bg rounded-lg px-4 py-2 font-semibold text-slate-950">Ingresar</a>
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
    .module-menu summary::-webkit-details-marker { display: none; }
    .module-menu[open] summary { background: color-mix(in srgb, var(--brand-primary) 10%, transparent); }
    .module-panel a { display: block; border-radius: .85rem; padding: .8rem .9rem; }
    .module-panel strong, .module-panel small { display: block; }
    .module-panel strong { font-size: .9rem; }
    .module-panel small { margin-top: .2rem; font-size: .75rem; line-height: 1.35; opacity: .65; }
    .landing-shell .module-panel { border-color: #e2e8f0; background: #fff; color: #0f172a; }
    .landing-shell .module-panel a:hover { background: #f1f5f9; }
    .app-surface .module-panel { border-color: rgb(255 255 255 / 12%); background: #0f172a; color: #f8fafc; }
    .app-surface .module-panel a:hover { background: rgb(255 255 255 / 8%); }
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
