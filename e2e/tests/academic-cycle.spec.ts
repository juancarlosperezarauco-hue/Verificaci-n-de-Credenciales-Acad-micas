import { test, expect } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { call, organizationProfile, verifyEmail, base } from '../support/audit-api.mjs';

const fixture = JSON.parse(readFileSync(
  resolve(import.meta.dirname, '../test-results/fixture.json'), 'utf8'));
test.use({ trace: 'off', screenshot: 'off' });
test('two institutions share one student identity while review, signed replacement and consent stay isolated',
  async ({ request }, info) => {
    test.skip(info.project.name !== 'desktop', 'Service acceptance is exercised once.');
    test.setTimeout(120_000);
    const password = 'Acceptance!' + randomUUID();
    const platform = await call('/api/auth/platform/login', {
      email: fixture.superEmail, password: fixture.superPassword,
    });
    async function institution() {
      const suffix = randomUUID(), email = 'admin-' + suffix + '@example.test', slug = 'accept-' + suffix;
      const registered = await call('/api/auth/organizations/register', {
        organizationSlug: slug, password,
        profile: organizationProfile('Institución ficticia de aceptación', email, suffix),
      }, null, 'POST', 201);
      await call('/api/students', null, registered.token, 'GET', 403);
      await call('/api/platform/organizations/' + registered.tenantId + '/status', {
        status: 'ACTIVE', reason: 'Aprobación ficticia para aceptación integrada',
      }, platform.token, 'PATCH');
      return { ...(await call('/api/auth/login', { organizationSlug: slug, email, password })), slug };
    }
    const admin = await institution(), other = await institution();
    const email = 'student-' + randomUUID() + '@example.test';
    const student = await call('/api/auth/student/register', { email, password }, null, 'POST', 201);
    expect((await call('/api/me/profile', null, student.token, 'GET')).profileComplete).toBe(false);
    const profile = await call('/api/me/profile', {
      fullName: 'Persona Ficticia Aceptación', country: 'BO', documentType: 'CI',
      documentNumber: randomUUID(), dateOfBirth: '2001-03-15', department: 'Santa Cruz',
    }, student.token, 'PATCH');
    await verifyEmail(email);
    for (const institution of [admin, other]) {
      const linked = await call('/api/students/redeem', { code: profile.searchCode }, institution.token);
      expect(linked.id).toBe(profile.id);
    }
    const program = await call('/api/institution/academic/programs', {
      name: 'Curso ficticio de aceptación', kind: 'Curso', period: '2026', active: true,
    }, admin.token, 'POST', 201);
    const record = await call('/api/institution/academic/records', {
      studentId: profile.id, programId: program.id, registrationCode: randomUUID(), enrolledOn: '2026-01-01',
    }, admin.token, 'POST', 201);
    await call('/api/institution/academic/records/' + record.id, {
      status: 'COMPLETED', completedOn: '2026-11-01', result: 'Aprobado',
    }, admin.token, 'PATCH');
    await call('/api/institution/academic/records/' + record.id, { status: 'WITHDRAWN' }, other.token, 'PATCH', 404);
    const role = await call('/api/access/roles', {
      name: 'Revisor ficticio', permissions: ['REQUEST_VIEW', 'REQUEST_REVIEW'],
    }, admin.token, 'POST', 201);
    const reviewerEmail = 'reviewer-' + randomUUID() + '@example.test';
    await call('/api/access/users', {
      fullName: 'Revisor Ficticio', email: reviewerEmail, password, roleIds: [role.id],
    }, admin.token, 'POST', 201);
    const reviewer = await call('/api/auth/login', { organizationSlug: admin.slug, email: reviewerEmail, password });
    const created = await call('/api/me/credential-requests', {
      recordId: record.id, credentialType: 'DIPLOMA', title: 'Diploma ficticio de aceptación',
    }, student.token, 'POST', 201);
    const actionPath = '/api/institution/credential-requests/' + created.id + '/actions';
    await call(actionPath, { action: 'START' }, reviewer.token);
    await call(actionPath, { action: 'OBSERVE', reason: 'Completa el título ficticio del diploma' }, reviewer.token);
    await call('/api/me/credential-requests/' + created.id, {
      title: 'Diploma ficticio corregido', message: 'Corrección ficticia presentada',
    }, student.token, 'PATCH');
    await call('/api/me/credential-requests/' + created.id + '/actions', { action: 'RESUBMIT' }, student.token);
    async function approve(id) {
      const path = '/api/institution/credential-requests/' + id + '/actions';
      await call(path, { action: 'START' }, reviewer.token);
      await call(path, { action: 'RECOMMEND' }, reviewer.token);
      await call(path, { action: 'APPROVE' }, reviewer.token, 'POST', 403);
      return call(path, { action: 'APPROVE' }, admin.token);
    }
    const approved = await approve(created.id);
    const signer = await call('/api/institution/signing-key/demo', { required: true }, admin.token);
    expect(signer.testOnly).toBe(true);
    async function issue(id) {
      const path = '/api/credentials/' + id;
      await call(path + '/generate-document', {}, admin.token);
      await call(path + '/issue', {}, admin.token, 'PATCH', 400);
      await call(path + '/sign', {}, admin.token);
      const headers = { 'Idempotency-Key': randomUUID() };
      const issued = await call(path + '/issue', {}, admin.token, 'PATCH', 200, headers);
      expect((await call(path + '/issue', {}, admin.token, 'PATCH', 200, headers)).id).toBe(issued.id);
      return issued;
    }
    const issued = await issue(approved.credentialId);
    const auth = { Authorization: 'Bearer ' + student.token };
    const download = await request.get(base + '/api/me/credentials/' + issued.id + '/document', { headers: auth });
    expect(download.status()).toBe(200);
    expect(download.headers()['cache-control']).toBe('no-store');
    const bytes = await download.body();
    expect(bytes.subarray(0, 4).toString()).toBe('%PDF');
    async function check(buffer: Buffer) {
      const result = await request.post(base + '/api/public/credentials/' + issued.verificationCode + '/check-document', {
        multipart: { file: { name: 'fictional.pdf', mimeType: 'application/pdf', buffer } },
      });
      expect(result.status()).toBe(200);
      return result.json();
    }
    const valid = await check(bytes);
    expect(valid.hashMatches).toBe(true); expect(valid.signature.status).toBe('VALID'); expect(valid.testSignature).toBe(true);
    const tampered = await check(Buffer.concat([bytes, Buffer.from('\n% fictional alteration\n')]));
    expect(tampered.hashMatches).toBe(false); expect(tampered.signature.status).not.toBe('VALID');
    const correction = await call('/api/me/credentials/' + issued.id + '/corrections', {
      recordId: record.id, title: 'Diploma ficticio reemplazado', reason: 'Corrección ficticia aprobada para aceptación',
    }, student.token, 'POST', 201);
    const replacement = await issue((await approve(correction.id)).credentialId);
    const previous = await call('/api/public/credentials/' + issued.verificationCode, null, null, 'GET');
    expect(previous.status).toBe('REVOKED'); expect(previous.replacedByCode).toBe(replacement.verificationCode);
    const historical = await request.get(base + '/api/me/credentials/' + issued.id + '/document', { headers: auth });
    expect(historical.status()).toBe(200); expect((await historical.body()).equals(bytes)).toBe(true);
    const consents = await call('/api/me/consents', null, student.token, 'GET');
    const consent = consents.find(item => item.institutionSlug === admin.slug && !item.revokedAt);
    expect(consent).toBeTruthy();
    await call('/api/me/consents/' + consent.id, null, student.token, 'DELETE');
    await call('/api/institution/academic/records/' + record.id, { status: 'WITHDRAWN' }, admin.token, 'PATCH', 404);
    const own = await call('/api/me/credentials/page', null, student.token, 'GET');
    expect(own.total).toBe(2);
  });
