import { randomUUID } from 'node:crypto';
import { statesByStep } from './state-mapping';
import {
  checkProtectedState,
  observeAdmission,
  respectAdmission,
} from './authentication';
import {
  test as base,
  expect,
  type Page,
  type Response,
  type TestInfo,
} from '@playwright/test';

export const test = base.extend({
  page: async ({ page, context }, use) => {
    observeAdmission(context);
    await respectAdmission();
    await use(page);
  },
});
export { expect };

test.afterEach(async ({ page }, info) => {
  if (page.isClosed() || info.status === info.expectedStatus) return;
  for (const input of await page.locator('input[type="password"]').all())
    await input.fill('').catch(() => {});
  if (
    await page
      .locator('input[type="password"]')
      .evaluateAll((inputs) =>
        inputs.every((input) => (input as HTMLInputElement).value === ''),
      )
  ) {
    await capture(page, info, info.title.match(/ATDD-\d+/g) ?? [], 'failure');
  }
});

export async function capture(
  page: Page,
  info: TestInfo,
  cases: string[],
  step: string,
) {
  await respectAdmission(15, 5);
  expect(
    await page
      .locator('input[type="password"]')
      .evaluateAll((inputs) =>
        inputs.every((input) => (input as HTMLInputElement).value === ''),
      ),
  ).toBe(true);
  const path = info.outputPath(`${step}.png`);
  await page.screenshot({ path, fullPage: true });
  await info.attach(step, { path, contentType: 'image/png' });
  await info.attach(`${step}-manifest`, {
    body: JSON.stringify({
      cases,
      step,
      route: new URL(page.url()).pathname,
      states: statesByStep[step] ?? [],
      scope: step.startsWith('approved-reference-')
        ? 'Approved static reference; no production proof'
        : 'Real production observation with attached assertions',
      theme:
        (await page.locator('html').getAttribute('data-theme')) ??
        (await page.locator('html').getAttribute('data-design-target')),
      viewport: page.viewportSize(),
      browser: info.project.use.browserName,
      browserVersion: page.context().browser()?.version(),
      sha:
        process.env.TESTED_REF ??
        process.env.TESTED_SHA ??
        process.env.GITHUB_SHA,
      runId: process.env.GITHUB_RUN_ID,
      runnerOS: process.env.RUNNER_OS,
      imageOS: process.env.ImageOS,
      imageVersion: process.env.ImageVersion,
      inputMethods: info.project.metadata.inputMethods ?? [
        'pointer',
        'keyboard',
      ],
      result:
        'Step assertions completed; full test conclusion is retained in JUnit/JSON reports',
      artifact: path,
      visualComparison:
        'Textual specification assertions; no pixel identity claim.',
    }),
    contentType: 'application/json',
  });
}

export async function signInMaintainer(page: Page) {
  checkProtectedState();
  await respectAdmission();
  await page.goto('/');
  await expect(
    page.getByRole('button', { name: 'Sign out', exact: true }),
  ).toBeVisible();
  const response = await page.request.post('/graphql', {
    headers: {
      'X-Spoolin-Client': 'web',
      Origin: new URL(page.url()).origin,
    },
    data: { query: 'query { session { authenticated role } }' },
  });
  expect(response.status(), 'Maintainer session read HTTP status').toBe(200);
  expect(await response.json()).toEqual({
    data: { session: { authenticated: true, role: 'MAINTAINER' } },
  });
}

export function uniqueName(kind: string) {
  return `A Remote ${kind} ${randomUUID().slice(0, 8)}`;
}

const backupRejection =
  'Changes are not backed up yet. Retry after the original draft save is confirmed.';
const publicDraftCodes = new Set([
  'FIELD_INVALID',
  'NOT_FOUND',
  'STALE_VERSION',
  'POLICY_CHANGED',
  'DRAFT_QUOTA_EXCEEDED',
  'QUOTA_EXCEEDED',
  'UNAVAILABLE',
  'FORBIDDEN',
  'UNAUTHENTICATED',
  'IDEMPOTENCY_CONFLICT',
  'RATE_LIMITED',
  'RPC_DATA_LOSS',
  'VALIDATION_FAILED',
  'INTERNAL_ERROR',
]);
const publicOperationStates = new Set([
  'PENDING',
  'SUCCEEDED',
  'FAILED',
  'EXPIRED',
]);

type DraftObservation = {
  field: 'saveDraft' | 'operation';
  status: number;
  codes: string[];
  state?: string;
};

function graphqlQuery(response: Response): string {
  if (new URL(response.url()).pathname !== '/graphql') return '';
  try {
    const query = response.request().postDataJSON()?.query;
    return typeof query === 'string' ? query : '';
  } catch {
    return '';
  }
}

