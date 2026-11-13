import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { OrganizationApi } from '../organization/organization.api';
import { BrandingService } from '../../core/branding.service';
import { BrandingStyle, InstitutionProfile } from '../../core/models';

@Component({
  imports: [ReactiveFormsModule],
  template: `
    <section class="mx-auto max-w-7xl px-6 py-12">
      <div class="flex flex-wrap items-end justify-between gap-5">
        <div><p class="brand-text text-sm font-semibold">Identidad institucional</p><h1 class="mt-2 text-4xl font-black">Logo, colores y respaldo</h1><p class="mt-3 max-w-3xl text-slate-400">Este diseño se aplica al portal y a los certificados generados por la institución.</p></div>
        @if (!canEdit()) { <span class="rounded-full bg-amber-400/10 px-4 py-2 text-sm text-amber-200">Tu cuenta tiene acceso de lectura</span> }
      </div>

      @if (loading()) { <p class="mt-10 text-slate-400">Cargando configuración…</p> }
      @if (!loading() && profile()) {
        <div class="mt-10 grid gap-8 lg:grid-cols-[1fr_420px]">
          <form [formGroup]="form" (ngSubmit)="save()" class="rounded-3xl border border-white/10 bg-slate-900/90 p-7">
            <h2 class="text-xl font-bold">Esquema visual</h2>
            <div class="mt-6 grid gap-5 sm:grid-cols-3">
              <label class="text-sm">Color principal<div class="mt-2 flex gap-2"><input type="color" formControlName="primaryColor" class="h-12 w-14 rounded-lg border-0 bg-transparent"><input formControlName="primaryColor" class="field !mt-0"></div></label>
              <label class="text-sm">Color secundario<div class="mt-2 flex gap-2"><input type="color" formControlName="secondaryColor" class="h-12 w-14 rounded-lg border-0 bg-transparent"><input formControlName="secondaryColor" class="field !mt-0"></div></label>
              <label class="text-sm">Color de acento<div class="mt-2 flex gap-2"><input type="color" formControlName="accentColor" class="h-12 w-14 rounded-lg border-0 bg-transparent"><input formControlName="accentColor" class="field !mt-0"></div></label>
              <label class="text-sm sm:col-span-3">Estilo<select formControlName="brandingStyle" class="field"><option value="MODERNO">Moderno</option><option value="CLASICO">Clásico</option><option value="MINIMALISTA">Minimalista</option></select></label>
              <label class="text-sm sm:col-span-3">Nombre de quien firma<input formControlName="signatoryName" class="field" placeholder="Ej. Dra. Ana Pérez"></label>
              <label class="text-sm sm:col-span-3">Cargo de quien firma<input formControlName="signatoryTitle" class="field" placeholder="Ej. Rectora / Director académico"></label>
            </div>
            <button [disabled]="form.invalid || saving() || !canEdit()" class="brand-bg mt-6 rounded-xl px-5 py-3 font-bold text-slate-950 disabled:opacity-40">{{ saving() ? 'Guardando…' : 'Guardar identidad visual' }}</button>
          </form>

          <aside class="space-y-6">
            <div class="rounded-3xl border border-white/10 bg-slate-900 p-6">
              <h2 class="font-bold">Logo o escudo</h2>
              <div class="mt-5 grid min-h-36 place-items-center rounded-2xl bg-white p-5">
                @if (logoPreview() || profile()?.logoUrl) { <img [src]="logoPreview() || profile()?.logoUrl" alt="Logo institucional" class="max-h-28 max-w-full object-contain"> }
                @else { <span class="text-sm text-slate-500">Aún no se cargó un logo</span> }
              </div>
              <label class="brand-border mt-4 block cursor-pointer rounded-xl border px-4 py-3 text-center text-sm font-semibold">Seleccionar logo<input type="file" accept="image/png,image/jpeg,image/webp" class="hidden" [disabled]="!canEdit()" (change)="selectLogo($event)"></label>
              @if (logoFile()) { <button (click)="uploadLogo()" [disabled]="uploadingLogo()" class="brand-bg mt-3 w-full rounded-xl px-4 py-3 font-bold text-slate-950">{{ uploadingLogo() ? 'Subiendo…' : 'Guardar logo' }}</button> }
              <p class="mt-3 text-xs text-slate-500">PNG, JPG o WebP · máximo 2 MB.</p>
            </div>

            <div class="rounded-3xl border border-white/10 bg-slate-900 p-6">
              <h2 class="font-bold">Respaldo de la organización</h2>
              <p class="mt-2 text-sm text-slate-400">Adjunta la resolución, acreditación o documento que verifica a la institución.</p>
              @if (profile()?.verificationDocumentName) {
                <div class="mt-4 rounded-xl bg-emerald-400/10 p-4 text-sm text-emerald-200"><p class="font-semibold">{{ profile()?.verificationDocumentName }}</p><p class="mt-1 truncate font-mono text-xs opacity-70">SHA-256: {{ profile()?.verificationDocumentHash }}</p></div>
                <button (click)="downloadVerification()" class="mt-3 rounded-lg border border-white/15 px-3 py-2 text-sm">Descargar respaldo</button>
              }
              <label class="mt-4 block cursor-pointer rounded-xl border border-white/15 px-4 py-3 text-center text-sm">Seleccionar PDF o imagen<input type="file" accept="application/pdf,image/png,image/jpeg" class="hidden" [disabled]="!canEdit()" (change)="selectVerification($event)"></label>
              @if (verificationFile()) { <p class="mt-3 text-sm text-slate-300">{{ verificationFile()?.name }}</p><button (click)="uploadVerification()" [disabled]="uploadingVerification()" class="brand-bg mt-3 w-full rounded-xl px-4 py-3 font-bold text-slate-950">{{ uploadingVerification() ? 'Subiendo…' : 'Guardar respaldo' }}</button> }
            </div>
          </aside>
        </div>

        <div class="brand-gradient mt-8 rounded-3xl p-[2px]">
          <div class="rounded-[calc(1.5rem-2px)] bg-slate-950 p-8 text-center">
            @if (profile()?.logoUrl) { <img [src]="profile()?.logoUrl" alt="Logo" class="mx-auto h-16 max-w-40 object-contain"> }
            <p class="brand-text mt-4 text-sm font-bold uppercase tracking-[.22em]">Vista previa</p>
            <h2 class="mt-2 text-3xl font-black">{{ profile()?.name }}</h2>
            <p class="mt-2 text-slate-400">{{ styleLabel(form.controls.brandingStyle.value) }} · Certificados y portal personalizados</p>
          </div>
        </div>
      }
      @if (message()) { <p class="mt-6 rounded-xl bg-emerald-400/10 p-4 text-emerald-200">{{ message() }}</p> }
      @if (error()) { <p role="alert" class="mt-6 rounded-xl bg-red-400/10 p-4 text-red-200">{{ error() }}</p> }
    </section>
  `,
})
export class InstitutionPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly access = inject(OrganizationApi);
  private readonly branding = inject(BrandingService);
  readonly profile = signal<InstitutionProfile | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly uploadingLogo = signal(false);
  readonly uploadingVerification = signal(false);
  readonly logoFile = signal<File | null>(null);
  readonly verificationFile = signal<File | null>(null);
  readonly logoPreview = signal('');
  readonly message = signal('');
  readonly error = signal('');
  readonly canEdit = () => this.access.can('ORGANIZATION_UPDATE');
  readonly form = this.fb.nonNullable.group({
    primaryColor: ['#22D3EE', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    secondaryColor: ['#2563EB', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    accentColor: ['#A855F7', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    brandingStyle: ['MODERNO' as BrandingStyle, Validators.required],
    signatoryName: ['', Validators.maxLength(180)],
    signatoryTitle: ['', Validators.maxLength(180)],
  });

  ngOnInit(): void { this.reload(); }

  save(): void {
    if (this.form.invalid || !this.canEdit()) return;
    this.saving.set(true); this.resetMessages();
    this.api.updateInstitutionProfile(this.form.getRawValue()).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: profile => { this.setProfile(profile); this.message.set('Identidad visual actualizada.'); },
      error: e => this.error.set(e.error?.message ?? 'No se pudo guardar la identidad visual.'),
    });
  }

  selectLogo(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.logoFile.set(file); this.logoPreview.set('');
    if (file) { const reader = new FileReader(); reader.onload = () => this.logoPreview.set(String(reader.result)); reader.readAsDataURL(file); }
  }

  uploadLogo(): void {
    const file = this.logoFile(); if (!file) return;
    this.uploadingLogo.set(true); this.resetMessages();
    this.api.uploadInstitutionLogo(file).pipe(finalize(() => this.uploadingLogo.set(false))).subscribe({
      next: profile => { this.setProfile(profile); this.logoFile.set(null); this.logoPreview.set(''); this.message.set('Logo institucional guardado.'); },
      error: e => this.error.set(e.error?.message ?? 'No se pudo cargar el logo.'),
    });
  }

  selectVerification(event: Event): void { this.verificationFile.set((event.target as HTMLInputElement).files?.[0] ?? null); }

  uploadVerification(): void {
    const file = this.verificationFile(); if (!file) return;
    this.uploadingVerification.set(true); this.resetMessages();
    this.api.uploadInstitutionVerificationDocument(file).pipe(finalize(() => this.uploadingVerification.set(false))).subscribe({
      next: profile => { this.setProfile(profile); this.verificationFile.set(null); this.message.set('Documento institucional verificado y almacenado.'); },
      error: e => this.error.set(e.error?.message ?? 'No se pudo cargar el documento institucional.'),
    });
  }

  downloadVerification(): void {
    this.api.downloadInstitutionVerificationDocument().subscribe(response => this.download(response.body!, this.profile()?.verificationDocumentName ?? 'respaldo-institucional'));
  }

  styleLabel(style: BrandingStyle): string { return ({ MODERNO: 'Moderno', CLASICO: 'Clásico', MINIMALISTA: 'Minimalista' })[style]; }

  private reload(): void {
    this.api.getInstitutionProfile().subscribe({
      next: profile => { this.setProfile(profile); this.loading.set(false); },
      error: () => { this.error.set('No se pudo cargar el perfil institucional.'); this.loading.set(false); },
    });
  }

  private setProfile(profile: InstitutionProfile): void {
    this.profile.set(profile); this.branding.apply(profile);
    this.form.patchValue({ primaryColor: profile.primaryColor, secondaryColor: profile.secondaryColor,
      accentColor: profile.accentColor, brandingStyle: profile.brandingStyle,
      signatoryName: profile.signatoryName ?? '', signatoryTitle: profile.signatoryTitle ?? '' });
  }

  private resetMessages(): void { this.message.set(''); this.error.set(''); }
  private download(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = name; link.click(); URL.revokeObjectURL(url);
  }
}
