import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Page } from '../academic/academic.api';
export interface ReportFilters {
  year: string;
  type: string;
  status: string;
}
export interface ReportRow {
  id: string;
  holderName: string;
  type: string;
  title: string;
  status: string;
  code: string | null;
  issuedAt: string | null;
}
export interface Group {
  label: string;
  total: number;
}
export interface Summary {
  linkedStudents: number;
  credentials: number;
  issued: number;
  drafts: number;
  revoked: number;
  byType: Group[];
  byMonth: Group[];
  generatedAt: string;
}
export interface PlatformStatistics {
  organizations: number;
  students: number;
  credentials: number;
  byStatus: Group[];
}
@Injectable({ providedIn: 'root' })
export class ReportsApi {
  private readonly http = inject(HttpClient);
  private params(f: ReportFilters) {
    let p = new HttpParams();
    for (const [k, v] of Object.entries(f)) if (v) p = p.set(k, v);
    return p;
  }
  summary(f: ReportFilters) {
    return this.http.get<Summary>('/api/institution/reports/summary', { params: this.params(f) });
  }
  rows(f: ReportFilters, page = 0) {
    return this.http.get<Page<ReportRow>>('/api/institution/reports/credentials', {
      params: this.params(f).set('page', page).set('size', 20),
    });
  }
  export(f: ReportFilters, format: 'pdf' | 'xlsx') {
    return this.http.get('/api/institution/reports/credentials.' + format, {
      params: this.params(f),
      responseType: 'blob',
    });
  }
  platform() {
    return this.http.get<PlatformStatistics>('/api/platform/statistics');
  }
}
