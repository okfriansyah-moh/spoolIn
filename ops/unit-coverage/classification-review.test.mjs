import assert from "node:assert/strict";
import { Buffer } from "node:buffer";
import {
  mkdtempSync,
  mkdirSync,
  writeFileSync,
  readFileSync,
  rmSync,
  existsSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import {
  ciContext,
  digest,
  hashBytes,
  issueRecord,
  reconcileIssues,
  reviewBody,
  selectPublication,
  validateReceipt,
} from "./review-contract.mjs";
import {
  currentReview,
  githubGet,
  publications,
  waitForReview,
} from "./review-channel.mjs";
import {
  applyTransitions,
  applyReview,
  approvedRecords,
  exportReview,
} from "./sonar-review.mjs";

const stamp = "2026-01-01T00:00:00.000Z";
const env = {
  GITHUB_ACTIONS: "true",
  GITHUB_REPOSITORY: "owner/repo",
  GITHUB_RUN_ID: "123",
  GITHUB_RUN_ATTEMPT: "1",
  SONAR_REVIEW_HEAD_SHA: "a".repeat(40),
  SONAR_REVIEW_PR_NUMBER: "17",
  GITHUB_ACTOR: "publisher",
  GITHUB_TOKEN: "fixture-token",
};
const bytes = Buffer.from("protective source and tests");
const read = () => bytes;

export function fixture() {
  const issue = {
    issue: "issue-1",
    rule: "typescript:S1313",
    component: "mcp",
    source: "mcp/src/network.ts",
    source_sha256: hashBytes(bytes),
    status: "OPEN",
    range: { startLine: 3, endLine: 3 },
    message: "Verify this protected network range",
    impacts: [{ softwareQuality: "SECURITY", severity: "MEDIUM" }],
  };
  const manifest = {
    schema_version: 1,
    ...ciContext(env),
    revision: "b".repeat(40),
    created_at: stamp,
    fingerprint: "f".repeat(64),
    components: [
      { component: "mcp", analysis_id: "analysis-1", issues: [issue] },
    ],
    rules: [{ key: issue.rule, name: "Review range" }],
    issues: [issue],
  };
  const receipt = receiptFor(manifest);
  const publication = {
    id: 456,
    commit_id: manifest.head_revision,
    author_association: "OWNER",
    state: "COMMENTED",
    user: { login: "publisher" },
    submitted_at: stamp,
    body: reviewBody(receipt),
  };
  return { manifest, receipt, publication };
}

export function receiptFor(manifest) {
  const decisions = [
    {
      issue: manifest.issues[0].issue,
      classification: "FALSE_POSITIVE",
      rationale:
        "The exact range configures a denylist and is never a request destination; protective cases verify rejection.",
      evidence: [
        { path: "mcp/test/network.test.ts", sha256: hashBytes(bytes) },
      ],
    },
  ];
  const { request, response } = invocationFor(manifest, decisions);
  const receipt = {
    schema_version: 1,
    repository: manifest.repository,
    run_id: manifest.run_id,
    run_attempt: manifest.run_attempt,
    revision: manifest.revision,
    head_revision: manifest.head_revision,
    manifest_sha256: digest(manifest),
    author: request.author,
    reviewer: request.reviewer,
    reviewed_at: manifest.created_at,
    decisions,
    review_evidence: {
      provider: "native-agent",
      agent_path: "/root/independent",
      request,
      response,
      request_sha256: digest(request),
      response_sha256: digest(response),
    },
  };
  return receipt;
}

function invocationFor(manifest, decisions) {
  const request = {
    author: "implementer",
    reviewer: "independent",
    manifest_sha256: digest(manifest),
    implementers: ["implementer", "publisher"],
    requested_at: manifest.created_at,
  };
  const response = {
    reviewer: "independent",
    manifest_sha256: digest(manifest),
    responded_at: manifest.created_at,
    decisions,
    report:
      "Independently traced the exact source and inspected protective network behavior checks.",
  };
  return { request, response };
}

test("accept an exact independently evidenced receipt, retaining distinct GitHub publisher", () => {
  const { manifest, receipt, publication } = fixture();
  assert.equal(validateReceipt(receipt, manifest, read), receipt);
  const found = selectPublication([publication], manifest);
  assert.equal(found.receipt.reviewer, "independent");
  assert.equal(found.publication.user.login, "publisher");
});

test("reject stale run attempts, repository, merge, head and altered manifest", () => {
  for (const field of [
    "run_id",
    "run_attempt",
    "repository",
    "revision",
    "head_revision",
    "manifest_sha256",
  ]) {
    const { manifest, receipt } = fixture();
    receipt[field] = "changed";
    assert.throws(() => validateReceipt(receipt, manifest, read));
  }
});

test("reject reviewer name alone, forged invocation hashes and implementation overlap", () => {
  const { manifest, receipt } = fixture();
  delete receipt.review_evidence;
  assert.throws(() => validateReceipt(receipt, manifest, read));
  const second = fixture();
  second.receipt.review_evidence.response.report += " changed";
  assert.throws(() => validateReceipt(second.receipt, second.manifest, read));
  const third = fixture();
  third.receipt.review_evidence.request.implementers.push("independent");
  third.receipt.review_evidence.request_sha256 = digest(
    third.receipt.review_evidence.request,
  );
  assert.throws(() => validateReceipt(third.receipt, third.manifest, read));
});

test("reject omitted, duplicate, unknown and code-fix decisions before mutation", () => {
  for (const mutation of [
    (r) => {
      r.decisions = [];
    },
    (r) => {
      r.decisions.push(r.decisions[0]);
    },
    (r) => {
      r.decisions[0].issue = "unknown";
    },
    (r) => {
      r.decisions[0].classification = "FIX_REQUIRED";
    },
  ]) {
    const { manifest, receipt } = fixture();
    mutation(receipt);
    receipt.review_evidence.response.decisions = receipt.decisions;
    receipt.review_evidence.response_sha256 = digest(
      receipt.review_evidence.response,
    );
    assert.throws(() => validateReceipt(receipt, manifest, read));
  }
});

test("reject source or protective test changes and traversal evidence", () => {
  const { manifest, receipt } = fixture();
  assert.throws(() =>
    validateReceipt(receipt, manifest, () => Buffer.from("changed")),
  );
  receipt.decisions[0].evidence[0].path = "../secret";
  receipt.review_evidence.response_sha256 = digest(
    receipt.review_evidence.response,
  );
  assert.throws(() => validateReceipt(receipt, manifest, read));
});

test("generic, untrusted and wrong-head reviews do not authorize; pending/dismissed/conflicting fail", () => {
  const { manifest, publication } = fixture();
  assert.equal(
    selectPublication([{ ...publication, body: "Approved" }], manifest),
    null,
  );
  assert.equal(
    selectPublication(
      [{ ...publication, author_association: "NONE" }],
      manifest,
    ),
    null,
  );
  assert.equal(
    selectPublication([{ ...publication, commit_id: "wrong" }], manifest),
    null,
  );
  for (const state of ["PENDING", "DISMISSED", "CHANGES_REQUESTED", "APPROVED"])
    assert.throws(() =>
      selectPublication([{ ...publication, state }], manifest),
    );
  assert.throws(() =>
    selectPublication([publication, { ...publication, id: 457 }], manifest),
  );
});

test("complete inventories reconcile dual impacts against both raw counters", () => {
  const { manifest } = fixture();
  const issue = manifest.issues[0];
  issue.impacts.push({ softwareQuality: "RELIABILITY", severity: "HIGH" });
  reconcileIssues([issue], { security_issues: 1, reliability_issues: 1 });
  assert.throws(() =>
    reconcileIssues([], { security_issues: 1, reliability_issues: 0 }),
  );
  assert.throws(() =>
    reconcileIssues([issue, issue], {
      security_issues: 2,
      reliability_issues: 2,
    }),
  );
  assert.throws(() =>
    reconcileIssues([issue], { security_issues: -1, reliability_issues: 1 }),
  );
});

test("source mapping requires actual rule, range, impact and maintained ownership", () => {
  const issue = {
    key: "i",
    project: "spoolin-unit-mcp",
    component: "spoolin-unit-mcp:mcp/src/network.ts",
    rule: "r",
    message: "m",
    issueStatus: "OPEN",
    textRange: { startLine: 1, endLine: 1 },
    impacts: [{ softwareQuality: "SECURITY" }],
  };
  const component = { id: "mcp", path: "mcp" };
  assert.equal(
    issueRecord(issue, component, read).source_sha256,
    hashBytes(bytes),
  );
  for (const field of ["rule", "textRange", "impacts", "project"])
    assert.throws(() =>
      issueRecord({ ...issue, [field]: null }, component, read),
    );
  assert.throws(() =>
    issueRecord(
      { ...issue, component: "spoolin-unit-mcp:../outside" },
      component,
      read,
    ),
  );
});

test("GitHub retrieval uses read-only token, bounded TLS endpoint and rejects HTTP failures", async () => {
  let observed;
  await githubGet("pulls/17", env, async (url, options) => {
    observed = { url, options };
    return { ok: true, json: async () => ({}) };
  });
  assert.equal(
    observed.url,
    "https://api.github.com/repos/owner/repo/pulls/17",
  );
  assert.equal(observed.options.redirect, "error");
  await assert.rejects(
    githubGet("pulls/17", env, async () => ({ ok: false, status: 403 })),
  );
});

test("paginate all reviews and reject a moved PR head", async () => {
  const { manifest, publication } = fixture();
  const get = async (path) => {
    if (path === "pulls/17") return { head: { sha: manifest.head_revision } };
    return path.endsWith("page=1")
      ? Array(100).fill({ body: "unrelated" })
      : [publication];
  };
  assert.equal((await publications(manifest, env, get)).length, 101);
  assert.equal(
    (await currentReview(manifest, env, read, get)).receipt.reviewer,
    "independent",
  );
  await assert.rejects(
    publications(manifest, env, async () => ({ head: { sha: "moved" } })),
  );
});

test("missing review times out without classifying; invalid wait bounds fail closed", async () => {
  const { manifest } = fixture();
  let time = 0;
  const get = async (path) =>
    path === "pulls/17" ? { head: { sha: manifest.head_revision } } : [];
  await assert.rejects(
    waitForReview(manifest, { ...env, SONAR_REVIEW_WAIT_SECONDS: "1" }, read, {
      get,
      now: () => time,
      pause: async () => {
        time += 15000;
      },
    }),
    /timeout/,
  );
  await assert.rejects(
    waitForReview(manifest, { ...env, SONAR_REVIEW_WAIT_SECONDS: "0" }, read),
  );
});

test("partial API failures retain attempted/applied identities and never continue", async () => {
  const states = [];
  let calls = 0;
  await assert.rejects(
    applyTransitions(
      [{ issue: "one" }, { issue: "two" }, { issue: "three" }],
      async (endpoint) => {
        if (endpoint === "issues/do_transition" && ++calls === 2)
          throw new Error("API unavailable");
      },
      (audit) => states.push(JSON.parse(JSON.stringify(audit))),
    ),
  );
  assert.equal(calls, 2);
  assert.deepEqual(states.at(-1).attempted, ["one", "two"]);
  assert.deepEqual(states.at(-1).applied, ["one"]);
  assert.match(states.at(-1).status, /failed/);
});

const runtimeComponents = [
  { id: "frontend", path: "frontend" },
  { id: "core", path: "services/core" },
  { id: "catalog", path: "services/catalog" },
  { id: "media", path: "services/media" },
  { id: "backend-support", path: "libraries/backend-support" },
  { id: "mcp", path: "mcp" },
];

function isolatedAnalysis(t) {
  const output = mkdtempSync(join(tmpdir(), "sonar-review-"));
  t.after(() => rmSync(output, { recursive: true, force: true }));
  mkdirSync(`${output}/sonar`);
  for (const component of runtimeComponents) {
    mkdirSync(`${output}/sonar/work-${component.id}`);
    mkdirSync(`${output}/${component.id}`);
    writeFileSync(
      `${output}/sonar/work-${component.id}/report-task.txt`,
      `ceTaskId=${component.id}\n`,
    );
    const producer = JSON.stringify({
      input_sha256: "inputs",
      inputs: {},
      toolchain_files: {},
    });
    writeFileSync(`${output}/${component.id}/producer.json`, producer);
    writeFileSync(
      `${output}/sonar/${component.id}-inputs.json`,
      JSON.stringify({
        input_sha256: "inputs",
        producer_sha256: hashBytes(producer),
      }),
    );
  }
  const state = {
    changedAnalysis: false,
    changedMessage: false,
    active: true,
    transitions: [],
    badMetric: false,
  };
  const options = {
    output,
    components: runtimeComponents,
    env,
    readSource: read,
    summary: { revision: "b".repeat(40), fingerprint: "f".repeat(64) },
    api: fakeSonar(state),
    currentFingerprint: () =>
      state.changedFingerprint ? "changed" : "f".repeat(64),
    checkInputs: () => {},
  };
  return { options, state };
}

function fakeSonar(state) {
  return async (endpoint, params) => {
    if (endpoint === "ce/task")
      return {
        task: { status: "SUCCESS", analysisId: `analysis-${params.id}` },
      };
    if (endpoint === "project_analyses/search")
      return {
        analyses: [
          {
            key: state.changedAnalysis
              ? "changed"
              : params.project.replace("spoolin-unit-", "analysis-"),
          },
        ],
      };
    if (endpoint === "measures/component") return fakeMetrics(params, state);
    if (endpoint === "issues/search") return fakeIssues(params, state);
    if (endpoint === "rules/show")
      return {
        rule: {
          key: params.key,
          name: state.changedRule ? "Changed rule" : "Review network range",
        },
      };
    if (endpoint === "issues/add_comment") return {};
    if (endpoint === "issues/do_transition") {
      state.transitions.push(params.issue);
      state.active = false;
      return {};
    }
    throw new Error(`Unexpected fixture endpoint ${endpoint}`);
  };
}

function fakeMetrics(params, state) {
  const count = params.component === "spoolin-unit-mcp" && state.active ? 1 : 0;
  return {
    component: {
      measures: [
        {
          metric: "security_issues",
          value: JSON.stringify({ total: state.badMetric ? 99 : count }),
        },
        { metric: "reliability_issues", value: JSON.stringify({ total: 0 }) },
      ],
    },
  };
}

function fakeIssues(params, state) {
  const issue = {
    key: "issue-1",
    rule: "typescript:S1313",
    project: "spoolin-unit-mcp",
    component: "spoolin-unit-mcp:mcp/src/network.ts",
    issueStatus: "OPEN",
    message: state.changedMessage
      ? "Changed meaning"
      : "Review protective range",
    textRange: { startLine: 3, endLine: 3 },
    impacts: [{ softwareQuality: "SECURITY", severity: "MEDIUM" }],
  };
  const issues =
    params.componentKeys === "spoolin-unit-mcp" && state.active ? [issue] : [];
  return { issues, paging: { total: issues.length } };
}

function publishedReview(manifest) {
  return {
    ...fixture().publication,
    commit_id: manifest.head_revision,
    submitted_at: manifest.created_at,
    body: reviewBody(receiptFor(manifest)),
  };
}

function githubFixture(t, manifest, publication) {
  const previous = globalThis.fetch;
  globalThis.fetch = async (url) => ({
    ok: true,
    json: async () =>
      url.endsWith("pulls/17")
        ? { head: { sha: manifest.head_revision } }
        : [publication],
  });
  t.after(() => {
    globalThis.fetch = previous;
  });
}

test("full six-component export, exact receipt application and strict reusable evidence", async (t) => {
  const { options, state } = isolatedAnalysis(t);
  await exportReview(options);
  const manifest = JSON.parse(
    readFileSync(`${options.output}/sonar/review-manifest.json`),
  );
  assert.equal(manifest.components.length, 6);
  assert.equal(manifest.rules[0].key, "typescript:S1313");
  const publication = publishedReview(manifest);
  githubFixture(t, manifest, publication);
  await applyReview(options);
  assert.deepEqual(state.transitions, ["issue-1"]);
  const records = await approvedRecords(options);
  assert.equal(records[0].reviewer, "independent");
  assert.equal(records[0].source_sha256, manifest.issues[0].source_sha256);
  publication.state = "DISMISSED";
  await assert.rejects(approvedRecords(options), /dismissed/);
  await assert.rejects(applyReview(options));
});

test("changed live analysis, message and producer block before any transition", async (t) => {
  for (const change of [
    "changedAnalysis",
    "changedMessage",
    "changedFingerprint",
    "changedRule",
    "producer",
  ]) {
    const { options, state } = isolatedAnalysis(t);
    await exportReview(options);
    if (change === "producer")
      writeFileSync(`${options.output}/mcp/producer.json`, "changed");
    else state[change] = true;
    await assert.rejects(applyReview(options));
    assert.deepEqual(state.transitions, []);
  }
});

test("raw counter mismatch cannot produce a complete review manifest", async (t) => {
  const { options, state } = isolatedAnalysis(t);
  state.badMetric = true;
  await assert.rejects(exportReview(options), /inventory differs/);
  assert.equal(
    existsSync(`${options.output}/sonar/review-manifest.json`),
    false,
  );
});

test("zero findings require no classification or review credentials", async (t) => {
  const { options, state } = isolatedAnalysis(t);
  state.active = false;
  options.env = { ...env, GITHUB_TOKEN: "" };
  await exportReview(options);
  await applyReview(options);
  assert.deepEqual(state.transitions, []);
  assert.equal(
    existsSync(`${options.output}/sonar/review-classifications.json`),
    false,
  );
});

test("retrieval and pagination cannot overrun the total review wait deadline", async () => {
  const { manifest } = fixture();
  let time = 0,
    calls = 0;
  const get = async (path) => {
    calls++;
    time += 400;
    return path === "pulls/17"
      ? { head: { sha: manifest.head_revision } }
      : Array(100).fill({ body: "unrelated" });
  };
  await assert.rejects(
    waitForReview(manifest, { ...env, SONAR_REVIEW_WAIT_SECONDS: "1" }, read, {
      get,
      now: () => time,
      pause: async () => {},
    }),
    /timeout/,
  );
  assert.equal(calls, 3);
  let fetched = false;
  await assert.rejects(
    githubGet(
      "pulls/17",
      env,
      async () => {
        fetched = true;
      },
      Date.now() - 1,
    ),
    /timeout/,
  );
  assert.equal(fetched, false);
});

test("GitHub second-resolution publication times permit only the same timestamp interval", () => {
  const { manifest, publication, receipt } = fixture();
  receipt.reviewed_at = "2026-01-01T00:00:00.999Z";
  publication.body = reviewBody(receipt);
  assert.ok(selectPublication([publication], manifest));
  receipt.reviewed_at = "2026-01-01T00:00:01.000Z";
  publication.body = reviewBody(receipt);
  assert.throws(() => selectPublication([publication], manifest), /postdates/);
});
