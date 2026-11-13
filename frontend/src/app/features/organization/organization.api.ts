import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { map, tap } from 'rxjs';
import { AuthResponse } from '../../core/models';
import { AccessProfile, OrganizationDetails, OrganizationStatus, OrganizationSummary, StaffUser, StatusEvent, TenantRole } from './organization.models';
@Injectable({providedIn:'root'})
export class OrganizationApi {
 private readonly http = inject(HttpClient);
 readonly access = signal<AccessProfile | null>(null);
 me() { return this.http.get<AccessProfile>('/api/access/me').pipe(tap(p => this.access.set(p))); }
 can(permission: string) { return this.access()?.permissions.includes(permission) ?? false; }
 platformLogin(body: {email:string; password:string}) {
  return this.http.post<Pick<AuthResponse,'token'|'tokenType'|'email'|'role'>>('/api/auth/platform/login',body)
    .pipe(map(r => ({...r, tenantId:null, institutionName:null, institutionType:null} as AuthResponse)));
 }
 register(body:{organizationSlug:string; password:string; profile:OrganizationDetails}) { return this.http.post<AuthResponse>('/api/auth/organizations/register',body); }
 details() { return this.http.get<OrganizationDetails>('/api/institution/details'); }
 myOrganization() { return this.http.get<OrganizationSummary>('/api/institution/status'); }
 submitReview() { return this.http.post<OrganizationSummary>('/api/institution/submit-review',{}); }
 saveDetails(body:OrganizationDetails) { return this.http.put<OrganizationDetails>('/api/institution/details',body); }
 organizations() { return this.http.get<OrganizationSummary[]>('/api/platform/organizations'); }
 organization(id:string) { return this.http.get<OrganizationSummary>('/api/platform/organizations/'+id); }
 organizationDetails(id:string) { return this.http.get<OrganizationDetails>('/api/platform/organizations/'+id+'/details'); }
 history(id:string) { return this.http.get<StatusEvent[]>('/api/platform/organizations/'+id+'/history'); }
 changeStatus(id:string,status:OrganizationStatus,reason:string) { return this.http.patch<OrganizationSummary>('/api/platform/organizations/'+id+'/status',{status,reason}); }
 roles() { return this.http.get<TenantRole[]>('/api/access/roles'); }
 permissions() { return this.http.get<string[]>('/api/access/permissions'); }
 saveRole(id:string|null,body:{name:string;permissions:string[]}) { return id ? this.http.put<TenantRole>('/api/access/roles/'+id,body) : this.http.post<TenantRole>('/api/access/roles',body); }
 deleteRole(id:string) { return this.http.delete<void>('/api/access/roles/'+id); }
 users() { return this.http.get<StaffUser[]>('/api/access/users'); }
 createUser(body:{fullName:string;email:string;password:string;roleIds:string[]}) { return this.http.post<StaffUser>('/api/access/users',body); }
 updateUser(id:string,body:{fullName:string;roleIds:string[]}) { return this.http.put<StaffUser>('/api/access/users/'+id,body); }
 disableUser(id:string) { return this.http.patch<void>('/api/access/users/'+id+'/disable',{}); }
}
export function requestError(error:unknown):string {
 if(error instanceof HttpErrorResponse) {
  if(error.status===0) return 'No se pudo conectar con el servidor. Intenta nuevamente.';
  if(error.status===401 || error.status===403) return 'Tu sesión no tiene acceso a esta operación. Inicia sesión nuevamente o consulta al administrador.';
  if(error.status===409) return 'El correo, identificación fiscal o licencia ya está registrado. Revisa los datos.';
  if(error.status===404) return 'El registro ya no está disponible.';
  if(error.status===400) return typeof error.error?.message==='string' ? error.error.message : 'Revisa los campos del formulario.';
 }
 return 'No se pudo completar la operación. Intenta nuevamente.';
}
