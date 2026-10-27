import { appendFileSync, writeFileSync } from "node:fs";
import {
  digest,
  requireValue,
  selectPublication,
  validateReceipt,
} from "./review-contract.mjs";

export async function githubGet(
  path,
  env,
  fetcher = globalThis.fetch,
  deadline = Date.now() + 30000,
) {
  const remaining = deadline - Date.now();
  requireValue(remaining > 0, "Review retrieval timeout");
  requireValue(
    Boolean(env.GITHUB_TOKEN),
    "Read-only GitHub token is required for review retrieval",
  );
  const response = await fetcher(
    `https://api.github.com/repos/${env.GITHUB_REPOSITORY}/${path}`,
    {
      headers: {
        Authorization: `Bearer ${env.GITHUB_TOKEN}`,
        Accept: "application/vnd.github+json",
      },
      redirect: "error",
      signal: AbortSignal.timeout(Math.min(30000, remaining)),
    },
  );
  requireValue(
    response.ok,
    `GitHub review retrieval returned HTTP ${response.status}`,
  );
  const result = await response.json();
  requireValue(Date.now() <= deadline, "Review retrieval timeout");
  return result;
}

export async function publications(manifest, env, get = githubGet) {
  if (manifest.target.kind === "pull_request") {
    const pr = await get(`pulls/${manifest.target.number}`, env);
    requireValue(
      pr.head?.sha === manifest.head_revision,
      "PR head changed during classification review",
    );
  }
  const route =
    manifest.target.kind === "pull_request"
      ? `pulls/${manifest.target.number}/reviews`
      : `commits/${manifest.head_revision}/comments`;
  const result = [];
  for (let page = 1; page <= 10; page++) {
    const entries = await get(`${route}?per_page=100&page=${page}`, env);
    requireValue(Array.isArray(entries), "Invalid GitHub review inventory");
    result.push(...entries);
    if (entries.length < 100) return result;
  }
  throw new Error(
    "Review inventory exceeds bounded pagination; no partial inventory accepted",
  );
}

export function reviewInstructions(manifest, directory, env) {
  const text = [
    "## Explicit Sonar classification review",
    `Run: ${manifest.run_id}, attempt: ${manifest.run_attempt}`,
    `Manifest SHA-256: ${digest(manifest)}`,
    `Active findings requiring individual review: ${manifest.issues.length}`,
    "Download the sonar-classification-review artifact. Review each exact issue against source and protective tests.",
    "Submit a structured review receipt using the documented protocol. Generic approvals and prior-run receipts do not authorize classification.",
    `Published review target: ${manifest.head_revision}; measured test merge: ${manifest.revision}.`,
  ].join("\n\n");
  writeFileSync(`${directory}/review-instructions.md`, text + "\n");
  if (env.GITHUB_STEP_SUMMARY)
    appendFileSync(env.GITHUB_STEP_SUMMARY, text + "\n");
}

export async function currentReview(
  manifest,
  env,
  readSource,
  get = githubGet,
) {
  const found = selectPublication(
    await publications(manifest, env, get),
    manifest,
  );
  if (!found) return null;
  validateReceipt(found.receipt, manifest, readSource);
  return found;
}

export async function waitForReview(manifest, env, readSource, options = {}) {
  const seconds = Number(env.SONAR_REVIEW_WAIT_SECONDS ?? "1200");
  requireValue(
    Number.isSafeInteger(seconds) && seconds > 0 && seconds <= 1200,
    "Invalid review wait bound",
  );
  const now = options.now ?? Date.now;
  const pause =
    options.pause ??
    ((ms) => new Promise((resolve) => setTimeout(resolve, ms)));
  const deadline = now() + seconds * 1000;
  const transport =
    options.get ??
    ((path, config) => githubGet(path, config, globalThis.fetch, deadline));
  const get = deadlineTransport(transport, now, deadline);
  while (now() < deadline) {
    const found = await currentReview(manifest, env, readSource, get);
    if (found) return found;
    await pause(Math.min(15000, Math.max(0, deadline - now())));
  }
  throw new Error(
    "Independent per-analysis review was not supplied before timeout; classifications remain blocked",
  );
}

function deadlineTransport(get, now, deadline) {
  return async (path, env) => {
    requireValue(now() < deadline, "Review retrieval timeout");
    const result = await get(path, env);
    requireValue(now() <= deadline, "Review retrieval timeout");
    return result;
  };
}
