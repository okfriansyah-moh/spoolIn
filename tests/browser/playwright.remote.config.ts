import { defineConfig } from '@playwright/test';
import { authenticatedStatePath } from './flows/remote/authentication';

if (process.env.GITHUB_ACTIONS !== 'true' || process.env.RUNNER_ENVIRONMENT !== 'github-hosted') {
  throw new Error('Application acceptance requires a verified GitHub-hosted runner.');
}

export default defineConfig({
  testDir: './flows/remote',
  globalSetup: './remote-global-setup.ts',
  forbidOnly: true,
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 180000,
  reporter: [['list'], ['junit', { outputFile: '../../artifacts/browser/junit.xml' }],
    ['json', { outputFile: '../../artifacts/browser/results.json' }]],
  outputDir: '../../artifacts/browser/captures',
  use: {
    baseURL: 'https://localhost:8443',
    ignoreHTTPSErrors: false,
    screenshot: 'off',
    trace: 'off',
    video: 'off',
  },
  projects: [
    ...(['chromium', 'firefox', 'webkit'] as const).flatMap((browserName) =>
      [1440, 390, 320].flatMap((width) => (['dark', 'light'] as const).map((colorScheme) => ({
        name: `${browserName}-${width}-${colorScheme}`,
        testMatch: /(?:catalog|manage|artwork|lifecycle|audit|presentation|references)\.spec\.ts/,
        metadata: { inputMethods: ['pointer', 'keyboard'], sourceStates: 97 },
        use: { browserName, viewport: { width, height: 900 }, colorScheme, reducedMotion: 'reduce' as const, storageState: authenticatedStatePath() },
      })))),
    ...(['chromium', 'webkit'] as const).flatMap((browserName) => [1440, 390, 320].flatMap((width) =>
      (['dark', 'light'] as const).map((colorScheme) => ({
        name: `${browserName}-${width}-${colorScheme}-touch`, testMatch: 'presentation.spec.ts',
        metadata: { inputMethods: ['touch'], sourceStates: 97 },
        use: { browserName, viewport: { width, height: 900 }, colorScheme, hasTouch: true, storageState: authenticatedStatePath() },
      })))),
    { name: 'account-chromium', testMatch: 'account.spec.ts',
      use: { browserName: 'chromium', viewport: { width: 1440, height: 900 }, colorScheme: 'dark' } },
  ],
});
