import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Student } from '../../core/models';
import { OrganizationApi } from '../organization/organization.api';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-7xl px-6 py-12">
      <div class="flex flex-wrap items-end justify-between gap-5">
        <div><p class="text-sm font-semibold text-cyan-300">{{ auth.current()?.institutionName }}</p><h1 class="mt-2 text-4xl font-black">Estudiantes autorizados</h1><p class="mt-3 text-slate-400">Pega el código de búsqueda que te entregue el estudiante para registrarlo en tu institución.</p></div>
        @if (students().length > 0 && access.can('CREDENTIAL_VIEW') && access.can('CREDENTIAL_CREATE')) { <a routerLink="/credentials" class="rounded-xl bg-cyan-400 px-5 py-3 font-bold text-slate-950">Emitir credencial</a> }
      </div>
      <div class="mt-10 grid gap-8 lg:grid-cols-[380px_1fr]">
        @if(access.can('STUDENT_LINK')){<form [formGroup]="form" (ngSubmit)="create()" class="h-fit rounded-2xl border border-white/10 bg-slate-900 p-6">
          <h2 class="text-xl font-bold">Registrar por código de búsqueda</h2>
          <p class="mt-3 text-sm text-slate-400">El código EST- identifica la cuenta del estudiante. Al registrarlo verás su nombre y documento. También puedes usar un código temporal destinado a esta institución.</p>
          <div class="mt-6 space-y-4 text-sm">
            <label class="block">Código del estudiante<input formControlName="code" class="field" placeholder="EST-…" maxlength="100" autocomplete="off"></label>
          </div>
          @if (error()) { <p class="mt-4 rounded-xl bg-red-500/10 p-3 text-sm text-red-300">{{ error() }}</p> }
          <button [disabled]="form.invalid || saving()" class="mt-6 w-full rounded-xl bg-cyan-400 px-4 py-3 font-bold text-slate-950 disabled:opacity-50">{{ saving() ? 'Buscando…' : 'Buscar y registrar' }}</button>
          @if(success()){<p role="status" class="mt-4 text-sm text-emerald-300">{{success()}}</p>}
        </form>}
        <div class="overflow-hidden rounded-2xl border border-white/10 bg-slate-900">
          <div class="border-b border-white/10 px-6 py-5"><h2 class="text-xl font-bold">Padrón institucional</h2><p class="mt-1 text-sm text-slate-400">{{ students().length }} estudiante(s)</p></div>
          @if (loading()) { <p class="p-6 text-slate-400">Cargando…</p> }
          @if (!loading() && students().length === 0) { <p class="p-6 text-slate-400">Aún no hay estudiantes registrados.</p> }
          <div class="divide-y divide-white/10">
            @for (student of students(); track student.id) {
              <article class="grid gap-3 p-6 sm:grid-cols-[1fr_auto] sm:items-center">
                <div><h3 class="font-bold">{{ student.fullName }}</h3><p class="mt-1 text-sm text-slate-400">{{ student.country }} · {{ student.documentType }}: {{ student.documentNumber }}</p></div>
                <span class="w-fit rounded-full bg-emerald-400/10 px-3 py-1 text-xs font-semibold text-emerald-300">Activo</span>
              </article>
            }
          </div>
        </div>
      </div>
    </section>
  `,
})
export class StudentsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly access = inject(OrganizationApi);
  readonly students = signal<Student[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern('\\s*([Ee][Ss][Tt]-)?[a-fA-F0-9]{32}\\s*')]],
  });

  ngOnInit(): void { this.reload(); }

  create(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.api.redeemStudent(this.form.getRawValue().code.trim().toUpperCase()).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (student) => { this.students.update((items) => [...items.filter(s=>s.id!==student.id), student].sort((a, b) => a.fullName.localeCompare(b.fullName))); this.form.reset(); this.success.set(student.fullName + ' ya está registrado en tu institución.'); },
      error: (response) => this.error.set(response.error?.message ?? 'No se pudo consultar el código.'),
    });
  }

  private reload(): void {
    this.api.listStudents().subscribe({ next: (items) => { this.students.set(items); this.loading.set(false); }, error: () => this.loading.set(false) });
  }
}
