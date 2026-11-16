import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ReportsApi, PlatformStatistics } from './reports.api';
import { cycleError } from '../academic/academic.api';
@Component({
  imports: [DecimalPipe],
  template: `<section class="org-page">
    <div class="page-heading">
      <div>
        <p class="org-eyebrow">Administración global</p>
        <h1 class="org-title">Estadísticas de la plataforma</h1>
        <p class="org-description">
          Totales agregados sin acceder a los expedientes de las instituciones.
        </p>
      </div>
      <button class="org-secondary" [disabled]="loading()" (click)="load()">Actualizar</button>
    </div>
    @if (error()) {
      <p role="alert" class="org-error mt-5">{{ error() }}</p>
    }
    @if (data(); as d) {
      <div class="mt-7 grid gap-5 sm:grid-cols-3">
        <article class="org-card">
          <p>Organizaciones</p>
          <strong class="mt-3 block text-3xl">{{ d.organizations | number }}</strong>
        </article>
        <article class="org-card">
          <p>Identidades estudiantiles globales</p>
          <strong class="mt-3 block text-3xl">{{ d.students | number }}</strong>
        </article>
        <article class="org-card">
          <p>Credenciales registradas</p>
          <strong class="mt-3 block text-3xl">{{ d.credentials | number }}</strong>
        </article>
      </div>
      <section class="org-card mt-7">
        <h2 class="text-xl font-semibold">Organizaciones por estado</h2>
        @for (g of d.byStatus; track g.label) {
          <p class="mt-4 flex justify-between border-b border-slate-100 pb-3">
            <span>{{ g.label }}</span
            ><strong>{{ g.total | number }}</strong>
          </p>
        } @empty {
          <p class="mt-4 text-slate-500">Todavía no hay organizaciones.</p>
        }
      </section>
    }
  </section>`,
})
export class PlatformStatisticsPage {
  private readonly api = inject(ReportsApi);
  private readonly destroy = inject(DestroyRef);
  readonly data = signal<PlatformStatistics | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  constructor() {
    this.load();
  }
  load() {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.api
      .platform()
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({ next: (r) => this.data.set(r), error: (e) => this.error.set(cycleError(e)) });
  }
}
