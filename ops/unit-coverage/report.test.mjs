import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";
import {
  componentResult,
  sonarCoverageResult,
  sonarQualityResult,
  junitResults,
  koverMetrics,
  vitestMetrics,
} from "./report.mjs";

const counts = (covered, total) => ({ covered, total });
const summary = (covered = 80, total = 100) => ({
  total: Object.fromEntries(
    ["lines", "branches", "functions", "statements"].map((name) => [
      name,
      counts(covered, total),
    ]),
  ),
});

function fixture(options = {}) {
  const directory = mkdtempSync(join(tmpdir(), "spoolin-unit-report-"));
  const coverage = options.coverage ?? summary();
  writeFileSync(
    join(directory, "coverage-summary.json"),
    JSON.stringify(coverage),
  );
  writeFileSync(
    join(directory, "test-results.json"),
    JSON.stringify({
      numTotalTests: 2,
      numPassedTests: 2,
      numFailedTests: 0,
      success: true,
      ...options.tests,
    }),
  );
  writeFileSync(join(directory, "index.html"), "<h1>Coverage</h1>");
  writeFileSync(
    join(directory, "lcov.info"),
    "SF:src/example.ts\nDA:1,1\nend_of_record\n",
  );
  return directory;
}

function result(directory, command = 0) {
  return componentResult(
    { id: "frontend", kind: "vitest" },
    command,
    directory,
  );
}

test("80% passes, but a percentage that only rounds to 80% fails", () => {
  for (const [covered, total, expected] of [
    [80, 100, "passed"],
    [799999, 1000000, "failed"],
    [0, 0, "failed"],
  ]) {
    const directory = fixture({ coverage: summary(covered, total) });
    try {
      assert.equal(result(directory).status, expected);
    } finally {
      rmSync(directory, { recursive: true });
    }
  }
});

test("missing reports, failed commands and failed or empty suites cannot pass", () => {
  for (const options of [
    { command: 1 },
    { tests: { success: false, numFailedTests: 1 } },
    { tests: { numTotalTests: 0, numPassedTests: 0 } },
    { remove: "coverage-summary.json" },
    { remove: "test-results.json" },
    { remove: "index.html" },
    { remove: "lcov.info" },
  ]) {
    const directory = fixture(options);
    try {
      if (options.remove) rmSync(join(directory, options.remove));
      assert.equal(result(directory, options.command ?? 0).status, "failed");
    } finally {
      rmSync(directory, { recursive: true });
    }
  }
});

test("Kover totals use report counters rather than adding nested class counters", () => {
  const measured =
    koverMetrics(`<report><package name="a"><counter type="LINE" missed="1" covered="1"/></package>
    <counter type="LINE" missed="20" covered="80"/><counter type="BRANCH" missed="5" covered="5"/>
    <counter type="METHOD" missed="2" covered="8"/></report>`);
  assert.deepEqual(measured.lines, { covered: 80, total: 100, percent: 80 });
  assert.equal(measured.branches.percent, 50);
  assert.equal(measured.functions.percent, 80);
  assert.throws(() => koverMetrics("<report>"), /Incomplete/);
  assert.throws(() => koverMetrics("<report></report>"), /Missing/);
  assert.throws(() => vitestMetrics(summary(-1)), /Invalid/);
});

test("JUnit skips and failures remain visible and skipped-only suites fail", () => {
  const xml =
    '<testsuite tests="3" failures="1" errors="0" skipped="1"></testsuite>';
  assert.deepEqual(junitResults([xml]), {
    total: 3,
    executed: 2,
    failures: 1,
    errors: 0,
    skipped: 1,
  });
  const directory = mkdtempSync(join(tmpdir(), "spoolin-kover-report-"));
  try {
    mkdirSync(join(directory, "tests"));
    writeFileSync(
      join(directory, "tests/TEST-Skipped.xml"),
      '<testsuite tests="1" skipped="1"></testsuite>',
    );
    writeFileSync(
      join(directory, "report.xml"),
      '<report><counter type="LINE" missed="0" covered="100"/></report>',
    );
    writeFileSync(join(directory, "index.html"), "<h1>Coverage</h1>");
    const measured = componentResult(
      { id: "core", kind: "kover" },
      0,
      directory,
    );
    assert.equal(measured.status, "failed");
    assert.ok(measured.reasons.includes("No tests executed"));
  } finally {
    rmSync(directory, { recursive: true });
  }
});

