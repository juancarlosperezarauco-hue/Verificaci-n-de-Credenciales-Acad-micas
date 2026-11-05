import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { PublicCredential } from '../../core/models';

@Component({
  imports: [ReactiveFormsModule],
  template: `
    <section class="mx-auto max-w-5xl px-6 py-16">
      <div class="text-center"><p class="brand-text text-sm font-semibold uppercase tracking-[.2em]">Consulta pública</p><h1 class="mt-4 text-4xl font-black sm:text-5xl">Verifica una credencial</h1><p class="mx-auto mt-5 max-w-2xl text-lg text-slate-400">Introduce el código impreso o abre el enlace incluido en el QR.</p></div>
      <form (ngSubmit)="verify()" class="mx-auto mt-10 flex max-w-2xl gap-3 rounded-2xl border border-white/10 bg-slate-900 p-3">
        <input [formControl]="code" placeholder="Ej. CRED-AB12CD34" class="min-w-0 flex-1 bg-transparent px-3 outline-none">
        <button [disabled]="code.invalid || loading()" class="brand-bg rounded-xl px-5 py-3 font-bold text-slate-950 disabled:opacity-50">Verificar</button>
      </form>
      @if (error()) { <div class="mx-auto mt-8 max-w-2xl rounded-2xl border border-red-400/30 bg-red-400/10 p-6 text-red-200">{{ error() }}</div> }
      @if (result(); as credential) {
        <article class="mx-auto mt-8 max-w-2xl overflow-hidden rounded-3xl border-2 bg-slate-900 shadow-2xl" [style.border-color]="credential.primaryColor || '#ffffff1a'">
          <div [class]="credential.status === 'ISSUED' ? 'bg-emerald-400 p-6 text-slate-950' : 'bg-red-400 p-6 text-slate-950'"><p class="text-sm font-bold uppercase tracking-wider">Resultado</p><h2 class="mt-2 text-3xl font-black">{{ credential.status === 'ISSUED' ? 'Credencial válida' : 'Credencial revocada' }}</h2></div>
          <div class="flex items-center gap-4 border-b border-white/10 p-7">@if (credential.institutionLogoUrl) { <img [src]="credential.institutionLogoUrl" alt="Logo de la institución" class="size-16 rounded-xl bg-white object-contain p-1"> }<div><p class="text-sm text-slate-400">Institución emisora</p><h3 class="mt-1 text-xl font-bold">{{ credential.organization }}</h3><p class="mt-1 text-xs" [style.color]="credential.primaryColor">{{ credential.institutionType }}</p></div></div>
          <dl class="grid gap-6 p-7 sm:grid-cols-2"><div><dt class="text-sm text-slate-400">Titular</dt><dd class="mt-1 font-semibold">{{ credential.holderName }}</dd></div><div><dt class="text-sm text-slate-400">Programa</dt><dd class="mt-1 font-semibold">{{ credential.title }}</dd></div><div><dt class="text-sm text-slate-400">Tipo</dt><dd class="mt-1 font-semibold">{{ credential.credentialType }}</dd></div><div><dt class="text-sm text-slate-400">Código</dt><dd class="mt-1 font-mono text-sm">{{ credential.verificationCode }}</dd></div>@if (credential.revocationReason) { <div class="sm:col-span-2"><dt class="text-sm text-red-300">Motivo</dt><dd class="mt-1">{{ credential.revocationReason }}</dd></div> }</dl>
        </article>
      }
    </section>
  `,
})
export class VerifyPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly code = new FormControl('', { nonNullable: true, validators: Validators.required });
  readonly result = signal<PublicCredential | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    const routeCode = this.route.snapshot.paramMap.get('code');
    if (routeCode) { this.code.setValue(routeCode); this.lookup(routeCode); }
  }

  verify(): void {
    if (this.code.invalid) return;
    const value = this.code.value.trim();
    void this.router.navigate(['/verify', value]);
    this.lookup(value);
  }

  private lookup(value: string): void {
    this.loading.set(true);
    this.error.set('');
    this.result.set(null);
    this.api.verifyCredential(value).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (credential) => this.result.set(credential),
      error: () => this.error.set('No se encontró una credencial con ese código.'),
    });
  }
}
