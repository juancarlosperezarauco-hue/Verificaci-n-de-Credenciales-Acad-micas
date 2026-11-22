// Creates only fictional records in the fixed, separate audit compose project.
import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { call, organizationProfile, verifyEmail } from './support/audit-api.mjs';

if (process.env.E2E_ALLOW_FIXTURES !== 'isolated-audit')
  throw new Error('Configura E2E_ALLOW_FIXTURES=isolated-audit para crear datos ficticios.');
const root = resolve(import.meta.dirname, '..');
const docker = process.env.DOCKER_BIN ?? 'docker';
const env = process.env.E2E_COMPOSE_ENV ? resolve(process.cwd(), process.env.E2E_COMPOSE_ENV)
  : resolve(root, '.local-backups/audit-compose.env');
const password = 'E2e!' + randomUUID();
const compose = ['compose', '-p', 'credentials-v3-audit', '-f', 'compose.audit.yml', '--env-file', env];
function bootstrap(name) {
  try {
    return execFileSync(docker, [...compose, 'exec', '-T', 'backend', 'printenv', name],
      { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] }).toString().trim();
  } catch {
    throw new Error('Configura el bootstrap ficticio de superadmin únicamente en el entorno audit.');
  }
}
const superEmail = bootstrap('BOOTSTRAP_SUPERADMIN_EMAIL');
const superPassword = bootstrap('BOOTSTRAP_SUPERADMIN_PASSWORD');
if (!/@[^@]+\.test$/.test(superEmail) || superPassword.length < 12)
  throw new Error('Usa una cuenta superadmin ficticia .test en audit.');
const platform = await call('/api/auth/platform/login', { email: superEmail, password: superPassword });
const slug = 'e2e-' + randomUUID();
const adminEmail = 'admin-' + randomUUID() + '@example.test';
const org = await call('/api/auth/organizations/register', {
  organizationSlug: slug, password,
  profile: organizationProfile('Instituto ficticio E2E', adminEmail, randomUUID()),
}, null, 'POST', 201);
const pending = await call('/api/platform/organizations/' + org.tenantId, null, platform.token, 'GET');
if (pending.status !== 'PENDING_REVIEW') throw new Error('La organización debe esperar aprobación de plataforma.');
await call('/api/platform/organizations/' + org.tenantId + '/status', {
  status: 'ACTIVE', reason: 'Aprobación ficticia de aceptación E2E',
}, platform.token, 'PATCH');
const admin = await call('/api/auth/login', { organizationSlug: slug, email: adminEmail, password });
const studentEmail = 'student-' + randomUUID() + '@example.test';
const student = await call('/api/auth/student/register', { email: studentEmail, password }, null, 'POST', 201);
const incomplete = await call('/api/me/profile', null, student.token, 'GET');
if (incomplete.profileComplete) throw new Error('El registro inicial no debe exigir ni inventar identidad.');
const profile = await call('/api/me/profile', {
  fullName: 'Estudiante Ficticio E2E', country: 'BO', documentType: 'CI',
  documentNumber: randomUUID(), dateOfBirth: '2000-05-12', department: 'La Paz',
}, student.token, 'PATCH');
await verifyEmail(studentEmail);
await call('/api/students/redeem', { code: profile.searchCode }, admin.token);
const program = await call('/api/institution/academic/programs', {
  name: 'Curso ficticio para E2E', kind: 'Curso', period: '2026', active: true,
}, admin.token, 'POST', 201);
const record = await call('/api/institution/academic/records', {
  studentId: profile.id, programId: program.id,
  registrationCode: 'E2E-' + randomUUID(), enrolledOn: '2026-01-01',
}, admin.token, 'POST', 201);
const credential = await call('/api/credentials', {
  studentId: profile.id, credentialType: 'CERTIFICADO', title: 'Certificado ficticio de prueba E2E',
}, admin.token, 'POST', 201);
await call('/api/credentials/' + credential.id + '/generate-document', {}, admin.token);
const issued = await call('/api/credentials/' + credential.id + '/issue', {}, admin.token, 'PATCH');
const request = await call('/api/me/credential-requests', {
  recordId: record.id, credentialType: 'DIPLOMA', title: 'Solicitud ficticia E2E',
}, student.token, 'POST', 201);
await call('/api/institution/credential-requests/' + request.id + '/actions', { action: 'START' }, admin.token);
await call('/api/institution/credential-requests/' + request.id + '/actions', {
  action: 'OBSERVE', reason: 'Observación ficticia para comprobar la bandeja de avisos',
}, admin.token);
mkdirSync(resolve(import.meta.dirname, 'test-results'), { recursive: true });
writeFileSync(resolve(import.meta.dirname, 'test-results/fixture.json'), JSON.stringify({
  slug, adminEmail, studentEmail, password, verificationCode: issued.verificationCode,
  tenantId: org.tenantId, superEmail, superPassword,
}), { mode: 0o600 });
console.log('Fixtures ficticios preparados por API y Mailpit; credenciales fuera de Git.');
