import { Component, computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../auth.service';
import { OrganizationApi } from '../../features/organization/organization.api';
import { NavigationComponent } from './navigation.component';

@Component({template:''}) class Destination {}
describe('Navigation disclosure behavior',()=>{
 const permissions=signal({permissions:['STUDENT_VIEW','IDENTITY_REVIEW','ORGANIZATION_VIEW','AUDIT_VIEW'],onboarding:false});
 beforeEach(()=>{permissions.set({permissions:['STUDENT_VIEW','IDENTITY_REVIEW','ORGANIZATION_VIEW','AUDIT_VIEW'],onboarding:false});const current=signal({role:'TENANT_ADMIN',token:'test'});TestBed.configureTestingModule({providers:[provideRouter([{path:'verify',component:Destination}]),{provide:AuthService,useValue:{current,isAuthenticated:computed(()=>true)}},{provide:OrganizationApi,useValue:{access:permissions}}]});});
 function create(){const fixture=TestBed.createComponent(NavigationComponent);fixture.detectChanges();const trigger=fixture.nativeElement.querySelector('.mobile-trigger') as HTMLButtonElement;trigger.focus();trigger.click();fixture.detectChanges();return {fixture,trigger,page:fixture.componentInstance};}
 it('Escape closes the disclosure and returns focus to its trigger',()=>{const {fixture,trigger,page}=create();expect(page.openMenu()).toBe('mobile');document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));fixture.detectChanges();expect(page.openMenu()).toBeNull();expect(fixture.nativeElement.querySelector('#mobile-navigation')).toBeNull();expect(document.activeElement).toBe(trigger);});
 it('outside clicks dismiss the disclosure and a route change closes it too',async()=>{const {fixture,trigger,page}=create();document.body.click();fixture.detectChanges();expect(page.openMenu()).toBeNull();trigger.click();fixture.detectChanges();await TestBed.inject(Router).navigateByUrl('/verify');fixture.detectChanges();expect(page.openMenu()).toBeNull();});
 it('permission changes remove restricted modules while navigation remains open',()=>{const {fixture}=create();expect(fixture.nativeElement.querySelector('#mobile-navigation a[href="/organization/identity"]')).not.toBeNull();permissions.set({permissions:['ORGANIZATION_VIEW'],onboarding:true});fixture.detectChanges();const panel=fixture.nativeElement.querySelector('#mobile-navigation');expect(panel.querySelector('a[href="/organization/identity"]')).toBeNull();expect(panel.querySelector('a[href="/students"]')).toBeNull();expect(panel.querySelector('a[href="/organization/team"]')).toBeNull();expect(panel.querySelector('a[href="/organization"]')).not.toBeNull();expect(panel.querySelector('a[href="/account/security"]')).not.toBeNull();});
});
