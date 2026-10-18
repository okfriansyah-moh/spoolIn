import { defineConfig } from '@playwright/test';
import { authenticatedStatePath } from './flows/remote/authentication';

if (
  process.env.GITHUB_ACTIONS !== 'true' ||
  process.env.RUNNER_ENVIRONMENT !== 'github-hosted'
) {
  throw new Error(
    'Application acceptance requires a verified GitHub-hosted runner.',
  );
}

const grafanaPort = Number(process.env.GRAFANA_HTTPS_PORT ?? 3000);
if (!Number.isInteger(grafanaPort) || grafanaPort < 1 || grafanaPort > 65535)
  throw new Error('Invalid loopback Grafana port.');

export default defineConfig({
  testDir: './flows/remote',
  globalSetup: './remote-global-setup.ts',
  forbidOnly: true,
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 180000,
  reporter: [
    ['list'],
    ['junit', { outputFile: '../../artifacts/browser/junit.xml' }],
    ['json', { outputFile: '../../artifacts/browser/results.json' }],
  ],
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
      [1440, 1024, 768, 390, 320].flatMap((width) =>
        (['dark', 'light'] as const).map((colorScheme) => ({
          name: `${browserName}-${width}-${colorScheme}`,
          testMatch:
            /(?:catalog|manage|artwork|lifecycle|audit|presentation(?:-responsive)?|references)\.spec\.ts/,
          metadata: { inputMethods: ['pointer', 'keyboard'], sourceStates: 97 },
          grep: /^(?!.*input-touch).*$/,
          use: {
            browserName,
            viewport: { width, height: 900 },
            colorScheme,
            reducedMotion: 'reduce' as const,
            storageState: authenticatedStatePath(),
          },
        })),
      ),
    ),
    ...(['chromium', 'webkit'] as const).flatMap((browserName) =>
      [1440, 1024, 768, 390, 320].flatMap((width) =>
        (['dark', 'light'] as const).map((colorScheme) => ({
          name: `${browserName}-${width}-${colorScheme}-touch`,
          testMatch: 'presentation.spec.ts',
          grep: /input-touch|presentation-properties/,
          metadata: { inputMethods: ['touch'], sourceStates: 97 },
          use: {
            browserName,
            viewport: { width, height: 900 },
            colorScheme,
            hasTouch: true,
            storageState: authenticatedStatePath(),
          },
        })),
      ),
    ),
    {
      name: 'operations-chromium',
      testMatch: 'operations.spec.ts',
      timeout: 360000,
      metadata: {
        inputMethods: ['pointer', 'keyboard'],
        scope: 'Actual private Grafana UI',
      },
      use: {
        browserName: 'chromium',
        baseURL: `https://localhost:${grafanaPort}`,
        viewport: { width: 1440, height: 1000 },
        colorScheme: 'dark',
        reducedMotion: 'reduce',
      },
    },
    {
      name: 'account-chromium',
      testMatch: 'account.spec.ts',
      use: {
        browserName: 'chromium',
        viewport: { width: 1440, height: 900 },
        colorScheme: 'dark',
      },
    },
  ],
});
