import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { cycleError, downloadBlob } from './academic.api';
import { ImportRow, readAcademicCsv, IMPORT_HEADERS } from './import-csv';
interface Preview {
  batchId: string | null;
  rows: number;
  expiresAt: string | null;
  problems: { row: number; code: string }[];
}
@Component({
  imports: [DatePipe],
  templateUrl: './academic-import.page.html',
})
export class AcademicImportPage {
  private readonly http = inject(HttpClient);
  private readonly destroy = inject(DestroyRef);
  readonly rows = signal<ImportRow[]>([]);
  readonly preview = signal<Preview | null>(null);
  readonly applied = signal(0);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly filename = signal('');
  private revision = 0;
  readonly reasons: Record<string, string> = {
    STUDENT_NOT_LINKED: 'Estudiante sin vínculo vigente',
    PROGRAM_UNAVAILABLE: 'Programa inexistente, ajeno o archivado',
    REGISTRATION_EXISTS: 'Código de matrícula duplicado',
    ENROLLMENT_EXISTS: 'El estudiante ya está inscrito en ese programa',
  };
  async choose(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.busy()) return;
    const revision = ++this.revision;
    this.rows.set([]);
    this.preview.set(null);
    this.applied.set(0);
    this.error.set('');
    if (file.size > 131072) {
      this.error.set('El CSV debe pesar hasta 128 KiB.');
      return;
    }
    try {
      const rows = readAcademicCsv(await file.text());
      if (revision !== this.revision) return;
      this.filename.set(file.name);
      this.rows.set(rows);
    } catch (e) {
      if (revision === this.revision)
        this.error.set(e instanceof Error ? e.message : 'No se pudo leer el CSV.');
    }
  }
  template() {
    downloadBlob(
      new Blob(['\uFEFF' + IMPORT_HEADERS.join(',') + '\r\n'], { type: 'text/csv;charset=utf-8' }),
      'plantilla-matriculas.csv',
    );
  }
  validate() {
    if (!this.rows().length || this.busy() || this.applied()) return;
    this.busy.set(true);
    this.error.set('');
    this.preview.set(null);
    this.http
      .post<Preview>('/api/institution/academic/imports/preview', { rows: this.rows() })
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({ next: (r) => this.preview.set(r), error: (e) => this.error.set(cycleError(e)) });
  }
  apply() {
    const id = this.preview()?.batchId;
    if (!id || this.busy() || this.applied()) return;
    this.busy.set(true);
    this.error.set('');
    this.http
      .post<{ recordIds: string[] }>('/api/institution/academic/imports/' + id + '/apply', {})
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (r) => this.applied.set(r.recordIds.length),
        error: (e) => this.error.set(cycleError(e)),
      });
  }
}
