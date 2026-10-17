import { defineConfig } from '@playwright/test';
import shell from './playwright.config';

/** Production SvelteKit routes for feature browser journeys (discovery, movies, …). */
export default defineConfig({
  ...shell,
  outputDir: '../../test-results/app',
  webServer: {
    command:
      'npm exec --workspace frontend -- vite --mode discovery-test --host 127.0.0.1 --port 43104',
    cwd: '../..',
    url: 'http://127.0.0.1:43104',
    reuseExistingServer: !process.env.CI,
  },
});
