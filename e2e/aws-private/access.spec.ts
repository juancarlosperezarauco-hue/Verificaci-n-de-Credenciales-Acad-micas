import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { resolve } from 'node:path';

test('AWS administrator can enter, open the lateral menu and sign out', async ({ page }) => {
  const text = readFileSync(resolve(homedir(), '.codex/private/credenciales-aws/acceso-inicial.txt'), 'utf8');
  const email = text.match(/^Correo: (.+)$/m)?.[1].trim();
  const password = text.match(/^Contraseña inicial: (.+)$/m)?.[1].trim();
  if (!email || !password) throw new Error('Missing private initial access file');
  await page.goto('/superadmin/login');
  try {
    // No traces, videos, credential screenshots or raw fill exceptions.
    try {
      await page.locator('input[type=email]').fill(email);
      await page.locator('input[type=password]').fill(password);
    } catch { throw new Error('Unable to complete private login fields'); }
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
    await expect(page).toHaveURL(/\/superadmin\/organizations$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Organizaciones', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Abrir menú lateral' }).click();
    const closed = page.waitForResponse(r => r.url().endsWith('/api/security/sessions/current') && r.request().method() === 'DELETE');
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    expect((await closed).status()).toBe(204);
    await expect(page).toHaveURL(/\/superadmin\/login$/);
  } finally {
    // Also close this test's own session if an assertion failed before UI logout.
    await page.evaluate(async () => {
      const session = JSON.parse(sessionStorage.getItem('credential_session') ?? 'null');
      if (session?.token) await fetch('/api/security/sessions/current', { method: 'DELETE', headers: { Authorization: 'Bearer ' + session.token } });
      sessionStorage.removeItem('credential_session');
    }).catch(() => {});
  }
});
