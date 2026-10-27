import { createHash } from "node:crypto";

export const reviewMarker = "<!-- spoolin-sonar-review:v1 -->";
export const digest = (value) =>
  createHash("sha256").update(JSON.stringify(value)).digest("hex");
export const hashBytes = (value) =>
  createHash("sha256").update(value).digest("hex");
export const compareText = (a, b) => (a < b ? -1 : a > b ? 1 : 0);

export function requireValue(condition, message) {
  if (!condition) throw new Error(message);
}

export function repositoryPath(value) {
  requireValue(
    typeof value === "string" && value.length > 0,
    "Invalid evidence/source path",
  );
  const controls = [...value].some(
    (char) => char.codePointAt(0) < 32 || char.codePointAt(0) === 127,
  );
  requireValue(
    !controls && !value.includes("\\") && !value.includes(":"),
    "Invalid evidence/source path",
  );
  requireValue(
    value.split("/").every((part) => part && part !== "." && part !== ".."),
    "Unsafe evidence/source path",
  );
  requireValue(
    !value.startsWith(".git/"),
    "Git internals cannot be review evidence",
  );
  return value;
}

export function ciContext(env) {
  requireValue(
    env.GITHUB_ACTIONS === "true",
    "Classification review is CI-only",
  );
  requireValue(
    /^[\w.-]+\/[\w.-]+$/.test(env.GITHUB_REPOSITORY),
    "Missing GitHub repository",
  );
  for (const field of ["GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT"])
    requireValue(/^[1-9]\d*$/.test(env[field]), `Invalid ${field}`);
  requireValue(
    /^[a-f0-9]{40}$/.test(env.SONAR_REVIEW_HEAD_SHA),
    "Missing published head revision",
  );
  requireValue(Boolean(env.GITHUB_ACTOR), "Missing implementation publisher");
  const number = Number(env.SONAR_REVIEW_PR_NUMBER ?? "0");
  requireValue(
    Number.isSafeInteger(number) && number >= 0,
    "Invalid PR number",
  );
  return {
    repository: env.GITHUB_REPOSITORY,
    run_id: env.GITHUB_RUN_ID,
    run_attempt: env.GITHUB_RUN_ATTEMPT,
    head_revision: env.SONAR_REVIEW_HEAD_SHA,
    implementation_publisher: env.GITHUB_ACTOR,
    target: number ? { kind: "pull_request", number } : { kind: "commit" },
  };
}

export function issueRecord(issue, component, readSource) {
  requireValue(
    issue.project === `spoolin-unit-${component.id}`,
    "Unexpected issue project",
  );
  const prefix = `${issue.project}:`;
  requireValue(
    issue.component?.startsWith(prefix),
    "Missing issue source mapping",
  );
  const source = repositoryPath(issue.component.slice(prefix.length));
  requireValue(
    source.startsWith(`${component.path}/src/`),
    "Issue outside maintained component source",
  );
  requireValue(
    issue.key && issue.rule && issue.message,
    "Incomplete issue identity",
  );
  requireValue(
    ["OPEN", "CONFIRMED"].includes(issue.issueStatus),
    "Unexpected active issue status",
  );
  const range = issueRange(issue);
  requireValue(
    Array.isArray(issue.impacts) && issue.impacts.length > 0,
    "Missing issue impacts",
  );
  requireValue(
    issue.impacts.some((impact) =>
      ["SECURITY", "RELIABILITY"].includes(impact.softwareQuality),
    ),
    "Issue is outside review qualities",
  );
  return {
    issue: issue.key,
    rule: issue.rule,
    component: component.id,
    source,
    source_sha256: hashBytes(readSource(source)),
    range,
    message: issue.message,
    status: issue.issueStatus,
    impacts: issue.impacts,
  };
}

function issueRange(issue) {
  const range = issue.textRange;
  requireValue(
    Number.isSafeInteger(range?.startLine) && range.startLine > 0,
    "Missing issue range",
  );
  requireValue(
    Number.isSafeInteger(range.endLine) && range.endLine >= range.startLine,
    "Invalid issue range",
  );
  return range;
}

