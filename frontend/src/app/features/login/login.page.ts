import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

@Component({
  imports: [ReactiveFormsModule],
  template: `
    <section class="mx-auto grid min-h-[calc(100vh-73px)] max-w-7xl place-items-center px-6 py-16">
      <div class="grid w-full max-w-5xl overflow-hidden rounded-3xl border border-white/10 bg-slate-900 shadow-2xl lg:grid-cols-2">
        <div class="hidden bg-gradient-to-br from-cyan-400 to-blue-600 p-12 text-slate-950 lg:block">
          <p class="text-sm font-bold uppercase tracking-[.25em]">Acceso institucional</p>
          <h1 class="mt-5 text-4xl font-black leading-tight">Emite credenciales verificables y resistentes a alteraciones.</h1>
          <p class="mt-6 text-lg text-slate-900/75">Cada organización mantiene sus usuarios y documentos separados.</p>
        </div>
        <form [formGroup]="form" (ngSubmit)="submit()" class="p-8 sm:p-12">
          <p class="text-sm font-semibold text-cyan-300">Bienvenido</p>
          <h2 class="mt-2 text-3xl font-bold">Inicia sesión</h2>
          <div class="mt-8 space-y-5">
            <label class="block text-sm">Organización
              <input formControlName="organizationSlug" placeholder="universidad-demo" class="mt-2 w-full rounded-xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-400">
            </label>
            <label class="block text-sm">Correo electrónico
              <input formControlName="email" type="email" placeholder="admin@universidad.edu" class="mt-2 w-full rounded-xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-400">
            </label>
            <label class="block text-sm">Contraseña
              <input formControlName="password" type="password" class="mt-2 w-full rounded-xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-400">
            </label>
          </div>
          @if (error()) { <p class="mt-5 rounded-xl bg-red-500/10 p-3 text-sm text-red-300">{{ error() }}</p> }
          <button [disabled]="form.invalid || loading()" class="mt-7 w-full rounded-xl bg-cyan-400 px-4 py-3 font-bold text-slate-950 disabled:opacity-50">
            {{ loading() ? 'Ingresando…' : 'Ingresar' }}
          </button>
        </form>
      </div>
    </section>
  `,
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({
    organizationSlug: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.api.login(this.form.getRawValue()).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (response) => {
        this.auth.save(response);
        void this.router.navigate(['/dashboard']);
      },
      error: () => this.error.set('No pudimos iniciar sesión. Revisa la organización y tus credenciales.'),
    });
  }
}
