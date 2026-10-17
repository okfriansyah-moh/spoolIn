# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-unavailable-title: non-existent UUID shows unavailable message with catalog link
- Location: flows/discovery/discovery.spec.ts:204:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('heading', { name: /unavailable/i })
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" getByRole('heading', { name: /unavailable/i }) with timeout 5000ms
  - waiting for getByRole('heading', { name: /unavailable/i })

```

```yaml
- link "Skip to content":
  - /url: "#main-content"
- banner:
  - link "SpoolIn home":
    - /url: /
    - text: SpoolIn
  - navigation "Main navigation":
    - link "Catalog":
      - /url: /
    - link "People":
      - /url: /people
  - button "Switch to dark theme": Switch theme
- main:
  - heading "404" [level=1]
  - paragraph: Not Found
```

# Test source

```ts
  113 |   context,
  114 | }) => {
  115 |   await freshSession(context);
  116 |   await goToDiscovery(page);
  117 | 
  118 |   const monthSelect = page.getByLabel(/month/i);
  119 |   await monthSelect.waitFor({ state: 'visible' });
  120 | 
  121 |   // Without year: month selector must be disabled (AC-014-C01)
  122 |   await expect(monthSelect).toBeDisabled();
  123 | 
  124 |   // Select a year first
  125 |   const yearSelect = page.getByLabel(/release year/i);
  126 |   await yearSelect.selectOption({ index: 1 });
  127 | 
  128 |   // Month is now enabled
  129 |   await expect(monthSelect).not.toBeDisabled();
  130 | 
  131 |   // Clear year → month must be disabled again
  132 |   await yearSelect.selectOption('');
  133 |   await expect(monthSelect).toBeDisabled();
  134 | });
  135 | 
  136 | // ---------------------------------------------------------------------------
  137 | // AC-007: clearing text preserves kind filter
  138 | // ---------------------------------------------------------------------------
  139 | test('T013-clear-text: clearing search text preserves type filter', async ({
  140 |   page,
  141 |   context,
  142 | }) => {
  143 |   await freshSession(context);
  144 |   await goToDiscovery(page);
  145 | 
  146 |   const kindSelect = page.getByLabel(/title type/i);
  147 |   const searchInput = page.getByLabel(/search titles/i);
  148 |   const clearBtn = page.getByRole('button', { name: /clear search text/i });
  149 | 
  150 |   await kindSelect.waitFor({ state: 'visible' });
  151 | 
  152 |   // Set kind filter
  153 |   await kindSelect.selectOption('MOVIE');
  154 | 
  155 |   // Enter text
  156 |   await searchInput.fill('Light');
  157 |   await page.waitForTimeout(350);
  158 | 
  159 |   // Clear text
  160 |   await clearBtn.click();
  161 | 
  162 |   // Kind filter must remain (AC-007-C01)
  163 |   await expect(kindSelect).toHaveValue('MOVIE');
  164 | 
  165 |   // Text field is cleared
  166 |   await expect(searchInput).toHaveValue('');
  167 | });
  168 | 
  169 | // ---------------------------------------------------------------------------
  170 | // ATDD-256-T1-R03 · catalog-empty: successful empty catalog shows distinct state
  171 | // ---------------------------------------------------------------------------
  172 | test('T013-empty-catalog: empty catalog state shows browse-again; does not claim failure', async ({
  173 |   page,
  174 |   context,
  175 | }) => {
  176 |   await freshSession(context);
  177 |   await goToDiscovery(page);
  178 | 
  179 |   // The page must not show a failure alert for the initial load
  180 |   // (catalog-empty is a distinct success state, not failure)
  181 |   const failureAlert = page.locator('[role="alert"]').filter({ hasText: /could not load/i });
  182 |   // Allow page to settle
  183 |   await page.waitForTimeout(2000);
  184 |   // If no real backend: page may show loading or empty – it must not show failure text
  185 |   // that's identical to the error state (AC-002-C01)
  186 |   const loadingText = page.getByText('Finding titles', { exact: false });
  187 |   const emptyText = page.getByText('catalog is empty', { exact: false });
  188 |   const catalogGrid = page.locator('.title-grid');
  189 | 
  190 |   const [isLoading, isEmpty, hasGrid, hasFailure] = await Promise.all([
  191 |     loadingText.isVisible(),
  192 |     emptyText.isVisible(),
  193 |     catalogGrid.isVisible().catch(() => false),
  194 |     failureAlert.isVisible().catch(() => false),
  195 |   ]);
  196 | 
  197 |   // One of loading, empty or grid must be visible
  198 |   expect(isLoading || isEmpty || hasGrid || !hasFailure).toBe(true);
  199 | });
  200 | 
  201 | // ---------------------------------------------------------------------------
  202 | // ATDD-257-T1-R03 · unavailable title: safe message, no archived data
  203 | // ---------------------------------------------------------------------------
  204 | test('T013-unavailable-title: non-existent UUID shows unavailable message with catalog link', async ({
  205 |   page,
  206 |   context,
  207 | }) => {
  208 |   await freshSession(context);
  209 |   await goToTitle(page, '00000000-0000-0000-0000-000000000001');
  210 | 
  211 |   await expect(
  212 |     page.getByRole('heading', { name: /unavailable/i }),
> 213 |   ).toBeVisible({ timeout: 5000 });
      |     ^ Error: expect(locator).toBeVisible() failed
  214 | 
  215 |   // Must provide catalog navigation (AC-005-C01)
  216 |   await expect(page.getByRole('link', { name: /return to catalog/i })).toBeVisible();
  217 | 
  218 |   // No archived data exposed (AC-005-C01)
  219 |   const body = await page.locator('body').textContent();
  220 |   expect(body).not.toContain('ARCHIVED');
  221 | });
  222 | 
  223 | // ---------------------------------------------------------------------------
  224 | // ATDD-257 / AC-001-C02: title detail must not show episode-tracking controls
  225 | // ---------------------------------------------------------------------------
  226 | test('T013-no-episode-controls: title detail has no episode tracking controls', async ({
  227 |   page,
  228 |   context,
  229 | }) => {
  230 |   await freshSession(context);
  231 |   await goToTitle(page, '00000000-0000-0000-0000-000000000002');
  232 | 
  233 |   // Even if unavailable, there must be no episode controls
  234 |   const episodeControls = page.getByRole('button', { name: /episode/i });
  235 |   await expect(episodeControls).toBeHidden();
  236 | });
  237 | 
  238 | // ---------------------------------------------------------------------------
  239 | // People listing: structure and navigation
  240 | // ---------------------------------------------------------------------------
  241 | test('T013-people-listing: people page has search input and navigation', async ({
  242 |   page,
  243 |   context,
  244 | }) => {
  245 |   await freshSession(context);
  246 |   await goToPeople(page);
  247 | 
  248 |   await expect(page.getByRole('heading', { level: 1 })).toContainText('People');
  249 |   await expect(page.getByLabel(/search people/i)).toBeVisible();
  250 |   await expect(page.getByRole('link', { name: 'Catalog', exact: true })).toBeVisible();
  251 | });
  252 | 
  253 | // ---------------------------------------------------------------------------
  254 | // Person detail: unavailable shows safe message
  255 | // ---------------------------------------------------------------------------
  256 | test('T013-unavailable-person: non-existent person UUID shows unavailable message', async ({
  257 |   page,
  258 |   context,
  259 | }) => {
  260 |   await freshSession(context);
  261 |   await goToPerson(page, '00000000-0000-0000-0000-000000000001');
  262 | 
  263 |   await expect(
  264 |     page.getByRole('heading', { name: /unavailable/i }),
  265 |   ).toBeVisible({ timeout: 5000 });
  266 | 
  267 |   await expect(page.getByRole('link', { name: /return to people/i })).toBeVisible();
  268 | });
  269 | 
  270 | // ---------------------------------------------------------------------------
  271 | // Browser Back restores criteria (AC-009, ATDD-139)
  272 | // ---------------------------------------------------------------------------
  273 | test('T013-back-restores-criteria: browser Back restores filter state', async ({
  274 |   page,
  275 |   context,
  276 | }) => {
  277 |   await freshSession(context);
  278 |   await goToDiscovery(page);
  279 | 
  280 |   const kindSelect = page.getByLabel(/title type/i);
  281 |   await kindSelect.waitFor({ state: 'visible' });
  282 | 
  283 |   // Set a filter
  284 |   await kindSelect.selectOption('MOVIE');
  285 |   await page.waitForTimeout(350);
  286 | 
  287 |   // Navigate away
  288 |   await page.click('a:has-text("People")');
  289 |   await page.waitForURL(/\/people/);
  290 | 
  291 |   // Go back
  292 |   await page.goBack();
  293 |   await page.waitForURL(/^\//);
  294 |   await page.waitForLoadState('networkidle');
  295 | 
  296 |   // Kind filter should be restored from URL
  297 |   const restoredKind = await page.getByLabel(/title type/i).inputValue();
  298 |   expect(restoredKind).toBe('MOVIE');
  299 | });
  300 | 
```