import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthResponse, PublicCredential } from '../../core/models';
export interface IdentityProfile { id:string; fullName:string|null; country:string|null; documentType:string|null; documentNumber:string|null; dateOfBirth:string|null; department:string|null; searchCode:string; profileComplete:boolean; }
export interface StudentRegistration { email:string; password:string; }
export interface StudentProfileUpdate { fullName:string; country:string; documentType:string; documentNumber:string; dateOfBirth:string; department:string; }
export interface Consent { id:string; institutionName:string; institutionSlug:string; expiresAt:string; usedAt:string|null; revokedAt:string|null; }
export interface GeneratedConsent { code:string; access:Consent; }
export interface StudentSecurity { email:string; emailVerified:boolean; active:boolean; }
@Injectable({providedIn:'root'})
export class StudentAccountApi {
  private readonly http=inject(HttpClient);
  register(body:StudentRegistration){return this.http.post<AuthResponse>('/api/auth/student/register',body);}
  login(body:{email:string;password:string}){return this.http.post<AuthResponse>('/api/auth/student/login',body);}
  profile(){return this.http.get<IdentityProfile>('/api/me/profile');}
  updateProfile(body:StudentProfileUpdate){return this.http.patch<IdentityProfile>('/api/me/profile',body);}
  rotateSearchCode(){return this.http.post<IdentityProfile>('/api/me/search-code/rotate',{});}
  history(){return this.http.get<PublicCredential[]>('/api/me/credentials');}
  consents(){return this.http.get<Consent[]>('/api/me/consents');}
  generate(institutionSlug:string){return this.http.post<GeneratedConsent>('/api/me/consents',{institutionSlug});}
  revoke(id:string){return this.http.delete<void>('/api/me/consents/'+id);}
  security(){return this.http.get<StudentSecurity>('/api/me/security');}
  resendVerification(){return this.http.post<void>('/api/me/security/email-verification',{});}
  requestPasswordReset(email:string){return this.http.post<void>('/api/auth/student/password-reset/request',{email});}
  confirmPasswordReset(token:string,newPassword:string){return this.http.post<void>('/api/auth/student/password-reset/confirm',{token,newPassword});}
  confirmEmail(token:string,type:'change'|'verification'){return this.http.post<void>(type==='change'?'/api/auth/student/email-change/confirm':'/api/auth/student/verify-email',{token});}
  changePassword(body:{currentPassword:string;newPassword:string}){return this.http.post<void>('/api/me/security/password',body);}
  requestEmailChange(body:{currentPassword:string;newEmail:string}){return this.http.post<void>('/api/me/security/email-change',body);}
  deactivate(currentPassword:string){return this.http.delete<void>('/api/me/security/account',{body:{currentPassword}});}
}
