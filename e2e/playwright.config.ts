import { defineConfig, devices } from '@playwright/test';
const baseURL=process.env.E2E_BASE_URL??'http://localhost:4301';
if(baseURL!=='http://localhost:4301')throw new Error('E2E solo se ejecuta en el puerto del entorno audit aislado.');
export default defineConfig({
  testDir:'./tests',
  outputDir:'test-results/artifacts',
  timeout:45_000,
  expect:{timeout:10_000},
  fullyParallel:false,
  workers:1,
  retries:process.env.CI?1:0,
  reporter:[['list'],['html',{open:'never'}]],
  use:{baseURL,trace:'retain-on-failure',screenshot:'only-on-failure',
    ...(process.env.E2E_CHROME_PATH?{launchOptions:{executablePath:process.env.E2E_CHROME_PATH}}:{})},
  projects:[
    {name:'desktop',use:{...devices['Desktop Chrome'],viewport:{width:1440,height:1000}}},
    {name:'mobile',use:{...devices['Pixel 7'],defaultBrowserType:'chromium'}},
  ],
});
