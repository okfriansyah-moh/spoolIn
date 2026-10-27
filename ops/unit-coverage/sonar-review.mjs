import { execFileSync } from "node:child_process";
import {
  existsSync,
  lstatSync,
  readFileSync,
  realpathSync,
  writeFileSync,
} from "node:fs";
import { resolve } from "node:path";
import { fingerprint } from "./report.mjs";
import {
  ciContext,
  classificationRecords,
  compareText,
  digest,
  hashBytes,
  issueRecord,
  reconcileIssues,
  repositoryPath,
  requireValue,
  reviewTemplate,
  validateManifest,
} from "./review-contract.mjs";
import {
  currentReview,
  reviewInstructions,
  waitForReview,
} from "./review-channel.mjs";

export function readTrackedSource(path) {
  repositoryPath(path);
  const absolute = resolve(path);
  requireValue(
    realpathSync(absolute) === absolute &&
      !lstatSync(absolute).isSymbolicLink(),
    "Symlink review evidence rejected",
  );
  execFileSync("git", ["ls-files", "--error-unmatch", "--", path], {
    stdio: "ignore",
  });
  return readFileSync(absolute);
}

function contextMatches(manifest, env) {
  const actual = ciContext(env);
  for (const field of Object.keys(actual))
    requireValue(
      digest(actual[field]) === digest(manifest[field]),
      `Review context changed: ${field}`,
    );
}

async function completedAnalysis(component, api, directory) {
  const task = Object.fromEntries(
    readFileSync(`${directory}/work-${component.id}/report-task.txt`, "utf8")
      .trim()
      .split("\n")
      .map((line) => line.split(/=(.*)/s).slice(0, 2)),
  );
  const result = await api("ce/task", { id: task.ceTaskId });
  requireValue(
    result.task.status === "SUCCESS" && result.task.analysisId,
    "Review requires completed analysis",
  );
  const latest = await api("project_analyses/search", {
    project: `spoolin-unit-${component.id}`,
    ps: "1",
  });
  requireValue(
    latest.analyses?.[0]?.key === result.task.analysisId,
    "Project analysis changed",
  );
  return result.task.analysisId;
}

async function issueMetrics(component, api) {
  const response = await api("measures/component", {
    component: `spoolin-unit-${component.id}`,
    metricKeys: "security_issues,reliability_issues",
  });
  return Object.fromEntries(
    response.component.measures.map((item) => [
      item.metric,
      JSON.parse(item.value).total,
    ]),
  );
}

async function activeIssues(component, api, readSource) {
  const result = [];
  for (let page = 1; page <= 20; page++) {
    const response = await api("issues/search", {
      componentKeys: `spoolin-unit-${component.id}`,
      impactSoftwareQualities: "SECURITY,RELIABILITY",
      issueStatuses: "OPEN,CONFIRMED",
      ps: "500",
      p: String(page),
    });
    requireValue(
      Array.isArray(response.issues) &&
        Number.isSafeInteger(response.paging?.total),
      "Invalid analyzer issue inventory",
    );
    result.push(
      ...response.issues.map((issue) =>
        issueRecord(issue, component, readSource),
      ),
    );
    if (result.length === response.paging.total)
      return result.sort((a, b) => compareText(a.issue, b.issue));
    requireValue(
      result.length < response.paging.total && response.issues.length > 0,
      "Incomplete analyzer issue pagination",
    );
  }
  throw new Error("Analyzer issue inventory exceeds bounded pagination");
}

function checkedProducer(component, directory) {
  const producer = JSON.parse(
    readFileSync(`${directory}/${component.id}-inputs.json`, "utf8"),
  );
  const bytes = readFileSync(`${directory}/../${component.id}/producer.json`);
  requireValue(
    hashBytes(bytes) === producer.producer_sha256,
    "Unit producer changed before review",
  );
  const receipt = JSON.parse(bytes);
  requireValue(
    receipt.input_sha256 === producer.input_sha256,
    "Producer input identity differs",
  );
  for (const field of ["inputs", "toolchain_files"])
    for (const [path, expected] of Object.entries(receipt[field])) {
      repositoryPath(path);
      requireValue(
        hashBytes(readFileSync(path)) === expected,
        "Measured producer input changed",
      );
    }
  return producer;
}

async function inventory(component, api, directory, readSource) {
  const analysis_id = await completedAnalysis(component, api, directory);
  const metrics = await issueMetrics(component, api);
  const issues = await activeIssues(component, api, readSource);
  reconcileIssues(issues, metrics);
  const producer = checkedProducer(component, directory);
  return { component: component.id, analysis_id, producer, metrics, issues };
}

