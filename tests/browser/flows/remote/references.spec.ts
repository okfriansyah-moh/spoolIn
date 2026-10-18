import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { type Page, type TestInfo } from '@playwright/test';
import { test, expect, capture } from './helpers';
import { designCoverage, designSourceHash, fullStateInventory } from './state-mapping';

const productionRepresentatives: Record<string, string> = {
  discovery: '/', management: '/manage/movies', people: '/people', audit: '/manage/audit', archives: '/manage/archives',
};

async function pixelDifference(page: Page, reference: Buffer, actual: Buffer) {
  return page.evaluate(async ({ expected, observed }) => {
    const decode = (data: string) => new Promise<HTMLImageElement>((done, fail) => {
      const image = new Image(); image.onload = () => done(image); image.onerror = fail; image.src = `data:image/png;base64,${data}`;
    });
    const [left, right] = await Promise.all([decode(expected), decode(observed)]);
    if (left.width !== right.width || left.height !== right.height) throw new Error('Pixel inputs require equivalent capture dimensions.');
    const canvas = document.createElement('canvas'); canvas.width = left.width; canvas.height = left.height;
    const context = canvas.getContext('2d');
    if (!context) throw new Error('Browser pixel comparison canvas unavailable.');
    context.drawImage(left, 0, 0); const a = context.getImageData(0, 0, canvas.width, canvas.height);
    context.clearRect(0, 0, canvas.width, canvas.height); context.drawImage(right, 0, 0);
    const b = context.getImageData(0, 0, canvas.width, canvas.height); let differingPixels = 0;
    for (let index = 0; index < a.data.length; index += 4) {
      const differs = [0, 1, 2, 3].some((offset) => a.data[index + offset] !== b.data[index + offset]);
      if (differs) differingPixels++;
      b.data[index] = differs ? 255 : 0; b.data[index + 1] = 0;
      b.data[index + 2] = differs ? 255 : 0; b.data[index + 3] = 255;
    }
    context.putImageData(b, 0, 0);
    return { width: canvas.width, height: canvas.height, differingPixels, totalPixels: canvas.width * canvas.height,
      diff: canvas.toDataURL('image/png').split(',')[1] ?? '' };
  }, { expected: reference.toString('base64'), observed: actual.toString('base64') });
}

async function compareRepresentative(page: Page, info: TestInfo, screen: string, reference: Buffer) {
  const route = productionRepresentatives[screen];
  if (!route) throw new Error('Representative production route missing.');
  await page.goto(route);
  await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible();
  await page.evaluate(() => document.fonts.ready);
  const actual = await page.screenshot();
  const comparison = await pixelDifference(page, reference, actual);
  await info.attach(`${screen}-reference-viewport`, { body: reference, contentType: 'image/png' });
  await info.attach(`${screen}-actual-viewport`, { body: actual, contentType: 'image/png' });
  await info.attach(`${screen}-diff-viewport`, { body: Buffer.from(comparison.diff, 'base64'), contentType: 'image/png' });
  await info.attach(`${screen}-pixel-result`, { body: JSON.stringify({ ...comparison, diff: undefined,
    exactMatchEligible: false, unexplainedPixels: comparison.differingPixels,
    reason: 'Approved fixture content and review chrome differ from production. Quantitative differences require inspection; no exact-match result is asserted.',
    result: 'Pending visual reconciliation', sha: process.env.TESTED_REF ?? process.env.GITHUB_SHA }), contentType: 'application/json' });
}

for (const screen of designCoverage.screens) {
  test(`Approved reference only: ${screen.id} states render independently of the application`, async ({ page }, info) => {
    const source = resolve('../../docs/design-system', screen.file);
    const sourceHash = createHash('sha256').update(readFileSync(source)).digest('hex');
    const theme = String(info.project.use.colorScheme ?? 'dark');
    let representative: Buffer | null = null;
    for (const state of screen.states) {
      await test.step(`Render approved ${screen.id}/${state} in ${theme}`, async () => {
        const reference = pathToFileURL(source);
        reference.searchParams.set('theme', theme);
        reference.hash = state;
        await page.goto(reference.href);
        await page.evaluate(() => document.fonts.ready);
        await expect(page.locator('html')).toHaveAttribute('data-design-target', theme);
        const active = page.locator('.screen:visible');
        await expect(active).toHaveCount(1);
        await expect(active).toHaveAttribute('id', state);
        await expect(active.getByRole('heading').first()).toBeVisible();
        expect(await page.locator('html').evaluate((root) => root.scrollWidth <= window.innerWidth)).toBe(true);
        await page.locator('input[type="password"]').evaluateAll((inputs) => inputs.forEach((input) => { (input as HTMLInputElement).value = ''; }));
        await capture(page, info, [], `approved-reference-${screen.id}-${state}`);
        if (state === screen.states[0] && productionRepresentatives[screen.id]) representative = await page.screenshot();
      });
    }
    if (representative) await compareRepresentative(page, info, screen.id, representative);
    await info.attach('reference-source-and-obligations', { body: JSON.stringify({ sourceHash, designSourceHash,
      sha: process.env.TESTED_REF ?? process.env.GITHUB_SHA, scope: 'Approved static reference only; fixture interactions do not prove production acceptance.',
      fonts: await page.locator('body').evaluate((body) => getComputedStyle(body).fontFamily),
      comparison: 'Reference fixture data and review chrome differ from production; full-page pixel equivalence is not established.',
      states: fullStateInventory().filter((state) => state.screen === screen.id) }), contentType: 'application/json' });
  });
}
