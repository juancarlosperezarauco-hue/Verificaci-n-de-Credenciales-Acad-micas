// Explicit acceptance against the PRIVATE AWS pilot. Never imports the audit seed.
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { homedir } from 'node:os';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

if (!process.argv.includes('--run-fictional-flow')) {
  throw new Error('Requires --run-fictional-flow: creates clearly labelled fictional pilot records.');
}
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const base = 'http://localhost:4302', mail = 'http://localhost:8126';
const aws = process.platform === 'win32'
  ? resolve(process.env.LOCALAPPDATA, 'Programs/Amazon/AWSCLIV2/aws.exe') : 'aws';
function awsRead(args) {
  return JSON.parse(execFileSync(aws, [...args, '--profile', 'credenciales', '--region', 'us-east-2', '--output', 'json', '--no-cli-pager'],
    { encoding: 'utf8', env: { ...process.env, AWS_PAGER: '', AWS_EC2_METADATA_DISABLED: 'true' }, timeout: 30_000 }));
}
assert.equal(awsRead(['sts', 'get-caller-identity']).Account, '098453331970');
const inventory = awsRead(['ec2', 'describe-instances', '--filters',
  'Name=tag:Deployment,Values=credenciales-private',
  'Name=tag:ManagedBy,Values=credentials-private-deploy', 'Name=instance-state-name,Values=running']);
assert.equal(inventory.Reservations.flatMap(r => r.Instances).length, 1, 'Expected one running owned pilot');

