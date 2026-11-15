import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
export interface DocumentTemplate{id:string;familyId:string;version:number;name:string;credentialType:string;heading:string;bodyText:string;orientation:string;active:boolean;}
export interface TemplateInput{previousVersionId:string|null;name:string;credentialType:string;heading:string;bodyText:string;orientation:string;}
export interface SigningStatus{configured:boolean;vaultAvailable:boolean;demoAvailable:boolean;required:boolean;testOnly:boolean;fingerprint:string|null;subject:string|null;expiresAt:string|null;}
@Injectable({providedIn:'root'})
export class TemplatesApi {
 private readonly http=inject(HttpClient);
 list(){return this.http.get<DocumentTemplate[]>('/api/institution/templates');}
 create(body:TemplateInput){return this.http.post<DocumentTemplate>('/api/institution/templates',body);}
 activate(id:string,active:boolean){return this.http.patch<DocumentTemplate>('/api/institution/templates/'+id+'/active',{active});}
 preview(id:string){return this.http.get('/api/institution/templates/'+id+'/preview',{responseType:'blob'});}
 signing(){return this.http.get<SigningStatus>('/api/institution/signing-key');}
 uploadSigner(file:File,password:string,required:boolean){const body=new FormData();body.append('file',file);body.append('password',password);body.append('required',String(required));return this.http.post<SigningStatus>('/api/institution/signing-key',body);}
 demo(required:boolean){return this.http.post<SigningStatus>('/api/institution/signing-key/demo',{required});}
 policy(required:boolean){return this.http.patch<SigningStatus>('/api/institution/signing-key/policy',{required});}
}
