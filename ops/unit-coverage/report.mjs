import { createHash } from "node:crypto";
import { execFileSync } from "node:child_process";
import {
  existsSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { join } from "node:path";

export const output = ".local/unit-coverage";
export const components = [
  { id: "frontend", path: "frontend", kind: "vitest" },
  { id: "core", path: "services/core", kind: "kover" },
  { id: "catalog", path: "services/catalog", kind: "kover" },
  { id: "media", path: "services/media", kind: "kover" },
  { id: "backend-support", path: "libraries/backend-support", kind: "kover" },
  { id: "mcp", path: "mcp", kind: "vitest" },
];

export function dockerExecution(read = readFileSync, exists = existsSync) {
  if (!exists("/.dockerenv"))
    throw new Error("Unit measurements require Docker");
  const memory = Number(read("/sys/fs/cgroup/memory.max", "utf8").trim());
  if (!Number.isSafeInteger(memory) || memory <= 0 || memory > 4294967296)
    throw new Error("Docker memory must be bounded to at most 4 GiB");
  const images = read("ops/local/toolchain.Dockerfile", "utf8")
    .split("\n")
    .filter((line) => line.startsWith("FROM "))
    .map((line) => line.split(" ")[1]);
  if (
    !images.length ||
    images.some((image) => !/^[^\s]+@sha256:[0-9a-f]{64}$/.test(image))
  )
    throw new Error("Docker toolchain images require exact digests");
  return {
    runtime: "docker",
    images,
    memory_bytes: memory,
    workers: 1,
    tests_separate_from_analysis: true,
  };
}

export function fingerprint() {
  const paths = execFileSync(
    "git",
    [
      "ls-files",
      "-z",
      "--cached",
      "--others",
      "--exclude-standard",
      "--",
      "frontend",
      "mcp",
      "services",
      "libraries",
      "contracts",
      "gradle",
      "build-tools",
      "data/seed",
      "ops/unit-coverage",
      "ops/local/toolchain.Dockerfile",
      "ops/bin/coverage.sh",
      "ops/bin/coverage-phase.sh",
      "build.gradle.kts",
      "settings.gradle.kts",
      "gradle.properties",
      "gradlew",
      "gradlew.bat",
      "package.json",
      "package-lock.json",
    ],
    { encoding: "utf8" },
  );
  const hash = createHash("sha256");
  for (const path of [...new Set(paths.split("\0").filter(Boolean))].sort()) {
    hash.update(path).update("\0");
    hash
      .update(existsSync(path) ? readFileSync(path) : "<deleted>")
      .update("\0");
  }
  return hash.digest("hex");
}

function metric(covered, total) {
  if (
    !Number.isInteger(covered) ||
    !Number.isInteger(total) ||
    covered < 0 ||
    total < covered
  ) {
    throw new Error("Invalid coverage counters");
  }
  return { covered, total, percent: total ? (covered * 100) / total : null };
}

function matchingClassification(issue, audit) {
  const record = audit.find((item) => item.issue === issue.key);
  const source = issue.component.slice(issue.project.length + 1);
  if (
    !record ||
    record.rule !== issue.rule ||
    record.source !== source ||
    record.status !== "FALSE_POSITIVE"
  )
    throw new Error(`Missing exact independent classification: ${issue.key}`);
  return record;
}

function classificationEvidence(record, readSource) {
  for (const [path, expected] of [
    [record.source, record.source_sha256],
    [record.evidence, record.evidence_sha256],
  ]) {
    if (
      createHash("sha256").update(readSource(path)).digest("hex") !== expected
    )
      throw new Error("Classification source or evidence changed");
  }
  if (
    !record.author ||
    !record.reviewer ||
    record.author === record.reviewer ||
    !record.rationale ||
    !Number.isFinite(Date.parse(record.reviewed_at))
  )
    throw new Error("Classification lacks independent review");
}

export function reviewedClassifications(issues, audit, readSource) {
  const seen = new Set();
  return issues.map((issue) => {
    if (issue.issueStatus !== "FALSE_POSITIVE" || seen.has(issue.key))
      throw new Error("Unreviewed or duplicate analyzer disposition");
    seen.add(issue.key);
    const record = matchingClassification(issue, audit);
    classificationEvidence(record, readSource);
    return record;
  });
}

export function sonarQualityResult(metrics) {
  const fields = {
    securityIssues: "security_issues",
    reliabilityIssues: "reliability_issues",
    duplicatedLines: "duplicated_lines",
    duplicatedBlocks: "duplicated_blocks",
  };
  const result = Object.fromEntries(
    Object.entries(fields).map(([field, metric]) => {
      const value = metrics[metric];
      if (!Number.isSafeInteger(value) || value < 0)
        throw new Error(`Missing or invalid Sonar ${metric}`);
      return [field, value];
    }),
  );
  return {
    ...result,
    clean: Object.values(result).every((value) => value === 0),
  };
}

export function sonarCoverageResult(metrics, gate, expected) {
  const quality = sonarQualityResult(metrics);
  const lines = metric(
    metrics.lines_to_cover - metrics.uncovered_lines,
    metrics.lines_to_cover,
  );
  const conditions = metric(
    metrics.conditions_to_cover - metrics.uncovered_conditions,
    metrics.conditions_to_cover,
  );
  const combined = metric(
    lines.covered + conditions.covered,
    lines.total + conditions.total,
  );
  for (const name of ["coverage", "line_coverage"]) {
    if (
      !Number.isFinite(metrics[name]) ||
      metrics[name] < 0 ||
      metrics[name] > 100
    )
      throw new Error(`Missing or invalid Sonar ${name}`);
  }
  return {
    coverage: metrics.coverage,
    lines: metrics.line_coverage,
    total: lines.total,
    covered: lines.covered,
    conditions,
    combined,
    quality,
    status:
      gate === "OK" &&
      quality.clean &&
      metrics.coverage >= 80 &&
      metrics.line_coverage >= 80 &&
      combined.percent >= 80 &&
      lines.percent >= 80 &&
      expected.percent >= 80
        ? "passed"
        : "failed",
  };
}

export function xmlAttributes(text) {
  return Object.fromEntries(
    [...text.matchAll(/([\w.-]+)="([^"]*)"/g)].map((match) => [
      match[1],
      match[2],
    ]),
  );
}