test("Sonar requires both combined coverage and line coverage without rounding up", () => {
  const metrics = {
    security_issues: 0,
    reliability_issues: 0,
    duplicated_lines: 0,
    duplicated_blocks: 0,
    coverage: 80,
    line_coverage: 90,
    lines_to_cover: 100,
    uncovered_lines: 10,
    conditions_to_cover: 100,
    uncovered_conditions: 30,
  };
  const measure = (changes = {}, gate = "OK", native = 90) =>
    sonarCoverageResult({ ...metrics, ...changes }, gate, { percent: native });
  assert.equal(measure().status, "passed");
  for (const name of [
    "security_issues",
    "reliability_issues",
    "duplicated_lines",
    "duplicated_blocks",
  ])
    assert.equal(measure({ [name]: 1 }).status, "failed");
  assert.equal(
    measure({ coverage: 79.6, uncovered_conditions: 31 }).status,
    "failed",
  );
  assert.equal(
    measure({ coverage: 80, uncovered_conditions: 31 }).status,
    "failed",
  );
  assert.equal(
    measure({ line_coverage: 79.9, uncovered_lines: 21 }).status,
    "failed",
  );
  assert.equal(measure({}, "ERROR").status, "failed");
  assert.equal(measure({}, "OK", 79).status, "failed");
  assert.throws(() => measure({ conditions_to_cover: undefined }), /Invalid/);
  assert.throws(() => measure({ uncovered_conditions: -1 }), /Invalid/);
  assert.throws(() => measure({ coverage: undefined }), /invalid/);
});

test("Sonar requires exact zero issue and duplication counters", () => {
  const clean = {
    security_issues: 0,
    reliability_issues: 0,
    duplicated_lines: 0,
    duplicated_blocks: 0,
  };
  assert.equal(sonarQualityResult(clean).clean, true);
  for (const name of Object.keys(clean)) {
    assert.equal(sonarQualityResult({ ...clean, [name]: 1 }).clean, false);
    for (const value of [undefined, -1, 0.1, NaN, "0"])
      assert.throws(
        () => sonarQualityResult({ ...clean, [name]: value }),
        /invalid/,
      );
  }
});

test("classifications require exact reviewed issue, source and evidence", async () => {
  const { reviewedClassifications } = await import("./report.mjs");
  const { createHash } = await import("node:crypto");
  const digest = (value) => createHash("sha256").update(value).digest("hex");
  const issue = {
    key: "reviewed",
    project: "p",
    component: "p:src/app.ts",
    rule: "r",
    issueStatus: "FALSE_POSITIVE",
  };
  const record = {
    issue: "reviewed",
    rule: "r",
    source: "src/app.ts",
    source_sha256: digest("source"),
    evidence: "review.md",
    evidence_sha256: digest("evidence"),
    status: "FALSE_POSITIVE",
    author: "writer",
    reviewer: "other",
    reviewed_at: "2026-10-26T01:00:00Z",
    rationale: "Protective constant verified",
  };
  const read = (path) => (path === "src/app.ts" ? "source" : "evidence");
  assert.deepEqual(reviewedClassifications([issue], [record], read), [record]);
  assert.throws(() => reviewedClassifications([issue], [], read));
  assert.throws(() =>
    reviewedClassifications([{ ...issue, key: "future" }], [record], read),
  );
  assert.throws(() =>
    reviewedClassifications(
      [{ ...issue, issueStatus: "ACCEPTED" }],
      [record],
      read,
    ),
  );
  assert.throws(() =>
    reviewedClassifications([issue], [record], () => "changed"),
  );
  assert.throws(() =>
    reviewedClassifications([issue], [{ ...record, reviewer: "writer" }], read),
  );
});

test("Docker measurement preflight requires pinned images and bounded actual memory", async () => {
  const { dockerExecution } = await import("./report.mjs");
  const image = "FROM image@sha256:" + "a".repeat(64);
  const read =
    (memory, toolchain = image) =>
    (path) =>
      path.endsWith("memory.max") ? memory : toolchain;
  const result = dockerExecution(read("3221225472"), () => true);
  assert.equal(result.memory_bytes, 3221225472);
  assert.equal(result.workers, 1);
  assert.equal(result.runtime, "docker");
  assert.throws(() => dockerExecution(read("1"), () => false));
  for (const memory of ["max", "0", "-1", "1.5", "4294967296"])
    assert.throws(() => dockerExecution(read(memory), () => true));
  assert.throws(() =>
    dockerExecution(read("1", "FROM image:latest"), () => true),
  );
});
