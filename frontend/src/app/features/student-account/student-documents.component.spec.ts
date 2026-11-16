import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { StudentDocumentsComponent } from './student-documents.component';
describe('Student document filters', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function create() {
    const f = TestBed.createComponent(StudentDocumentsComponent);
    http
      .expectOne('/api/me/credentials/filters')
      .flush({
        institutions: [{ id: 'tenant', name: 'Universidad' }],
        types: ['CERTIFICADO'],
        years: [2025],
      });
    http
      .expectOne('/api/me/credentials/page?page=0&size=10')
      .flush({ items: [], total: 0, page: 0, size: 10 });
    f.detectChanges();
    return f;
  }
  it('sends institution, year and document type to the paginated endpoint', () => {
    const page = create().componentInstance;
    page.institution = 'tenant';
    page.year = '2025';
    page.type = 'CERTIFICADO';
    page.load();
    page.load();
    const req = http.expectOne(
      '/api/me/credentials/page?page=0&size=10&institutionId=tenant&year=2025&type=CERTIFICADO',
    );
    req.flush({ items: [], total: 0, page: 0, size: 10 });
    expect(page.ready()).toBeTrue();
    expect(page.loading()).toBeFalse();
  });
  it('clears the successful view after a failed filter request and allows retry', () => {
    const page = create().componentInstance;
    page.load(1);
    http
      .expectOne('/api/me/credentials/page?page=1&size=10')
      .flush({}, { status: 503, statusText: 'Offline' });
    expect(page.ready()).toBeFalse();
    expect(page.error()).toBeTruthy();
    page.load();
    http
      .expectOne('/api/me/credentials/page?page=0&size=10')
      .flush({ items: [], total: 0, page: 0, size: 10 });
    expect(page.error()).toBe('');
  });
});
