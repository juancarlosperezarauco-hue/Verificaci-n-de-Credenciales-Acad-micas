import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen bg-slate-950 text-slate-100">
      <header class="border-b border-white/10 bg-slate-950/90 backdrop-blur">
        <div class="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <a routerLink="/" class="flex items-center gap-3 font-semibold">
            <span class="grid size-10 place-items-center rounded-xl bg-cyan-400 font-black text-slate-950">VC</span>
            <span>Credenciales Académicas</span>
          </a>
          <nav class="flex items-center gap-2 text-sm">
            <a routerLink="/verify" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Verificar</a>
            @if (auth.isAuthenticated()) {
              <a routerLink="/dashboard" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Panel</a>
              <a routerLink="/credentials" routerLinkActive="bg-white/10" class="rounded-lg px-3 py-2">Credenciales</a>
              <button (click)="logout()" class="rounded-lg border border-white/15 px-3 py-2">Salir</button>
            } @else {
              <a routerLink="/login" class="rounded-lg bg-cyan-400 px-4 py-2 font-semibold text-slate-950">Ingresar</a>
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
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
