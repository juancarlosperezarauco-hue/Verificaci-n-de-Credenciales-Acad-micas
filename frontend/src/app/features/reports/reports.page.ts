import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import {
  Chart,
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
  ChartData,
  ChartOptions,
} from 'chart.js';
import { Page, cycleError, downloadBlob } from '../academic/academic.api';
import { ReportsApi, ReportFilters, ReportRow, Summary } from './reports.api';
Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);
@Component({
  imports: [DatePipe, DecimalPipe, FormsModule, BaseChartDirective],
  templateUrl: './reports.page.html',
})
export class ReportsPage {
  private readonly api = inject(ReportsApi);
  private readonly destroy = inject(DestroyRef);
  readonly summary = signal<Summary | null>(null);
  readonly rows = signal<Page<ReportRow>>({ items: [], total: 0, page: 0, size: 20 });
  readonly loading = signal(false);
  readonly ready = signal(false);
  readonly error = signal('');
  readonly exporting = signal('');
  readonly chart = signal<ChartData<'bar'>>({ labels: [], datasets: [] });
  readonly chartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
  };
  readonly types = ['DIPLOMA', 'CERTIFICADO', 'TITULO', 'RECONOCIMIENTO', 'CONSTANCIA'];
  filters: ReportFilters = { year: '', type: '', status: '' };
  private applied: ReportFilters = { ...this.filters };
  constructor() {
    this.load();
  }
  load(page = 0, apply = true) {
    if (this.loading() || this.exporting()) return;
    const filters = apply ? { ...this.filters } : { ...this.applied };
    this.loading.set(true);
    this.ready.set(false);
    this.error.set('');
    forkJoin({ summary: this.api.summary(filters), rows: this.api.rows(filters, page) })
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.applied = filters;
          this.summary.set(r.summary);
          this.rows.set(r.rows);
          this.ready.set(true);
          this.chart.set({
            labels: r.summary.byMonth.map((x) => x.label),
            datasets: [
              {
                data: r.summary.byMonth.map((x) => x.total),
                backgroundColor: '#2563eb',
                borderRadius: 5,
              },
            ],
          });
        },
        error: (e) => this.error.set(cycleError(e)),
      });
  }
  export(format: 'pdf' | 'xlsx') {
    if (this.loading() || this.exporting() || !this.ready()) return;
    this.exporting.set(format);
    this.error.set('');
    this.api
      .export(this.applied, format)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.exporting.set('')),
      )
      .subscribe({
        next: (blob) => downloadBlob(blob, 'credenciales.' + format),
        error: async (e) => {
          let message = 'No se pudo exportar. Si hay más de 1000 registros, aplica filtros.';
          if (e.error instanceof Blob) {
            try {
              message = JSON.parse(await e.error.text()).message ?? message;
            } catch {}
          }
          this.error.set(message);
        },
      });
  }
}
