import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
export interface Page<T>{items:T[];total:number;page:number;size:number;}
export interface Program{id:string;name:string;kind:string;period:string;requirements:string|null;active:boolean;}
export interface AcademicRecord{id:string;tenantId:string;institutionName:string;studentId:string;studentName:string;programId:string;programName:string;period:string;requirements:string|null;registrationCode:string;status:'ENROLLED'|'COMPLETED'|'WITHDRAWN';enrolledOn:string;completedOn:string|null;result:string|null;notes:string|null;createdAt:string;}
export const ACADEMIC_LABELS:Record<string,string>={ENROLLED:'Matriculado',COMPLETED:'Finalizado',WITHDRAWN:'Retirado'};
export function cycleError(e:HttpErrorResponse){return e.status===409?'El registro cambió o ya existe. Actualiza la lista y comprueba los datos.':typeof e.error?.message==='string'?e.error.message:e.status===403?'No tienes permiso para esta operación.':'No se pudo completar la operación. Inténtalo nuevamente.';}
export function downloadBlob(blob:Blob,name:string){const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
@Injectable({providedIn:'root'})
export class AcademicApi {
 private readonly http=inject(HttpClient);
 programs(){return this.http.get<Program[]>('/api/institution/academic/programs');}
 saveProgram(id:string|null,body:{name:string;kind:string;period:string;requirements:string;active:boolean}){return id?this.http.put<Program>('/api/institution/academic/programs/'+id,body):this.http.post<Program>('/api/institution/academic/programs',body);}
 records(page=0){return this.http.get<Page<AcademicRecord>>('/api/institution/academic/records',{params:{page,size:20}});}
 enroll(body:{studentId:string;programId:string;registrationCode:string;enrolledOn:string}){return this.http.post<AcademicRecord>('/api/institution/academic/records',body);}
 update(id:string,body:{status:string;completedOn:string|null;result:string;notes:string}){return this.http.patch<AcademicRecord>('/api/institution/academic/records/'+id,body);}
 own(){return this.http.get<AcademicRecord[]>('/api/me/academic-records');}
}
