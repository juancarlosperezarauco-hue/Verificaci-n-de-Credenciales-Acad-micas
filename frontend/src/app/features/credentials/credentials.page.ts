import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import QRCode from 'qrcode';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Credential } from '../../core/models';

@Component({
  imports: [ReactiveFormsModule],
  template: `
    <section class="mx-auto max-w-7xl px-6 py-12">
      <div><p class="text-sm font-semibold text-cyan-300">Módulo de emisión</p><h1 class="mt-2 text-4xl font-black">Credenciales académicas</h1></div>
      <div class="mt-10 grid gap-8 lg:grid-cols-[360px_1fr]">
        <form [formGroup]="form" (ngSubmit)="create()" class="h-fit rounded-2xl border border-white/10 bg-slate-900 p-6">
          <h2 class="text-xl font-bold">Crear borrador</h2>
          <div class="mt-6 space-y-4 text-sm">
            <label class="block">Nombre del titular<input formControlName="holderName" class="mt-2 w-full rounded-xl border border-white/10 bg-slate-950 px-4 py-3"></label>
            <label class="block">Documento<input formControlName="holderDocument" class="mt-2 w-full rounded-xl border border-white/10 bg-slate-950 px-4 py-3"></label>
            <label class="block">Tipo<select formControlName="credentialType" class="mt-2 w-full rounded-xl border border-white/10 bg-slate-950 px-4 py-3"><option value="DIPLOMA">Diploma</option><option value="CERTIFICADO">Certificado</option><option value="TITULO">Título</option></select></label>
            <label class="block">Nombre del programa<input formControlName="title" class="mt-2 w-full rounded-xl border border-white/10 bg-slate-950 px-4 py-3"></label>
          </div>
          <button [disabled]="form.invalid || saving()" class="mt-6 w-full rounded-xl bg-cyan-400 px-4 py-3 font-bold text-slate-950 disabled:opacity-50">{{ saving() ? 'Guardando…' : 'Guardar borrador' }}</button>
        </form>

        <div class="overflow-hidden rounded-2xl border border-white/10 bg-slate-900">
          <div class="border-b border-white/10 px-6 py-5"><h2 class="text-xl font-bold">Documentos registrados</h2></div>
          @if (loading()) { <p class="p-6 text-slate-400">Cargando…</p> }
          @if (!loading() && credentials().length === 0) { <p class="p-6 text-slate-400">No existen credenciales todavía.</p> }
          <div class="divide-y divide-white/10">
            @for (item of credentials(); track item.id) {
              <article class="grid gap-4 p-6 sm:grid-cols-[1fr_auto] sm:items-center">
                <div><div class="flex flex-wrap items-center gap-3"><h3 class="font-bold">{{ item.title }}</h3><span [class]="statusClass(item.status)">{{ item.status }}</span></div><p class="mt-1 text-sm text-slate-400">{{ item.holderName }} · {{ item.credentialType }}</p>@if (item.documentName) { <p class="mt-2 text-xs text-cyan-300">Documento: {{ item.documentName }}</p> }@if (item.verificationCode) { <p class="mt-2 font-mono text-xs text-slate-500">{{ item.verificationCode }}</p> }</div>
                <div class="flex flex-wrap gap-2">
                  @if (item.status === 'DRAFT') { <label class="cursor-pointer rounded-lg border border-white/15 px-3 py-2 text-sm">{{ item.documentName ? 'Cambiar PDF' : 'Adjuntar PDF' }}<input type="file" accept="application/pdf" class="hidden" (change)="upload(item, $event)"></label><button (click)="issue(item)" [disabled]="!item.documentHash" class="rounded-lg bg-emerald-400 px-3 py-2 text-sm font-bold text-slate-950 disabled:cursor-not-allowed disabled:opacity-40">Emitir</button> }
                  @if (item.status === 'ISSUED') { <button (click)="showQr(item)" class="rounded-lg border border-white/15 px-3 py-2 text-sm">Ver QR</button><button (click)="revoke(item)" class="rounded-lg border border-red-400/30 px-3 py-2 text-sm text-red-300">Revocar</button> }
                </div>
              </article>
            }
          </div>
        </div>
      </div>
      @if (qrData()) {
        <div class="fixed inset-0 grid place-items-center bg-slate-950/80 p-6" (click)="qrData.set('')">
          <div class="rounded-2xl bg-white p-7 text-center text-slate-950" (click)="$event.stopPropagation()"><h2 class="text-xl font-bold">Código de verificación</h2><img [src]="qrData()" alt="QR de la credencial" class="mx-auto mt-5 size-64"><button (click)="qrData.set('')" class="mt-5 rounded-lg bg-slate-900 px-4 py-2 text-white">Cerrar</button></div>
        </div>
      }
    </section>
  `,
})
export class CredentialsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  readonly credentials = signal<Credential[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly qrData = signal('');
  readonly form = this.fb.nonNullable.group({
    holderName: ['', Validators.required],
    holderDocument: [''],
    credentialType: ['DIPLOMA', Validators.required],
    title: ['', Validators.required],
  });

  ngOnInit(): void { this.reload(); }

  create(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.api.createCredential(this.form.getRawValue()).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (item) => { this.credentials.update((items) => [item, ...items]); this.form.reset({ credentialType: 'DIPLOMA', holderName: '', holderDocument: '', title: '' }); },
    });
  }

  issue(item: Credential): void {
    this.api.issueCredential(item.id).subscribe((updated) => this.replace(updated));
  }

  upload(item: Credential, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.api.uploadDocument(item.id, file).subscribe({
      next: (updated) => { this.replace(updated); input.value = ''; },
      error: () => { window.alert('No se pudo cargar el documento. Verifica que MinIO esté disponible.'); input.value = ''; },
    });
  }

  revoke(item: Credential): void {
    const reason = window.prompt('Motivo de revocación:');
    if (!reason?.trim()) return;
    this.api.revokeCredential(item.id, reason.trim()).subscribe((updated) => this.replace(updated));
  }

  async showQr(item: Credential): Promise<void> {
    this.qrData.set(await QRCode.toDataURL(`${window.location.origin}/verify/${item.verificationCode}`, { width: 320, margin: 1 }));
  }

  statusClass(status: Credential['status']): string {
    const color = status === 'ISSUED' ? 'bg-emerald-400/10 text-emerald-300' : status === 'REVOKED' ? 'bg-red-400/10 text-red-300' : 'bg-amber-400/10 text-amber-300';
    return `rounded-full px-3 py-1 text-xs font-semibold ${color}`;
  }

  private reload(): void {
    this.api.listCredentials().subscribe({ next: (items) => { this.credentials.set(items); this.loading.set(false); }, error: () => this.loading.set(false) });
  }

  private replace(updated: Credential): void {
    this.credentials.update((items) => items.map((item) => item.id === updated.id ? updated : item));
  }
}
