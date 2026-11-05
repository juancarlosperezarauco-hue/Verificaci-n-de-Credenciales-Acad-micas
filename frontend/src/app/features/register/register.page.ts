import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { concat, finalize, last, switchMap, tap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-5xl px-6 py-12 sm:py-16">
      <div class="overflow-hidden rounded-3xl border border-white/10 bg-slate-900 shadow-2xl lg:grid lg:grid-cols-[.85fr_1.15fr]">
        <div class="bg-gradient-to-br from-cyan-400 via-sky-500 to-blue-700 p-8 text-slate-950 sm:p-12">
          <p class="text-sm font-black uppercase tracking-[.22em]">Paso 1</p>
          <h1 class="mt-5 text-4xl font-black leading-tight">Registra tu institución educativa</h1>
          <p class="mt-5 text-slate-900/75">Crea un espacio privado para administrar estudiantes y emitir sus diplomas o certificados verificables.</p>
          <ol class="mt-8 space-y-4 text-sm font-semibold">
            <li>1. Datos de la institución</li><li>2. Vinculación mediante código del estudiante</li><li>3. Emisión de credenciales</li>
          </ol>
        </div>
        <form [formGroup]="form" (ngSubmit)="submit()" class="p-8 sm:p-12">
          <p class="text-sm font-semibold text-cyan-300">Nueva cuenta institucional</p>
          <h2 class="mt-2 text-3xl font-bold">Datos de registro</h2>
          <div class="mt-8 grid gap-5 sm:grid-cols-2">
            <label class="block text-sm sm:col-span-2">Nombre de la institución
              <input formControlName="organizationName" placeholder="Ej. Universidad Central" class="field">
            </label>
            <label class="block text-sm">Tipo de institución
              <select formControlName="institutionType" class="field">
                <option value="UNIVERSIDAD">Universidad</option><option value="COLEGIO">Colegio</option>
                <option value="INSTITUTO">Instituto</option><option value="ACADEMIA">Academia</option>
                <option value="CENTRO_FORMACION">Centro de formación</option><option value="OTRO">Otro</option>
              </select>
            </label>
            <label class="block text-sm">Identificador de acceso
              <input formControlName="organizationSlug" placeholder="universidad-central" class="field">
            </label>
            <label class="block text-sm sm:col-span-2">Correo del administrador
              <input formControlName="email" type="email" placeholder="admin@institucion.edu" class="field">
            </label>
            <label class="block text-sm sm:col-span-2">Contraseña
              <input formControlName="password" type="password" placeholder="Mínimo 10 caracteres" class="field">
            </label>
            <div class="sm:col-span-2 mt-2 border-t border-white/10 pt-6"><p class="font-bold">Identidad visual</p><p class="mt-1 text-xs text-slate-500">Podrás cambiarla después desde el módulo Institución.</p></div>
            <label class="block text-sm">Color principal<div class="mt-2 flex items-center gap-3"><input type="color" formControlName="primaryColor" class="h-11 w-14 rounded-lg border-0 bg-transparent"><input formControlName="primaryColor" class="field !mt-0"></div></label>
            <label class="block text-sm">Color secundario<div class="mt-2 flex items-center gap-3"><input type="color" formControlName="secondaryColor" class="h-11 w-14 rounded-lg border-0 bg-transparent"><input formControlName="secondaryColor" class="field !mt-0"></div></label>
            <label class="block text-sm">Color de acento<div class="mt-2 flex items-center gap-3"><input type="color" formControlName="accentColor" class="h-11 w-14 rounded-lg border-0 bg-transparent"><input formControlName="accentColor" class="field !mt-0"></div></label>
            <label class="block text-sm">Estilo<select formControlName="brandingStyle" class="field"><option value="MODERNO">Moderno</option><option value="CLASICO">Clásico</option><option value="MINIMALISTA">Minimalista</option></select></label>
            <label class="block text-sm sm:col-span-2">Logo o escudo (opcional)<input type="file" accept="image/png,image/jpeg,image/webp" class="field file:mr-3 file:rounded-lg file:border-0 file:px-3 file:py-1" (change)="selectLogo($event)"></label>
            <label class="block text-sm sm:col-span-2">Documento de respaldo institucional (opcional)<input type="file" accept="application/pdf,image/png,image/jpeg" class="field file:mr-3 file:rounded-lg file:border-0 file:px-3 file:py-1" (change)="selectVerification($event)"></label>
          </div>
          @if (error()) { <p class="mt-5 rounded-xl bg-red-500/10 p-3 text-sm text-red-300">{{ error() }}</p> }
          <button [disabled]="form.invalid || loading()" class="mt-7 w-full rounded-xl bg-cyan-400 px-4 py-3 font-bold text-slate-950 disabled:opacity-50">{{ loading() ? 'Registrando…' : 'Registrar institución' }}</button>
          <p class="mt-5 text-center text-sm text-slate-400">¿Ya tienes una institución? <a routerLink="/login" class="font-semibold text-cyan-300">Inicia sesión</a></p>
        </form>
      </div>
    </section>
  `,
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly logoFile = signal<File | null>(null);
  readonly verificationFile = signal<File | null>(null);
  readonly form = this.fb.nonNullable.group({
    organizationName: ['', [Validators.required, Validators.maxLength(160)]],
    organizationSlug: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(80)]],
    institutionType: ['UNIVERSIDAD' as const, Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(10)]],
    primaryColor: ['#22D3EE', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    secondaryColor: ['#2563EB', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    accentColor: ['#A855F7', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    brandingStyle: ['MODERNO' as const, Validators.required],
  });

  selectLogo(event: Event): void { this.logoFile.set((event.target as HTMLInputElement).files?.[0] ?? null); }
  selectVerification(event: Event): void { this.verificationFile.set((event.target as HTMLInputElement).files?.[0] ?? null); }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    const value = this.form.getRawValue();
    const registration = { organizationName: value.organizationName, organizationSlug: value.organizationSlug,
      institutionType: value.institutionType, email: value.email, password: value.password };
    this.api.registerInstitution(registration).pipe(
      tap(response => this.auth.save(response)),
      // El perfil y los archivos se guardan después de obtener el token institucional.
      switchMap(() => {
        const operations = [this.api.updateInstitutionProfile({ primaryColor: value.primaryColor,
          secondaryColor: value.secondaryColor, accentColor: value.accentColor,
          brandingStyle: value.brandingStyle, signatoryName: '', signatoryTitle: '' })];
        if (this.logoFile()) operations.push(this.api.uploadInstitutionLogo(this.logoFile()!));
        if (this.verificationFile()) operations.push(this.api.uploadInstitutionVerificationDocument(this.verificationFile()!));
        return concat(...operations).pipe(last());
      }),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: () => { void this.router.navigate(['/students']); },
      error: (response) => this.error.set(response.error?.message ?? 'No se pudo registrar la institución.'),
    });
  }
}
