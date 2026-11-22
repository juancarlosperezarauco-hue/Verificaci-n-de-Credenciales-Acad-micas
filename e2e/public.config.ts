import { defineConfig } from '@playwright/test';
import audit from './playwright.config';

// A separate, public-only configuration keeps fixture-generating tests restricted to audit.
export default defineConfig({
  ...audit,
  testMatch: 'public-landing.spec.ts',
  outputDir: 'test-results/public-landing',
  use: { ...audit.use, baseURL: 'http://localhost:4300' },
});
