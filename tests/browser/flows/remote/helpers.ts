import { readFileSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import { test as base, expect, type Page, type TestInfo } from '@playwright/test';

export const test = base.extend({});
export { expect };

test.afterEach(async ({ page }, info) => {
  if (page.isClosed() || info.status === info.expectedStatus) return;
  await page.locator('input[type="password"]').fill('').catch(() => {});
  if (await page.locator('input[type="password"]').count() === 0) {
    await capture(page, info, info.title.match(/ATDD-\d+/g) ?? [], 'failure');
  }
});

export async function capture(page: Page, info: TestInfo, cases: string[], step: string) {
  expect(await page.locator('input[type="password"]').count()).toBe(0);
  const path = info.outputPath(`${step}.png`);
  await page.screenshot({ path, fullPage: true });
  await info.attach(step, { path, contentType: 'image/png' });
  await info.attach(`${step}-manifest`, {
    body: JSON.stringify({ cases, step, route: new URL(page.url()).pathname,
      theme: await page.locator('html').getAttribute('data-theme'), viewport: page.viewportSize(),
      browser: info.project.use.browserName, browserVersion: page.context().browser()?.version(),
      sha: process.env.TESTED_SHA ?? process.env.GITHUB_SHA, runId: process.env.GITHUB_RUN_ID,
      runnerOS: process.env.RUNNER_OS, result: info.status ?? 'assertions-completed',
      artifact: path, visualComparison: 'Textual specification assertions; no pixel identity claim.' }),
    contentType: 'application/json',
  });
}

export async function signInMaintainer(page: Page) {
  const path = process.env.SPOOLIN_BROWSER_MAINTAINER_PASSWORD_FILE;
  if (!path) throw new Error('Protected remote Maintainer password file is required.');
  const password = readFileSync(path, 'utf8').trim();
  await page.goto('/sign-in');
  await page.getByLabel('Email', { exact: true }).fill('maintainer@example.invalid');
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Sign out', exact: true })).toBeVisible();
  await expect(page.locator('input[type="password"]')).toHaveCount(0);
}

export function uniqueName(kind: string) {
  return `A Remote ${kind} ${randomUUID().slice(0, 8)}`;
}

async function saveCreated(page: Page, mutation: string, button: string) {
  const responsePromise = page.waitForResponse((response) => {
    if (new URL(response.url()).pathname !== '/graphql') return false;
    return response.request().postDataJSON()?.query?.includes(`mutation ${mutation}`) === true;
  });
  await page.getByRole('button', { name: button, exact: true }).click();
  const response = await responsePromise;
  expect(response.ok()).toBe(true);
  const body = await response.json();
  expect(body.errors).toBeUndefined();
  const operation = body.data[mutation === 'CreateMovie' ? 'createMovie' : 'createPerson'].operation;
  expect(operation.state).toBe('SUCCEEDED');
  expect(operation.resultId).toMatch(/^[a-f0-9-]{36}$/i);
  return String(operation.resultId);
}

export async function createMovie(page: Page, title = uniqueName('Movie')) {
  await page.goto('/manage/movies');
  await page.getByRole('link', { name: 'Create movie', exact: true }).click();
  await page.getByLabel('Title · Required', { exact: true }).fill(title);
  await page.getByLabel('Synopsis · Required', { exact: true }).fill('A remote acceptance movie with persisted metadata.');
  await page.getByLabel('Release year · Required').fill('2024');
  await page.locator('#movie-genres input[type="checkbox"]').first().check();
  const id = await saveCreated(page, 'CreateMovie', 'Create movie');
  await expect(page.getByText('Movie saved', { exact: true })).toBeVisible();
  await page.goto(`/manage/movies/${id}`);
  await expect(page.getByLabel('Title · Required', { exact: true })).toHaveValue(title);
  return { id, title };
}

export async function createPerson(page: Page, name = uniqueName('Person')) {
  await page.goto('/manage/people');
  await page.getByRole('link', { name: 'Create person', exact: true }).click();
  await page.getByLabel('Professional name · Required').fill(name);
  await page.getByLabel('Biography · Optional').fill('Remote acceptance biography.');
  const id = await saveCreated(page, 'CreatePerson', 'Save person');
  await expect(page.getByText('Person saved', { exact: true })).toBeVisible();
  await page.goto(`/manage/people/${id}`);
  await expect(page.getByLabel('Professional name · Required')).toHaveValue(name);
  return { id, name };
}
