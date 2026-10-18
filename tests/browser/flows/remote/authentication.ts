import {
  chmodSync,
  mkdirSync,
  readFileSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { isAbsolute, join, relative, resolve } from 'node:path';
import type { BrowserContext, Page } from '@playwright/test';

export function protectedBrowserDirectory(): string {
  if (
    process.env.GITHUB_ACTIONS !== 'true' ||
    process.env.RUNNER_ENVIRONMENT !== 'github-hosted'
  ) {
    throw new Error(
      'Browser acceptance requires a verified GitHub-hosted runner.',
    );
  }
  const root = process.env.RUNNER_TEMP;
  if (!root || !isAbsolute(root))
    throw new Error('Protected runner temporary directory is required.');
  const directory = join(
    root,
    `spoolin-browser-${process.env.GITHUB_RUN_ID}-${process.env.GITHUB_RUN_ATTEMPT}`,
  );
  mkdirSync(directory, { recursive: true, mode: 0o700 });
  chmodSync(directory, 0o700);
  return directory;
}

export function authenticatedStatePath(): string {
  return join(protectedBrowserDirectory(), 'maintainer-state.json');
}

export function saveProtectedState(path: string, value: unknown): void {
  if (relative(protectedBrowserDirectory(), resolve(path)).startsWith('..'))
    throw new Error('Protected state must remain under RUNNER_TEMP.');
  writeFileSync(path, JSON.stringify(value), { mode: 0o600 });
  chmodSync(path, 0o600);
}

export function checkProtectedState(): void {
  if ((statSync(authenticatedStatePath()).mode & 0o077) !== 0)
    throw new Error('Browser session state permissions are too broad.');
}

type Admission = { reads: number[]; mutations: number[]; resumeAt: number };
const admissionPath = () => join(protectedBrowserDirectory(), 'admission.json');
const limits = () => ({
  reads: Number(process.env.RATE_LIMIT_READ_MAX_REQUESTS ?? 120),
  mutations: Number(process.env.RATE_LIMIT_MUTATION_MAX_REQUESTS ?? 60),
  window:
    Math.max(
      Number(process.env.RATE_LIMIT_READ_WINDOW_SECONDS ?? 60),
      Number(process.env.RATE_LIMIT_MUTATION_WINDOW_SECONDS ?? 60),
    ) * 1000,
});

function admission(): Admission {
  try {
    return JSON.parse(readFileSync(admissionPath(), 'utf8'));
  } catch {
    return { reads: [], mutations: [], resumeAt: 0 };
  }
}

export async function respectAdmission(
  readReserve = 40,
  mutationReserve = 15,
): Promise<void> {
  const policy = limits();
  if (
    ![policy.reads, policy.mutations, policy.window].every(
      (value) => Number.isFinite(value) && value > 0,
    )
  )
    throw new Error('Invalid browser admission configuration.');
  for (;;) {
    const state = admission();
    const now = Date.now();
    state.reads = state.reads.filter((time) => time > now - policy.window);
    state.mutations = state.mutations.filter(
      (time) => time > now - policy.window,
    );
    const readTime =
      state.reads.length + Math.min(readReserve, policy.reads) > policy.reads
        ? (state.reads[0] ?? now) + policy.window
        : now;
    const writeTime =
      state.mutations.length + Math.min(mutationReserve, policy.mutations) >
      policy.mutations
        ? (state.mutations[0] ?? now) + policy.window
        : now;
    const wait = Math.max(state.resumeAt, readTime, writeTime) - now;
    if (wait <= 0) return;
    await new Promise((done) => setTimeout(done, Math.min(wait + 100, 30000)));
  }
}

export function observeAdmission(context: BrowserContext): void {
  context.on('request', (request) => {
    if (new URL(request.url()).pathname !== '/graphql') return;
    const state = admission();
    const query = request.postDataJSON()?.query;
    const key =
      typeof query === 'string' && /^\s*mutation\b/.test(query)
        ? 'mutations'
        : 'reads';
    state[key].push(Date.now());
    saveProtectedState(admissionPath(), state);
  });
  context.on('response', async (response) => {
    if (new URL(response.url()).pathname !== '/graphql') return;
    const retryHeader = response.headers()['retry-after'];
    const body = await response.json().catch(() => null);
    const serialized = JSON.stringify(body);
    if (response.status() !== 429 && !serialized?.includes('RATE_LIMITED'))
      return;
    const retryMs = Number(serialized?.match(/"retryAfterMs":(\d+)/)?.[1] ?? 0);
    const retrySeconds = Number(retryHeader ?? 0);
    const state = admission();
    state.resumeAt = Date.now() + Math.max(retryMs, retrySeconds * 1000, 1000);
    saveProtectedState(admissionPath(), state);
  });
}

export async function authenticateMaintainer(page: Page): Promise<void> {
  const path = process.env.SPOOLIN_BROWSER_MAINTAINER_PASSWORD_FILE;
  if (!path)
    throw new Error('Protected remote Maintainer password file is required.');
  await respectAdmission();
  await page.goto('/sign-in');
  await page
    .getByLabel('Email', { exact: true })
    .fill('maintainer@example.invalid');
  await page
    .getByLabel('Password', { exact: true })
    .fill(readFileSync(path, 'utf8').trim());
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await page.getByRole('button', { name: 'Sign out', exact: true }).waitFor();
}
