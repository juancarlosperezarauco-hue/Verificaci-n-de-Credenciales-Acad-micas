import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const fixture = JSON.parse(readFileSync(
  resolve(import.meta.dirname, '../test-results/fixture.json'), 'utf8'));

test('security headers protect the running app and account recovery pages', async ({ page, request }) => {
  const response = await page.goto('/verify/' + fixture.verificationCode);
  expect(response?.headers()['x-frame-options']).toBe('DENY');
  expect(response?.headers()['x-content-type-options']).toBe('nosniff');
  expect(response?.headers()['referrer-policy']).toBe('no-referrer');
  expect(response?.headers()['content-security-policy']).toContain("script-src 'self'");
  await expect(page.getByText('Certificado ficticio de prueba E2E · CERTIFICADO', { exact: true })).toBeVisible();
  const inlineRan = await page.evaluate(async () => {
    const marker = document.createElement('div');
    marker.id = 'csp-probe';
    document.body.appendChild(marker);
    const script = document.createElement('script');
    script.textContent = 'document.getElementById("csp-probe").textContent="executed"';
    document.body.appendChild(script);
    await new Promise(resolve => setTimeout(resolve, 50));
    const ran = marker.textContent === 'executed';
    script.remove(); marker.remove();
    return ran;
  });
  expect(inlineRan).toBe(false);
  for (const path of ['/student/recover?token=fictional', '/student/confirm-email?token=fictional']) {
    const recovery = await request.get(path);
    expect(recovery.status()).toBe(200);
    expect(recovery.headers()['cache-control']).toBe('no-store');
    expect(recovery.headers()['content-security-policy']).toContain("frame-ancestors 'none'");
    expect(recovery.headers()['referrer-policy']).toBe('no-referrer');
  }
});
