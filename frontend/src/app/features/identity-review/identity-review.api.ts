import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
export type ReviewStatus='PENDING'|'VERIFIED'|'REJECTED';
export type ImagePart='FRONT'|'BACK'|'PHOTO';
export interface IdentitySnapshot{fullName:string;country:string;documentType:string;documentNumber:string;dateOfBirth:string;department:string;}
export interface IdentityReview{id:string;studentId:string;tenantId:string;institutionName:string;status:ReviewStatus;effectiveStatus:ReviewStatus|'UNVERIFIED';profile:IdentitySnapshot;profileChanged:boolean;sharingActive:boolean;photoPresent:boolean;submittedAt:string;reviewedAt:string|null;withdrawnAt:string|null;reason:string|null;documentsAvailable?:boolean;}
export interface IdentityState{profileComplete:boolean;emailVerified:boolean;institutions:{id:string;name:string;slug:string}[];submissions:IdentityReview[];}
export interface ReviewPage{items:IdentityReview[];total:number;page:number;size:number;}
export const REVIEW_LABELS:Record<string,string>={UNVERIFIED:'Sin verificar',PENDING:'En revisión',VERIFIED:'Verificado',REJECTED:'Rechazado'};
@Injectable({providedIn:'root'})
export class IdentityReviewApi {
 private readonly http=inject(HttpClient);
 own(){return this.http.get<IdentityState>('/api/me/identity-verifications');}
 submit(tenantId:string,front:File,back:File,photo:File|null){const form=new FormData();form.append('tenantId',tenantId);form.append('accepted','true');form.append('front',front);form.append('back',back);if(photo)form.append('photo',photo);return this.http.post<IdentityReview>('/api/me/identity-verifications',form);}
 withdraw(id:string){return this.http.post<void>('/api/me/identity-verifications/'+id+'/withdraw',{});}
 inbox(status:ReviewStatus|'',page=0){return this.http.get<ReviewPage>('/api/institution/identity-verifications',{params:{...(status?{status}:{}),page,size:20}});}
 detail(id:string){return this.http.get<IdentityReview>('/api/institution/identity-verifications/'+id);}
 decide(id:string,status:'VERIFIED'|'REJECTED',reason:string){return this.http.post<IdentityReview>('/api/institution/identity-verifications/'+id+'/decision',{status,reason:status==='REJECTED'?reason:null});}
 file(own:boolean,id:string,part:ImagePart){return this.http.get((own?'/api/me':'/api/institution')+'/identity-verifications/'+id+'/files/'+part,{responseType:'blob'});}
}
