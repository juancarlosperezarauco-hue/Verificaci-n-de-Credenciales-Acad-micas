import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Page } from '../academic/academic.api';
import { DocumentTemplate } from '../templates/templates.api';
export type RequestStatus='SUBMITTED'|'IN_REVIEW'|'OBSERVED'|'REVIEWED'|'APPROVED'|'FULFILLED'|'REJECTED'|'WITHDRAWN';
export interface RequestAttachment{id:string;name:string;contentType:string;createdAt:string;}
export interface CredentialRequest{id:string;tenantId:string;institutionName:string;studentId:string;studentName:string;recordId:string;programName:string;credentialType:string;title:string;message:string|null;status:RequestStatus;reviewerId:string|null;approverId:string|null;templateVersionId:string|null;sourceCredentialId:string|null;credentialId:string|null;profileChanged:boolean;createdAt:string;updatedAt:string;attachments:RequestAttachment[];events:{id:string;action:string;status:string;reason:string|null;occurredAt:string}[];}
export const REQUEST_LABELS:Record<string,string>={SUBMITTED:'Enviada',IN_REVIEW:'En revisión',OBSERVED:'Necesita correcciones',REVIEWED:'Revisión terminada',APPROVED:'Aprobada para emisión',FULFILLED:'Documento emitido',REJECTED:'Rechazada',WITHDRAWN:'Retirada'};
@Injectable({providedIn:'root'})
export class WorkflowApi {
 private readonly http=inject(HttpClient);
 own(){return this.http.get<CredentialRequest[]>('/api/me/credential-requests');}
 inbox(status:string,page=0){return this.http.get<Page<CredentialRequest>>('/api/institution/credential-requests',{params:{...(status?{status}:{}),page,size:20}});}
 detail(id:string){return this.http.get<CredentialRequest>('/api/institution/credential-requests/'+id);}
 create(body:{recordId:string;credentialType:string;title:string;message:string;templateVersionId:string|null}){return this.http.post<CredentialRequest>('/api/me/credential-requests',body);}
 correction(id:string,body:{recordId:string;title:string;reason:string;templateVersionId:string|null}){return this.http.post<CredentialRequest>('/api/me/credentials/'+id+'/corrections',body);}
 catalog(record:string){return this.http.get<DocumentTemplate[]>('/api/me/academic-records/'+record+'/templates');}
 action(own:boolean,id:string,action:string,reason:string){return this.http.post<CredentialRequest>((own?'/api/me':'/api/institution')+'/credential-requests/'+id+'/actions',{action,reason});}
 update(id:string,body:{title:string;message:string;templateVersionId:string|null}){return this.http.patch<CredentialRequest>('/api/me/credential-requests/'+id,body);}
 upload(id:string,file:File){const body=new FormData();body.append('file',file);return this.http.post<RequestAttachment>('/api/me/credential-requests/'+id+'/attachments',body);}
 remove(id:string,attachment:string){return this.http.delete<void>('/api/me/credential-requests/'+id+'/attachments/'+attachment);}
 file(own:boolean,id:string,attachment:string){return this.http.get((own?'/api/me':'/api/institution')+'/credential-requests/'+id+'/attachments/'+attachment,{responseType:'blob'});}
}
