import { test, expect } from '@playwright/test';

// Anonymous navigation only: no fixtures, login, uploads or database changes.
test('landing remains readable and its public entry points work on desktop and mobile', async ({ page }, info) => {
  const errors: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Cada logro académico,');
  const widths = info.project.name === 'mobile' ? [320, 412] : [1440];
  for (const width of widths) {
    await page.setViewportSize({ width, height: 1000 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    await expect(page.locator('.hero').getByRole('link', { name: 'Registrar institución', exact: true })).toBeVisible();
    await page.screenshot({ path: info.outputPath('landing-' + width + '.png'), fullPage: true, scale: 'css' });
    await page.evaluate(() => scrollTo(0, 0));
    await page.screenshot({ path: info.outputPath('hero-' + width + '.png'), scale: 'css' });
  }
  const question = page.locator('summary').filter({ hasText: '¿El estudiante necesita una cuenta por cada institución?' });
  await question.focus();
  await page.keyboard.press('Enter');
  await expect(page.getByText(/^No\. El estudiante tiene una cuenta global\./)).toBeVisible();
  await page.keyboard.press('Enter');
  await expect(page.getByText(/^No\. El estudiante tiene una cuenta global\./)).toBeHidden();
  await page.locator('.hero').getByRole('link', { name: 'Verificar credencial', exact: true }).click();
  await expect(page).toHaveURL(/\/verify$/);
  await page.goBack();
  await page.getByRole('link', { name: 'Crea tu cuenta personal', exact: true }).click();
  await expect(page).toHaveURL(/\/student\/register$/);
  await page.goBack();
  await page.locator('.hero').getByRole('link', { name: 'Registrar institución', exact: true }).click();
  await expect(page).toHaveURL(/\/register$/);
  expect(errors).toEqual([]);
});
