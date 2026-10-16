import { defineConfig } from '@playwright/test';
import shell from './playwright.config';

export default defineConfig({
  ...shell,
  reporter: [
    ['list'],
    ['junit', { outputFile: '../../test-results/policy/junit.xml' }],
  ],
  outputDir: '../../test-results/policy/browser',
  use: { ...shell.use, baseURL: 'http://127.0.0.1:43109' },
  webServer: {
    command:
      'npm exec --workspace frontend -- vite --mode policy-test --host 127.0.0.1 --port 43109',
    cwd: '../..',
    url: 'http://127.0.0.1:43109/policy',
    reuseExistingServer: false,
  },
});
