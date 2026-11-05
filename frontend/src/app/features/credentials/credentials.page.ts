import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import QRCode from 'qrcode';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Credential, Student } from '../../core/models';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-7xl px-6 py-12">
      <div><p class="brand-text text-sm font-semibold">Paso 3 · Módulo de emisión</p><h1 class="mt-2 text-4xl font-black">Diplomas y certificados</h1><p class="mt-3 text-slate-400">Selecciona uno o varios estudiantes, crea los borradores y genera documentos con el estilo de tu institución.</p></div>
      <div class="mt-10 grid gap-8 lg:grid-cols-[390px_1fr]">
        <form [formGroup]="form" (ngSubmit)="create()" class="h-fit rounded-2xl border border-white/10 bg-slate-900 p-6">
          <h2 class="text-xl font-bold">Crear certificados</h2>
          <div class="mt-6 space-y-4 text-sm">
            <fieldset><legend class="font-semibold">Estudiantes seleccionados ({{ selectedStudentIds().length }})</legend><div class="mt-2 max-h-52 space-y-2 overflow-auto rounded-xl border border-white/10 bg-slate-950 p-3">
              @for (student of students(); track student.id) { <label class="flex cursor-pointer items-start gap-3 rounded-lg p-2 hover:bg-white/5"><input type="checkbox" class="mt-1 size-4" [checked]="isSelected(student.id)" (change)="toggleStudent(student.id, $event)"><span><span class="block font-semibold">{{ student.fullName }}</span><span class="text-xs text-slate-500">{{ student.documentType }} {{ student.documentNumber }}</span></span></label> }
              @if (!loadingStudents() && students().length === 0) { <span class="text-slate-500">No hay estudiantes vinculados.</span> }
            </div><div class="mt-2 flex gap-3 text-xs"><button type="button" (click)="selectAll()" class="brand-text">Seleccionar todos</button><button type="button" (click)="selectedStudentIds.set([])" class="text-slate-400">Limpiar</button></div></fieldset>
            <label class="block">Tipo<select formControlName="credentialType" class="field"><option value="DIPLOMA">Diploma</option><option value="CERTIFICADO">Certificado</option><option value="TITULO">Título</option><option value="RECONOCIMIENTO">Reconocimiento</option><option value="CONSTANCIA">Constancia</option></select></label>
            <label class="block">Nombre del programa o reconocimiento<input formControlName="title" class="field"></label>
          </div>
          @if (!loadingStudents() && students().length === 0) { <p class="mt-5 rounded-xl bg-amber-400/10 p-3 text-sm text-amber-200">Primero debes <a routerLink="/students" class="font-bold underline">vincular un estudiante con su código</a>.</p> }
          <button [disabled]="form.invalid || selectedStudentIds().length === 0 || saving()" class="brand-bg mt-6 w-full rounded-xl px-4 py-3 font-bold text-slate-950 disabled:opacity-50">{{ saving() ? 'Creando…' : 'Crear ' + selectedStudentIds().length + ' borrador(es)' }}</button>
          @if(error()){<p role="alert" class="mt-4 text-sm text-red-300">{{error()}}</p>}
        </form>

        <div class="overflow-hidden rounded-2xl border border-white/10 bg-slate-900">
          <div class="border-b border-white/10 px-6 py-5"><h2 class="text-xl font-bold">Documentos registrados</h2></div>
          @if (loading()) { <p class="p-6 text-slate-400">Cargando…</p> }
          @if (!loading() && credentials().length === 0) { <p class="p-6 text-slate-400">No existen credenciales todavía.</p> }
          <div class="divide-y divide-white/10">
            @for (item of credentials(); track item.id) {
              <article class="grid gap-4 p-6 sm:grid-cols-[1fr_auto] sm:items-center">
                <div><div class="flex flex-wrap items-center gap-3"><h3 class="font-bold">{{ item.title }}</h3><span [class]="statusClass(item.status)">{{ item.status }}</span></div><p class="mt-1 text-sm text-slate-400">{{ item.holderName }} · {{ item.credentialType }}</p>@if (item.documentName) { <p class="brand-text mt-2 text-xs">Documento: {{ item.documentName }}</p> }@if (item.verificationCode) { <p class="mt-2 font-mono text-xs text-slate-500">{{ item.verificationCode }}</p> }</div>
                <div class="flex flex-wrap gap-2">
                  @if (item.status === 'DRAFT') { <button (click)="generate(item)" [disabled]="generating() === item.id" class="brand-bg rounded-lg px-3 py-2 text-sm font-bold text-slate-950 disabled:opacity-40">{{ generating() === item.id ? 'Generando…' : 'Generar PDF' }}</button><label class="cursor-pointer rounded-lg border border-white/15 px-3 py-2 text-sm">{{ item.documentName ? 'Reemplazar PDF' : 'Adjuntar PDF propio' }}<input type="file" accept="application/pdf" class="hidden" (change)="upload(item, $event)"></label><button (click)="issue(item)" [disabled]="!item.documentHash" class="rounded-lg bg-emerald-400 px-3 py-2 text-sm font-bold text-slate-950 disabled:cursor-not-allowed disabled:opacity-40">Emitir</button> }
                  @if (item.documentName) { <button (click)="download(item)" class="rounded-lg border border-white/15 px-3 py-2 text-sm">Descargar</button> }
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
  readonly students = signal<Student[]>([]);
  readonly loading = signal(true);
  readonly loadingStudents = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly qrData = signal('');
  readonly selectedStudentIds = signal<string[]>([]);
  readonly generating = signal('');
  readonly form = this.fb.nonNullable.group({
    credentialType: ['DIPLOMA', Validators.required],
    title: ['', Validators.required],
  });

  ngOnInit(): void { this.reload(); this.loadStudents(); }

  create(): void {
    if (this.form.invalid || this.selectedStudentIds().length === 0) return;
    this.saving.set(true);
    this.error.set('');
    this.api.createCredentials({ studentIds: this.selectedStudentIds(), ...this.form.getRawValue() }).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (created) => { this.credentials.update((items) => [...created, ...items]); this.form.reset({ credentialType: 'DIPLOMA', title: '' }); this.selectedStudentIds.set([]); },
      error: e => this.error.set(e.error?.message ?? 'No se pudo crear el borrador.'),
    });
  }

  toggleStudent(id: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedStudentIds.update(ids => checked ? [...new Set([...ids, id])] : ids.filter(value => value !== id));
  }

  isSelected(id: string): boolean { return this.selectedStudentIds().includes(id); }
  selectAll(): void { this.selectedStudentIds.set(this.students().map(student => student.id)); }

  generate(item: Credential): void {
    this.generating.set(item.id); this.error.set('');
    this.api.generateCredentialDocument(item.id).pipe(finalize(() => this.generating.set(''))).subscribe({
      next: updated => this.replace(updated),
      error: e => this.error.set(e.error?.message ?? 'No se pudo generar el PDF institucional.'),
    });
  }

  download(item: Credential): void {
    this.api.downloadCredentialDocument(item.id).subscribe({ next: response => {
      const url = URL.createObjectURL(response.body!); const link = document.createElement('a');
      link.href = url; link.download = item.documentName ?? 'credencial.pdf'; link.click(); URL.revokeObjectURL(url);
    }, error: () => this.error.set('No se pudo descargar el documento.') });
  }

  issue(item: Credential): void {
    this.error.set('');
    this.api.issueCredential(item.id).subscribe({next:updated=>this.replace(updated),
      error:e=>this.error.set(e.error?.message ?? 'No se pudo emitir la credencial.')});
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

  private loadStudents(): void {
    this.api.listStudents().subscribe({ next: (items) => { this.students.set(items); this.loadingStudents.set(false); }, error: () => this.loadingStudents.set(false) });
  }

  private replace(updated: Credential): void {
    this.credentials.update((items) => items.map((item) => item.id === updated.id ? updated : item));
  }
}