export function reconcileIssues(issues, metrics) {
  requireValue(
    new Set(issues.map((issue) => issue.issue)).size === issues.length,
    "Duplicate issue inventory",
  );
  for (const [quality, metric] of [
    ["SECURITY", "security_issues"],
    ["RELIABILITY", "reliability_issues"],
  ]) {
    const count = issues.filter((issue) =>
      issue.impacts.some((impact) => impact.softwareQuality === quality),
    ).length;
    requireValue(
      Number.isSafeInteger(metrics[metric]) && metrics[metric] >= 0,
      "Invalid issue metric",
    );
    requireValue(
      count === metrics[metric],
      `Issue inventory differs from ${metric}`,
    );
  }
}

function boundReceipt(receipt, manifest, now) {
  requireValue(receipt.schema_version === 1, "Unsupported review receipt");
  requireValue(
    receipt.manifest_sha256 === digest(manifest),
    "Review manifest changed",
  );
  for (const field of [
    "repository",
    "run_id",
    "run_attempt",
    "revision",
    "head_revision",
  ])
    requireValue(receipt[field] === manifest[field], `Review ${field} differs`);
  const reviewed = Date.parse(receipt.reviewed_at);
  requireValue(
    reviewed >= Date.parse(manifest.created_at) && reviewed <= now,
    "Invalid review time",
  );
  requireValue(
    receipt.author && receipt.reviewer && receipt.author !== receipt.reviewer,
    "Independent reviewer required",
  );
}

function invocationEvidence(receipt, manifest) {
  const evidence = receipt.review_evidence;
  requireValue(
    evidence?.provider && evidence.agent_path,
    "Missing independent invocation evidence",
  );
  requireValue(
    evidence.agent_path.split("/").at(-1) ===
      receipt.reviewer.split("/").at(-1),
    "Invocation agent identity differs",
  );
  const { request, response } = evidence;
  requireValue(
    evidence.request_sha256 === digest(request) &&
      evidence.response_sha256 === digest(response),
    "Review invocation hashes differ",
  );
  requireValue(
    request?.manifest_sha256 === digest(manifest) &&
      response?.manifest_sha256 === digest(manifest),
    "Invocation reviewed another manifest",
  );
  invocationIdentities(request, response, receipt, manifest);
  requireValue(
    typeof response.report === "string" && response.report.trim().length >= 40,
    "Missing independent review report",
  );
  requireValue(
    digest(response.decisions) === digest(receipt.decisions),
    "Decisions differ from independent review response",
  );
  const start = Date.parse(request.requested_at),
    end = Date.parse(response.responded_at);
  requireValue(
    start >= Date.parse(manifest.created_at) &&
      end >= start &&
      end <= Date.parse(receipt.reviewed_at),
    "Invalid invocation chronology",
  );
}

function invocationIdentities(request, response, receipt, manifest) {
  requireValue(
    request.author === receipt.author &&
      request.reviewer === receipt.reviewer &&
      response.reviewer === receipt.reviewer,
    "Invocation identities differ",
  );
  requireValue(
    Array.isArray(request.implementers) &&
      request.implementers.includes(receipt.author),
    "Missing implementation identity",
  );
  requireValue(
    request.implementers.includes(manifest.implementation_publisher) &&
      !request.implementers.includes(receipt.reviewer),
    "Reviewer implemented this selection",
  );
}

function decisionEvidence(decision, issue, readSource) {
  requireValue(
    decision.classification === "FALSE_POSITIVE",
    `Finding requires a code fix: ${issue.issue}`,
  );
  requireValue(
    typeof decision.rationale === "string" &&
      decision.rationale.trim().length >= 40,
    "Missing issue-specific rationale",
  );
  requireValue(
    Array.isArray(decision.evidence) && decision.evidence.length > 0,
    "Missing protective review evidence",
  );
  requireValue(
    hashBytes(readSource(issue.source)) === issue.source_sha256,
    "Reviewed source changed",
  );
  for (const evidence of decision.evidence)
    requireValue(
      hashBytes(readSource(repositoryPath(evidence.path))) === evidence.sha256,
      "Protective evidence changed",
    );
}

export function validateReceipt(
  receipt,
  manifest,
  readSource,
  now = Date.now(),
) {
  validateManifest(manifest);
  boundReceipt(receipt, manifest, now);
  invocationEvidence(receipt, manifest);
  requireValue(
    Array.isArray(receipt.decisions) &&
      receipt.decisions.length === manifest.issues.length,
    "Incomplete review decisions",
  );
  const decisions = new Map(
    receipt.decisions.map((decision) => [decision.issue, decision]),
  );
  requireValue(
    decisions.size === receipt.decisions.length,
    "Duplicate review decision",
  );
  for (const issue of manifest.issues) {
    const decision = decisions.get(issue.issue);
    requireValue(Boolean(decision), "Missing or unknown review issue");
    decisionEvidence(decision, issue, readSource);
  }
  return receipt;
}

