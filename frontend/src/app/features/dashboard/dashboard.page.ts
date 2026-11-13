import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { OrganizationApi } from '../organization/organization.api';
import { Credential, PortalItem, PortalItemType, Student } from '../../core/models';

@Component({
  imports: [RouterLink, ReactiveFormsModule],
  template: `
    <section class="mx-auto max-w-7xl px-6 py-10 sm:py-14">
      <div class="brand-gradient overflow-hidden rounded-3xl p-[2px] shadow-2xl">
        <div class="relative overflow-hidden rounded-[calc(1.5rem-2px)] bg-slate-950 px-7 py-10 sm:px-12 sm:py-14">
          <div class="absolute -right-20 -top-24 size-72 rounded-full opacity-20 blur-3xl" [style.background]="'var(--brand-accent)'" aria-hidden="true"></div>
          <div class="relative max-w-4xl">
            <p class="brand-text text-sm font-black uppercase tracking-[.2em]">{{ auth.current()?.institutionType }} · Portal institucional</p>
            <h1 class="mt-4 text-4xl font-black leading-tight sm:text-6xl">Centro de Gestión Académica</h1>
            <p class="mt-5 max-w-3xl text-lg leading-8 text-slate-300">Administra estudiantes, publicaciones, fechas académicas y credenciales verificables desde un solo lugar para {{ auth.current()?.institutionName }}.</p>
            <div class="mt-8 flex flex-wrap gap-3"><a routerLink="/credentials" class="brand-bg rounded-xl px-5 py-3 font-bold text-slate-950">Emitir credencial</a><a routerLink="/students" class="rounded-xl border border-white/20 px-5 py-3 font-bold">Registrar estudiante</a>@if (canManage()) { <button (click)="toggleEditor()" class="rounded-xl border border-white/20 px-5 py-3 font-bold">{{ showEditor() ? 'Cerrar editor' : 'Publicar contenido' }}</button> }</div>
          </div>
        </div>
      </div>

      @if (showEditor()) {
        <form [formGroup]="portalForm" (ngSubmit)="publish()" class="mt-8 rounded-3xl border border-white/10 bg-slate-900 p-7">
          <div class="flex flex-wrap items-center justify-between gap-4"><div><p class="brand-text text-sm font-bold">Administración del portal</p><h2 class="mt-1 text-2xl font-black">Nueva publicación</h2></div><span class="text-xs text-slate-500">Visible solamente dentro de esta institución</span></div>
          <div class="mt-6 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <label class="text-sm">Tipo<select formControlName="itemType" class="field"><option value="ANNOUNCEMENT">Aviso</option><option value="DEADLINE">Fecha importante</option><option value="RESOURCE">Recurso</option></select></label>
            <label class="text-sm lg:col-span-2">Título<input formControlName="title" class="field" placeholder="Ej. Cierre de emisión de diplomas"></label>
            <label class="text-sm">Prioridad<select formControlName="priority" class="field"><option [value]="0">Normal</option><option [value]="5">Importante</option><option [value]="10">Urgente</option></select></label>
            <label class="text-sm md:col-span-2">Descripción<textarea formControlName="description" class="field min-h-28 resize-y" placeholder="Información que debe conocer el personal académico"></textarea></label>
            <label class="text-sm">Fecha y hora<input type="datetime-local" formControlName="eventAt" class="field"></label>
            <label class="text-sm">Enlace opcional<input formControlName="link" class="field" placeholder="https://… o /credentials"></label>
          </div>
          <div class="mt-5 flex items-center gap-4"><button [disabled]="portalForm.invalid || publishing()" class="brand-bg rounded-xl px-5 py-3 font-bold text-slate-950 disabled:opacity-40">{{ publishing() ? 'Publicando…' : 'Publicar' }}</button>@if (portalError()) { <p class="text-sm text-red-300">{{ portalError() }}</p> }</div>
        </form>
      }

      <div class="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <article class="rounded-2xl border p-6" [style.border-color]="'color-mix(in srgb, var(--brand-primary) 35%, transparent)'" [style.background]="'color-mix(in srgb, var(--brand-primary) 7%, #0f172a)'"><p class="brand-text">Estudiantes registrados</p><p class="mt-3 text-4xl font-black">{{ students().length }}</p></article>
        <article class="rounded-2xl border border-white/10 bg-slate-900 p-6"><p class="text-slate-400">Credenciales totales</p><p class="mt-3 text-4xl font-black">{{ credentials().length }}</p></article>
        <article class="rounded-2xl border border-emerald-400/20 bg-emerald-400/5 p-6"><p class="text-emerald-300">Credenciales emitidas</p><p class="mt-3 text-4xl font-black">{{ issued() }}</p></article>
        <article class="rounded-2xl border border-amber-400/20 bg-amber-400/5 p-6"><p class="text-amber-300">Pendientes de emisión</p><p class="mt-3 text-4xl font-black">{{ drafts() }}</p></article>
      </div>

      <div class="mt-12 grid gap-10 lg:grid-cols-[1.25fr_.75fr]">
        <section>
          <div class="flex items-end justify-between gap-4"><div><p class="brand-text text-sm font-bold uppercase tracking-wider">Lo que necesitas saber</p><h2 class="mt-2 text-3xl font-black">Avisos institucionales</h2></div><span class="text-sm text-slate-500">{{ announcements().length }} publicados</span></div>
          @if (loading()) { <p class="mt-6 text-slate-400">Cargando información…</p> }
          @if (!loading() && announcements().length === 0) { <div class="mt-6 rounded-2xl border border-dashed border-white/15 p-7 text-slate-400">Aún no hay avisos. El administrador puede publicar el primero desde “Publicar contenido”.</div> }
          <div class="mt-6 grid gap-4">
            @for (item of announcements(); track item.id) {
              <article class="group rounded-2xl border border-white/10 bg-slate-900 p-6 transition hover:border-white/20"><div class="flex items-start justify-between gap-5"><div><div class="flex flex-wrap items-center gap-2"><span class="rounded-full px-3 py-1 text-xs font-bold" [class]="item.priority >= 10 ? 'bg-red-400/10 text-red-300' : item.priority >= 5 ? 'bg-amber-400/10 text-amber-300' : 'bg-white/10 text-slate-300'">{{ item.priority >= 10 ? 'URGENTE' : item.priority >= 5 ? 'IMPORTANTE' : 'AVISO' }}</span><span class="text-xs text-slate-500">{{ formatDate(item.createdAt, false) }}</span></div><h3 class="mt-4 text-xl font-bold">{{ item.title }}</h3>@if (item.description) { <p class="mt-2 leading-7 text-slate-400">{{ item.description }}</p> }@if (item.link) { <a [href]="item.link" class="brand-text mt-4 inline-block text-sm font-bold">Abrir información →</a> }</div>@if (canManage()) { <button (click)="remove(item)" class="rounded-lg px-3 py-2 text-sm text-slate-500 hover:bg-red-400/10 hover:text-red-300" aria-label="Eliminar aviso">Eliminar</button> }</div></article>
            }
          </div>
        </section>

        <aside>
          <p class="brand-text text-sm font-bold uppercase tracking-wider">Calendario académico</p><h2 class="mt-2 text-3xl font-black">Próximas fechas</h2>
          @if (!loading() && deadlines().length === 0) { <div class="mt-6 rounded-2xl border border-dashed border-white/15 p-6 text-sm text-slate-400">No existen fechas programadas.</div> }
          <div class="mt-6 space-y-3">
            @for (item of deadlines(); track item.id) {
              <article class="grid grid-cols-[68px_1fr] overflow-hidden rounded-2xl border border-white/10 bg-slate-900"><div class="brand-bg grid place-items-center p-3 text-center text-slate-950"><span class="text-xs font-black uppercase">{{ month(item.eventAt) }}</span><strong class="text-3xl leading-none">{{ day(item.eventAt) }}</strong></div><div class="p-4"><div class="flex justify-between gap-3"><h3 class="font-bold">{{ item.title }}</h3>@if (canManage()) { <button (click)="remove(item)" class="text-xs text-slate-500 hover:text-red-300">Eliminar</button> }</div>@if (item.description) { <p class="mt-1 text-sm text-slate-400">{{ item.description }}</p> }<p class="mt-2 text-xs text-slate-500">{{ formatDate(item.eventAt, true) }}</p></div></article>
            }
          </div>
        </aside>
      </div>

      <section class="mt-14">
        <p class="brand-text text-sm font-bold uppercase tracking-wider">Referencias y servicios</p><h2 class="mt-2 text-3xl font-black">Accesos rápidos</h2>
        <div class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <a routerLink="/students" class="resource-card"><span class="resource-icon">ES</span><span><strong>Estudiantes</strong><small>Buscar, vincular y consultar</small></span></a>
          <a routerLink="/credentials" class="resource-card"><span class="resource-icon">CR</span><span><strong>Credenciales</strong><small>Diplomas y certificados</small></span></a>
          <a routerLink="/institution" class="resource-card"><span class="resource-icon">IN</span><span><strong>Institución</strong><small>Logo, colores y respaldo</small></span></a>
          <a routerLink="/verify" class="resource-card"><span class="resource-icon">VE</span><span><strong>Verificación</strong><small>Consulta pública por código</small></span></a>
          @for (item of resources(); track item.id) { <div class="resource-card"><a [href]="item.link || '#'" class="flex min-w-0 flex-1 items-center gap-4"><span class="resource-icon">RE</span><span class="min-w-0"><strong>{{ item.title }}</strong><small>{{ item.description || 'Recurso institucional' }}</small></span></a>@if (canManage()) { <button (click)="remove(item)" class="ml-auto text-xs text-slate-500 hover:text-red-300">Eliminar</button> }</div> }
        </div>
      </section>

      <section class="mt-14 rounded-3xl border border-white/10 bg-slate-900 p-7"><div class="flex items-center justify-between gap-4"><div><p class="brand-text text-sm font-bold">Actividad reciente</p><h2 class="mt-1 text-2xl font-black">Últimas credenciales</h2></div><a routerLink="/credentials" class="text-sm font-bold text-slate-300">Ver todas →</a></div>@if (!loading() && credentials().length === 0) { <p class="mt-5 text-slate-400">Todavía no hay credenciales registradas.</p> }<div class="mt-5 divide-y divide-white/10">@for (item of credentials().slice(0, 5); track item.id) { <div class="flex items-center justify-between gap-4 py-4"><div><p class="font-semibold">{{ item.title }}</p><p class="text-sm text-slate-400">{{ item.holderName }} · {{ item.credentialType }}</p></div><span [class]="statusClass(item.status)">{{ statusLabel(item.status) }}</span></div> }</div></section>
    </section>
  `,
  styles: [`
    .resource-card { display:flex; align-items:center; gap:1rem; min-height:6.5rem; padding:1.2rem; border:1px solid rgb(255 255 255 / .1); border-radius:1rem; background:#0f172a; }
    .resource-card:hover { border-color:color-mix(in srgb, var(--brand-primary) 45%, transparent); transform:translateY(-2px); }
    .resource-card strong,.resource-card small { display:block; }
    .resource-card small { margin-top:.25rem; color:#94a3b8; font-size:.75rem; }
    .resource-icon { display:grid; place-items:center; width:2.75rem; height:2.75rem; flex:none; border-radius:.8rem; background:color-mix(in srgb, var(--brand-primary) 16%, transparent); color:var(--brand-primary); font-weight:900; font-size:.75rem; }
  `],
})
export class DashboardPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  readonly auth = inject(AuthService);
  readonly access = inject(OrganizationApi);
  readonly credentials = signal<Credential[]>([]);
  readonly students = signal<Student[]>([]);
  readonly portalItems = signal<PortalItem[]>([]);
  readonly loading = signal(true);
  readonly publishing = signal(false);
  readonly showEditor = signal(false);
  readonly portalError = signal('');
  readonly issued = computed(() => this.credentials().filter(item => item.status === 'ISSUED').length);
  readonly drafts = computed(() => this.credentials().filter(item => item.status === 'DRAFT').length);
  readonly announcements = computed(() => this.portalItems().filter(item => item.itemType === 'ANNOUNCEMENT'));
  readonly deadlines = computed(() => this.portalItems().filter(item => item.itemType === 'DEADLINE'
      && new Date(item.eventAt ?? 0).getTime() >= Date.now() - 86_400_000)
    .sort((a, b) => new Date(a.eventAt ?? 0).getTime() - new Date(b.eventAt ?? 0).getTime()));
  readonly resources = computed(() => this.portalItems().filter(item => item.itemType === 'RESOURCE'));
  readonly portalForm = this.fb.nonNullable.group({
    itemType: ['ANNOUNCEMENT' as PortalItemType, Validators.required],
    title: ['', [Validators.required, Validators.maxLength(180)]],
    description: ['', Validators.maxLength(1200)],
    eventAt: [''], link: ['', Validators.maxLength(500)], priority: [0],
  });

  ngOnInit(): void {
    forkJoin({ students: this.access.can('STUDENT_VIEW')?this.api.listStudents():of([] as Student[]), credentials: this.access.can('CREDENTIAL_VIEW')?this.api.listCredentials():of([] as Credential[]), portal: this.api.listPortalItems() })
      .subscribe({ next: result => { this.students.set(result.students); this.credentials.set(result.credentials); this.portalItems.set(result.portal); this.loading.set(false); }, error: () => this.loading.set(false) });
  }

  canManage(): boolean { return ['ADMIN', 'TENANT_ADMIN'].includes(this.auth.current()?.role ?? ''); }
  toggleEditor(): void { this.showEditor.update(value => !value); }

  publish(): void {
    if (this.portalForm.invalid) return;
    const value = this.portalForm.getRawValue();
    if (value.itemType === 'DEADLINE' && !value.eventAt) { this.portalError.set('Indica la fecha y hora del evento.'); return; }
    this.publishing.set(true); this.portalError.set('');
    this.api.createPortalItem({ ...value, eventAt: value.eventAt ? new Date(value.eventAt).toISOString() : undefined })
      .pipe(finalize(() => this.publishing.set(false))).subscribe({
        next: item => { this.portalItems.update(items => [item, ...items]); this.portalForm.reset({ itemType: 'ANNOUNCEMENT', title: '', description: '', eventAt: '', link: '', priority: 0 }); this.showEditor.set(false); },
        error: error => this.portalError.set(error.error?.message ?? 'No se pudo publicar el contenido.'),
      });
  }

  remove(item: PortalItem): void {
    if (!window.confirm(`¿Retirar “${item.title}” del portal?`)) return;
    this.api.deletePortalItem(item.id).subscribe({ next: () => this.portalItems.update(items => items.filter(value => value.id !== item.id)) });
  }

  formatDate(value?: string, includeTime = false): string {
    if (!value) return '';
    return new Intl.DateTimeFormat('es-BO', { dateStyle: 'long', ...(includeTime ? { timeStyle: 'short' as const } : {}) }).format(new Date(value));
  }
  month(value?: string): string { return value ? new Intl.DateTimeFormat('es-BO', { month: 'short' }).format(new Date(value)).replace('.', '') : ''; }
  day(value?: string): string { return value ? String(new Date(value).getDate()).padStart(2, '0') : ''; }
  statusLabel(status: Credential['status']): string { return ({ DRAFT: 'BORRADOR', ISSUED: 'EMITIDA', REVOKED: 'REVOCADA' })[status]; }
  statusClass(status: Credential['status']): string { return `rounded-full px-3 py-1 text-xs font-bold ${status === 'ISSUED' ? 'bg-emerald-400/10 text-emerald-300' : status === 'REVOKED' ? 'bg-red-400/10 text-red-300' : 'bg-amber-400/10 text-amber-300'}`; }
}
