import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Page } from '../academic/academic.api';
import { StudentProfileUpdate } from '../student-account/student-account.api';
export interface IdentityCase{id:string;studentId:string;country:string;documentType:string;documentNumber:string;reason:string;status:'OPEN'|'OBSERVED'|'RESOLVED'|'REJECTED'|'WITHDRAWN';resolution:string|null;documentsAvailable:boolean;createdAt:string;resolvedAt:string|null;events:{action:string;status:string;reason:string|null;occurredAt:string}[];}
export const CASE_LABELS:Record<string,string>={OPEN:'Abierta',OBSERVED:'Necesita aclaración',RESOLVED:'Perfil corregido',REJECTED:'Rechazada',WITHDRAWN:'Retirada'};
@Injectable({providedIn:'root'})
export class IdentityCenterApi {
 private readonly http=inject(HttpClient);
 avatarStatus(){return this.http.get<{present:boolean}>('/api/me/avatar/status');}
 avatar(){return this.http.get('/api/me/avatar',{responseType:'blob'});}
 uploadAvatar(file:File){const body=new FormData();body.append('file',file);return this.http.post<void>('/api/me/avatar',body);}
 removeAvatar(){return this.http.delete<void>('/api/me/avatar');}
 own(){return this.http.get<IdentityCase[]>('/api/me/identity-disputes');}
 create(claim:{country:string;documentType:string;documentNumber:string;reason:string},front:File,back:File){const body=new FormData();Object.entries(claim).forEach(([key,value])=>body.append(key,value));body.append('front',front);body.append('back',back);return this.http.post<IdentityCase>('/api/me/identity-disputes',body);}
 resubmit(id:string,explanation:string,front:File,back:File){const body=new FormData();body.append('explanation',explanation);body.append('front',front);body.append('back',back);return this.http.post<IdentityCase>('/api/me/identity-disputes/'+id+'/resubmit',body);}
 withdraw(id:string){return this.http.post<IdentityCase>('/api/me/identity-disputes/'+id+'/withdraw',{});}
 inbox(status:string,page=0){return this.http.get<Page<IdentityCase>>('/api/platform/identity-disputes',{params:{...(status?{status}:{}),page,size:20}});}
 resolve(id:string,action:string,reason:string,profile:StudentProfileUpdate|null){return this.http.post<IdentityCase>('/api/platform/identity-disputes/'+id+'/resolution',{action,reason,profile});}
 file(own:boolean,id:string,part:string){return this.http.get((own?'/api/me':'/api/platform')+'/identity-disputes/'+id+'/files/'+part,{responseType:'blob'});}
}
