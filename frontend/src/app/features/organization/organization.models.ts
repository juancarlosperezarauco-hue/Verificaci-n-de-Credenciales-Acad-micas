import { InstitutionType } from '../../core/models';
export type OrganizationStatus = 'PENDING_REVIEW' | 'ACTIVE' | 'OBSERVED' | 'SUSPENDED' | 'REJECTED' | 'INACTIVE';
export interface Representative { fullName: string; documentType: string; documentNumber: string; position: string; email: string; phone: string; }
export interface OrganizationDetails {
 legalName: string; institutionType: InstitutionType; educationLevels: string[];
 country: string; region: string; address: string; phone: string; contactEmail: string;
 website: string; domain: string; taxId: string; educationLicense: string;
 ministryRegistration: string; accreditation: string; representative: Representative;
}
export interface OrganizationSummary { id: string; name: string; slug: string; institutionType: InstitutionType; active: boolean; status: OrganizationStatus; }
export interface StatusEvent { id: string; actorId: string; previousStatus: OrganizationStatus; status: OrganizationStatus; reason: string; createdAt: string; }
export interface AccessProfile { userId: string; tenantId: string | null; role: string; permissions: string[]; }
export interface TenantRole { id: string; name: string; permissions: string[]; }
export interface StaffUser { id: string; fullName: string | null; email: string; active: boolean; role: string; roleIds: string[]; }
export const STATUS_LABELS: Record<OrganizationStatus, string> = {
 PENDING_REVIEW: 'En revisión', ACTIVE: 'Activa', OBSERVED: 'Observada', SUSPENDED: 'Suspendida', REJECTED: 'Rechazada', INACTIVE: 'Inactiva'
};
export const PERMISSION_LABELS: Record<string, string> = {
 ORGANIZATION_VIEW: 'Consultar organización', ORGANIZATION_UPDATE: 'Editar organización',
 USER_VIEW: 'Consultar usuarios', USER_CREATE: 'Crear usuarios', USER_UPDATE: 'Editar usuarios', USER_DISABLE: 'Desactivar usuarios',
 ROLE_VIEW: 'Consultar roles', ROLE_CREATE: 'Crear roles', ROLE_UPDATE: 'Editar roles', ROLE_DELETE: 'Eliminar roles',
 STUDENT_VIEW: 'Consultar estudiantes', STUDENT_LINK: 'Vincular estudiantes',
 CREDENTIAL_VIEW: 'Consultar credenciales', CREDENTIAL_CREATE: 'Crear credenciales', CREDENTIAL_ISSUE: 'Emitir credenciales',
 CREDENTIAL_REVOKE: 'Revocar credenciales', IDENTITY_REVIEW: 'Revisar identidad', REPORT_VIEW: 'Consultar reportes', AUDIT_VIEW: 'Consultar auditoría'
};

