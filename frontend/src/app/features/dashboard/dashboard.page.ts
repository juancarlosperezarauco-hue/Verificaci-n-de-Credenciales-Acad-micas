import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Credential, Student } from '../../core/models';

@Component({
  imports: [RouterLink],
  template: `
    <section class="mx-auto max-w-7xl px-6 py-12">
      <div class="flex flex-wrap items-end justify-between gap-5">
        <div><p class="text-sm font-semibold text-cyan-300">{{ auth.current()?.institutionType }} · Resumen institucional</p><h1 class="mt-2 text-4xl font-black">{{ auth.current()?.institutionName || 'Panel de control' }}</h1></div>
        <div class="flex flex-wrap gap-3"><a routerLink="/students" class="rounded-xl border border-white/15 px-5 py-3 font-bold">Vincular estudiante</a><a routerLink="/credentials" class="rounded-xl bg-cyan-400 px-5 py-3 font-bold text-slate-950">Nueva credencial</a></div>
      </div>
      <div class="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <article class="rounded-2xl border border-cyan-400/20 bg-cyan-400/5 p-6"><p class="text-cyan-300">Estudiantes</p><p class="mt-3 text-4xl font-black">{{ students().length }}</p></article>
        <article class="rounded-2xl border border-white/10 bg-slate-900 p-6"><p class="text-slate-400">Credenciales</p><p class="mt-3 text-4xl font-black">{{ credentials().length }}</p></article>
        <article class="rounded-2xl border border-emerald-400/20 bg-emerald-400/5 p-6"><p class="text-emerald-300">Emitidas</p><p class="mt-3 text-4xl font-black">{{ issued() }}</p></article>
        <article class="rounded-2xl border border-amber-400/20 bg-amber-400/5 p-6"><p class="text-amber-300">Borradores</p><p class="mt-3 text-4xl font-black">{{ drafts() }}</p></article>
      </div>
      <div class="mt-8 rounded-2xl border border-white/10 bg-slate-900 p-7">
        <h2 class="text-xl font-bold">Actividad reciente</h2>
        @if (loading()) { <p class="mt-5 text-slate-400">Cargando información…</p> }
        @if (!loading() && credentials().length === 0) { <p class="mt-5 text-slate-400">Todavía no hay credenciales registradas.</p> }
        <div class="mt-5 divide-y divide-white/10">
          @for (item of credentials().slice(0, 5); track item.id) {
            <div class="flex items-center justify-between gap-4 py-4"><div><p class="font-semibold">{{ item.title }}</p><p class="text-sm text-slate-400">{{ item.holderName }}</p></div><span class="rounded-full bg-white/10 px-3 py-1 text-xs">{{ item.status }}</span></div>
          }
        </div>
      </div>
    </section>
  `,
})
export class DashboardPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly credentials = signal<Credential[]>([]);
  readonly students = signal<Student[]>([]);
  readonly loading = signal(true);
  readonly issued = computed(() => this.credentials().filter((item) => item.status === 'ISSUED').length);
  readonly drafts = computed(() => this.credentials().filter((item) => item.status === 'DRAFT').length);

  ngOnInit(): void {
    this.api.listStudents().subscribe({ next: (items) => this.students.set(items) });
    this.api.listCredentials().subscribe({
      next: (items) => { this.credentials.set(items); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
