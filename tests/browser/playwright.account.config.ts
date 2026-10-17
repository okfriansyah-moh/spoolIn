import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './flows/account',
  forbidOnly: true,
  retries: 0,
  workers: 1,
  timeout: 180000,
  reporter: [
    ['list'],
    [
      'junit',
      {
        outputFile:
          process.env.ACCOUNT_JUNIT || '/tmp/account-results/junit.xml',
      },
    ],
  ],
  outputDir: process.env.ACCOUNT_RESULTS || '/tmp/account-results/browser',
  use: {
    baseURL: process.env.ACCOUNT_ORIGIN,
    trace: 'off',
    video: 'off',
    screenshot: 'off',
  },
  projects: [
    ...(process.env.ACCOUNT_BROWSER_ENGINE === 'firefox'
      ? [
          {
            name: 'firefox-account',
            use: {
              browserName: 'firefox' as const,
              viewport: { width: 390, height: 900 },
              reducedMotion: 'reduce' as const,
            },
          },
        ]
      : [
          {
            name: 'chromium-account',
            use: {
              browserName: 'chromium' as const,
              channel: 'chrome',
              viewport: { width: 390, height: 900 },
              reducedMotion: 'reduce' as const,
            },
          },
        ]),
  ],
});
