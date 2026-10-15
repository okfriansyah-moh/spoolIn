import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './flows',
  forbidOnly: true,
  retries: 0,
  reporter: [
    ['list'],
    ['junit', { outputFile: '../../test-results/junit.xml' }],
  ],
  use: { trace: 'retain-on-failure', screenshot: 'only-on-failure' },
});
