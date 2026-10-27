import { defineConfig } from 'vitest/config';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { svelteTesting } from '@testing-library/svelte/vite';
const coverageDirectory = process.env.UNIT_COVERAGE_DIRECTORY ?? 'coverage';
export default defineConfig({
  plugins: [svelte()],
  environments: {
    __vitest__: {
      consumer: 'server',
      optimizeDeps: { noDiscovery: true, include: [] },
    },
  },
  resolve: {
    alias: {
      '$app/state': new URL('./src/lib/state/kit.fixture.ts', import.meta.url)
        .pathname,
      '$app/navigation': new URL(
        './src/lib/state/kit.fixture.ts',
        import.meta.url,
      ).pathname,
      '$app/paths': new URL(
        './src/lib/components/paths.fixture.ts',
        import.meta.url,
      ).pathname,
    },
  },
  test: {
    projects: [
      {
        test: {
          name: 'server',
          environment: 'node',
          include: ['src/**/*.test.ts'],
          exclude: ['src/**/*.dom.test.ts'],
        },
      },
      {
        plugins: [svelteTesting({ autoCleanup: false })],
        resolve: { conditions: ['browser'] },
        test: {
          name: 'account-dom',
          environment: 'jsdom',
          include: ['src/**/*.dom.test.ts'],
          environmentOptions: { jsdom: { url: 'https://localhost/' } },
        },
      },
    ],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.ts', 'src/**/*.svelte'],
      exclude: ['src/lib/generated/**', '**/*.test.ts', '**/*.fixture.ts'],
      reporter: ['text', 'lcov', 'json', 'json-summary', 'html'],
      reportsDirectory: coverageDirectory,
      reportOnFailure: true,
      ...(process.env.UNIT_COVERAGE_DIRECTORY
        ? { thresholds: { lines: 80 } }
        : {}),
    },
    reporters: ['default', 'json', 'junit'],
    outputFile: {
      json: `${coverageDirectory}/test-results.json`,
      junit: `${coverageDirectory}/junit.xml`,
    },
  },
});
