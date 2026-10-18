import {
  chmodSync,
  mkdirSync,
  readFileSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import type { BrowserContext, Page, Request, Response } from '@playwright/test';

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

export async function authenticateMaintainer(
  page: Page,
  diagnostics: AuthenticationDiagnostics,
): Promise<void> {
  const path = process.env.SPOOLIN_BROWSER_MAINTAINER_PASSWORD_FILE;
  if (!path)
    throw new Error('Protected remote Maintainer password file is required.');
  await respectAdmission();
  const navigation = await page.goto('/sign-in');
  diagnostics.navigation(navigation?.status() ?? null);
  await diagnostics.capture('before-credentials').catch(() => {
    console.warn('Pre-credential diagnostic capture unavailable.');
  });
  await page
    .getByLabel('Email', { exact: true })
    .fill('maintainer@example.invalid');
  const passwordInput = page.getByLabel('Password', { exact: true });
  await passwordInput.waitFor({ state: 'visible' });
  try {
    await passwordInput.fill(readFileSync(path, 'utf8').trim());
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await page.getByRole('button', { name: 'Sign out', exact: true }).waitFor();
  } catch {
    // Playwright's raw fill error may contain its argument. Retain only sanitized diagnostics after entry.
    throw new Error(
      'Maintainer authentication failed after credential entry. Inspect sanitized setup diagnostics.',
    );
  }
}

type DiagnosticStage = 'before-credentials' | 'failure';
type PolicyObservation = { status: number; codes: string[] };
type DiagnosticState = {
  navigationStatus: number | null;
  assets: Record<string, number | 'request-failed'>;
  errors: Record<string, number>;
  policy: PolicyObservation[];
};
export type AuthenticationDiagnostics = {
  navigation: (status: number | null) => void;
  capture: (stage: DiagnosticStage) => Promise<void>;
  dispose: () => void;
};

function errorClass(message: string): string {
  if (
    /content.security.policy|refused to (?:execute|load)|unsafe-eval|unsafe-inline/i.test(
      message,
    )
  )
    return 'csp';
  if (/mime.type|module.script/i.test(message)) return 'module-or-mime';
  if (/syntaxerror|unexpected.token/i.test(message)) return 'syntax';
  if (/certificate|err_cert/i.test(message)) return 'tls';
  if (
    /failed.to.fetch|network|err_connection|failed.to.load.resource/i.test(
      message,
    )
  )
    return 'network';
  return 'other-javascript';
}

function publicAsset(raw: string): string | null {
  const url = new URL(raw);
  if (url.origin !== 'https://localhost:8443' || url.search || url.hash)
    return null;
  return url.pathname === '/icon.svg' ||
    /^\/_app\/immutable\/(?:entry|chunks|nodes|assets)\/[A-Za-z0-9._-]{1,120}\.(?:js|css|woff2)$/.test(
      url.pathname,
    )
    ? url.pathname
    : null;
}

async function policyCodes(response: Response): Promise<string[]> {
  // Inspect only the public policy read. Never retain body, messages, session fields, headers or variables.
  try {
    const bytes = await response.body();
    if (bytes.length > 65536) return ['BODY_EXCEEDS_DIAGNOSTIC_BOUND'];
    const body = JSON.parse(bytes.toString('utf8'));
    const allowed = new Set([
      'UNAVAILABLE',
      'UNAUTHENTICATED',
      'FORBIDDEN',
      'RATE_LIMITED',
      'VALIDATION_FAILED',
      'POLICY_CHANGED',
      'INTERNAL_SERVER_ERROR',
    ]);
    const errors = Array.isArray(body?.errors) ? body.errors : [];
    return [
      ...new Set<string>(
        errors.slice(0, 8).map((error: { extensions?: { code?: unknown } }) => {
          const code = error?.extensions?.code;
          return typeof code === 'string' && allowed.has(code)
            ? code
            : 'UNCLASSIFIED_GRAPHQL_ERROR';
        }),
      ),
    ];
  } catch {
    return ['NON_JSON_OR_UNREADABLE'];
  }
}

async function visibleLoginState(page: Page) {
  return page.evaluate(() => {
    const text = document.body?.innerText ?? '';
    const states = [
      'Checking your session',
      'Sign-in is temporarily unavailable',
      'Check before continuing',
      'Sign in again before continuing protected work.',
      'Signing in…',
    ];
    return {
      readyState: document.readyState,
      mainCount: document.querySelectorAll('main').length,
      headingCount: document.querySelectorAll('h1,h2,[role="heading"]').length,
      formCount: document.querySelectorAll('form').length,
      emailInputs: document.querySelectorAll('input[type="email"]').length,
      passwordInputs: document.querySelectorAll('input[type="password"]')
        .length,
      moduleScripts: document.querySelectorAll('script[type="module"]').length,
      visibleCharacterCount: Math.min(text.length, 10000),
      knownStates: states.filter((state) => text.includes(state)),
    };
  });
}

async function safeLoginScreenshot(page: Page, path: string): Promise<string> {
  const url = new URL(page.url());
  if (
    url.origin !== 'https://localhost:8443' ||
    !['/sign-in', '/'].includes(url.pathname)
  )
    return 'omitted-unsupported-route';
  // Before entry or after failure, clear fields without dispatching input events or submissions.
  await page.locator('input,textarea').evaluateAll((inputs) => {
    for (const input of inputs)
      (input as HTMLInputElement | HTMLTextAreaElement).value = '';
  });
  await page.screenshot({
    path,
    fullPage: false,
    timeout: 5000,
    mask: [page.locator('input,textarea,[contenteditable="true"],iframe')],
  });
  return 'captured-with-fields-and-frames-masked';
}

async function captureLoginDiagnostic(
  page: Page,
  state: DiagnosticState,
  stage: DiagnosticStage,
): Promise<void> {
  const directory = fileURLToPath(
    new URL('../../../../artifacts/browser/setup/', import.meta.url),
  );
  mkdirSync(directory, { recursive: true });
  const screenshot = await safeLoginScreenshot(
    page,
    join(directory, `${stage}.png`),
  ).catch(() => 'unavailable');
  const url = new URL(page.url());
  const route =
    url.origin === 'https://localhost:8443' &&
    ['/sign-in', '/'].includes(url.pathname)
      ? url.pathname
      : 'other-route';
  const dom = await visibleLoginState(page).catch(() => ({
    unavailable: true,
  }));
  writeFileSync(
    join(directory, `${stage}.json`),
    JSON.stringify(
      {
        stage,
        route,
        navigationStatus: state.navigationStatus,
        dom,
        screenshot,
        publicAssetStatuses: state.assets,
        javascriptErrorClasses: state.errors,
        policyReads: state.policy,
        testedSha: process.env.TESTED_SHA ?? 'UNVERIFIED',
        runId: process.env.GITHUB_RUN_ID,
        attempt: process.env.GITHUB_RUN_ATTEMPT,
        browser: 'chromium',
        viewport: page.viewportSize(),
        proof:
          'Setup diagnosis only; authentication assertion remains required.',
      },
      null,
      2,
    ) + '\n',
  );
}

export function observeAuthentication(page: Page): AuthenticationDiagnostics {
  const state: DiagnosticState = {
    navigationStatus: null,
    assets: {},
    errors: {},
    policy: [],
  };
  const recordError = (message: string) => {
    const kind = errorClass(message);
    state.errors[kind] = Math.min((state.errors[kind] ?? 0) + 1, 1000);
  };
  const onConsole = (message: import('@playwright/test').ConsoleMessage) => {
    if (message.type() === 'error') recordError(message.text());
  };
  const onError = (error: Error) => recordError(error.message);
  const onResponse = (response: Response) => {
    const asset = publicAsset(response.url());
    if (asset && Object.keys(state.assets).length < 60)
      state.assets[asset] = response.status();
    const url = new URL(response.url());
    if (
      url.origin !== 'https://localhost:8443' ||
      url.pathname !== '/graphql' ||
      state.policy.length >= 16
    )
      return;
    let query: unknown;
    try {
      query = response.request().postDataJSON()?.query;
    } catch {
      return;
    }
    if (
      typeof query !== 'string' ||
      !/^\s*(?:query\b|\{)/.test(query) ||
      !/\bpolicy\s*\{/.test(query)
    )
      return;
    const observation = { status: response.status(), codes: [] as string[] };
    state.policy.push(observation);
    void policyCodes(response).then((codes) => {
      observation.codes = codes;
    });
  };
  const onRequestFailed = (request: Request) => {
    const asset = publicAsset(request.url());
    if (asset && Object.keys(state.assets).length < 60)
      state.assets[asset] = 'request-failed';
  };
  page.on('requestfailed', onRequestFailed);
  page.on('console', onConsole);
  page.on('pageerror', onError);
  page.on('response', onResponse);
  return {
    navigation: (status) => {
      state.navigationStatus = status;
    },
    capture: (stage) => captureLoginDiagnostic(page, state, stage),
    dispose() {
      page.off('requestfailed', onRequestFailed);
      page.off('console', onConsole);
      page.off('pageerror', onError);
      page.off('response', onResponse);
    },
  };
}
