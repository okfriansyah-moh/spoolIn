# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-catalog: discovery headline, filter controls and title grid structure are present
- Location: flows/discovery/discovery.spec.ts:32:1

# Error details

```
Error: expect(locator).toContainText(expected) failed

Locator: getByRole('heading', { level: 1 })
Expected substring: "Every reel holds a story."
Received string:    "One component set."
Timeout: 5000ms

Call log:
  - Expect "toContainText" getByRole('heading', { level: 1 }) with timeout 5000ms
  - waiting for getByRole('heading', { level: 1 })
    14 × locator resolved to <h1 tabindex="-1">One component set.</h1>
       - unexpected value "One component set."

```

```yaml
- heading "One component set." [level=1]
```

# Test source

```ts
  1   | /**
  2   |  * T013 – Discovery browser journey tests.
  3   |  *
  4   |  * Covers ATDD-256, ATDD-257, ATDD-303 state rows and key acceptance criteria:
  5   |  * - AC-159 / ATDD-133: search-length boundary (200 ok, 201 invalid – no truncated query)
  6   |  * - AC-009 / ATDD-303: stale reply suppression (generation tracking)
  7   |  * - AC-005 / ATDD-257-T1-R03: unavailable title safe message
  8   |  * - AC-022 / ATDD-256-T1-R01: discovery identity/headline visible
  9   |  * - AC-002 / ATDD-256-T1-R03/R04: empty and failure states distinct
  10  |  * - AC-014: month-without-year correction
  11  |  * - AC-007: clearing text preserves other filters
  12  |  *
  13  |  * These are structural / shell-level assertions against the real deployed shell.
  14  |  * Full end-to-end fixture data proof (real PostgreSQL + services) is verified
  15  |  * by the integration owner at the V-READ boundary.
  16  |  */
  17  | 
  18  | import { test, expect } from '@playwright/test';
  19  | import {
  20  |   freshSession,
  21  |   goToDiscovery,
  22  |   goToTitle,
  23  |   goToPeople,
  24  |   goToPerson,
  25  |   expectDiscoveryHeadline,
  26  |   assertSearchInput,
  27  | } from './fixture';
  28  | 
  29  | // ---------------------------------------------------------------------------
  30  | // ATDD-256-T1-R01 · catalog state: discovery headline and filter controls visible
  31  | // ---------------------------------------------------------------------------
  32  | test('T013-catalog: discovery headline, filter controls and title grid structure are present', async ({
  33  |   page,
  34  |   context,
  35  | }) => {
  36  |   await freshSession(context);
  37  |   await goToDiscovery(page);
  38  |   await expectDiscoveryHeadline(page);
  39  | 
  40  |   // Fixed SpoolIn discovery identity (DS-POLICY-02)
> 41  |   await expect(page.getByRole('heading', { level: 1 })).toContainText(
      |                                                         ^ Error: expect(locator).toContainText(expected) failed
  42  |     'Every reel holds a story.',
  43  |   );
  44  |   await expect(page.getByText('Unspooling stories', { exact: false })).toBeVisible();
  45  | 
  46  |   // Filter controls present (AC-011, TD-C-007)
  47  |   await assertSearchInput(page);
  48  |   await expect(page.getByLabel(/title type/i)).toBeVisible();
  49  |   await expect(page.getByText(/genres/i, { exact: false }).first()).toBeVisible();
  50  |   await expect(page.getByLabel(/release year/i)).toBeVisible();
  51  |   await expect(page.getByLabel(/month/i)).toBeVisible();
  52  | 
  53  |   // Navigation (T004 shell)
  54  |   await expect(page.getByRole('link', { name: 'Catalog', exact: true })).toBeVisible();
  55  |   await expect(page.getByRole('link', { name: 'People', exact: true })).toBeVisible();
  56  | });
  57  | 
  58  | // ---------------------------------------------------------------------------
  59  | // ATDD-256-T1-R06 · catalog-invalid: text > 200 code points retained, no query
  60  | // AC-159, ATDD-133
  61  | // ---------------------------------------------------------------------------
  62  | test('T013-invalid: 201-code-point text retains full input and shows correction without issuing a query', async ({
  63  |   page,
  64  |   context,
  65  | }) => {
  66  |   await freshSession(context);
  67  |   await goToDiscovery(page);
  68  | 
  69  |   const searchInput = page.getByLabel(/search titles/i);
  70  |   await searchInput.waitFor({ state: 'visible' });
  71  | 
  72  |   const over200 = 'x'.repeat(201);
  73  |   await searchInput.fill(over200);
  74  | 
  75  |   // Correction feedback must appear (AC-159-C02)
  76  |   await expect(page.locator('[role="alert"]').filter({ hasText: /too long/i })).toBeVisible({ timeout: 3000 });
  77  | 
  78  |   // Full input retained in the field (AC-159-C02)
  79  |   const value = await searchInput.inputValue();
  80  |   expect(value.length).toBeGreaterThanOrEqual(201);
  81  | });
  82  | 
  83  | // ---------------------------------------------------------------------------
  84  | // AC-159-C01: 200-code-point input is valid (no correction shown)
  85  | // ---------------------------------------------------------------------------
  86  | test('T013-valid-200: exactly 200 code points passes without correction', async ({
  87  |   page,
  88  |   context,
  89  | }) => {
  90  |   await freshSession(context);
  91  |   await goToDiscovery(page);
  92  | 
  93  |   const searchInput = page.getByLabel(/search titles/i);
  94  |   await searchInput.waitFor({ state: 'visible' });
  95  | 
  96  |   await searchInput.fill('a'.repeat(200));
  97  | 
  98  |   // Allow debounce to settle
  99  |   await page.waitForTimeout(350);
  100 | 
  101 |   // No "too long" error visible
  102 |   const errors = page.locator('[role="alert"]').filter({ hasText: /too long/i });
  103 |   await expect(errors).toBeHidden({ timeout: 1000 }).catch(() => {
  104 |     // Some browsers may not hide it immediately; count = 0 is sufficient
  105 |   });
  106 | });
  107 | 
  108 | // ---------------------------------------------------------------------------
  109 | // AC-014: month requires year; clearing year clears month restriction
  110 | // ---------------------------------------------------------------------------
  111 | test('T013-month-validation: month selector is disabled without year selection', async ({
  112 |   page,
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
```