async function observeDraftResponse(
  response: Response,
  field: DraftObservation['field'],
): Promise<DraftObservation> {
  const observation: DraftObservation = {
    field,
    status: response.status(),
    codes: [],
  };
  try {
    const body = await response.json();
    const result = body.data?.[field];
    const operation = field === 'saveDraft' ? result?.operation : result;
    const codes: unknown[] = [result?.problem?.code, operation?.problem?.code];
    if (Array.isArray(body.errors)) {
      codes.push(
        ...body.errors.map(
          (error: { extensions?: { code?: unknown } }) =>
            error.extensions?.code,
        ),
      );
    }
    observation.codes = [
      ...new Set(
        codes
          .filter((code) => code != null)
          .map((code) =>
            typeof code === 'string' && publicDraftCodes.has(code)
              ? code
              : 'UNCLASSIFIED',
          ),
      ),
    ];
    if (publicOperationStates.has(operation?.state))
      observation.state = operation.state;
  } catch {
    observation.codes = ['UNREADABLE_RESPONSE'];
  }
  return observation;
}

async function saveCreated(page: Page, mutation: string, button: string) {
  // Keep only bounded public codes/statuses: never retain variables, bodies, IDs or credentials.
  const observations: Promise<DraftObservation>[] = [];
  const observe = (response: Response) => {
    if (observations.length >= 8) return;
    const query = graphqlQuery(response);
    if (/\bsaveDraft\s*\(/.test(query)) {
      observations.push(observeDraftResponse(response, 'saveDraft'));
    } else if (query.includes('query RecoverDraftOperation')) {
      observations.push(observeDraftResponse(response, 'operation'));
    }
  };
  page.on('response', observe);
  try {
    const responsePromise = page.waitForResponse((response) =>
      graphqlQuery(response).includes(`mutation ${mutation}`),
    );
    const rejected = page
      .getByRole('alert')
      .filter({ hasText: backupRejection });
    const outcome = Promise.race([
      responsePromise.then((response) => ({ response })),
      rejected
        .waitFor({ state: 'visible', timeout: 0 })
        .then(() => ({ response: null })),
    ]);
    await page.getByRole('button', { name: button, exact: true }).click();
    const { response } = await outcome;
    if (!response) {
      throw new Error(
        `Create submission rejected by required draft backup gate: ${JSON.stringify(await Promise.all(observations))}`,
      );
    }
    expect(response.ok(), `Create HTTP status ${response.status()}`).toBe(true);
    const body = await response.json();
    expect(body.errors).toBeUndefined();
    const operation =
      body.data[mutation === 'CreateMovie' ? 'createMovie' : 'createPerson']
        .operation;
    expect(operation.state).toBe('SUCCEEDED');
    expect(operation.resultId).toMatch(/^[a-f0-9-]{36}$/i);
    return String(operation.resultId);
  } finally {
    page.off('response', observe);
  }
}

export async function createMovie(page: Page, title = uniqueName('Movie')) {
  await respectAdmission(45, 20);
  await page.goto('/manage/movies');
  await page.getByRole('link', { name: 'Create movie', exact: true }).click();
  await page.getByLabel('Title · Required', { exact: true }).fill(title);
  await page
    .getByLabel('Synopsis · Required', { exact: true })
    .fill('A remote acceptance movie with persisted metadata.');
  await page.getByLabel('Release year · Required').fill('2024');
  await page.getByText('Choose genres', { exact: true }).click();
  await page.locator('#movie-genres input[type="checkbox"]').first().check();
  const id = await saveCreated(page, 'CreateMovie', 'Create movie');
  await expect(page.getByText('Movie saved', { exact: true })).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Create movie', exact: true }),
  ).toBeDisabled();
  await page
    .getByRole('link', { name: 'Edit saved movie', exact: true })
    .click();
  await expect(page).toHaveURL(new RegExp(`/manage/movies/${id}$`));
  await expect(
    page.getByLabel('Title · Required', { exact: true }),
  ).toHaveValue(title);
  return { id, title };
}

export async function createPerson(page: Page, name = uniqueName('Person')) {
  await respectAdmission(45, 20);
  await page.goto('/manage/people');
  await page.getByRole('link', { name: 'Create person', exact: true }).click();
  await page.getByLabel('Professional name · Required').fill(name);
  await page
    .getByLabel('Biography · Optional')
    .fill('Remote acceptance biography.');
  const id = await saveCreated(page, 'CreatePerson', 'Save person');
  await expect(page.getByText('Person saved', { exact: true })).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Save person', exact: true }),
  ).toBeDisabled();
  await page
    .getByRole('link', { name: 'Edit saved person', exact: true })
    .click();
  await expect(page).toHaveURL(new RegExp(`/manage/people/${id}$`));
  await expect(page.getByLabel('Professional name · Required')).toHaveValue(
    name,
  );
  return { id, name };
}

export async function readApi(
  page: Page,
  query: string,
  variables: Record<string, unknown> = {},
) {
  await respectAdmission(5, 0);
  const headers = {
    'X-Spoolin-Client': 'web',
    Origin: new URL(page.url()).origin,
  };
  const bootstrap = await page.request.post('/graphql', {
    headers,
    data: { query: 'query { session { csrfToken } }' },
  });
  expect(bootstrap.ok()).toBe(true);
  const token = (await bootstrap.json()).data.session.csrfToken;
  expect(typeof token).toBe('string');
  const response = await page.request.post('/graphql', {
    headers: { ...headers, 'X-CSRF-Token': token },
    data: { query, variables },
  });
  expect(response.ok()).toBe(true);
  const body = await response.json();
  expect(body.errors).toBeUndefined();
  return body.data;
}
