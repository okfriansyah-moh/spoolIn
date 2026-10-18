import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { createHash } from 'node:crypto';
import { test, expect, type Page, type Response, type TestInfo } from '@playwright/test';

type Panel = { id: number; title: string; targets: { expr: string }[] };
type Frame = { schema?: { fields?: { type?: string }[] }; data?: { values?: unknown[][] } };
type QueryResult = { error?: string; frames?: Frame[] };
type Observation = { httpStatus: number; frameCount: number; finiteSamples: number; error: boolean };
const dashboardFile = resolve('../../ops/monitoring/grafana/spoolin.json');
const dashboard = JSON.parse(readFileSync(dashboardFile, 'utf8')) as { title: string; uid: string; panels: Panel[] };
const authenticated = new WeakSet<Page>();

function finiteSamples(frames: Frame[]): number {
  return frames.reduce((total, frame) => total + (frame.schema?.fields ?? []).reduce((sum, field, index) => {
    if (field.type !== 'number') return sum;
    return sum + (frame.data?.values?.[index] ?? []).filter((value) => typeof value === 'number' && Number.isFinite(value)).length;
  }, 0), 0);
}

async function observeQueries(response: Response, observations: Map<string, Observation>) {
  if (new URL(response.url()).pathname !== '/api/ds/query') return;
  const request = response.request().postDataJSON();
  const body = await response.json().catch(() => null);
  for (const query of request?.queries ?? []) {
    if (typeof query.expr !== 'string') continue;
    const result = body?.results?.[query.refId] as QueryResult | undefined;
    const frames = result?.frames ?? [];
    observations.set(query.expr, { httpStatus: response.status(), frameCount: frames.length,
      finiteSamples: finiteSamples(frames), error: !response.ok() || !!result?.error });
  }
}

async function signInOperator(page: Page) {
  const file = process.env.GRAFANA_ADMIN_PASSWORD_CLIENT_FILE;
  if (!file) throw new Error('Protected runner-owned Grafana credential file is required.');
  const attributes = statSync(file);
  if (!attributes.isFile() || attributes.size > 4096 || (attributes.mode & 0o077) !== 0 || attributes.uid !== process.getuid?.()) {
    throw new Error('Grafana credential file must be runner-owned with private permissions.');
  }
  await page.goto('/login');
  // Versioned upstream selectors: grafana/grafana v13.2.3 packages/grafana-e2e-selectors/src/selectors/pages.ts.
  await page.getByTestId('data-testid Username input field').fill('operator');
  await page.getByTestId('data-testid Password input field').fill(readFileSync(file, 'utf8').trim())
    .catch(() => { throw new Error('Protected operator credential entry failed.'); });
  await page.getByTestId('data-testid Login button').click();
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/);
  await expect(page.locator('input[type="password"]')).toHaveCount(0);
  authenticated.add(page);
}

async function navigateDashboard(page: Page) {
  const dashboards = page.getByRole('link', { name: 'Dashboards', exact: true }).first();
  if (!await dashboards.isVisible()) await page.getByRole('button', { name: /open menu|toggle menu/i }).click();
  await dashboards.click();
  const search = page.getByTestId('data-testid Browse dashboards search input');
  await expect(search).toBeVisible();
  await search.fill(dashboard.title);
  await page.getByRole('link', { name: dashboard.title, exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`/d/${dashboard.uid}/`));
  await expect(page.getByText(dashboard.title, { exact: true }).first()).toBeVisible();
}

