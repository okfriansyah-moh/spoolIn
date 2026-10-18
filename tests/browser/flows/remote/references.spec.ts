import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { test, expect, capture } from './helpers';
import { designCoverage, designSourceHash, fullStateInventory } from './state-mapping';

for (const screen of designCoverage.screens) {
  test(`Approved reference only: ${screen.id} states render independently of the application`, async ({ page }, info) => {
    const source = resolve('../../docs/design-system', screen.file);
    const sourceHash = createHash('sha256').update(readFileSync(source)).digest('hex');
    const theme = String(info.project.use.colorScheme ?? 'dark');
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
      });
    }
    await info.attach('reference-source-and-obligations', { body: JSON.stringify({ sourceHash, designSourceHash,
      sha: process.env.TESTED_REF ?? process.env.GITHUB_SHA, scope: 'Approved static reference only; fixture interactions do not prove production acceptance.',
      fonts: await page.locator('body').evaluate((body) => getComputedStyle(body).fontFamily),
      comparison: 'Reference fixture data and review chrome differ from production; full-page pixel equivalence is not established.',
      states: fullStateInventory().filter((state) => state.screen === screen.id) }), contentType: 'application/json' });
  });
}
