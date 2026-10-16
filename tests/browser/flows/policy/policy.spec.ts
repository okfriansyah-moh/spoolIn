import { test, expect, type Page } from '@playwright/test';
import {
  frame,
  headings,
  draft,
  noOverflow,
  themeRetains,
  checkCorrection,
  recordEnvironment,
  reference,
} from './helpers';

async function textBoundary(page: Page) {
  const field = page.locator('#policy-name');
  await expect(page.getByRole('table')).toContainText('é / é');
  await expect(page.getByRole('table')).toContainText(
    '<script>alert(1)</script>',
  );
  await field.focus();
  await page.keyboard.press('Tab');
  await page.keyboard.press('Enter');
  await checkCorrection(page, '#policy-name');
  await expect(field).toHaveValue('V');
  await field.fill('  李 · O’Connor  ');
  await themeRetains(page, '#policy-name', '  李 · O’Connor  ');
  await page.getByRole('button', { name: 'Check name', exact: true }).click();
  await expect(
    page.getByRole('status').filter({ hasText: 'The name meets' }),
  ).toBeVisible();
  await expect(field).toHaveValue('  李 · O’Connor  ');
  await field.fill('🙂'.repeat(100));
  await page.getByRole('button', { name: 'Check name', exact: true }).click();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await field.fill('🙂'.repeat(101));
  await page.getByRole('button', { name: 'Check name', exact: true }).click();
  await checkCorrection(page, '#policy-name');
  await expect(field).toHaveValue('🙂'.repeat(101));
}
async function clearing(page: Page) {
  const field = page.locator('#policy-original');
  await expect(field).toHaveValue('Cahaya Terakhir');
  await page.getByRole('button', { name: 'Check field change' }).click();
  await expect(
    page
      .getByRole('status')
      .filter({ hasText: 'Original title stays unchanged.' }),
  ).toBeVisible();
  await field.fill('\u0085 \u00a0');
  await page.getByRole('button', { name: 'Check field change' }).click();
  await expect(
    page
      .getByRole('status')
      .filter({ hasText: 'Original title will be cleared.' }),
  ).toBeVisible();
  await field.fill('Maya / Echo');
  await themeRetains(page, '#policy-original', 'Maya / Echo');
  await page.getByRole('button', { name: 'Check field change' }).click();
  await expect(
    page
      .getByRole('status')
      .filter({ hasText: 'Original title will become Maya / Echo.' }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Leave unchanged' }).click();
  await expect(field).toHaveValue('Cahaya Terakhir');
  await expect(
    page
      .getByRole('status')
      .filter({ hasText: 'Original title stays unchanged.' }),
  ).toBeVisible();
}
async function policyChanged(page: Page) {
  const field = page.locator('#policy-draft');
  const initialContext = await page.getByRole('status').innerText();
  await expect(field).toHaveValue(draft);
  await expect(page.locator('#policy-draft-help')).toContainText(
    '100 Unicode code points',
  );
  await page.getByRole('button', { name: 'Review draft', exact: true }).click();
  await checkCorrection(page, '#policy-draft');
  await expect(field).toHaveValue(draft);
  await themeRetains(page, '#policy-draft', draft);
  await field.fill('A first line.\nA second line · 李');
  await page.getByRole('button', { name: 'Review draft', exact: true }).click();
  await expect(
    page
      .getByRole('status')
      .filter({ hasText: 'The revised draft meets the current rule.' }),
  ).toBeVisible();
  await expect(field).toHaveValue('A first line.\nA second line · 李');
  await expect(page.locator('main')).not.toContainText(
    'Synopsis now exceeds the effective maximum',
  );
  expect(initialContext).toContain('The synopsis limit has changed');
  await expect(
    page
      .getByRole('status')
      .filter({ hasText: 'The synopsis limit has changed' }),
  ).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await expect(page.locator('#policy-draft-help')).toContainText(
    '100 Unicode code points',
  );
  await expect(page.locator('main')).toContainText(
    '31 Unicode code points retained.',
  );
}
async function unavailable(page: Page) {
  await expect(page.locator('#policy-retained')).toHaveValue(draft);
  await expect(page.locator('#policy-retained')).toBeDisabled();
  await expect(
    page.getByRole('button', { name: 'Review draft', exact: true }),
  ).toBeDisabled();
  await themeRetains(page, '#policy-retained', draft);
  await expect(
    page.getByRole('link', { name: 'Read the movies' }),
  ).toHaveAttribute('href', '/');
  await expect(page.locator('main')).not.toContainText(
    /KEY_FILE|jdbc:|stack trace|password=/,
  );
  await page.getByRole('link', { name: 'Read the movies' }).click();
  await expect(
    page.getByRole('heading', { name: 'One component set.' }),
  ).toBeVisible();
  await page.goBack();
  await expect(page.locator('#policy-retained')).toHaveValue(draft);
}
const actions: Record<string, (page: Page) => Promise<void>> = {
  'text-boundaries': textBoundary,
  'optional-clearing': clearing,
  'policy-changed': policyChanged,
  'policy-unavailable': unavailable,
};
for (const [index, state] of Object.keys(headings).entries()) {
  test(`ATDD-267-T1-R0${index + 1} ${state}: controlled policy component`, async ({
    page,
  }, info) => {
    await page.goto(`/policy?state=${state}`);
    await expect(
      page.getByRole('heading', { name: headings[state], exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole('link', { name: /SpoolIn/ }).first(),
    ).toBeVisible();
    await noOverflow(page);
    await frame(page, info, state, 'initial');
    await recordEnvironment(page, info, state);
    const action = actions[state];
    if (!action) throw new Error('Missing policy interaction');
    await action(page);
    await noOverflow(page);
    await frame(page, info, state, 'result');
    const destination =
      state === 'text-boundaries' ? 'optional-clearing' : 'text-boundaries';
    await page.goto(`/policy?state=${destination}`);
    await expect(
      page.getByRole('heading', { name: headings[destination], exact: true }),
    ).toBeVisible();
    await page.goBack();
    await expect(
      page.getByRole('heading', { name: headings[state], exact: true }),
    ).toBeVisible();
    await noOverflow(page);
    await reference(page, info, state);
  });
}

test('C03 raised person and title minima change one-code-point guidance', async ({
  page,
}, info) => {
  await page.goto('/policy?state=text-boundaries&minimum=raised');
  const table = page.getByRole('table');
  await expect(table).toContainText('person 2–100');
  await expect(table).toContainText('Use 2 to 100 Unicode code points.');
  await expect(table).toContainText('Use 2 to 255 Unicode code points.');
  await expect(table).not.toContainText('Valid professional name');
  await expect(table).not.toContainText(
    'Accepted under the current title rule',
  );
  await expect(table).toContainText('é / é');
  await expect(table).toContainText('O’Connor · 李 · Jean-Luc');
  await noOverflow(page);
  await frame(page, info, 'text-boundaries', 'raised-minima');
  await recordEnvironment(page, info, 'raised-minima');
});
