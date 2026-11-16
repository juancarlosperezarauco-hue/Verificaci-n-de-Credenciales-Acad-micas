import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthResponse } from './models';
export interface DeviceSession {id:string;device:string;createdAt:string;lastSeenAt:string;expiresAt:string;current:boolean;mfaVerified:boolean;}
export interface MfaStatus {enabled:boolean;recoveryCodesRemaining:number;pendingSetup?:boolean;pendingExpiresAt?:string|null;}
export interface MfaSetup {secret:string;otpauthUri:string;expiresAt:string;}
@Injectable({providedIn:'root'})
export class AccountSecurityApi {
 private readonly http=inject(HttpClient);
 sessions(){return this.http.get<DeviceSession[]>('/api/security/sessions');}
 revoke(id:string){return this.http.delete<void>('/api/security/sessions/'+id);}
 logout(){return this.http.delete<void>('/api/security/sessions/current');}
 revokeOthers(){return this.http.post<void>('/api/security/sessions/revoke-others',{});}
 mfaStatus(){return this.http.get<MfaStatus>('/api/security/mfa');}
 setup(password:string){return this.http.post<MfaSetup>('/api/security/mfa/setup',{password});}
 confirm(code:string){return this.http.post<{session:AuthResponse;recoveryCodes:string[]}>('/api/security/mfa/confirm',{code});}
 disable(password:string,code:string){return this.http.delete<void>('/api/security/mfa',{body:{password,code}});}
 replacement(password:string,code:string){return this.http.post<MfaSetup>('/api/security/mfa/replacement',{password,code});}
 regenerate(password:string,code:string){return this.http.post<{session:AuthResponse;recoveryCodes:string[]}>('/api/security/mfa/recovery-codes',{password,code});}
 cancelPending(){return this.http.delete<void>('/api/security/mfa/pending');}
 verify(challengeToken:string,code:string){return this.http.post<AuthResponse>('/api/auth/mfa/verify',{challengeToken,code});}
}
