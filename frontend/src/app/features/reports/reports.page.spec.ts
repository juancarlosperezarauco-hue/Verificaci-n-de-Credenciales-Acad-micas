import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReportsPage } from './reports.page';

describe('Institutional reports', () => {
  let http: HttpTestingController;
  const summary = {
    linkedStudents: 1,
    credentials: 1,
    issued: 1,
    drafts: 0,
    revoked: 0,
    byType: [],
    byMonth: [],
    generatedAt: '2026-11-15T12:00:00Z',
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function initial() {
    const f = TestBed.createComponent(ReportsPage);
    http.expectOne('/api/institution/reports/summary').flush(summary);
    http
      .expectOne('/api/institution/reports/credentials?page=0&size=20')
      .flush({ items: [], total: 0, page: 0, size: 20 });
    f.detectChanges();
    return f;
  }
  it('requests aggregate results and renders an actual empty response', () => {
    const f = initial();
    expect(f.nativeElement.textContent).toContain('No hay resultados');
    expect(f.componentInstance.ready()).toBeTrue();
  });
  it('keeps exports and pagination tied to the applied filters when the form is edited', () => {
    const page = initial().componentInstance;
    page.filters.year = '2025';
    page.load();
    http.expectOne('/api/institution/reports/summary?year=2025').flush(summary);
    http
      .expectOne('/api/institution/reports/credentials?year=2025&page=0&size=20')
      .flush({ items: [], total: 30, page: 0, size: 20 });
    page.filters.year = '2026';
    page.load(1, false);
    http.expectOne('/api/institution/reports/summary?year=2025').flush(summary);
    http
      .expectOne('/api/institution/reports/credentials?year=2025&page=1&size=20')
      .flush({ items: [], total: 30, page: 1, size: 20 });
    page.export('pdf');
    page.export('pdf');
    http
      .expectOne('/api/institution/reports/credentials.pdf?year=2025')
      .flush(new Blob(['error']), { status: 503, statusText: 'Offline' });
  });
  it('does not display stale counts as a successful query after failure', () => {
    const page = initial().componentInstance;
    page.load();
    http
      .expectOne('/api/institution/reports/summary')
      .flush({}, { status: 503, statusText: 'Offline' });
    http.expectOne('/api/institution/reports/credentials?page=0&size=20');
    expect(page.ready()).toBeFalse();
    expect(page.error()).toBeTruthy();
    page.export('xlsx');
    http.expectNone((r) => r.url.endsWith('.xlsx'));
  });
});
