import { defineConfig } from '@playwright/test';

if (process.env.GITHUB_ACTIONS !== 'true' || process.env.RUNNER_ENVIRONMENT !== 'github-hosted') {
  throw new Error('Application acceptance requires a verified GitHub-hosted runner.');
}

export default defineConfig({
  testDir: './flows/remote',
  forbidOnly: true,
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 60000,
  reporter: [['list'], ['junit', { outputFile: '../../artifacts/browser/junit.xml' }],
    ['json', { outputFile: '../../artifacts/browser/results.json' }]],
  outputDir: '../../artifacts/browser/captures',
  use: {
    baseURL: 'https://localhost:8443',
    ignoreHTTPSErrors: false,
    screenshot: 'only-on-failure',
    trace: 'off',
    video: 'off',
  },
  projects: [
    ...(['chromium', 'firefox', 'webkit'] as const).flatMap((browserName) =>
      [1440, 390, 320].flatMap((width) => (['dark', 'light'] as const).map((colorScheme) => ({
        name: `${browserName}-${width}-${colorScheme}`,
        testMatch: 'catalog.spec.ts',
        use: { browserName, viewport: { width, height: 900 }, colorScheme, reducedMotion: 'reduce' as const },
      })))),
    { name: 'account-chromium', testMatch: 'account.spec.ts', use: { browserName: 'chromium' } },
  ],
});
