import { defineConfig } from 'vitest/config';
const coverageDirectory = process.env.UNIT_COVERAGE_DIRECTORY ?? 'coverage';
export default defineConfig({
  test: {
    environment: 'node',
    include: ['test/**/*.test.ts'],
    maxWorkers: 1,
    fileParallelism: false,
    testTimeout: 15000,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.ts'],
      // Protobuf compiler output is checked through contract consumers; all authored source remains included.
      exclude: ['src/generated/**'],
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
