import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { AccountSecurityPage } from './account-security.page';
import { MaintenancePage } from './maintenance.page';
import { OrganizationProfilePage } from '../organization/organization-profile.page';
describe('Security polish and historical registration',()=>{
 let http:HttpTestingController;let auth:{save:jasmine.Spy;logout:jasmine.Spy;current:()=>any};
 const session={token:'new-token',role:'TENANT_ADMIN',email:'test@example.test',tenantId:'tenant',tokenType:'Bearer'};
 beforeEach(()=>{auth={save:jasmine.createSpy(),logout:jasmine.createSpy(),current:()=>session};TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:AuthService,useValue:auth}]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 function security(){const fixture=TestBed.createComponent(AccountSecurityPage);fixture.detectChanges();http.expectOne('/api/security/sessions').flush([]);http.expectOne('/api/security/mfa').flush({enabled:true,recoveryCodesRemaining:5,pendingSetup:false});return fixture;}
 const status={retentionDays:30,cleanupBatch:500,encryptionBatch:50,scheduled:true,jwtKeyId:'next',mfaKeyId:'next',mailKeyId:'next',pendingMfa:1,pendingMail:2,activeJwtByKey:[{keyId:'legacy',sessions:1}]};
 it('requires password and a factor before replacing an authenticator and keeps the current session',()=>{
  const fixture=security(),page=fixture.componentInstance;page.choose('replace');page.actionForm.setValue({password:'test-password',code:''});page.recover();http.expectNone('/api/security/mfa/replacement');
  page.actionForm.controls.code.setValue('REC-test');page.recover();const req=http.expectOne('/api/security/mfa/replacement');expect(req.request.body).toEqual({password:'test-password',code:'REC-test'});req.flush({secret:'pending-secret',otpauthUri:'otpauth://test',expiresAt:'2026-11-14T22:00:00Z'});
  expect(auth.save).not.toHaveBeenCalled();expect(page.mfa()?.enabled).toBeTrue();expect(page.setup()?.secret).toBe('pending-secret');expect(page.actionForm.controls.password.value).toBe('');
 });
 it('renewal saves only the final session and replaces displayed backups',()=>{
  const page=security().componentInstance;page.choose('codes');page.actionForm.setValue({password:'test-password',code:'REC-old'});page.recover();http.expectOne('/api/security/mfa/recovery-codes').flush({session,recoveryCodes:['REC-new']});http.expectOne('/api/security/sessions').flush([]);expect(auth.save).toHaveBeenCalledOnceWith(session);expect(page.recoveryCodes()).toEqual(['REC-new']);expect(page.actionForm.controls.code.value).toBe('');expect(page.notice()).toContain('renovados');
 });
 it('cancellation clears seed and QR without disabling MFA',()=>{
  const page=security().componentInstance;page.setup.set({secret:'temporary',otpauthUri:'uri',expiresAt:'future'});page.qr.set('private-qr');page.cancelSetup();const req=http.expectOne('/api/security/mfa/pending');expect(req.request.method).toBe('DELETE');req.flush(null);http.expectOne('/api/security/mfa').flush({enabled:true,recoveryCodesRemaining:5,pendingSetup:false});expect(page.setup()).toBeNull();expect(page.qr()).toBe('');expect(page.mfa()?.enabled).toBeTrue();expect(auth.logout).not.toHaveBeenCalled();page.choose('replace');page.actionForm.controls.password.setValue('temporary');page.beginDisable();expect(page.action()).toBeNull();expect(page.actionForm.controls.password.value).toBe('');expect(page.disabling()).toBeTrue();
 });
 it('historical 404 displays the form and prefills only known identification',()=>{
  const fixture=TestBed.createComponent(OrganizationProfilePage),page=fixture.componentInstance;http.expectOne('/api/institution/status').flush({name:'Academia histórica',institutionType:'ACADEMIA',status:'PENDING_REVIEW'});http.expectOne('/api/institution/details').flush({message:'Not found'},{status:404,statusText:'Not Found'});fixture.detectChanges();expect(page.sections.flatMap(s=>s.fields).find(f=>f.key==='representativeEmail')?.hint).toContain('no cambia');expect(page.failed()).toBeFalse();expect(page.missing()).toBeTrue();expect(page.form.controls['legalName'].value).toBe('Academia histórica');expect(page.form.controls['institutionType'].value).toBe('ACADEMIA');expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
 });
 it('late institution metadata does not overwrite typed values',()=>{
  const page=TestBed.createComponent(OrganizationProfilePage).componentInstance;const org=http.expectOne('/api/institution/status');http.expectOne('/api/institution/details').flush({},{status:404,statusText:'Not Found'});page.form.controls['legalName'].setValue('Nombre escrito');page.form.controls['legalName'].markAsDirty();org.flush({name:'Nombre anterior',institutionType:'ACADEMIA',status:'PENDING_REVIEW'});expect(page.form.controls['legalName'].value).toBe('Nombre escrito');expect(page.form.controls['institutionType'].value).toBe('ACADEMIA');
 });
 it('maintenance requires an explicit operation and displays only key identifiers',()=>{
  const fixture=TestBed.createComponent(MaintenancePage);fixture.detectChanges();http.expectOne('/api/platform/security/maintenance').flush(status);fixture.detectChanges();const page=fixture.componentInstance;page.run();http.expectNone('/api/platform/security/maintenance/re-encrypt');expect(fixture.nativeElement.querySelector('input')).toBeNull();expect(fixture.nativeElement.textContent).toContain('legacy');page.confirming.set('encrypt');page.run();const req=http.expectOne('/api/platform/security/maintenance/re-encrypt');expect(req.request.body).toEqual({});req.flush({mfa:1,mail:2});http.expectOne('/api/platform/security/maintenance').flush({...status,pendingMfa:0,pendingMail:0});expect(page.notice()).toContain('3 filas');
 });
 it('failed maintenance does not show success or clear the confirmation',()=>{
  const fixture=TestBed.createComponent(MaintenancePage);fixture.detectChanges();http.expectOne('/api/platform/security/maintenance').flush(status);const page=fixture.componentInstance;page.confirming.set('cleanup');page.run();http.expectOne('/api/platform/security/maintenance/cleanup').flush({message:'No se pudo leer el contenido protegido'},{status:400,statusText:'Bad Request'});expect(page.notice()).toBe('');expect(page.confirming()).toBe('cleanup');expect(page.error()).toContain('contenido');expect(page.busy()).toBeFalse();
 });
});