async function captureDashboard(page: Page, info: TestInfo, step: string) {
  if (!authenticated.has(page) || await page.locator('input[type="password"]').count()) return;
  const path = info.outputPath(`${step}.png`);
  await page.screenshot({ path });
  await info.attach(step, { path, contentType: 'image/png' });
  await info.attach(`${step}-manifest`, { body: JSON.stringify({ cases: ['ATDD-206'], step,
    route: new URL(page.url()).pathname, viewport: page.viewportSize(), browser: 'chromium',
    browserVersion: page.context().browser()?.version(), sha: process.env.TESTED_REF ?? process.env.GITHUB_SHA,
    runId: process.env.GITHUB_RUN_ID, runAttempt: process.env.GITHUB_RUN_ATTEMPT,
    runnerOS: process.env.RUNNER_OS, imageOS: process.env.ImageOS, imageVersion: process.env.ImageVersion,
    inputMethods: ['pointer', 'keyboard'], fontFamily: await page.locator('body').evaluate((body) => getComputedStyle(body).fontFamily),
    dashboardSourceSha256: createHash('sha256').update(readFileSync(dashboardFile)).digest('hex'),
    result: 'Rendered step with assertions; complete test result remains in JUnit/JSON.' }), contentType: 'application/json' });
}

test.afterEach(async ({ page }, info) => {
  if (info.status !== info.expectedStatus) await captureDashboard(page, info, 'dashboard-failure');
});

async function assertPanel(page: Page, info: TestInfo, panel: Panel, observations: Map<string, Observation>) {
  // Versioned upstream container/heading selectors: grafana/grafana v13.2.3 e2e-selectors/components.ts.
  const region = page.getByLabel(`${panel.title} panel`, { exact: true });
  await region.scrollIntoViewIfNeeded();
  await expect(region).toBeVisible();
  await expect(page.getByTestId(`data-testid Panel header ${panel.title}`)).toBeVisible();
  for (const target of panel.targets) {
    await expect.poll(() => observations.get(target.expr)?.finiteSamples ?? 0,
      { timeout: 90000, message: `Actual rendered datasource samples are required for ${panel.title}` }).toBeGreaterThan(0);
    expect(observations.get(target.expr)).toMatchObject({ httpStatus: 200, error: false });
  }
  await expect(region.getByText(/^(No data|Unauthorized|Access denied|Data source error)$/i)).toHaveCount(0);
  await expect(region.getByTestId('data-testid Panel data error message')).toHaveCount(0);
  const canvas = region.locator('canvas').first();
  await expect(canvas).toBeVisible();
  const bounds = await canvas.boundingBox();
  expect(bounds?.width ?? 0).toBeGreaterThan(0);
  expect(bounds?.height ?? 0).toBeGreaterThan(0);
  await captureDashboard(page, info, `dashboard-panel-${panel.id}`);
}

test('ATDD-206 partial: protected operator dashboard renders every provisioned panel with actual datasource samples', async ({ page }, info) => {
  const observations = new Map<string, Observation>();
  page.on('response', (response) => { void observeQueries(response, observations); });
  await signInOperator(page);
  await navigateDashboard(page);
  for (const panel of dashboard.panels) await assertPanel(page, info, panel, observations);
  await page.keyboard.press('Tab');
  const focus = page.locator(':focus-visible');
  await expect(focus).toHaveCount(1);
  const focusStyle = await focus.evaluate((element) => { const style = getComputedStyle(element);
    return { outline: style.outlineStyle, width: style.outlineWidth, shadow: style.boxShadow }; });
  expect((focusStyle.outline !== 'none' && Number.parseFloat(focusStyle.width) > 0) || focusStyle.shadow !== 'none').toBe(true);
  await captureDashboard(page, info, 'dashboard-keyboard-focus');
  await info.attach('rendered-dashboard-data', { body: JSON.stringify({ dashboardUid: dashboard.uid,
    observations: dashboard.panels.map((panel) => ({ id: panel.id, title: panel.title,
      queries: panel.targets.map((target) => ({ expression: target.expr, ...observations.get(target.expr) })) })),
    scope: 'Actual authenticated browser panels and their own datasource responses; no API health result substitutes for rendering.',
    remaining: 'Complete metric fault matrices, actual assistive-reader speech and provider-delivery obligations require their separate proof.' }), contentType: 'application/json' });
});