export function koverMetrics(xml) {
  if (!xml.trimEnd().endsWith("</report>"))
    throw new Error("Incomplete Kover report");
  const totals = xml.match(
    /((?:\s*<counter\b[^>]*\/>)+)\s*<\/report>\s*$/,
  )?.[1];
  if (!totals) throw new Error("Missing report-level counters");
  const counters = Object.fromEntries(
    [...totals.matchAll(/<counter\b([^>]+)\/>/g)].map((match) => {
      const value = xmlAttributes(match[1]);
      return [
        value.type,
        metric(
          Number(value.covered),
          Number(value.covered) + Number(value.missed),
        ),
      ];
    }),
  );
  if (!counters.LINE) throw new Error("Missing line counters");
  return {
    lines: counters.LINE,
    branches: counters.BRANCH,
    functions: counters.METHOD,
  };
}

export function vitestMetrics(summary) {
  return Object.fromEntries(
    ["lines", "branches", "functions", "statements"].map((key) => {
      const value = summary.total?.[key];
      if (!value) throw new Error(`Missing ${key} counters`);
      return [key, metric(value.covered, value.total)];
    }),
  );
}

export function junitResults(xmlFiles) {
  const counts = { total: 0, failures: 0, errors: 0, skipped: 0 };
  for (const xml of xmlFiles) {
    for (const match of xml.matchAll(/<testsuite\b([^>]+)>/g)) {
      const attributes = xmlAttributes(match[1]);
      for (const [key, attr] of Object.entries({
        total: "tests",
        failures: "failures",
        errors: "errors",
        skipped: "skipped",
      })) {
        const value = Number(attributes[attr] ?? 0);
        if (!Number.isInteger(value) || value < 0)
          throw new Error("Invalid JUnit test counters");
        counts[key] += value;
      }
    }
  }
  counts.executed = counts.total - counts.skipped;
  return counts;
}

function testResults(component, directory) {
  if (component.kind === "vitest") {
    const tests = JSON.parse(
      readFileSync(join(directory, "test-results.json"), "utf8"),
    );
    for (const key of ["numTotalTests", "numPassedTests", "numFailedTests"]) {
      if (!Number.isInteger(tests[key]) || tests[key] < 0)
        throw new Error("Invalid Vitest test counters");
    }
    return {
      total: tests.numTotalTests,
      executed: tests.numPassedTests + tests.numFailedTests,
      failures: tests.numFailedTests,
      errors: tests.success === true ? 0 : 1,
      skipped: tests.numPendingTests ?? 0,
    };
  }
  const testDirectory = join(directory, "tests");
  const files = readdirSync(testDirectory).filter(
    (file) => file.startsWith("TEST-") && file.endsWith(".xml"),
  );
  return junitResults(
    files.map((file) => readFileSync(join(testDirectory, file), "utf8")),
  );
}

export function componentResult(
  component,
  command,
  directory = join(output, component.id),
) {
  const result = {
    component: component.id,
    status: "failed",
    command,
    html: `${component.id}/index.html`,
  };
  try {
    result.metrics =
      component.kind === "vitest"
        ? vitestMetrics(
            JSON.parse(
              readFileSync(join(directory, "coverage-summary.json"), "utf8"),
            ),
          )
        : koverMetrics(readFileSync(join(directory, "report.xml"), "utf8"));
    result.tests = testResults(component, directory);
    const failures = [];
    if (command !== 0) failures.push("Suite command failed");
    if (!(result.tests.executed > 0)) failures.push("No tests executed");
    if (result.tests.failures || result.tests.errors)
      failures.push("Tests failed");
    if (!(result.metrics.lines.percent >= 80))
      failures.push("Line coverage below 80% or empty");
    if (!statSync(join(directory, "index.html")).isFile())
      failures.push("HTML report missing");
    if (
      component.kind === "vitest" &&
      !readFileSync(join(directory, "lcov.info"), "utf8").includes(
        "end_of_record",
      )
    ) {
      failures.push("LCOV report missing or empty");
    }
    result.reasons = failures;
    if (!failures.length) result.status = "passed";
  } catch (error) {
    result.reasons = [error.message];
  }
  return result;
}

