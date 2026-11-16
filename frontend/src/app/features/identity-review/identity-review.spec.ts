import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { StudentIdentityPage } from './student-identity.page';
import { InstitutionIdentityPage } from './institution-identity.page';
import { IdentityPreviewComponent } from './identity-preview.component';
import { IdentityReview, IdentityState } from './identity-review.api';

describe('Private institutional identity review',()=>{
 let http:HttpTestingController;
 const row:IdentityReview={id:'submission',studentId:'student',tenantId:'tenant',institutionName:'Instituto de prueba',status:'PENDING',effectiveStatus:'PENDING',profile:{fullName:'Estudiante Prueba',country:'BO',documentType:'CI',documentNumber:'test-document',dateOfBirth:'2000-01-01',department:'La Paz'},profileChanged:false,sharingActive:true,photoPresent:false,submittedAt:'2026-11-15T12:00:00Z',reviewedAt:null,withdrawnAt:null,reason:null};
 const state:IdentityState={profileComplete:true,emailVerified:true,institutions:[{id:'tenant',name:'Instituto de prueba',slug:'instituto'}],submissions:[]};
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 function student(data:IdentityState=state){const fixture=TestBed.createComponent(StudentIdentityPage);http.expectOne('/api/me/identity-verifications').flush(data);fixture.detectChanges();return fixture;}
 function institution(){const fixture=TestBed.createComponent(InstitutionIdentityPage);http.expectOne(r=>r.url==='/api/institution/identity-verifications').flush({items:[row],total:1,page:0,size:20});fixture.detectChanges();return fixture;}
 function file(name='front.png'){return new File(['image'],name,{type:'image/png'});}
 it('does not show upload before profile and email prerequisites or without linked institutions',()=>{
  const fixture=student({...state,emailVerified:false});expect(fixture.nativeElement.querySelector('form')).toBeNull();expect(fixture.nativeElement.textContent).toContain('confirma tu correo');
 });
 it('requires selected recipient and explicit consent and resets consent when recipient changes',()=>{
  const page=student().componentInstance;page.files.FRONT=file();page.files.BACK=file('back.png');page.form.controls.tenantId.setValue('tenant');page.submit();http.expectNone(r=>r.method==='POST');page.form.controls.accepted.setValue(true);page.form.controls.tenantId.setValue('other-tenant');expect(page.form.controls.accepted.value).toBeFalse();page.submit();http.expectNone(r=>r.method==='POST');
 });
 it('sends multipart to the selected institution and clears local file references after success',()=>{
  const page=student().componentInstance;page.files.FRONT=file();page.files.BACK=file('back.png');page.form.setValue({tenantId:'tenant',accepted:true});page.submit();const req=http.expectOne('/api/me/identity-verifications');expect(req.request.body instanceof FormData).toBeTrue();expect(req.request.body.get('tenantId')).toBe('tenant');expect(req.request.body.get('accepted')).toBe('true');expect(req.request.body.has('photo')).toBeFalse();req.flush(row);http.expectOne('/api/me/identity-verifications').flush({...state,submissions:[row]});expect(page.files.FRONT).toBeNull();expect(page.form.controls.accepted.value).toBeFalse();expect(page.notice()).toContain('enviados');
 });
 it('keeps selected files on conflict and reports no success',()=>{
  const page=student().componentInstance;page.files.FRONT=file();page.files.BACK=file();page.form.setValue({tenantId:'tenant',accepted:true});page.submit();http.expectOne('/api/me/identity-verifications').flush({},{status:409,statusText:'Conflict'});expect(page.notice()).toBe('');expect(page.files.FRONT).not.toBeNull();expect(page.error()).toContain('pendiente');
 });
 it('rejects an oversized image before making a request',()=>{
  const page=student().componentInstance;const input=document.createElement('input');const selected=new File([new Uint8Array(5*1024*1024+1)],'huge.png',{type:'image/png'});Object.defineProperty(input,'files',{value:[selected]});page.choose('FRONT',{target:input} as unknown as Event);expect(page.files.FRONT).toBeNull();expect(page.error()).toContain('5 MiB');http.expectNone(r=>r.method==='POST');
 });
 it('withdrawal requires explicit confirmation and closes any private preview',()=>{
  const page=student({...state,submissions:[row]}).componentInstance;page.withdraw();http.expectNone(r=>r.method==='POST');page.preview.set(row);page.withdrawing.set(row);page.withdraw();http.expectOne('/api/me/identity-verifications/submission/withdraw').flush(null);http.expectOne('/api/me/identity-verifications').flush({...state,submissions:[{...row,sharingActive:false}]});expect(page.preview()).toBeNull();expect(page.notice()).toContain('retirado');
 });
 it('does not send rejection with missing reason or before confirmation',()=>{
  const page=institution().componentInstance;page.open(row.id);http.expectOne('/api/institution/identity-verifications/submission').flush(row);page.decision='REJECTED';page.confirm();expect(page.confirming()).toBeFalse();page.save();http.expectNone(r=>r.method==='POST');page.reason='El documento no es legible.';page.save();http.expectNone(r=>r.method==='POST');page.confirm();page.save();const req=http.expectOne('/api/institution/identity-verifications/submission/decision');expect(req.request.body.reason).toContain('no es legible');req.flush({...row,status:'REJECTED'});http.expectOne(r=>r.url==='/api/institution/identity-verifications').flush({items:[],total:0,page:0,size:20});expect(page.selected()).toBeNull();expect(page.notice()).toContain('guardada');
 });
 it('a stale decision closes private details and reports a conflict instead of success',()=>{
  const page=institution().componentInstance;page.open(row.id);http.expectOne('/api/institution/identity-verifications/submission').flush(row);page.decision='VERIFIED';page.confirm();page.save();http.expectOne('/api/institution/identity-verifications/submission/decision').flush({},{status:409,statusText:'Conflict'});expect(page.selected()).toBeNull();expect(page.showImages()).toBeFalse();expect(page.notice()).toBe('');expect(page.error()).toContain('perfil cambió');
 });
 it('private preview loads authenticated blobs and releases object URLs on close',()=>{
  const create=spyOn(URL,'createObjectURL').and.returnValues('blob:front','blob:back');const revoke=spyOn(URL,'revokeObjectURL');const fixture=TestBed.createComponent(IdentityPreviewComponent);fixture.componentRef.setInput('submissionId','submission');fixture.componentRef.setInput('own',true);fixture.detectChanges();for(const part of ['FRONT','BACK'])http.expectOne('/api/me/identity-verifications/submission/files/'+part).flush(new Blob(['image'],{type:'image/jpeg'}));fixture.detectChanges();expect(create).toHaveBeenCalledTimes(2);expect(fixture.nativeElement.querySelectorAll('img').length).toBe(2);fixture.destroy();expect(revoke).toHaveBeenCalledWith('blob:front');expect(revoke).toHaveBeenCalledWith('blob:back');
 });
 it('failed private preview does not retain partially received images',()=>{
  const fixture=TestBed.createComponent(IdentityPreviewComponent);fixture.componentRef.setInput('submissionId','submission');fixture.detectChanges();http.expectOne('/api/institution/identity-verifications/submission/files/FRONT').flush(new Blob(['image']));http.expectOne('/api/institution/identity-verifications/submission/files/BACK').flush(new Blob(),{status:404,statusText:'Not Found'});expect(fixture.componentInstance.images()).toEqual([]);expect(fixture.componentInstance.error()).toContain('autorización');fixture.destroy();
 });
});
