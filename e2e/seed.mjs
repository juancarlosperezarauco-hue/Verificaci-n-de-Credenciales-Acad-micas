// Creates only fictional records in the fixed, separate audit compose project.
import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { mkdirSync,writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
if(process.env.E2E_ALLOW_FIXTURES!=='isolated-audit')throw new Error('Configura E2E_ALLOW_FIXTURES=isolated-audit para crear datos ficticios.');
const root=resolve(import.meta.dirname,'..');
const base='http://127.0.0.1:4301';
const docker=process.env.DOCKER_BIN??'docker';
const env=process.env.E2E_COMPOSE_ENV?resolve(process.cwd(),process.env.E2E_COMPOSE_ENV):resolve(root,'.local-backups/audit-compose.env');
const password='E2e!'+randomUUID();
async function call(path,body,token,method='POST'){
  const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body!==null?{body:JSON.stringify(body)}:{})});
  if(!r.ok)throw new Error('Falló la preparación de datos: '+path+' HTTP '+r.status);
  const text=await r.text();return text?JSON.parse(text):null;
}
function uuid(value){if(!/^[0-9a-f-]{36}$/i.test(value))throw new Error('Identificador de fixture no válido');return value;}
const slug='e2e-'+randomUUID();
const adminEmail='admin-'+randomUUID()+'@example.test';
const org=await call('/api/auth/register',{organizationName:'Instituto ficticio E2E',organizationSlug:slug,institutionType:'INSTITUTO',email:adminEmail,password});
const studentEmail='student-'+randomUUID()+'@example.test';
const student=await call('/api/auth/student/register',{email:studentEmail,password,fullName:'Estudiante Ficticio E2E',country:'BO',documentType:'CI',documentNumber:randomUUID(),dateOfBirth:'2000-05-12',department:'La Paz'});
const profile=await call('/api/me/profile',null,student.token,'GET');
execFileSync(docker,['compose','-p','credentials-v3-audit','-f','compose.audit.yml','--env-file',env,'exec','-T','postgres',
  'sh','-c','psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'],
  {cwd:root,input:"update tenants set active=true where id='"+uuid(org.tenantId)+"';\nupdate organization_governance set status='ACTIVE' where tenant_id='"+uuid(org.tenantId)+"';\nupdate student_accounts set email_verified=true where id='"+uuid(profile.id)+"';\n",stdio:['pipe','ignore','pipe']});
const admin=await call('/api/auth/login',{organizationSlug:slug,email:adminEmail,password});
await call('/api/students/redeem',{code:profile.searchCode},admin.token);
const program=await call('/api/institution/academic/programs',{name:'Curso ficticio para E2E',kind:'Curso',period:'2026',active:true},admin.token);
const record=await call('/api/institution/academic/records',{studentId:profile.id,programId:program.id,registrationCode:'E2E-'+randomUUID(),enrolledOn:'2026-01-01'},admin.token);
const credential=await call('/api/credentials',{studentId:profile.id,credentialType:'CERTIFICADO',title:'Certificado ficticio de prueba E2E'},admin.token);
await call('/api/credentials/'+credential.id+'/generate-document',{},admin.token);
const issued=await call('/api/credentials/'+credential.id+'/issue',{},admin.token,'PATCH');
const request=await call('/api/me/credential-requests',{recordId:record.id,credentialType:'DIPLOMA',title:'Solicitud ficticia E2E'},student.token);
await call('/api/institution/credential-requests/'+request.id+'/actions',{action:'START'},admin.token);
await call('/api/institution/credential-requests/'+request.id+'/actions',{action:'OBSERVE',reason:'Observación ficticia para comprobar la bandeja de avisos'},admin.token);
mkdirSync(resolve(import.meta.dirname,'test-results'),{recursive:true});
writeFileSync(resolve(import.meta.dirname,'test-results/fixture.json'),JSON.stringify({slug,adminEmail,studentEmail,password,verificationCode:issued.verificationCode,tenantId:org.tenantId}),{mode:0o600});
console.log('Fixtures ficticios preparados en audit; credenciales guardadas fuera de Git.');
