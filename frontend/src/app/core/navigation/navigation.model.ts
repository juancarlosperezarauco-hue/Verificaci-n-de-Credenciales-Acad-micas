export interface NavigationLink {label:string;path:string;}
export interface NavigationModel {primary:NavigationLink[];organization:NavigationLink[];organizationLabel:string;account:NavigationLink[];}

/** Keep each role's navigation independent from the header layout. API guards remain authoritative. */
export function navigationFor(role:string|null,permissions:readonly string[],onboarding:boolean):NavigationModel {
 const link=(label:string,path:string):NavigationLink=>({label,path});
 const verify=link('Verificar','/verify');
 const account=[link('Mis sesiones','/account/security')];
 if(role==='STUDENT')return {primary:[link('Mi expediente','/student/account'),link('Mi identidad','/student/identity'),verify],organization:[],organizationLabel:'',account:[link('Seguridad de mi cuenta','/student/security'),...account]};
 if(role==='SUPER_ADMIN')return {primary:[link('Organizaciones','/superadmin/organizations'),verify],organizationLabel:'Administración',organization:[link('Auditoría','/superadmin/audit'),link('Correos','/superadmin/mail'),link('Mantenimiento','/superadmin/security')],account};
 if(!role)return {primary:[link('Estudiantes','/student/login'),link('Instituciones','/register'),link('Verificación','/verify')],organization:[],organizationLabel:'',account:[]};
 const can=(permission:string)=>permissions.includes(permission);
 const primary:NavigationLink[]=[];const organization:NavigationLink[]=[];
 if(can('ORGANIZATION_VIEW')&&!onboarding)primary.push(link('Panel','/dashboard'));
 if(can('STUDENT_VIEW')&&!onboarding)primary.push(link('Estudiantes','/students'));
 if(can('CREDENTIAL_VIEW')&&!onboarding)primary.push(link('Credenciales','/credentials'));
 primary.push(verify);
 if(can('IDENTITY_REVIEW')&&!onboarding)organization.push(link('Revisar identidad','/organization/identity'));
 if(can('ORGANIZATION_VIEW'))organization.push(link('Perfil institucional','/organization'),link('Imagen institucional','/institution'));
 if(['ADMIN','TENANT_ADMIN'].includes(role)&&!onboarding)organization.push(link('Equipo y roles','/organization/team'));
 if(can('AUDIT_VIEW')&&!onboarding)organization.push(link('Auditoría','/organization/audit'));
 return {primary,organization,organizationLabel:'Organización',account};
}
