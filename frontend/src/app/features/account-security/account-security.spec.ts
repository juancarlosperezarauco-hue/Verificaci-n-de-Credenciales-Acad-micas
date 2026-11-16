import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { AccessLoginPage } from '../organization/access-login.page';
import { AccountSecurityPage } from './account-security.page';

describe('MFA and device sessions', () => {
  let http: HttpTestingController;
  let auth: {save:jasmine.Spy;logout:jasmine.Spy;current:()=>any};
  let navigate: jasmine.Spy;
  const session={token:'final-token',tokenType:'Bearer',email:'admin@example.test',role:'TENANT_ADMIN',tenantId:'tenant',institutionName:'Institución'};
  const challenge={mfaRequired:true as const,challengeToken:'temporary-challenge',expiresAt:'2026-11-15T12:00:00Z'};
  const device={id:'current',device:'Chrome · Windows',createdAt:'2026-11-14T12:00:00Z',lastSeenAt:'2026-11-14T12:00:00Z',expiresAt:'2026-11-15T12:00:00Z',current:true,mfaVerified:false};
  beforeEach(()=>{
    auth={save:jasmine.createSpy('save'),logout:jasmine.createSpy('logout'),current:()=>session};
    TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:AuthService,useValue:auth}]});
    http=TestBed.inject(HttpTestingController);
    navigate=spyOn(TestBed.inject(Router),'navigate').and.resolveTo(true);
  });
  afterEach(()=>http.verify());
  function login(){
    const fixture=TestBed.createComponent(AccessLoginPage);
    fixture.componentInstance.form.setValue({organizationSlug:'institucion',email:session.email,password:'SecretPassword123!'});
    fixture.componentInstance.submit();http.expectOne('/api/auth/login').flush(challenge);
    return fixture;
  }
  function account(){
    const fixture=TestBed.createComponent(AccountSecurityPage);fixture.detectChanges();
    http.expectOne('/api/security/sessions').flush([device,{...device,id:'other',current:false}]);
    http.expectOne('/api/security/mfa').flush({enabled:false,recoveryCodesRemaining:0});fixture.detectChanges();return fixture;
  }
  it('never stores a session or navigates after only the password factor',()=>{
    const fixture=login(),page=fixture.componentInstance;fixture.detectChanges();
    expect(auth.save).not.toHaveBeenCalled();expect(navigate).not.toHaveBeenCalled();http.expectNone('/api/access/me');
    expect(page.form.controls.password.value).toBe('');
    expect(fixture.nativeElement.textContent).toContain('Confirma tu acceso');
    expect(fixture.nativeElement.querySelector('input[type="password"]')).toBeNull();
    page.cancelMfa();expect(page.challenge()).toBeNull();
  });
  it('stores the final JWT only after MFA verification and uses server permissions',()=>{
    const page=login().componentInstance;page.codeForm.setValue({code:'123456'});page.verifyMfa();
    const req=http.expectOne('/api/auth/mfa/verify');expect(req.request.body).toEqual({challengeToken:challenge.challengeToken,code:'123456'});
    req.flush(session);expect(auth.save).toHaveBeenCalledOnceWith(session);
    http.expectOne('/api/access/me').flush({role:'TENANT_ADMIN',permissions:['ORGANIZATION_VIEW']});
    expect(navigate).toHaveBeenCalledWith(['/dashboard']);expect(page.codeForm.controls.code.value).toBe('');
  });
  it('keeps an invalid second factor out of storage and clears the entered code',()=>{
    const page=login().componentInstance;page.codeForm.setValue({code:'000000'});page.verifyMfa();
    http.expectOne('/api/auth/mfa/verify').flush({}, {status:401,statusText:'Unauthorized'});
    expect(auth.save).not.toHaveBeenCalled();expect(page.challenge()).toEqual(challenge);expect(page.codeForm.controls.code.value).toBe('');expect(page.error()).toContain('código');
  });
  it('revokes other devices without logging out the current one',()=>{
    const page=account().componentInstance;page.confirming.set('others');page.revoke();
    const req=http.expectOne('/api/security/sessions/revoke-others');expect(req.request.method).toBe('POST');req.flush(null);
    http.expectOne('/api/security/sessions').flush([device]);expect(auth.logout).not.toHaveBeenCalled();expect(page.sessions().length).toBe(1);
    page.confirming.set('current');page.revoke();http.expectOne('/api/security/sessions/current').flush(null);
    expect(auth.logout).toHaveBeenCalled();expect(navigate).toHaveBeenCalledWith(['/login']);
  });
  it('preserves the local session when server revocation fails',()=>{
    const page=account().componentInstance;page.confirming.set('current');page.revoke();
    http.expectOne('/api/security/sessions/current').flush({}, {status:503,statusText:'Unavailable'});
    expect(auth.logout).not.toHaveBeenCalled();expect(page.confirming()).toBe('current');expect(page.busy()).toBeFalse();expect(page.error()).toContain('operación');
  });
  it('clears the setup seed after enrollment and shows backup codes only until dismissed',()=>{
    const fixture=account(),page=fixture.componentInstance;
    page.setup.set({secret:'PRIVATE-SEED',otpauthUri:'otpauth://test',expiresAt:challenge.expiresAt});page.qr.set('data:image/png;base64,test');
    page.confirmForm.setValue({code:'123456'});page.enable();http.expectOne('/api/security/mfa/confirm').flush({session,recoveryCodes:['REC-private']});
    http.expectOne('/api/security/sessions').flush([device]);fixture.detectChanges();
    expect(page.setup()).toBeNull();expect(page.qr()).toBe('');expect(page.confirmForm.controls.code.value).toBe('');expect(fixture.nativeElement.textContent).not.toContain('PRIVATE-SEED');
    expect(fixture.nativeElement.textContent).toContain('REC-private');
    const hide=Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).find(b=>b.textContent?.includes('Ya guardé'))!;hide.click();fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('REC-private');
  });
  it('offers global students their own sessions without administrative MFA',()=>{
    auth.current=()=>({...session,role:'STUDENT',tenantId:null});const fixture=TestBed.createComponent(AccountSecurityPage);fixture.detectChanges();
    http.expectOne('/api/security/sessions').flush([device]);http.expectNone('/api/security/mfa');fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Autenticación en dos pasos');expect(fixture.nativeElement.textContent).toContain('Correo y contraseña');
  });
});
