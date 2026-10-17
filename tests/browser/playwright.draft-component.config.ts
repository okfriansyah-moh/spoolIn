import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './flows/draft-component',
  forbidOnly: true,
  retries: 0,
  workers: 1,
  timeout: 60000,
  reporter: [
    ['list'],
    ['junit', { outputFile: '../../test-results/draft-component/junit.xml' }],
  ],
  outputDir: '../../test-results/draft-component/browser',
  use: {
    baseURL: 'http://127.0.0.1:43110',
    trace: 'off',
    video: 'off',
    screenshot: 'off',
  },
  projects: [
    {
      name: 'chromium-draft',
      use: {
        browserName: 'chromium',
        viewport: { width: 390, height: 900 },
        reducedMotion: 'reduce',
      },
    },
  ],
  webServer: {
    command:
      'npm exec --workspace frontend -- vite --mode draft-test --host 127.0.0.1 --port 43110',
    cwd: '../..',
    url: 'http://127.0.0.1:43110/draft',
    reuseExistingServer: !process.env.CI,
  },
});
