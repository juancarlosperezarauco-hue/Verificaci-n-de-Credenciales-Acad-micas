import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const fixture = JSON.parse(readFileSync(
  resolve(import.meta.dirname, '../test-results/fixture.json'), 'utf8'));

// Synthetic identities only. Do not publish session tokens or identity files in traces.
test.use({ trace: 'off', screenshot: 'off' });
test('private OCR reads submitted evidence and leaves the decision to a reviewer', async ({ page, request }, info) => {
  test.skip(info.project.name !== 'desktop', 'The same service flow is exercised once.');
  test.setTimeout(90_000);
  expect(fixture.studentEmail).toMatch(/@[^@]+\.test$/);
  const studentLogin = await request.post('/api/auth/student/login', {
    data: { email: fixture.studentEmail, password: fixture.password },
  });
  expect(studentLogin.ok()).toBeTruthy();
  const student = { Authorization: 'Bearer ' + (await studentLogin.json()).token };
  const adminLogin = await request.post('/api/auth/login', {
    data: { organizationSlug: fixture.slug, email: fixture.adminEmail, password: fixture.password },
  });
  expect(adminLogin.ok()).toBeTruthy();
  const admin = { Authorization: 'Bearer ' + (await adminLogin.json()).token };
  const availability = await request.get('/api/institution/identity-analysis/status', { headers: admin });
  expect(availability.ok()).toBeTruthy();
  expect((await availability.json()).configured).toBe(true);
  const profileResponse = await request.get('/api/me/profile', { headers: student });
  expect(profileResponse.ok()).toBeTruthy();
  const profile = await profileResponse.json();

  await page.setViewportSize({ width: 1600, height: 500 });
  await page.setContent('<div style="padding:40px;background:white;color:black;font:48px sans-serif;white-space:pre-line"></div>');
  await page.locator('div').evaluate((element, text) => { element.textContent = text; },
    'DOCUMENTO FICTICIO DE PRUEBA\n' + profile.fullName + '\nCI ' + profile.documentNumber);
  const image = await page.screenshot();
  const submitted = await request.post('/api/me/identity-verifications', {
    headers: student,
    multipart: {
      tenantId: fixture.tenantId, accepted: 'true',
      front: { name: 'fictional-front.png', mimeType: 'image/png', buffer: image },
      back: { name: 'fictional-back.png', mimeType: 'image/png', buffer: image },
    },
  });
  expect(submitted.status()).toBe(201);
  const id = (await submitted.json()).id;
  try {
    const endpoint = '/api/institution/identity-verifications/' + id + '/analysis';
    const denied = await request.post(endpoint, { headers: student, data: {} });
    expect(denied.status()).toBe(403);
    const analyzed = await request.post(endpoint, { headers: admin, data: {}, timeout: 40_000 });
    expect(analyzed.status()).toBe(200);
    const result = await analyzed.json();
    expect(result.engine).toBe('tesseract');
    expect(result.human_review_required).toBe(true);
    expect(result.confidence).toBeGreaterThanOrEqual(0);
    expect(result.confidence).toBeLessThanOrEqual(1);
    expect(result.text.toUpperCase()).toContain('FICTICIO');
    const own = await request.get('/api/me/identity-verifications', { headers: student });
    expect(own.ok()).toBeTruthy();
    expect((await own.json()).submissions.find((item: { id: string }) => item.id === id).status).toBe('PENDING');
  } finally {
    // Keep retries independent without turning the OCR result into an identity decision.
    const withdrawn = await request.post('/api/me/identity-verifications/' + id + '/withdraw', {
      headers: student, data: {},
    });
    expect(withdrawn.status()).toBe(204);
  }
});
