import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { AuditPage } from './audit.page';
import { MailPage } from './mail.page';
describe('Audit and mail monitoring',()=>{
 let http:HttpTestingController;
 let route:{snapshot:{data:{global?:boolean}}};
 const item={id:'event',occurredAt:'2026-11-14T12:00:00Z',tenantId:'tenant',actorId:'actor',actorType:'USER',actorRole:'TENANT_ADMIN',action:'ROLE_UPDATED',resourceType:'ROLE',resourceId:'role',correlationId:'request',changes:'permissions: [AUDIT_VIEW] → []'};
 const response={items:[item],total:25,page:0,size:20};
 beforeEach(()=>{route={snapshot:{data:{}}};TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),{provide:ActivatedRoute,useValue:route}]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('never sends a tenant selector from the tenant audit view',()=>{
  const fixture=TestBed.createComponent(AuditPage);fixture.detectChanges();http.expectOne(r=>r.url==='/api/audit/tenant').flush(response);
  fixture.componentInstance.form.patchValue({tenantId:'11111111-1111-1111-1111-111111111111',action:'ROLE_UPDATED'});fixture.componentInstance.apply();
  const req=http.expectOne(r=>r.url==='/api/audit/tenant');expect(req.request.params.has('tenantId')).toBeFalse();expect(req.request.params.get('action')).toBe('ROLE_UPDATED');req.flush(response);fixture.detectChanges();
  expect(fixture.nativeElement.textContent).toContain('Permisos del rol actualizados');expect(fixture.nativeElement.textContent).toContain('actor');
 });
 it('uses the global API only on a global route and preserves filters across pages',()=>{
  route.snapshot.data.global=true;const fixture=TestBed.createComponent(AuditPage);fixture.detectChanges();http.expectOne(r=>r.url==='/api/platform/audit').flush(response);
  const page=fixture.componentInstance;page.form.patchValue({tenantId:'11111111-1111-1111-1111-111111111111',action:'ROLE_UPDATED',from:'2026-11-01',to:'2026-11-14'});page.apply();
  const filtered=http.expectOne(r=>r.url==='/api/platform/audit');expect(filtered.request.params.get('tenantId')).toBe('11111111-1111-1111-1111-111111111111');expect(filtered.request.params.has('before')).toBeTrue();filtered.flush(response);
  page.move(1);const next=http.expectOne(r=>r.url==='/api/platform/audit');expect(next.request.params.get('page')).toBe('1');expect(next.request.params.get('action')).toBe('ROLE_UPDATED');next.flush({...response,page:1});
 });
 it('validates UUID and date range before sending filters',()=>{
  const fixture=TestBed.createComponent(AuditPage);fixture.detectChanges();http.expectOne(r=>r.url==='/api/audit/tenant').flush({items:[],total:0,page:0,size:20});const page=fixture.componentInstance;
  page.form.patchValue({actorId:'bad-id'});page.apply();expect(page.error()).toContain('UUID');http.expectNone(r=>r.url==='/api/audit/tenant');
  page.form.patchValue({actorId:'',from:'2026-12-01',to:'2026-01-01'});page.apply();expect(page.error()).toContain('fecha');http.expectNone(r=>r.url==='/api/audit/tenant');
 });
 it('hides old results on failure and allows a fresh query',()=>{
  const fixture=TestBed.createComponent(AuditPage);fixture.detectChanges();http.expectOne(r=>r.url==='/api/audit/tenant').flush(response);fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('Permisos del rol');
  fixture.componentInstance.load();http.expectOne(r=>r.url==='/api/audit/tenant').flush({}, {status:503,statusText:'Unavailable'});fixture.detectChanges();expect(fixture.nativeElement.querySelector('article')).toBeNull();expect(fixture.nativeElement.textContent).toContain('Reintentar');
  fixture.componentInstance.load();http.expectOne(r=>r.url==='/api/audit/tenant').flush({items:[],total:0,page:0,size:20});fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('No hay registros');
 });
 it('shows safe delivery categories and explains SMTP acceptance',()=>{
  const fixture=TestBed.createComponent(MailPage);fixture.detectChanges();http.expectOne(r=>r.url==='/api/platform/mail').flush({items:[{id:'mail-id',studentId:'student-id',kind:'PASSWORD_RESET',state:'FAILED',attempts:5,createdAt:item.occurredAt,nextAttemptAt:item.occurredAt,sentAt:null,lastError:'DELIVERY_FAILED'}],total:1,page:0,size:20});fixture.detectChanges();
  expect(fixture.nativeElement.textContent).toContain('Intentos agotados');expect(fixture.nativeElement.textContent).toContain('solicitar un enlace nuevo');expect(fixture.nativeElement.textContent).toContain('no confirma');expect(fixture.nativeElement.querySelector('input[type="password"]')).toBeNull();
 });
 it('refreshes mail with the last applied filter and retains it for pagination',()=>{
  const fixture=TestBed.createComponent(MailPage);fixture.detectChanges();http.expectOne(r=>r.url==='/api/platform/mail').flush({items:[],total:25,page:0,size:20});const page=fixture.componentInstance;page.state.setValue('RETRY');page.apply();
  const req=http.expectOne(r=>r.url==='/api/platform/mail');expect(req.request.params.get('state')).toBe('RETRY');req.flush({items:[],total:25,page:0,size:20});page.state.setValue('FAILED');page.move(1);const next=http.expectOne(r=>r.url==='/api/platform/mail');expect(next.request.params.get('state')).toBe('RETRY');expect(next.request.params.get('page')).toBe('1');next.flush({items:[],total:25,page:1,size:20});
 });
});
