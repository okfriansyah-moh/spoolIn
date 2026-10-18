import { defineConfig } from 'vitest/config';
export default defineConfig({
  test: {
    environment: 'node',
    include: ['test/**/*.test.ts'],
    maxWorkers: 1,
    fileParallelism: false,
    testTimeout: 15000,
    coverage: {
      provider: 'v8', include: ['src/**/*.ts'],
      // Protobuf compiler output is checked through contract consumers; all authored source remains included.
      exclude: ['src/generated/**'],
      reporter: ['text', 'lcov', 'json', 'json-summary'], reportsDirectory: 'coverage',
    },
    reporters: ['default', 'json', 'junit'],
    outputFile: { json: 'coverage/test-results.json', junit: 'coverage/junit.xml' },
  },
});