const report = { startedAt: new Date().toISOString(), passed: [], fixtureOrganizationIds: [], cleanupErrors: [], success: false };
const tokens = new Set();
let platform;
async function call(path, body, token, method = 'POST', expected = 200, extra = {}) {
  assert(path.startsWith('/api/'));
  const response = await fetch(base + path, { method, redirect: 'error', signal: AbortSignal.timeout(30_000),
    headers: { 'Content-Type': 'application/json', 'User-Agent': 'Credenciales-AWS-Acceptance/1.0',
      ...(token ? { Authorization: 'Bearer ' + token } : {}), ...extra },
    ...(body !== null ? { body: JSON.stringify(body) } : {}) });
  assert.equal(response.status, expected, method + ' ' + path + ': unexpected HTTP status');
  const text = await response.text(), result = text ? JSON.parse(text) : null;
  if (result?.token) tokens.add(result.token);
  return result;
}
function passed(name) { report.passed.push(name); process.stdout.write('OK: ' + name + '\n'); }
function profile(email, suffix) {
  return { legalName: 'PRUEBA AWS - Institución ficticia ' + suffix.slice(0, 8), institutionType: 'INSTITUTO', educationLevels: [],
    country: 'BO', region: 'Santa Cruz', address: 'Dirección ficticia de pruebas AWS', phone: '70000000',
    contactEmail: email, website: '', domain: '', taxId: suffix, educationLicense: 'TEST-' + suffix,
    ministryRegistration: '', accreditation: '', representative: { fullName: 'Representante ficticio AWS',
      documentType: 'CI', documentNumber: suffix, position: 'Administrador', email, phone: '70000001' } };
}
async function verifyEmail(email) {
  assert(email.endsWith('@example.test'));
  const deadline = Date.now() + 30_000;
  while (Date.now() < deadline) {
    const response = await fetch(mail + '/api/v1/messages?limit=200', { signal: AbortSignal.timeout(5000) });
    assert.equal(response.status, 200);
    const message = (await response.json()).messages?.find(m => m.Subject === 'Verifica tu correo' && m.To?.some(t => t.Address === email));
    if (message) {
      const detail = await fetch(mail + '/api/v1/message/' + encodeURIComponent(message.ID));
      assert.equal(detail.status, 200);
      const token = (await detail.json()).Text?.match(/token=([A-Za-z0-9_-]+)/)?.[1];
      if (token) return call('/api/auth/student/verify-email', { token }, null, 'POST', 204);
    }
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw new Error('Fictional verification email did not arrive');
}

try {
  const access = readFileSync(resolve(homedir(), '.codex/private/credenciales-aws/acceso-inicial.txt'), 'utf8');
  const email = access.match(/^Correo: (.+)$/m)?.[1].trim();
  const password = access.match(/^Contraseña inicial: (.+)$/m)?.[1].trim();
  assert(email && password, 'Missing private administrator access file');
  platform = await call('/api/auth/platform/login', { email, password });
  assert(platform.token, 'Administrator requires an updated password or human MFA verification');
  await call('/api/platform/organizations', null, platform.token, 'GET');
  passed('Superadministración y consulta protegida');
  const fictionalPassword = 'AwsPilot!' + randomUUID();
  async function institution() {
    const suffix = randomUUID(), email = 'aws-admin-' + suffix + '@example.test', slug = 'awscheck-' + suffix;
    const registered = await call('/api/auth/organizations/register', {
      organizationSlug: slug, password: fictionalPassword, profile: profile(email, suffix),
    }, null, 'POST', 201);
    report.fixtureOrganizationIds.push(registered.tenantId);
    await call('/api/students', null, registered.token, 'GET', 403);
    await call('/api/platform/organizations/' + registered.tenantId + '/status', {
      status: 'ACTIVE', reason: 'PRUEBA AWS: aprobación de institución ficticia para aceptación',
    }, platform.token, 'PATCH');
    return { ...(await call('/api/auth/login', { organizationSlug: slug, email, password: fictionalPassword })), slug };
  }
  const admin = await institution(), other = await institution();
  passed('Registro de dos instituciones, bloqueo pendiente y aprobación');
  const studentEmail = 'aws-student-' + randomUUID() + '@example.test';
  const student = await call('/api/auth/student/register', { email: studentEmail, password: fictionalPassword }, null, 'POST', 201);
  assert.equal((await call('/api/me/profile', null, student.token, 'GET')).profileComplete, false);
  const studentProfile = await call('/api/me/profile', { fullName: 'PRUEBA AWS - Estudiante ficticio', country: 'BO',
    documentType: 'CI', documentNumber: randomUUID(), dateOfBirth: '2001-03-15', department: 'Santa Cruz' }, student.token, 'PATCH');
  await verifyEmail(studentEmail);
  passed('Registro del estudiante, perfil separado y confirmación por correo real en Mailpit');
  for (const institution of [admin, other]) {
    assert.equal((await call('/api/students/redeem', { code: studentProfile.searchCode }, institution.token)).id, studentProfile.id);
  }
  passed('Un estudiante global vinculado por código a dos instituciones sin duplicarlo');
  const program = await call('/api/institution/academic/programs', {
    name: 'PRUEBA AWS - Curso ficticio', kind: 'Curso', period: '2026', active: true,
  }, admin.token, 'POST', 201);
  const record = await call('/api/institution/academic/records', { studentId: studentProfile.id, programId: program.id,
    registrationCode: randomUUID(), enrolledOn: '2026-01-01' }, admin.token, 'POST', 201);
  await call('/api/institution/academic/records/' + record.id, { status: 'COMPLETED', completedOn: '2026-11-01', result: 'Aprobado' }, admin.token, 'PATCH');
  await call('/api/institution/academic/records/' + record.id, { status: 'WITHDRAWN' }, other.token, 'PATCH', 404);
  passed('Programa, matrícula, conclusión y aislamiento entre instituciones');
  const role = await call('/api/access/roles', { name: 'PRUEBA AWS - Revisor', permissions: ['REQUEST_VIEW', 'REQUEST_REVIEW'] }, admin.token, 'POST', 201);
  const reviewerEmail = 'aws-reviewer-' + randomUUID() + '@example.test';
  await call('/api/access/users', { fullName: 'Revisor ficticio AWS', email: reviewerEmail, password: fictionalPassword, roleIds: [role.id] }, admin.token, 'POST', 201);
  const reviewer = await call('/api/auth/login', { organizationSlug: admin.slug, email: reviewerEmail, password: fictionalPassword });
  const created = await call('/api/me/credential-requests', { recordId: record.id, credentialType: 'DIPLOMA', title: 'PRUEBA AWS - Diploma ficticio' }, student.token, 'POST', 201);
  const path = '/api/institution/credential-requests/' + created.id + '/actions';
  await call(path, { action: 'START' }, reviewer.token);
  await call(path, { action: 'OBSERVE', reason: 'PRUEBA AWS: corregir título ficticio' }, reviewer.token);
  await call('/api/me/credential-requests/' + created.id, { title: 'PRUEBA AWS - Diploma corregido', message: 'Corrección ficticia' }, student.token, 'PATCH');
  await call('/api/me/credential-requests/' + created.id + '/actions', { action: 'RESUBMIT' }, student.token);
  async function approve(id) {
    const path = '/api/institution/credential-requests/' + id + '/actions';
    for (const action of ['START', 'RECOMMEND']) await call(path, { action }, reviewer.token);
    await call(path, { action: 'APPROVE' }, reviewer.token, 'POST', 403);
    return call(path, { action: 'APPROVE' }, admin.token);
  }
  const approved = await approve(created.id);
  passed('Rol personalizado, observación, corrección y separación revisor/aprobador');
  assert.equal((await call('/api/institution/signing-key/demo', { required: true }, admin.token)).testOnly, true);
  async function issue(id) {
    const path = '/api/credentials/' + id;
    await call(path + '/generate-document', {}, admin.token);
    await call(path + '/issue', {}, admin.token, 'PATCH', 400);
    await call(path + '/sign', {}, admin.token);
    const headers = { 'Idempotency-Key': randomUUID() };
    const issued = await call(path + '/issue', {}, admin.token, 'PATCH', 200, headers);
    assert.equal((await call(path + '/issue', {}, admin.token, 'PATCH', 200, headers)).id, issued.id);
    return issued;
  }
  const issued = await issue(approved.credentialId);
  passed('Generación PDF, firma de demostración y emisión idempotente');
  async function download(id) {
    const response = await fetch(base + '/api/me/credentials/' + id + '/document', { headers: { Authorization: 'Bearer ' + student.token } });
    assert.equal(response.status, 200); assert.equal(response.headers.get('cache-control'), 'no-store');
    return Buffer.from(await response.arrayBuffer());
  }
  const bytes = await download(issued.id); assert.equal(bytes.subarray(0, 4).toString(), '%PDF');
  async function check(bytes) {
    const form = new FormData(); form.set('file', new Blob([bytes], { type: 'application/pdf' }), 'fictional.pdf');
    const response = await fetch(base + '/api/public/credentials/' + issued.verificationCode + '/check-document', { method: 'POST', body: form });
    assert.equal(response.status, 200); return response.json();
  }
  const valid = await check(bytes);
  assert.equal(valid.hashMatches, true); assert.equal(valid.signature.status, 'VALID'); assert.equal(valid.testSignature, true);
  const tampered = await check(Buffer.concat([bytes, Buffer.from('\n% PRUEBA AWS alteracion\n')]));
  assert.equal(tampered.hashMatches, false); assert.notEqual(tampered.signature.status, 'VALID');
  passed('Descarga real de MinIO y rechazo del PDF manipulado');
  const correction = await call('/api/me/credentials/' + issued.id + '/corrections', {
    recordId: record.id, title: 'PRUEBA AWS - Diploma reemplazado', reason: 'Corrección ficticia de aceptación',
  }, student.token, 'POST', 201);
  const replacement = await issue((await approve(correction.id)).credentialId);
  const previous = await call('/api/public/credentials/' + issued.verificationCode, null, null, 'GET');
  assert.equal(previous.status, 'REVOKED'); assert.equal(previous.replacedByCode, replacement.verificationCode);
  assert((await download(issued.id)).equals(bytes));
  passed('Reemplazo del certificado, revocación anterior e historial original intacto');
  await call('/api/credentials/' + replacement.id + '/revoke', { reason: 'Fin de prueba ficticia AWS' }, admin.token, 'PATCH');
  assert.equal((await call('/api/public/credentials/' + replacement.verificationCode, null, null, 'GET')).status, 'REVOKED');
  passed('Revocación explícita y consulta pública por código');
  const consents = await call('/api/me/consents', null, student.token, 'GET');
  const consent = consents.find(c => c.institutionSlug === admin.slug && !c.revokedAt); assert(consent);
  await call('/api/me/consents/' + consent.id, null, student.token, 'DELETE');
  await call('/api/institution/academic/records/' + record.id, { status: 'WITHDRAWN' }, admin.token, 'PATCH', 404);
  assert.equal((await call('/api/me/credentials/page', null, student.token, 'GET')).total, 2);
  passed('Revocación del consentimiento y expediente personal conservado');
  report.success = true;
} catch (error) {
  report.error = error.message;
  process.stderr.write('FALLO: ' + error.message + '\n'); process.exitCode = 1;
} finally {
  // Only sessions created by this script; no other accounts or rows are removed.
  for (const token of tokens) {
    if (token === platform?.token) continue;
    try { await call('/api/security/sessions/current', null, token, 'DELETE', 204); }
    catch { report.cleanupErrors.push('No se pudo cerrar una sesión ficticia'); }
  }
  for (const id of report.fixtureOrganizationIds) {
    try { await call('/api/platform/organizations/' + id + '/status', {
      status: 'SUSPENDED', reason: 'PRUEBA AWS finalizada; organización ficticia conservada para revisión',
    }, platform.token, 'PATCH'); }
    catch { report.cleanupErrors.push('No se pudo suspender una organización ficticia'); }
  }
  if (platform?.token) {
    try { await call('/api/security/sessions/current', null, platform.token, 'DELETE', 204); }
    catch { report.cleanupErrors.push('No se pudo cerrar la sesión de comprobación del superadministrador'); }
  }
  report.finishedAt = new Date().toISOString();
  const reportPath = resolve(root, '.local-backups/aws-private/acceptance-latest.json');
  mkdirSync(dirname(reportPath), { recursive: true }); writeFileSync(reportPath, JSON.stringify(report, null, 2));
  process.stdout.write(JSON.stringify({ success: report.success, checksPassed: report.passed.length, cleanupErrors: report.cleanupErrors.length, reportPath }) + '\n');
  if (report.cleanupErrors.length) process.exitCode = 1;
}
