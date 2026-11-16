import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { OrganizationApi } from '../organization/organization.api';
import { DashboardPage } from './dashboard.page';
import { PortalItem } from '../../core/models';

describe('Institutional workspace', () => {
  let http: HttpTestingController;
  const notice: PortalItem = {
    id: 'notice',
    itemType: 'ANNOUNCEMENT',
    title: 'Aviso de prueba',
    description: 'Información institucional',
    priority: 0,
    createdAt: '2026-11-15T12:00:00Z',
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            current: () => ({ role: 'TENANT_ADMIN', institutionName: 'Instituto de prueba' }),
          },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function access(permissions: string[]) {
    TestBed.inject(OrganizationApi).access.set({
      userId: 'admin',
      tenantId: 'tenant',
      role: 'TENANT_ADMIN',
      permissions,
    });
  }
  function create(snapshot: object|null = {linkedStudents:null,credentials:null,issued:null,drafts:null,recent:null}) {
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    if(snapshot!==null)http.expectOne('/api/institution/dashboard').flush(snapshot);
    return fixture;
  }
  it('does not request restricted resources or advertise unavailable modules', () => {
    access(['ORGANIZATION_VIEW']);
    const fixture = create();
    http.expectNone('/api/students');
    http.expectNone('/api/credentials');
    http.expectOne('/api/portal/items').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/students"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/credentials"]')).toBeNull();
    expect(fixture.componentInstance.studentsReady()).toBeFalse();
  });
  it('renders server totals without counting the bounded recent documents', () => {
    access(['ORGANIZATION_VIEW', 'STUDENT_VIEW', 'CREDENTIAL_VIEW']);
    const fixture = create({linkedStudents:2500,credentials:140,issued:125,drafts:15,recent:[
        {
          id: 'doc',
          title: 'Curso',
          holderName: 'Ana',
          credentialType: 'CERTIFICADO',
          status: 'ISSUED',
          createdAt: '2026-11-15T12:00:00Z',
        },
      ]});
    http.expectOne('/api/portal/items').flush([]);
    fixture.detectChanges();
    expect(fixture.componentInstance.studentsReady()).toBeTrue();
    expect(fixture.componentInstance.credentialsReady()).toBeTrue();
    expect(fixture.componentInstance.issued()).toBe(125);
    expect(fixture.nativeElement.querySelector('.metric>strong').textContent).toContain('2500');
    expect(fixture.nativeElement.textContent).toContain('Ana');
  });
  it('keeps the portal available when the aggregate request fails',()=>{
    access(['ORGANIZATION_VIEW','STUDENT_VIEW','CREDENTIAL_VIEW']);
    const fixture=create(null);
    http.expectOne('/api/institution/dashboard').flush({}, {status:503,statusText:'Unavailable'});
    http.expectOne('/api/portal/items').flush([notice]);
    fixture.detectChanges();
    expect(fixture.componentInstance.studentsReady()).toBeFalse();
    expect(fixture.componentInstance.credentialsReady()).toBeFalse();
    expect(fixture.componentInstance.portalReady()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Aviso de prueba');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('resumen institucional');
  });
  it('shows a genuine empty state only after a successful empty response', () => {
    access(['CREDENTIAL_VIEW']);
    const fixture = create(null);
    expect(fixture.nativeElement.textContent).not.toContain('Todavía no hay credenciales');
    http.expectOne('/api/institution/dashboard').flush({linkedStudents:null,credentials:0,issued:0,drafts:0,recent:[]});
    http.expectOne('/api/portal/items').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Todavía no hay credenciales');
    expect(fixture.componentInstance.credentialsReady()).toBeTrue();
  });
  it('requires a date for an agenda item and prevents duplicate publication during a request', () => {
    access([]);
    const page = create().componentInstance;
    http.expectOne('/api/portal/items').flush([]);
    page.portalForm.patchValue({ itemType: 'DEADLINE', title: 'Cierre de gestión' });
    page.publish();
    http.expectNone((r) => r.method === 'POST');
    expect(page.portalError()).toContain('fecha');
    page.portalForm.controls.eventAt.setValue('2026-12-01T10:00');
    page.publish();
    page.publish();
    const req = http.expectOne('/api/portal/items');
    expect(req.request.body.eventAt).toContain('2026-12-01');
    req.flush({ ...notice, itemType: 'DEADLINE' });
    expect(page.publishing()).toBeFalse();
  });
  it('keeps the editor and its text after a failed publication', () => {
    access([]);
    const page = create().componentInstance;
    http.expectOne('/api/portal/items').flush([]);
    page.showEditor.set(true);
    page.portalForm.patchValue({ title: 'Texto que no debe perderse' });
    page.publish();
    http
      .expectOne('/api/portal/items')
      .flush({ message: 'No se pudo publicar' }, { status: 503, statusText: 'Unavailable' });
    expect(page.showEditor()).toBeTrue();
    expect(page.portalForm.controls.title.value).toContain('no debe perderse');
    expect(page.portalError()).toContain('No se pudo publicar');
  });
  it('preserves a notice when deletion fails and reports the error', () => {
    access([]);
    const page = create().componentInstance;
    http.expectOne('/api/portal/items').flush([notice]);
    spyOn(window, 'confirm').and.returnValue(true);
    page.remove(notice);
    http
      .expectOne('/api/portal/items/notice')
      .flush({ message: 'Servicio no disponible' }, { status: 503, statusText: 'Unavailable' });
    expect(page.portalItems().length).toBe(1);
    expect(page.portalError()).toContain('Servicio no disponible');
    expect(page.removing()).toBe('');
  });
});
