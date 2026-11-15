export interface NavigationLink {
  label: string;
  path: string;
}
export interface NavigationModel {
  primary: NavigationLink[];
  organization: NavigationLink[];
  organizationLabel: string;
  account: NavigationLink[];
}

export function navigationIcon(path: string): string {
  if (path.includes('signing')) return 'key';
  if (path.includes('templates')) return 'document';
  if (path.includes('requests')) return 'inbox';
  if (path.includes('academic')) return 'academic';
  if (path.includes('identity')) return 'shield';
  if (path.includes('students')) return 'users';
  if (path.includes('credentials')) return 'document';
  if (path.includes('team')) return 'users';
  if (path.includes('audit')) return 'chart';
  if (path.includes('mail')) return 'mail';
  if (path.includes('storage')) return 'folder';
  if (path.includes('security')) return 'settings';
  if (path.includes('institution')) return 'palette';
  if (path.includes('organization') || path === '/register') return 'building';
  if (path.includes('verify')) return 'search';
  if (path.includes('student')) return 'user';
  return 'grid';
}

/** Keep each role's navigation independent from the header layout. API guards remain authoritative. */
export function navigationFor(
  role: string | null,
  permissions: readonly string[],
  onboarding: boolean,
): NavigationModel {
  const link = (label: string, path: string): NavigationLink => ({ label, path });
  const verify = link('Verificar', '/verify');
  const account = [link('Mis sesiones', '/account/security')];
  if (role === 'STUDENT')
    return {
      primary: [
        link('Mi perfil', '/student/account'),
        link('Expediente académico', '/student/academic'),
        link('Mis solicitudes', '/student/requests'),
        link('Verificación de identidad', '/student/identity'),
        link('Foto y asistencia de identidad', '/student/identity-center'),
        verify,
      ],
      organization: [],
      organizationLabel: '',
      account: [link('Seguridad de mi cuenta', '/student/security'), ...account],
    };
  if (role === 'SUPER_ADMIN')
    return {
      primary: [
        link('Organizaciones', '/superadmin/organizations'),
        link('Disputas de identidad', '/superadmin/identity-disputes'),
        verify,
      ],
      organizationLabel: 'Administración',
      organization: [
        link('Auditoría', '/superadmin/audit'),
        link('Correos', '/superadmin/mail'),
        link('Mantenimiento', '/superadmin/security'),
        link('Archivos y conservación', '/superadmin/storage'),
      ],
      account,
    };
  if (!role)
    return {
      primary: [
        link('Estudiantes', '/student/login'),
        link('Instituciones', '/register'),
        link('Verificación', '/verify'),
      ],
      organization: [],
      organizationLabel: '',
      account: [],
    };
  const can = (permission: string) => permissions.includes(permission);
  const primary: NavigationLink[] = [];
  const organization: NavigationLink[] = [];
  if (can('ORGANIZATION_VIEW') && !onboarding) primary.push(link('Panel', '/dashboard'));
  if (can('STUDENT_VIEW') && !onboarding) primary.push(link('Estudiantes', '/students'));
  if (can('ACADEMIC_VIEW') && !onboarding)
    primary.push(link('Expedientes académicos', '/organization/academic'));
  if (can('REQUEST_VIEW') && !onboarding)
    primary.push(link('Solicitudes de documentos', '/organization/requests'));
  if (can('CREDENTIAL_VIEW') && !onboarding) primary.push(link('Credenciales', '/credentials'));
  primary.push(verify);
  if (can('IDENTITY_REVIEW') && !onboarding)
    organization.push(link('Revisar identidad', '/organization/identity'));
  if (can('TEMPLATE_VIEW') && !onboarding)
    organization.push(link('Plantillas de documentos', '/organization/templates'));
  if (can('SIGNING_MANAGE') && !onboarding)
    organization.push(link('Firma institucional', '/organization/signing'));
  if (can('ORGANIZATION_VIEW'))
    organization.push(
      link('Perfil institucional', '/organization'),
      link('Imagen institucional', '/institution'),
    );
  if (['ADMIN', 'TENANT_ADMIN'].includes(role) && !onboarding)
    organization.push(link('Equipo y roles', '/organization/team'));
  if (can('AUDIT_VIEW') && !onboarding) organization.push(link('Auditoría', '/organization/audit'));
  return { primary, organization, organizationLabel: 'Organización', account };
}
