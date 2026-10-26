import { spawnSync, execFileSync } from "node:child_process";
import {
  closeSync,
  cpSync,
  mkdirSync,
  openSync,
  readdirSync,
  rmSync,
} from "node:fs";
import { join, resolve } from "node:path";
import {
  components,
  componentResult,
  dockerExecution,
  fingerprint,
  output,
  writeSummary,
} from "./report.mjs";

function execute(id, argv, env = {}) {
  console.log(`Running ${id}; log: ${output}/logs/${id}.log`);
  const log = openSync(`${output}/logs/${id}.log`, "w");
  try {
    const result = spawnSync(argv[0], argv.slice(1), {
      env: { ...process.env, ...env },
      stdio: ["ignore", log, log],
      timeout: 1800000,
    });
    if (result.error) console.error(`${id}: ${result.error.message}`);
    return result.status ?? 1;
  } finally {
    closeSync(log);
  }
}

function gradleArguments(component) {
  const project = `:${component.path.replaceAll("/", ":")}`;
  return [
    "./gradlew",
    "-PunitCoverage=true",
    `${project}:unitTest`,
    "--rerun",
    `${project}:koverXmlReportUnit`,
    `${project}:koverHtmlReportUnit`,
    `${project}:verifyUnitTests`,
    "--no-daemon",
    "--no-parallel",
    "--max-workers=1",
    "-Dorg.gradle.jvmargs=-Xmx1024m",
  ];
}

function runComponent(component) {
  const directory = `${output}/${component.id}`;
  mkdirSync(directory, { recursive: true });
  if (component.kind === "vitest") {
    return execute(
      component.id,
      [
        "npm",
        "run",
        "test:coverage",
        "--workspace",
        component.path,
        "--",
        "--maxWorkers=1",
        "--no-file-parallelism",
      ],
      { UNIT_COVERAGE_DIRECTORY: resolve(directory) },
    );
  }
  rmSync(`${component.path}/build/reports/unit-coverage`, {
    recursive: true,
    force: true,
  });
  rmSync(`${component.path}/build/test-results/unitTest`, {
    recursive: true,
    force: true,
  });
  rmSync(`${component.path}/build/kover/bin-reports/unitTest.ic`, {
    force: true,
  });
  rmSync(`${component.path}/build/classes/kotlin/test`, {
    recursive: true,
    force: true,
  });
  rmSync(`${component.path}/build/classes/java/test`, {
    recursive: true,
    force: true,
  });
  const status = execute(component.id, gradleArguments(component));
  copyUnitReports(component, directory);
  return status;
}

function copyUnitReports(component, directory) {
  for (const [from, to] of [
    ["reports/unit-coverage/html", ""],
    ["reports/unit-coverage/report.xml", "report.xml"],
    ["test-results/unitTest", "tests"],
  ]) {
    try {
      cpSync(`${component.path}/build/${from}`, `${directory}/${to}`, {
        recursive: true,
      });
    } catch {
      /* Missing outputs are reported as failed by componentResult. */
    }
  }
}

const summary = {
  startedAt: new Date().toISOString(),
  revision: execFileSync("git", ["rev-parse", "HEAD"], {
    encoding: "utf8",
  }).trim(),
  dirty: Boolean(
    execFileSync("git", ["status", "--porcelain"], { encoding: "utf8" }).trim(),
  ),
  fingerprint: fingerprint(),
  threshold: 80,
  status: "failed",
  sonarUrl: process.env.SONAR_PUBLIC_URL ?? "http://localhost:9000",
  components: [],
};
// Retain the directory inode so an active report-server bind mount sees new runs.
mkdirSync(output, { recursive: true });
for (const entry of readdirSync(output))
  rmSync(join(output, entry), { recursive: true, force: true });
mkdirSync(`${output}/logs`, { recursive: true });
try {
  summary.execution = dockerExecution();
  const capture = execute("quality-inputs", [
    "python3",
    "ops/unit-coverage/quality-proof.py",
    "capture",
  ]);
  if (capture !== 0) throw new Error("Quality input capture failed");
  const setup = execute("dependencies", ["npm", "ci"]);
  const reportChecks = execute("report-contracts", [
    "node",
    "--test",
    "ops/unit-coverage/report.test.mjs",
  ]);
  if (reportChecks !== 0)
    summary.reason = "Coverage report regression checks failed.";
  for (const component of components) {
    const status = setup === 0 ? runComponent(component) : setup;
    summary.components.push(componentResult(component, status));
  }
  const receipt = execute("quality-producers", [
    "python3",
    "ops/unit-coverage/quality-proof.py",
    "finish",
  ]);
  if (receipt !== 0) summary.reason = "Unit producer provenance failed";
  if (fingerprint() !== summary.fingerprint)
    summary.reason = "Source inputs changed during the run; rerun coverage.";
  if (
    !summary.reason &&
    summary.components.every((item) => item.status === "passed")
  )
    summary.status = "passed";
} catch (error) {
  summary.reason = error.message;
} finally {
  summary.finishedAt = new Date().toISOString();
  writeSummary(summary);
  for (const item of summary.components)
    console.log(
      `${item.component}: ${item.metrics?.lines.percent?.toFixed(2) ?? "unavailable"}% lines — ${item.status}`,
    );
  console.log(`Reports: ${output}/index.html (make coverage-report to serve)`);
}
process.exitCode = summary.status === "passed" ? 0 : 1;
