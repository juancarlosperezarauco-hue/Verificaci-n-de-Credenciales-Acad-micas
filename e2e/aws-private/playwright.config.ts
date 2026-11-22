import { defineConfig, devices } from '@playwright/test';
if (process.env.AWS_PRIVATE_BROWSER_CHECK !== '1') throw new Error('Private AWS browser checks require explicit AWS_PRIVATE_BROWSER_CHECK=1.');
export default defineConfig({
  testDir: '..',
  testMatch: ['**/tests/public-landing.spec.ts', '**/aws-private/access.spec.ts'],
  outputDir: '../test-results/aws-private-live',
  workers: 1,
  retries: 0,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:4302',
    trace: 'off', screenshot: 'off', video: 'off',
    ...(process.env.E2E_CHROME_PATH ? { launchOptions: { executablePath: process.env.E2E_CHROME_PATH } } : {}),
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 1000 } } },
    { name: 'mobile', use: { ...devices['Pixel 7'], defaultBrowserType: 'chromium' } },
  ],
});
