import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
export interface ResultPage<T>{items:T[];total:number;page:number;size:number;}
export interface AuditItem{id:string;occurredAt:string;tenantId:string|null;actorId:string|null;actorType:string;actorRole:string;action:string;resourceType:string;resourceId:string;correlationId:string;changes:string|null;}
export interface MailItem{id:string;studentId:string;kind:string;state:string;attempts:number;createdAt:string;nextAttemptAt:string;sentAt:string|null;lastError:string|null;}
export const ACTION_LABELS:Record<string,string>={ORGANIZATION_REGISTERED:'Organización registrada',ORGANIZATION_UPDATED:'Datos institucionales actualizados',ORGANIZATION_STATUS_CHANGED:'Estado institucional cambiado',ROLE_CREATED:'Rol creado',ROLE_UPDATED:'Permisos del rol actualizados',ROLE_DELETED:'Rol eliminado',USER_CREATED:'Usuario interno creado',USER_ROLES_CHANGED:'Roles de usuario actualizados',USER_DISABLED:'Usuario desactivado',CREDENTIAL_CREATED:'Borrador creado',DOCUMENT_ATTACHED:'Documento adjuntado',DOCUMENT_GENERATED:'Documento generado',CREDENTIAL_ISSUED:'Credencial emitida',CREDENTIAL_REVOKED:'Credencial revocada',STUDENT_REGISTERED:'Estudiante registrado',EMAIL_VERIFIED:'Correo confirmado',PASSWORD_RESET_REQUESTED:'Recuperación solicitada',PASSWORD_RESET:'Contraseña recuperada',PASSWORD_CHANGED:'Contraseña cambiada',EMAIL_CHANGE_REQUESTED:'Cambio de correo solicitado',EMAIL_CHANGED:'Correo cambiado',ACCOUNT_DEACTIVATED:'Cuenta desactivada',MFA_ENABLED:'MFA habilitado',MFA_DISABLED:'MFA deshabilitado',SESSION_REVOKED:'Sesión cerrada',OTHER_SESSIONS_REVOKED:'Otras sesiones cerradas'};
export const MAIL_LABELS:Record<string,string>={PENDING:'Pendiente',PROCESSING:'En proceso',RETRY:'Reintento programado',SENT:'Aceptado por SMTP',FAILED:'Intentos agotados',CANCELLED:'Cancelado'};
export const MAIL_KINDS:Record<string,string>={EMAIL_VERIFICATION:'Confirmación de correo',PASSWORD_RESET:'Recuperación de contraseña',EMAIL_CHANGE:'Confirmación de nuevo correo'};
export const MAIL_ERRORS:Record<string,string>={TOKEN_UNAVAILABLE:'El enlace dejó de estar vigente',PAYLOAD_UNAVAILABLE:'No se pudo leer el contenido protegido',SENDER_UNAVAILABLE:'Servicio de correo no configurado',DELIVERY_FAILED:'El servicio de correo no aceptó el envío',DELIVERY_INTERRUPTED:'Se agotaron los intentos tras interrupciones'};
@Injectable({providedIn:'root'})
export class AuditApi{
 private readonly http=inject(HttpClient);
 events(global:boolean,filters:Record<string,string|number>){return this.http.get<ResultPage<AuditItem>>(global?'/api/platform/audit':'/api/audit/tenant',{params:this.params(filters)});}
 mail(filters:Record<string,string|number>){return this.http.get<ResultPage<MailItem>>('/api/platform/mail',{params:this.params(filters)});}
 private params(filters:Record<string,string|number>){let params=new HttpParams();for(const [key,value] of Object.entries(filters))if(value!=='')params=params.set(key,String(value));return params;}
}
