import { Component, effect, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { BrandingService } from './core/branding.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-surface min-h-screen bg-slate-950 text-slate-100">
      <header class="border-b border-white/10 bg-slate-950/90 backdrop-blur">
        <div class="mx-auto flex flex-wrap gap-4 max-w-7xl items-center justify-between px-6 py-4">
          <a routerLink="/" class="flex items-center gap-3 font-semibold">
            @if (branding.profile()?.logoUrl) {
              <img [src]="branding.profile()?.logoUrl" alt="Logo institucional" class="size-11 rounded-xl bg-white object-contain p-1">
            } @else {
              <span class="brand-bg grid size-10 place-items-center rounded-xl font-black text-slate-950">VC</span>
            }
            <span>{{ branding.profile()?.name ?? 'Credenciales Académicas' }}</span>
          </a>
          <nav class="flex flex-wrap items-center gap-2 text-sm">
            <a routerLink="/" [routerLinkActiveOptions]="{ exact: true }" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Inicio</a>
            <a routerLink="/verify" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Verificar</a>
            @if (auth.isAuthenticated()) {
              @if (auth.current()?.role === 'STUDENT') {
                <a routerLink="/student/account" class="rounded-lg px-3 py-2">Mi cuenta</a>
              } @else {
              <a routerLink="/dashboard" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Panel</a>
              <a routerLink="/students" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Estudiantes</a>
              <a routerLink="/credentials" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Credenciales</a>
              <a routerLink="/institution" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Institución</a>
              }
              <button (click)="logout()" class="rounded-lg border border-white/15 px-3 py-2">Salir</button>
            } @else {
              <a routerLink="/student/login" class="rounded-lg px-3 py-2">Soy estudiante</a>
              <a routerLink="/register" class="rounded-lg px-3 py-2">Registrar institución</a>
              <a routerLink="/login" class="brand-bg rounded-lg px-4 py-2 font-semibold text-slate-950">Ingresar</a>
            }
          </nav>
        </div>
      </header>
      <main><router-outlet /></main>
    </div>
  `,
})
export class App {
  readonly auth = inject(AuthService);
  readonly branding = inject(BrandingService);
  private readonly router = inject(Router);

  constructor() {
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
