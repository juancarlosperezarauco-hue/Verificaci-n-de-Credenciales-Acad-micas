import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { PublicCredential } from '../../core/models';
import { Page, cycleError, downloadBlob } from '../academic/academic.api';
import { StudentAccountApi } from './student-account.api';

interface Filters {
  institutions: { id: string; name: string }[];
  types: string[];
  years: number[];
}
@Component({
  selector: 'app-student-documents',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './student-documents.component.html',
})
export class StudentDocumentsComponent {
  private readonly http = inject(HttpClient);
  private readonly api = inject(StudentAccountApi);
  private readonly destroy = inject(DestroyRef);
  readonly filters = signal<Filters>({ institutions: [], types: [], years: [] });
  readonly result = signal<Page<PublicCredential>>({ items: [], total: 0, page: 0, size: 10 });
  readonly loading = signal(false);
  readonly ready = signal(false);
  readonly error = signal('');
  readonly downloading = signal('');
  institution = '';
  year = '';
  type = '';
  constructor() {
    this.http
      .get<Filters>('/api/me/credentials/filters')
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => this.filters.set(r),
        error: () =>
          this.error.set('No se pudieron cargar los filtros. Puedes volver a consultar.'),
      });
    this.load();
  }
  load(page = 0) {
    if (this.loading()) return;
    this.loading.set(true);
    this.ready.set(false);
    this.error.set('');
    let params = new HttpParams().set('page', page).set('size', 10);
    if (this.institution) params = params.set('institutionId', this.institution);
    if (this.year) params = params.set('year', this.year);
    if (this.type) params = params.set('type', this.type);
    this.http
      .get<Page<PublicCredential>>('/api/me/credentials/page', { params })
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.result.set(r);
          this.ready.set(true);
        },
        error: (e) => this.error.set(cycleError(e)),
      });
  }
  download(c: PublicCredential) {
    if (!c.credentialId || this.downloading()) return;
    this.downloading.set(c.credentialId);
    this.error.set('');
    this.api
      .document(c.credentialId)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.downloading.set('')),
      )
      .subscribe({
        next: (blob) => downloadBlob(blob, c.documentName ?? 'credencial.pdf'),
        error: (e) => this.error.set(cycleError(e)),
      });
  }
}
