import { defineConfig, devices } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
if (process.env.AWS_PRIVATE_BROWSER_CHECK !== '1') throw new Error('Private AWS browser checks require explicit AWS_PRIVATE_BROWSER_CHECK=1.');
const demoURL = process.env.UNIVERSITY_DEMO_URL;
if (demoURL) {
  const owned = JSON.parse(readFileSync(resolve(import.meta.dirname, '../../.local-backups/aws-private/university-demo.json'), 'utf8'));
  if (!/^https:\/\/[a-z0-9-]+\.trycloudflare\.com$/.test(demoURL)
      || owned.url !== demoURL || owned.instanceId !== 'i-07ef6f96873de70a4') {
    throw new Error('Public demo target must match the URL recorded from the owned AWS tunnel.');
  }
}
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
    baseURL: demoURL ?? 'http://localhost:4302',
    trace: 'off', screenshot: 'off', video: 'off',
    ...(process.env.E2E_CHROME_PATH ? { launchOptions: { executablePath: process.env.E2E_CHROME_PATH } } : {}),
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 1000 } } },
    { name: 'mobile', use: { ...devices['Pixel 7'], defaultBrowserType: 'chromium' } },
  ],
});
