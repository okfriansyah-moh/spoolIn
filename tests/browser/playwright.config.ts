import { defineConfig } from '@playwright/test';

const chromium = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE;
const firefoxEndpoint = process.env.PLAYWRIGHT_FIREFOX_WS;
export default defineConfig({
  testDir: './flows',
  forbidOnly: true,
  retries: 0,
  workers: 1,
  timeout: 60000,
  reporter: [
    ['list'],
    ['junit', { outputFile: '../../test-results/junit.xml' }],
  ],
  outputDir: '../../test-results/shell',
  use: {
    baseURL: 'http://127.0.0.1:43104',
    trace: 'on',
    screenshot: 'only-on-failure',
  },
  projects: [
    ...[1440, 390, 320].flatMap((width) =>
      (['dark', 'light'] as const).map((colorScheme) => ({
        name: `chromium-${width}-${colorScheme}`,
        use: {
          browserName: 'chromium' as const,
          viewport: { width, height: 900 },
          hasTouch: width < 700,
          colorScheme,
          reducedMotion: 'reduce' as const,
          launchOptions: chromium ? { executablePath: chromium } : {},
          video: { mode: 'on' as const, size: { width, height: 900 } },
        },
      })),
    ),
    {
      name: 'firefox-390-dark',
      use: {
        browserName: 'firefox',
        ...(firefoxEndpoint
          ? {
              connectOptions: {
                wsEndpoint: firefoxEndpoint,
                exposeNetwork: '<loopback>',
              },
            }
          : {}),
        viewport: { width: 390, height: 900 },
        colorScheme: 'dark',
        video: { mode: 'on', size: { width: 390, height: 900 } },
      },
    },
    {
      name: 'webkit-390-dark',
      use: {
        browserName: 'webkit',
        viewport: { width: 390, height: 900 },
        colorScheme: 'dark',
        video: { mode: 'on', size: { width: 390, height: 900 } },
      },
    },
  ],
  webServer: {
    command:
      'npm exec --workspace frontend -- vite --mode discovery-test --host 127.0.0.1 --port 43104',
    cwd: '../..',
    url: 'http://127.0.0.1:43104',
    reuseExistingServer: !process.env.CI,
  },
});
