import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { permissionGuard } from '../../core/auth.guard';
import { OrganizationRegisterPage } from './organization-register.page';
import { OrganizationTeamPage } from './organization-team.page';
import { PlatformOrganizationsPage } from './platform-organizations.page';
import { buildDetailsForm } from './organization.form';
import { OrganizationApi } from './organization.api';
import { AccessLoginPage } from './access-login.page';
import { InstitutionPage } from '../institution/institution.page';

describe('Organization workflows',()=>{
 let http:HttpTestingController;let auth:{save:jasmine.Spy;isAuthenticated:()=>boolean;logout:jasmine.Spy};
 const values={legalName:'Colegio de pruebas',institutionType:'COLEGIO',educationLevels:['SECUNDARIA'],country:'bo',region:'Santa Cruz',address:'Av. Educación 100',phone:'70000000',contactEmail:'contacto@colegio.test',website:'https://colegio.test',domain:'colegio.test',taxId:'123456',educationLicense:'EDU-123',ministryRegistration:'',accreditation:'',representativeName:'Ana Pérez',documentType:'CI',documentNumber:'1234567',position:'Directora',representativeEmail:'ana@colegio.test',representativePhone:'70000001',organizationSlug:'colegio-prueba',password:'PasswordPrueba123!',confirmPassword:'PasswordPrueba123!'};
 beforeEach(()=>{auth={save:jasmine.createSpy(),isAuthenticated:()=>true,logout:jasmine.createSpy()};
 TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:AuthService,useValue:auth}]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('validates every step and sends the real nested registration contract',()=>{
  const fixture=TestBed.createComponent(OrganizationRegisterPage);const page=fixture.componentInstance;fixture.detectChanges();
  page.next();expect(page.step()).toBe(0);expect(page.error()).toContain('campos');
  page.form.patchValue(values);for(let i=0;i<4;i++)page.next();expect(page.step()).toBe(4);
  page.confirmed.set(true);page.submit();
  const req=http.expectOne('/api/auth/organizations/register');expect(req.request.body.profile.representative.fullName).toBe('Ana Pérez');
  expect(req.request.body.profile.country).toBe('BO');expect(req.request.body.profile.educationLevels).toEqual(['SECUNDARIA']);
  expect(req.request.body.profile.confirmPassword).toBeUndefined();
  req.flush({message:'Duplicado'},{status:409,statusText:'Conflict'});
  expect(page.error()).toContain('registrado');expect(page.loading()).toBeFalse();expect(auth.save).not.toHaveBeenCalled();
 });
 it('blocks mismatching passwords and registration without explicit confirmation',()=>{
  const page=TestBed.createComponent(OrganizationRegisterPage).componentInstance;page.form.patchValue({...values,confirmPassword:'OtraPassword123!'});
  page.step.set(3);page.next();expect(page.step()).toBe(3);expect(page.error()).toContain('coinciden');
  page.step.set(4);page.submit();http.expectNone('/api/auth/organizations/register');
 });
 it('rejects malformed country, unsafe website and empty education levels',()=>{
  const form=buildDetailsForm();form.patchValue({...values,country:'Bolivia',website:'javascript:alert(1)',educationLevels:[]});
  expect(form.get('country')?.invalid).toBeTrue();expect(form.get('website')?.invalid).toBeTrue();expect(form.get('educationLevels')?.invalid).toBeTrue();
 });
 it('guards direct routes using server permissions and rejects students from team management',async()=>{
  const route={data:{roles:['ADMIN','TENANT_ADMIN']}} as unknown as ActivatedRouteSnapshot;
  const result=TestBed.runInInjectionContext(()=>permissionGuard(route,{} as RouterStateSnapshot)) as Observable<boolean>;
  const resolved=firstValueFrom(result);http.expectOne('/api/access/me').flush({userId:'student',tenantId:null,role:'STUDENT',permissions:[]});
  expect(String(await resolved)).toBe('/access-denied');
 });
 it('allows an employee only when the required permission exists',async()=>{
  const route={data:{permission:'CREDENTIAL_VIEW'}} as unknown as ActivatedRouteSnapshot;
  const result=TestBed.runInInjectionContext(()=>permissionGuard(route,{} as RouterStateSnapshot)) as Observable<boolean>;
  const resolved=firstValueFrom(result);http.expectOne('/api/access/me').flush({userId:'staff',tenantId:'tenant',role:'CUSTOM',permissions:['CREDENTIAL_VIEW']});
  expect(await resolved).toBeTrue();expect(TestBed.inject(OrganizationApi).can('CREDENTIAL_CREATE')).toBeFalse();
 });
 it('loads real roles and creates employees without accepting a global role',()=>{
  const fixture=TestBed.createComponent(OrganizationTeamPage);const page=fixture.componentInstance;
  http.expectOne('/api/access/users').flush([]);http.expectOne('/api/access/roles').flush([{id:'role-1',name:'Docente',permissions:['STUDENT_VIEW']}]);http.expectOne('/api/access/permissions').flush(['STUDENT_VIEW']);
  fixture.detectChanges();page.userForm.setValue({fullName:'Docente Uno',email:'docente@test.edu',password:'PasswordPrueba123!',roleIds:['role-1']});page.saveUser();
  const req=http.expectOne('/api/access/users');expect(req.request.method).toBe('POST');expect(req.request.body.roleIds).toEqual(['role-1']);expect(req.request.body.role).toBeUndefined();
  req.flush({message:'Error'},{status:409,statusText:'Conflict'});expect(page.error()).toContain('registrado');expect(page.busy()).toBeFalse();
 });
 it('filters organizations by name and actual backend status',()=>{
  const fixture=TestBed.createComponent(PlatformOrganizationsPage);const page=fixture.componentInstance;
  http.expectOne('/api/platform/organizations').flush([{id:'1',name:'Colegio Norte',slug:'norte',institutionType:'COLEGIO',active:true,status:'ACTIVE'},{id:'2',name:'Universidad Sur',slug:'sur',institutionType:'UNIVERSIDAD',active:false,status:'PENDING_REVIEW'}]);
  fixture.detectChanges();page.status.set('PENDING_REVIEW');expect(page.filtered().length).toBe(1);expect(page.filtered()[0].name).toBe('Universidad Sur');
  page.query.set('norte');expect(page.filtered().length).toBe(0);
 });
 it('explains invalid login credentials without reporting an expired session',()=>{
  const page=TestBed.createComponent(AccessLoginPage).componentInstance;
  page.form.setValue({organizationSlug:'colegio-prueba',email:'docente@test.edu',password:'PasswordPrueba123!'});
  page.submit();http.expectOne('/api/auth/login').flush({}, {status:401,statusText:'Unauthorized'});
  expect(page.error()).toContain('datos de acceso');expect(page.loading()).toBeFalse();expect(auth.save).not.toHaveBeenCalled();
 });
 it('hides team mutation forms when the initial load fails and recovers on retry',()=>{
  const fixture=TestBed.createComponent(OrganizationTeamPage);const page=fixture.componentInstance;
  const users=http.expectOne('/api/access/users');const roles=http.expectOne('/api/access/roles');const permissions=http.expectOne('/api/access/permissions');
  roles.flush([]);permissions.flush([]);users.flush({}, {status:503,statusText:'Unavailable'});fixture.detectChanges();
  expect(page.ready()).toBeFalse();expect(fixture.nativeElement.querySelector('form')).toBeNull();
  page.roleForm.setValue({name:'Docente'});page.saveRole();http.expectNone('/api/access/roles');
  page.load();http.expectOne('/api/access/users').flush([]);http.expectOne('/api/access/roles').flush([]);http.expectOne('/api/access/permissions').flush([]);fixture.detectChanges();
  expect(page.ready()).toBeTrue();expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
 });
 it('allows institutional branding edits for a custom role with update permission',()=>{
  const page=TestBed.createComponent(InstitutionPage).componentInstance;const api=TestBed.inject(OrganizationApi);
  api.access.set({userId:'staff',tenantId:'tenant',role:'CUSTOM',permissions:['ORGANIZATION_VIEW']});expect(page.canEdit()).toBeFalse();
  api.access.set({userId:'staff',tenantId:'tenant',role:'CUSTOM',permissions:['ORGANIZATION_VIEW','ORGANIZATION_UPDATE']});expect(page.canEdit()).toBeTrue();
 });
 it('selects multiple education levels with independent checkboxes',()=>{
  const fixture=TestBed.createComponent(OrganizationRegisterPage);const page=fixture.componentInstance;fixture.detectChanges();
  const inputs=fixture.nativeElement.querySelectorAll('input[type="checkbox"]') as NodeListOf<HTMLInputElement>;
  inputs[0].click();inputs[2].click();expect(page.form.get('educationLevels')?.value).toEqual(['INICIAL','SECUNDARIA']);
  inputs[0].click();expect(page.form.get('educationLevels')?.value).toEqual(['SECUNDARIA']);
 });
});