export function reviewBody(receipt) {
  return `${reviewMarker}\n\`\`\`json\n${JSON.stringify(receipt, null, 2)}\n\`\`\``;
}

export function reviewTemplate(manifest) {
  const fields = [
    "repository",
    "run_id",
    "run_attempt",
    "revision",
    "head_revision",
  ];
  return {
    schema_version: 1,
    ...Object.fromEntries(fields.map((field) => [field, manifest[field]])),
    manifest_sha256: digest(manifest),
    author: "",
    reviewer: "",
    reviewed_at: "",
    review_evidence: {
      provider: "native-agent",
      agent_path: "",
      request: {},
      response: {},
      request_sha256: "",
      response_sha256: "",
    },
    decisions: manifest.issues.map(({ issue }) => ({
      issue,
      classification: "UNREVIEWED",
      rationale: "",
      evidence: [],
    })),
  };
}

function matchingBody(body, manifest) {
  if (!body?.startsWith(reviewMarker) || !body.includes(digest(manifest)))
    return null;
  const match = body
    .trim()
    .match(/^<!-- spoolin-sonar-review:v1 -->\n```json\n([\s\S]+)\n```$/);
  requireValue(Boolean(match), "Malformed review receipt body");
  return JSON.parse(match[1]);
}

export function selectPublication(publications, manifest) {
  const matches = [];
  for (const publication of publications) {
    if (!["OWNER", "COLLABORATOR"].includes(publication.author_association))
      continue;
    if (publication.commit_id !== manifest.head_revision) continue;
    const receipt = matchingBody(publication.body, manifest);
    if (receipt) matches.push({ publication, receipt });
  }
  requireValue(
    matches.length <= 1,
    "Conflicting or duplicate review receipts; fresh analysis required",
  );
  if (!matches.length) return null;
  const { publication } = matches[0];
  if (manifest.target.kind === "pull_request")
    requireValue(
      publication.state === "COMMENTED",
      "Pending or dismissed review cannot authorize classification",
    );
  const timestamp = publication.submitted_at ?? publication.created_at;
  requireValue(
    Date.parse(timestamp) >= Date.parse(manifest.created_at),
    "Review publication predates analysis",
  );
  requireValue(
    publication.user?.login && Number.isSafeInteger(publication.id),
    "Missing GitHub publisher identity",
  );
  requireValue(
    Date.parse(matches[0].receipt.reviewed_at) < Date.parse(timestamp) + 1000,
    "Review evidence postdates publication",
  );
  requireValue(
    Date.parse(timestamp) <= Date.now(),
    "Review publication is in the future",
  );
  return matches[0];
}

export function classificationRecords(
  receipt,
  manifest,
  evidencePath,
  evidenceBytes,
) {
  const decisions = new Map(
    receipt.decisions.map((decision) => [decision.issue, decision]),
  );
  return manifest.issues.map((issue) => ({
    issue: issue.issue,
    rule: issue.rule,
    source: issue.source,
    source_sha256: issue.source_sha256,
    status: "FALSE_POSITIVE",
    rationale: decisions.get(issue.issue).rationale,
    evidence: evidencePath,
    evidence_sha256: hashBytes(evidenceBytes),
    author: receipt.author,
    reviewer: receipt.reviewer,
    reviewed_at: receipt.reviewed_at,
  }));
}

export function validateManifest(manifest) {
  requireValue(
    manifest.schema_version === 1 && Array.isArray(manifest.components),
    "Invalid review manifest",
  );
  const issues = manifest.components.flatMap((entry) => entry.issues);
  requireValue(
    digest(issues) === digest(manifest.issues),
    "Manifest omits or changes component findings",
  );
  requireValue(
    new Set(issues.map((issue) => issue.issue)).size === issues.length,
    "Duplicate manifest issue",
  );
  const rules = [...new Set(issues.map((issue) => issue.rule))].sort(
    compareText,
  );
  const exported = manifest.rules.map((rule) => rule.key).sort(compareText);
  requireValue(
    digest(rules) === digest(exported),
    "Missing or duplicate manifest rules",
  );
}
