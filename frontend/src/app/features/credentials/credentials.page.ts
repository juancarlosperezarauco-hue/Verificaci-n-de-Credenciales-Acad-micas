import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import QRCode from 'qrcode';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Credential, Student } from '../../core/models';
import { OrganizationApi } from '../organization/organization.api';
import { DocumentTemplate, TemplatesApi } from '../templates/templates.api';
import { cycleError, downloadBlob } from '../academic/academic.api';

@Component({
  imports: [ReactiveFormsModule, FormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-7xl px-6 py-12">
      <div><p class="org-eyebrow">Emisión institucional</p><h1 class="org-title">Diplomas y certificados</h1><p class="org-description">Genera el PDF, comprueba su contenido, fírmalo si corresponde y confirma su emisión. Las solicitudes aprobadas aparecen aquí como borradores.</p></div>
      @if(error()){<p role="alert" class="org-error mt-5">{{error()}}</p>}
      <div class="mt-10 grid gap-8 lg:grid-cols-[390px_1fr]">
        @if(access.can('CREDENTIAL_CREATE')){<form [formGroup]="form" (ngSubmit)="create()" class="h-fit rounded-2xl border border-slate-200 bg-white p-6">
          <h2 class="text-xl font-bold">Crear certificados</h2>
          <div class="mt-6 space-y-4 text-sm">
            <fieldset><legend class="font-semibold">Estudiantes seleccionados ({{ selectedStudentIds().length }})</legend><div class="mt-2 max-h-52 space-y-2 overflow-auto rounded-xl border border-slate-200 bg-slate-50 p-3">
              @for (student of students(); track student.id) { <label class="flex cursor-pointer items-start gap-3 rounded-lg p-2 hover:bg-white/5"><input type="checkbox" class="mt-1 size-4" [checked]="isSelected(student.id)" (change)="toggleStudent(student.id, $event)"><span><span class="block font-semibold">{{ student.fullName }}</span><span class="text-xs text-slate-500">{{ student.documentType }} {{ student.documentNumber }}</span></span></label> }
              @if (!loadingStudents() && students().length === 0) { <span class="text-slate-500">No hay estudiantes vinculados.</span> }
            </div><div class="mt-2 flex gap-3 text-xs"><button type="button" (click)="selectAll()" class="brand-text">Seleccionar todos</button><button type="button" (click)="selectedStudentIds.set([])" class="text-slate-600">Limpiar</button></div></fieldset>
            <label class="block">Tipo<select formControlName="credentialType" class="field"><option value="DIPLOMA">Diploma</option><option value="CERTIFICADO">Certificado</option><option value="TITULO">Título</option><option value="RECONOCIMIENTO">Reconocimiento</option><option value="CONSTANCIA">Constancia</option></select></label>
            <label class="block">Nombre del programa o reconocimiento<input formControlName="title" class="field"></label>
          </div>
          @if (!loadingStudents() && students().length === 0) { <p class="mt-5 rounded-xl bg-amber-400/10 p-3 text-sm text-amber-800">Primero debes <a routerLink="/students" class="font-bold underline">vincular un estudiante con su código</a>.</p> }
          <button [disabled]="form.invalid || selectedStudentIds().length === 0 || saving()" class="brand-bg mt-6 w-full rounded-xl px-4 py-3 font-bold text-slate-950 disabled:opacity-50">{{ saving() ? 'Creando…' : 'Crear ' + selectedStudentIds().length + ' borrador(es)' }}</button>
          @if(error()){<p role="alert" class="mt-4 text-sm text-red-700">{{error()}}</p>}
        </form>}

        <div class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <div class="border-b border-slate-200 px-6 py-5"><h2 class="text-xl font-bold">Documentos registrados</h2></div>
          @if (loading()) { <p class="p-6 text-slate-600">Cargando…</p> }
          @if (!loading() && credentials().length === 0) { <p class="p-6 text-slate-600">No existen credenciales todavía.</p> }
          <div class="divide-y divide-slate-200">
            @for (item of credentials(); track item.id) {
              <article class="grid gap-4 p-6 sm:grid-cols-[1fr_auto] sm:items-center">
                <div><div class="flex flex-wrap items-center gap-3"><h3 class="font-bold">{{ item.title }}</h3><span [class]="statusClass(item.status)">{{ item.status }}</span></div><p class="mt-1 text-sm text-slate-600">{{ item.holderName }} · {{ item.credentialType }}</p>@if (item.documentName) { <p class="brand-text mt-2 text-xs">Documento: {{ item.documentName }}</p> }@if(item.signedAt){<p class="mt-2 text-sm font-semibold text-blue-700">PDF firmado {{item.testSignature?'· Certificado de PRUEBA':''}}</p>}@if(item.sourceRequestId){<p class="mt-2 text-xs text-slate-500">Procede de una solicitud aprobada.</p>}@if (item.verificationCode) { <p class="mt-2 font-mono text-xs text-slate-500">{{ item.verificationCode }}</p> }</div>
                <div class="flex max-w-xl flex-wrap items-end gap-2">
                  @if (item.status === 'DRAFT') {
                    @if(access.can('CREDENTIAL_CREATE')){
                      @if(!item.sourceRequestId&&templates().length){<label class="text-xs">Plantilla<select class="field" [ngModel]="selectedTemplate[item.id]??''" (ngModelChange)="selectedTemplate[item.id]=$event" [disabled]="!!generating()"><option value="">{{item.templateVersionId?'Mantener plantilla actual':'Predeterminada'}}</option>@for(t of templates();track t.id){@if(t.active&&t.credentialType===item.credentialType){<option [value]="t.id">{{t.name}} · v{{t.version}}</option>}}</select></label>}
                      <button (click)="generate(item)" [disabled]="!!generating()" class="org-secondary">{{ generating() === item.id ? 'Procesando…' : 'Generar PDF' }}</button><label class="cursor-pointer rounded-lg border border-slate-300 px-3 py-2 text-sm">{{ item.documentName ? 'Reemplazar PDF' : 'Adjuntar PDF propio' }}<input type="file" accept="application/pdf,.pdf" [disabled]="!!generating()" class="hidden" (change)="upload(item, $event)"></label>}
                    @if(access.can('CREDENTIAL_SIGN')){<button (click)="sign(item)" [disabled]="!!generating()||!item.documentHash||!!item.signedAt" class="org-secondary">Firmar PDF</button>}
                    @if(access.can('CREDENTIAL_ISSUE')){<button (click)="issue(item)" [disabled]="!!generating()||!item.documentHash" class="org-primary">Emitir</button>}
                  }
                  @if (item.documentName) { <button (click)="download(item)" class="rounded-lg border border-slate-300 px-3 py-2 text-sm">Descargar</button> }
                  @if (item.status === 'ISSUED') { <button (click)="showQr(item)" class="rounded-lg border border-slate-300 px-3 py-2 text-sm">Ver QR</button>@if(access.can('CREDENTIAL_REVOKE')){<button (click)="revoke(item)" class="rounded-lg border border-red-400/30 px-3 py-2 text-sm text-red-700">Revocar</button>} }
                </div>
              </article>
            }
          </div>
        </div>
      </div>
      @if (qrData()) {
        <div class="fixed inset-0 grid place-items-center bg-slate-950/80 p-6" (click)="qrData.set('')">
          <div class="rounded-2xl bg-white p-7 text-center text-slate-950" (click)="$event.stopPropagation()"><h2 class="text-xl font-bold">Código de verificación</h2><img [src]="qrData()" alt="QR de la credencial" class="mx-auto mt-5 size-64"><button (click)="qrData.set('')" class="mt-5 rounded-lg bg-blue-700 px-4 py-2 text-white">Cerrar</button></div>
        </div>
      }
    </section>
  `,
})
export class CredentialsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  readonly access = inject(OrganizationApi);
  readonly credentials = signal<Credential[]>([]);
  readonly students = signal<Student[]>([]);
  readonly loading = signal(true);
  readonly loadingStudents = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly qrData = signal('');
  readonly selectedStudentIds = signal<string[]>([]);
  readonly generating = signal('');
  private readonly templateApi=inject(TemplatesApi);
  readonly templates=signal<DocumentTemplate[]>([]);
  readonly selectedTemplate:Partial<Record<string,string>>={};
  private readonly issueKeys=new Map<string,string>();
  readonly form = this.fb.nonNullable.group({
    credentialType: ['DIPLOMA', Validators.required],
    title: ['', Validators.required],
  });

  ngOnInit(): void { this.reload(); if(this.access.can('STUDENT_VIEW'))this.loadStudents();else this.loadingStudents.set(false); if(this.access.can('TEMPLATE_VIEW'))this.templateApi.list().subscribe({next:r=>this.templates.set(r),error:e=>this.error.set(cycleError(e))}); }

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
    if(this.generating())return;
    if(item.documentName&&!window.confirm('El PDF actual y su firma serán reemplazados por un PDF nuevo. ¿Continuar?'))return;
    this.generating.set(item.id); this.error.set('');
    this.api.generateCredentialDocument(item.id,this.selectedTemplate[item.id]||undefined).pipe(finalize(() => this.generating.set(''))).subscribe({
      next: updated => this.replace(updated),
      error: e => this.error.set(e.error?.message ?? 'No se pudo generar el PDF institucional.'),
    });
  }

  download(item: Credential): void {
    this.api.downloadCredentialDocument(item.id).subscribe({ next: response => {
      downloadBlob(response.body!,item.documentName??'credencial.pdf');
    }, error: () => this.error.set('No se pudo descargar el documento.') });
  }

  issue(item: Credential): void {
    if(this.generating()||!window.confirm('Confirma que revisaste el PDF y su destinatario. La emisión impide modificarlo; una corrección requerirá un reemplazo. ¿Emitir?'))return;
    this.error.set('');this.generating.set(item.id);
    const key=this.issueKeys.get(item.id)??crypto.randomUUID();this.issueKeys.set(item.id,key);
    this.api.issueCredential(item.id,key).pipe(finalize(()=>this.generating.set(''))).subscribe({next:()=>this.reload(),
      error:e=>this.error.set(e.error?.message ?? 'No se pudo emitir la credencial.')});
  }

  sign(item:Credential){if(this.generating()||!window.confirm('Se firmará el PDF actual con el certificado configurado para esta institución. ¿Continuar?'))return;this.generating.set(item.id);this.error.set('');this.api.signCredential(item.id).pipe(finalize(()=>this.generating.set(''))).subscribe({next:r=>this.replace(r),error:e=>this.error.set(cycleError(e))});}

  upload(item: Credential, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if(this.generating()){input.value='';return;}
    if(file.size>10*1024*1024){this.error.set('El PDF debe tener hasta 10 MiB.');input.value='';return;}
    if(item.documentName&&!window.confirm('Reemplazar el PDF eliminará su firma actual. ¿Continuar?')){input.value='';return;}
    this.generating.set(item.id);this.error.set('');
    this.api.uploadDocument(item.id, file).pipe(finalize(()=>this.generating.set(''))).subscribe({
      next: (updated) => { this.replace(updated); input.value = ''; },
      error: e => { this.error.set(cycleError(e)); input.value = ''; },
    });
  }

  revoke(item: Credential): void {
    const reason = window.prompt('Motivo de revocación:');
    if (!reason?.trim()) return;
    if(this.generating())return;this.generating.set(item.id);
    this.api.revokeCredential(item.id, reason.trim()).pipe(finalize(()=>this.generating.set(''))).subscribe({next:updated=>this.replace(updated),error:e=>this.error.set(cycleError(e))});
  }

  async showQr(item: Credential): Promise<void> {
    this.qrData.set(await QRCode.toDataURL(`${window.location.origin}/verify/${item.verificationCode}`, { width: 320, margin: 1 }));
  }

  statusClass(status: Credential['status']): string {
    const color = status === 'ISSUED' ? 'bg-emerald-400/10 text-emerald-800' : status === 'REVOKED' ? 'bg-red-400/10 text-red-700' : 'bg-amber-400/10 text-amber-800';
    return `rounded-full px-3 py-1 text-xs font-semibold ${color}`;
  }

  private reload(): void {
    this.api.listCredentials().subscribe({ next: (items) => { this.credentials.set(items); this.loading.set(false); }, error: e => {this.loading.set(false);this.error.set(cycleError(e));} });
  }

  private loadStudents(): void {
    this.api.listStudents().subscribe({ next: (items) => { this.students.set(items); this.loadingStudents.set(false); }, error: () => this.loadingStudents.set(false) });
  }

  private replace(updated: Credential): void {
    this.credentials.update((items) => items.map((item) => item.id === updated.id ? updated : item));
  }
}