const escape = (value) =>
  String(value).replace(
    /[&<>"']/g,
    (character) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        character
      ],
  );
const percent = (value) =>
  value == null ? "Unavailable" : `${value.toFixed(2)}%`;

function sonarSection(publication) {
  if (!publication) return "";
  const rows = publication.components
    .map(
      (item) => `<tr>
    <th>${escape(item.component)}</th><td>${escape(item.status)}</td>
    <td>${percent(item.coverage)}</td><td>${percent(item.lines)}</td>
    <td>${item.covered ?? "—"} / ${item.total ?? "—"}</td>
    <td>${item.conditions?.covered ?? "—"} / ${item.conditions?.total ?? "—"}</td>
    <td>${item.quality?.securityIssues ?? "—"}</td><td>${item.quality?.reliabilityIssues ?? "—"}</td>
    <td>${item.quality?.duplicatedLines ?? "—"} / ${item.quality?.duplicatedBlocks ?? "—"}</td>
    <td>${escape(item.gate ?? item.reason ?? "Unavailable")}</td></tr>`,
    )
    .join("");
  return `<h2>Published Sonar analysis</h2><p>Published: ${escape(publication.publishedAt)}.
    Both Coverage and Lines must reach 80%; security, reliability and duplicated lines/blocks must be zero for every component.</p>
    <div class="scroll"><table><thead><tr><th>Component</th><th>Status</th>
    <th>Coverage</th><th>Lines</th><th>Covered / total lines</th>
    <th>Covered / total conditions</th><th>Security</th><th>Reliability</th><th>Duplicate lines / blocks</th><th>Gate</th></tr></thead><tbody>${rows}</tbody></table></div>`;
}

export function writeSummary(summary, publication) {
  writeFileSync(
    join(output, "summary.json"),
    `${JSON.stringify(summary, null, 2)}\n`,
  );
  const rows = summary.components
    .map(
      (item) => `<tr><th>${escape(item.component)}</th>
    <td>${escape(item.status)}</td><td>${percent(item.metrics?.lines.percent)}</td>
    <td>${item.metrics?.lines.covered ?? "—"} / ${item.metrics?.lines.total ?? "—"}</td>
    <td>${percent(item.metrics?.branches?.percent)}</td><td>${percent(item.metrics?.functions?.percent)}</td>
    <td>${item.tests?.executed ?? "—"} executed; ${(item.tests?.failures ?? 0) + (item.tests?.errors ?? 0)} failed</td>
    <td><a href="${item.html}">HTML</a> · <a href="${escape(summary.sonarUrl)}/component_measures?id=spoolin-unit-${item.component}&amp;metric=coverage&amp;view=list">Sonar</a></td>
    <td>${escape((item.reasons ?? []).join("; "))}</td></tr>`,
    )
    .join("");
  writeFileSync(
    join(output, "index.html"),
    `<!doctype html><html lang="en"><meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1"><title>SpoolIn unit coverage</title>
    <style>body{font:16px system-ui;margin:2rem;line-height:1.5}table{border-collapse:collapse}td,th{padding:.6rem;border:1px solid #999;text-align:left}.scroll{overflow:auto}a{color:#0645ad}</style>
    <h1>SpoolIn unit coverage — ${escape(summary.status)}</h1><p>Minimum: 80% executable lines in every component.</p>
    <p>Revision: <code>${escape(summary.revision)}</code>${summary.dirty ? " (working changes)" : ""}<br>
    Started: ${escape(summary.startedAt)}<br>Finished: ${escape(summary.finishedAt)}</p>
    <p>${escape(summary.reason ?? "")}</p><div class="scroll"><table><thead><tr><th>Component</th><th>Status</th><th>Lines</th>
    <th>Covered / total</th><th>Branches</th><th>Functions / methods</th><th>Tests</th><th>Reports</th><th>Failure</th></tr></thead><tbody>${rows}</tbody></table></div>
    ${sonarSection(publication)}
    <p>Frontend HTML and the enforced threshold include TypeScript and Svelte. Sonar's frontend view covers TypeScript; its Svelte analysis is unavailable.
    Sonar publication requires both combined line/condition coverage and line coverage to reach 80%. Sonar links show the last published analysis, which may predate this run. See <a href="sonar-results.json">Sonar publication results</a>.</p></html>`,
  );
}
