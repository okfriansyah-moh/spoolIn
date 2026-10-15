import { defineConfig } from 'vitest/config';
import { svelte } from '@sveltejs/vite-plugin-svelte';
export default defineConfig({
  plugins: [svelte()],
  resolve: {
    alias: {
      '$app/paths': new URL(
        './src/lib/components/paths.fixture.ts',
        import.meta.url,
      ).pathname,
    },
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/lib/**/*.ts', 'src/lib/**/*.svelte'],
      exclude: ['src/lib/generated/**', '**/*.test.ts'],
      reporter: ['text', 'lcov', 'json'],
      reportsDirectory: 'coverage',
    },
    reporters: ['default', 'json'],
    outputFile: { json: 'coverage/test-results.json' },
  },
});
