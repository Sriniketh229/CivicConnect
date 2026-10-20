import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  timeout: 45000,
  reporter: 'list',
 use: {
  baseURL: 'http://localhost:5173',
  ...devices['Desktop Chrome'],
  headless: true,
  navigationTimeout: 15000,
  actionTimeout: 10000,
},
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