export async function exportReview({
  api,
  components,
  output,
  summary,
  env = process.env,
  readSource = readTrackedSource,
}) {
  const directory = `${output}/sonar`;
  const entries = [];
  for (const component of components)
    entries.push(await inventory(component, api, directory, readSource));
  const rules = await ruleDetails(entries, api);
  const manifest = {
    schema_version: 1,
    ...ciContext(env),
    revision: summary.revision,
    fingerprint: summary.fingerprint,
    created_at: new Date().toISOString(),
    components: entries,
    rules,
    issues: entries.flatMap((entry) => entry.issues),
  };
  requireValue(
    new Set(manifest.issues.map((issue) => issue.issue)).size ===
      manifest.issues.length,
    "Duplicate cross-component issue",
  );
  writeFileSync(
    `${directory}/review-manifest.json`,
    JSON.stringify(manifest, null, 2) + "\n",
  );
  writeFileSync(
    `${directory}/review-template.json`,
    JSON.stringify(reviewTemplate(manifest), null, 2) + "\n",
  );
  reviewInstructions(manifest, directory, env);
  console.log(
    `Review manifest ${digest(manifest)}: ${manifest.issues.length} findings`,
  );
}

async function ruleDetails(entries, api) {
  const rules = [];
  const keys = [
    ...new Set(
      entries.flatMap((entry) => entry.issues.map((issue) => issue.rule)),
    ),
  ].sort(compareText);
  for (const key of keys) {
    const result = await api("rules/show", { key });
    requireValue(
      result.rule?.key === key && result.rule.name,
      "Missing analyzer rule details",
    );
    rules.push(result.rule);
  }
  return rules;
}

async function unchangedAnalysis(manifest, options) {
  contextMatches(manifest, options.env);
  requireValue(
    options.currentFingerprint() === manifest.fingerprint,
    "Source inventory changed while awaiting review",
  );
  options.checkInputs();
  requireValue(
    digest(await ruleDetails(manifest.components, options.api)) ===
      digest(manifest.rules),
    "Analyzer rules changed during review",
  );
  requireValue(
    options.summary.fingerprint === manifest.fingerprint &&
      options.summary.revision === manifest.revision,
    "Measured review inputs changed",
  );
  const entries = [];
  for (const component of options.components)
    entries.push(
      await inventory(
        component,
        options.api,
        `${options.output}/sonar`,
        options.readSource,
      ),
    );
  requireValue(
    digest(entries) === digest(manifest.components),
    "Live analysis, findings or source changed after review export",
  );
}

export async function applyReview(options) {
  const settings = {
    env: process.env,
    readSource: readTrackedSource,
    currentFingerprint: fingerprint,
    checkInputs: () =>
      execFileSync(
        "python3",
        ["ops/unit-coverage/quality-proof.py", "check-inputs"],
        { maxBuffer: 1048576 },
      ),
    ...options,
  };
  const directory = `${settings.output}/sonar`;
  const manifest = JSON.parse(
    readFileSync(`${directory}/review-manifest.json`, "utf8"),
  );
  validateManifest(manifest);
  await unchangedAnalysis(manifest, settings);
  if (!manifest.issues.length) return;
  requireValue(
    !existsSync(`${directory}/review-mutations.json`),
    "Prior classification attempt exists; fresh analysis required",
  );
  const found = await waitForReview(
    manifest,
    settings.env,
    settings.readSource,
  );
  await unchangedAnalysis(manifest, settings);
  await applyApproved(found, manifest, settings, directory);
}

function retainReceipt(found, manifest, directory) {
  const proof = {
    manifest_sha256: digest(manifest),
    ...found,
    publication_body_sha256: hashBytes(found.publication.body),
  };
  const path = `${directory}/review-receipt.json`;
  const bytes = JSON.stringify(proof, null, 2) + "\n";
  writeFileSync(path, bytes);
  return classificationRecords(found.receipt, manifest, path, bytes);
}

async function refreshedMetrics(manifest, api) {
  for (let attempt = 0; attempt < 12; attempt++) {
    const values = [];
    for (const component of manifest.components)
      values.push(await issueMetrics({ id: component.component }, api));
    if (
      values.every(
        (metrics) =>
          metrics.security_issues === 0 && metrics.reliability_issues === 0,
      )
    )
      return;
    await new Promise((done) => setTimeout(done, 5000));
  }
  throw new Error(
    "Classifications did not produce zero issue metrics; strict verification remains blocked",
  );
}

