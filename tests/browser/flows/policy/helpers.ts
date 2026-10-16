import { expect, type Page, type TestInfo } from '@playwright/test';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve, extname } from 'node:path';
import { createHash, randomUUID } from 'node:crypto';

const evidence = resolve(
  '../../.ai/evidence/t009-policy/visual',
  `run-${randomUUID().replaceAll('-', '')}`,
);
export const draft =
  'After fifteen years away, a railway archivist returns to her coastal hometown to record its last overnight train.';
export const headings: Record<string, string> = {
  'text-boundaries': 'Names and text stay intact',
  'optional-clearing': 'Optional movie information',
  'policy-changed': 'Review the current rule',
  'policy-unavailable': 'Editing is temporarily unavailable',
};
export async function frame(
  page: Page,
  info: TestInfo,
  state: string,
  phase: string,
) {
  await mkdir(evidence, { recursive: true });
  await page.screenshot({
    path: resolve(evidence, `${info.project.name}-${state}-${phase}.png`),
    fullPage: true,
  });
  await page.waitForTimeout(700);
}
export async function noOverflow(page: Page) {
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
}
export async function themeRetains(page: Page, field: string, value: string) {
  const input = page.locator(field);
  const toggle = page.getByRole('button', { name: /Switch to .* theme/ });
  const url = page.url();
  await toggle.focus();
  await page.keyboard.press('Enter');
  await expect(toggle).toBeFocused();
  await expect(input).toHaveValue(value);
  expect(page.url()).toBe(url);
  await page.keyboard.press('Enter');
  await expect(input).toHaveValue(value);
}
export async function checkCorrection(page: Page, id: string) {
  const alert = page.getByRole('alert');
  await expect(alert).toBeFocused();
  await expect(page.locator(id)).toHaveAttribute('aria-invalid', 'true');
  const descriptions = await page.locator(id).getAttribute('aria-describedby');
  expect(descriptions).toContain(`${id.slice(1)}-help`);
  expect(descriptions).toContain(`${id.slice(1)}-error`);
  await alert.getByRole('link').focus();
  await page.keyboard.press('Enter');
  await expect(page.locator(id)).toBeFocused();
}
export async function recordEnvironment(
  page: Page,
  info: TestInfo,
  state: string,
) {
  const fontSource = await readFile(
    resolve('../../docs/design-system/mockup/tokens.css'),
  );
  const metadata = await page.evaluate(() => ({
    viewport: { width: innerWidth, height: innerHeight },
    cssZoom: getComputedStyle(document.body).zoom,
    theme: document.documentElement.dataset.theme,
    font: getComputedStyle(document.body).fontFamily,
    reducedMotion: matchMedia('(prefers-reduced-motion: reduce)').matches,
    touchPoints: navigator.maxTouchPoints,
  }));
  await writeFile(
    resolve(evidence, `${info.project.name}-${state}-environment.json`),
    JSON.stringify(
      {
        ...metadata,
        project: info.project.name,
        browser: page.context().browser()?.version(),
        trigger: `Controlled /policy?state=${state}`,
        capturedAt: new Date().toISOString(),
        fontSource: 'docs/design-system/mockup/tokens.css',
        fontSourceHash: createHash('sha256').update(fontSource).digest('hex'),
        boundary:
          'Rendered policy components with controlled data; no business save or transport claim',
      },
      null,
      2,
    ),
  );
}
export async function reference(page: Page, info: TestInfo, state: string) {
  const root = resolve('../../docs/design-system/mockup');
  await page.route('**/__policy_reference/**', async (route) => {
    const name =
      new URL(route.request().url()).pathname.split(
        '/__policy_reference/',
      )[1] ?? '';
    const path = resolve(root, name);
    if (!path.startsWith(`${root}/`)) return route.abort();
    const types: Record<string, string> = {
      '.html': 'text/html',
      '.css': 'text/css',
      '.js': 'application/javascript',
      '.svg': 'image/svg+xml',
      '.png': 'image/png',
      '.woff2': 'font/woff2',
    };
    return route.fulfill({
      body: await readFile(path),
      contentType: types[extname(path)] ?? 'application/octet-stream',
    });
  });
  await page.goto(`/__policy_reference/policies.html#${state}`);
  await page.locator('html').evaluate((element, theme) => {
    (element as HTMLElement).dataset.designTarget = theme;
    (element as HTMLElement).dataset.theme = theme;
  }, info.project.use.colorScheme ?? 'dark');
  await expect(page.locator(`#${state}`)).toBeVisible();
  await page.locator(`#${state}`).screenshot({
    path: resolve(evidence, `${info.project.name}-${state}-reference.png`),
  });
}
