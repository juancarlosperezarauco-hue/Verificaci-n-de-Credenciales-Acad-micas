import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { permissionGuard } from '../../core/auth.guard';
import { OrganizationApi } from './organization.api';
import { OrganizationProfilePage } from './organization-profile.page';
import { AccessLoginPage } from './access-login.page';

describe('Restricted organization onboarding',()=>{
 let http:HttpTestingController;let router:Router;
 const access={userId:'admin',tenantId:'tenant',role:'TENANT_ADMIN',permissions:['ORGANIZATION_VIEW','ORGANIZATION_UPDATE'],onboarding:true};
 const organization={id:'tenant',name:'Academia Nueva',slug:'academia',institutionType:'ACADEMIA',active:false,status:'OBSERVED',reviewNote:'Corrige la dirección.'};
 const details={legalName:'Academia Nueva',institutionType:'ACADEMIA',educationLevels:[],country:'BO',region:'La Paz',address:'Av. Prueba 100',phone:'70000000',contactEmail:'contact@example.test',representative:{fullName:'Directora Prueba',documentType:'CI',documentNumber:'123456',position:'Directora',email:'admin@example.test',phone:'70000001'}};
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:AuthService,useValue:{isAuthenticated:()=>true,save:jasmine.createSpy(),logout:jasmine.createSpy()}}]});
 http=TestBed.inject(HttpTestingController);router=TestBed.inject(Router);spyOn(router,'navigate').and.returnValue(Promise.resolve(true));});
 afterEach(()=>http.verify());
 it('redirects direct operational routes while admitting the institutional profile',async()=>{
  const check=async(data:Record<string,unknown>)=>{
   const result=TestBed.runInInjectionContext(()=>permissionGuard({data} as ActivatedRouteSnapshot,{} as RouterStateSnapshot)) as Observable<boolean>;
   const resolved=firstValueFrom(result);http.expectOne('/api/access/me').flush(access);return resolved;
  };
  expect(String(await check({permission:'ORGANIZATION_VIEW'}))).toBe('/organization');
  expect(await check({permission:'ORGANIZATION_VIEW',allowOnboarding:true})).toBeTrue();
 });
 it('shows observation and resubmission without exposing team management',()=>{
  TestBed.inject(OrganizationApi).access.set(access);
  const fixture=TestBed.createComponent(OrganizationProfilePage);const page=fixture.componentInstance;
  http.expectOne('/api/institution/status').flush(organization);http.expectOne('/api/institution/details').flush(details);fixture.detectChanges();
  expect(fixture.nativeElement.textContent).toContain('Corrige la dirección.');expect(page.isAdmin()).toBeFalse();
  expect(fixture.nativeElement.querySelector('a[href="/organization/team"]')).toBeNull();
  page.submitReview();const request=http.expectOne('/api/institution/submit-review');expect(request.request.body).toEqual({});
  request.flush({...organization,status:'PENDING_REVIEW'});expect(page.organization()?.status).toBe('PENDING_REVIEW');expect(page.notice()).toContain('enviadas');
 });
 it('refreshes approval and permissions without replacing unsaved form data',()=>{
  const api=TestBed.inject(OrganizationApi);api.access.set(access);
  const page=TestBed.createComponent(OrganizationProfilePage).componentInstance;
  http.expectOne('/api/institution/status').flush(organization);http.expectOne('/api/institution/details').flush(details);
  page.form.controls['address'].setValue('Cambio sin guardar');page.form.markAsDirty();page.refreshReview();
  http.expectOne('/api/access/me').flush({...access,onboarding:false,permissions:[...access.permissions,'CREDENTIAL_VIEW']});
  http.expectOne('/api/institution/status').flush({...organization,status:'ACTIVE',active:true});
  expect(api.can('CREDENTIAL_VIEW')).toBeTrue();expect(page.form.controls['address'].value).toBe('Cambio sin guardar');
  expect(page.isAdmin()).toBeTrue();expect(page.notice()).toContain('habilitada');
 });
 it('logs the applicant into the profile rather than the operational dashboard',()=>{
  const page=TestBed.createComponent(AccessLoginPage).componentInstance;
  page.form.setValue({organizationSlug:'academia',email:'admin@example.test',password:'PasswordPrueba123!'});page.submit();
  http.expectOne('/api/auth/login').flush({token:'test',role:'TENANT_ADMIN',tenantId:'tenant'});
  http.expectOne('/api/access/me').flush(access);expect(router.navigate).toHaveBeenCalledWith(['/organization']);
 });
});