async function applyApproved(found, manifest, settings, directory) {
  const records = retainReceipt(found, manifest, directory);
  const persist = (value) =>
    writeFileSync(
      `${directory}/review-mutations.json`,
      JSON.stringify(value, null, 2) + "\n",
    );
  let audit = { status: "applying", attempted: [], applied: [] };
  try {
    audit = await applyTransitions(records, settings.api, persist);
    await refreshedMetrics(manifest, settings.api);
    const confirmed = await currentReview(
      manifest,
      settings.env,
      settings.readSource,
    );
    requireValue(
      confirmed && digest(confirmed) === digest(found),
      "Review changed or revoked during classification",
    );
    audit.status = "applied; strict quality verification still required";
    persist(audit);
    writeFileSync(
      `${directory}/review-classifications.json`,
      JSON.stringify(records, null, 2) + "\n",
    );
  } catch (error) {
    if (existsSync(`${directory}/review-mutations.json`))
      audit = JSON.parse(
        readFileSync(`${directory}/review-mutations.json`, "utf8"),
      );
    audit.status = "failed; fresh analysis required";
    audit.error = error.message;
    persist(audit);
    throw error;
  }
}

export async function applyTransitions(records, api, persist) {
  const audit = { status: "applying", attempted: [], applied: [] };
  try {
    for (const record of records) {
      audit.attempted.push(record.issue);
      persist(audit);
      await api(
        "issues/add_comment",
        {
          issue: record.issue,
          text: `${record.rationale}\nIndependent reviewer: ${record.reviewer}. Evidence: ${record.evidence} (SHA-256 ${record.evidence_sha256}).`,
        },
        "POST",
      );
      await api(
        "issues/do_transition",
        { issue: record.issue, transition: "falsepositive" },
        "POST",
      );
      audit.applied.push(record.issue);
      persist(audit);
    }
  } catch (error) {
    audit.status = "failed; fresh analysis required";
    audit.error = error.message;
    persist(audit);
    throw error;
  }
  return audit;
}

export async function approvedRecords(options) {
  const settings = {
    env: process.env,
    readSource: readTrackedSource,
    currentFingerprint: fingerprint,
    checkInputs: () =>
      execFileSync(
        "python3",
        ["ops/unit-coverage/quality-proof.py", "check-inputs"],
        { maxBuffer: 1048576 },
      ),
    ...options,
  };
  const directory = `${settings.output}/sonar`;
  if (!existsSync(`${directory}/review-classifications.json`)) return [];
  const manifest = JSON.parse(
    readFileSync(`${directory}/review-manifest.json`, "utf8"),
  );
  validateManifest(manifest);
  await approvedAnalysis(manifest, settings, directory);
  const evidencePath = `${directory}/review-receipt.json`;
  const bytes = readFileSync(evidencePath);
  const proof = JSON.parse(bytes);
  const current = await currentReview(
    manifest,
    settings.env,
    settings.readSource,
  );
  requireValue(
    current &&
      digest(current.receipt) === digest(proof.receipt) &&
      digest(current.publication) === digest(proof.publication),
    "Published approval changed or revoked",
  );
  const records = classificationRecords(
    current.receipt,
    manifest,
    evidencePath,
    bytes,
  );
  const stored = JSON.parse(
    readFileSync(`${directory}/review-classifications.json`, "utf8"),
  );
  requireValue(
    digest(records) === digest(stored),
    "Applied classification evidence changed",
  );
  return records;
}

async function approvedAnalysis(manifest, settings, directory) {
  contextMatches(manifest, settings.env);
  requireValue(
    settings.currentFingerprint() === manifest.fingerprint,
    "Approved source inventory changed",
  );
  settings.checkInputs();
  requireValue(
    digest(await ruleDetails(manifest.components, settings.api)) ===
      digest(manifest.rules),
    "Approved analyzer rules changed",
  );
  requireValue(
    settings.summary.fingerprint === manifest.fingerprint &&
      settings.summary.revision === manifest.revision,
    "Approved measurement inputs changed",
  );
  for (const component of settings.components) {
    checkedProducer(component, directory);
    const actual = await completedAnalysis(component, settings.api, directory);
    requireValue(
      actual ===
        manifest.components.find((entry) => entry.component === component.id)
          ?.analysis_id,
      "Approved analysis changed",
    );
  }
}
