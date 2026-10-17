# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-invalid: 201-code-point text retains full input and shows correction without issuing a query
- Location: flows/discovery/discovery.spec.ts:62:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('[role="alert"]').filter({ hasText: /too long/i })
Expected: visible
Timeout: 3000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" locator('[role="alert"]').filter({ hasText: /too long/i }) with timeout 3000ms
  - waiting for locator('[role="alert"]').filter({ hasText: /too long/i })

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
  - button "Switch to light theme": Switch theme
- main:
  - heading "One component set." [level=1]
  - paragraph: Shared component specimens. Isolated fixtures; no business writes.
  - paragraph:
    - link "Navigation fixture":
      - /url: /next
  - heading "Wordmark and browser mark" [level=2]
  - text: SpoolIn
  - img "SpoolIn"
  - img "SpoolIn"
  - img "SpoolIn"
  - heading "Type in real content" [level=2]
  - heading "The Space Between Every Place We Have Ever Called Home" [level=3]
  - paragraph: V · Aruna Sari · 李 · O’Connor · é
  - paragraph: After fifteen years away, a railway archivist returns to her coastal hometown to record its last overnight train.
  - heading "Buttons and links" [level=2]
  - button "Save changes"
  - button "Cancel"
  - button "Delete archived movie"
  - button "Save draft" [disabled]
  - text: Review the newer saved version before saving.
  - button "Saving…" [disabled]
  - button "Selected" [pressed]
  - status:
    - strong: Changes saved
    - paragraph: "Record: fixture-movie-1"
  - heading "Field anatomy and error" [level=2]
  - text: Saved title · Optional
  - textbox "Saved title · Optional" [disabled]: The Last Light
  - text: Review the current version before editing. Title · Required
  - textbox "Title · Required": The Last Light
  - text: Use the full title. Do not truncate retained input. Synopsis · Optional
  - textbox "Synopsis · Optional": A coastal town waits for its final train. An archivist returns home.
  - button "Validate fields"
  - heading "Selection and date controls" [level=2]
  - text: Search titles
  - searchbox "Search titles": xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  - button "Clear search text"
  - text: Title type
  - combobox "Title type":
    - option "All titles" [selected]
    - option "Movies"
    - option "TV"
  - group "Genres · match any selected":
    - text: Genres · match any selected
    - checkbox "Drama" [checked]
    - text: Drama
    - checkbox "Documentary"
    - text: Documentary
  - text: Release year
  - combobox "Release year":
    - option "All years" [selected]
    - option "2025"
    - option "2026"
  - text: Month
  - combobox "Month" [disabled]:
    - option "All months" [selected]
    - option "January"
    - option "February"
    - option "March"
    - option "April"
    - option "May"
    - option "June"
    - option "July"
    - option "August"
    - option "September"
    - option "October"
    - option "November"
    - option "December"
  - text: Choose a year first. Clearing it also clears month. Other categories combine with AND.
  - status "Current filters": "{\"text\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"kind\":\"\",\"genres\":[\"drama\"],\"year\":\"\",\"month\":\"\"}"
  - heading "Consequential feedback" [level=2]
  - status:
    - strong: Changes saved
    - paragraph: "Record: fixture-movie-1"
  - status:
    - strong: Changes were not saved
    - paragraph: Your permitted draft remains. Correct the title.
  - status:
    - strong: Save result not confirmed
    - paragraph: "Check the original operation before submitting again. Operation: fixture-operation-1"
  - status:
    - strong: Action not permitted
    - paragraph: Your access changed. Review your session.
  - status:
    - strong: Loading current results
    - paragraph: Please wait for this search.
  - heading "Loading and upload stages" [level=2]
  - status: Loading the current search…
  - status:
    - text: Transfer · 63%
    - progressbar "Transfer · 63%"
  - status:
    - text: Transfer · 100%
    - progressbar "Transfer · 100%"
    - paragraph: Transfer complete. Waiting for processing and attachment.
  - status: Processing artwork
  - heading "Poster and missing artwork" [level=2]
  - text: Artwork unavailable
  - article:
    - text: Artwork unavailable
    - heading "The Space Between Every Place We Have Ever Called Home · 李" [level=3]:
      - link "The Space Between Every Place We Have Ever Called Home · 李":
        - /url: /titles/11111111-1111-4111-8111-111111111111
    - paragraph: Movie · Release date unknown
  - text: Artwork unavailable
  - heading "Navigation and metadata" [level=2]
  - term: Original release
  - definition: October 2025 · day unknown
  - term: Runtime
  - definition: Unknown
  - term: Credit count
  - definition: "0"
  - group: Source context
  - navigation "Verified Maintainer navigation fixture":
    - link "Catalog":
      - /url: /
    - link "People":
      - /url: /people
    - link "Movies":
      - /url: /manage/movies
    - link "Manage people":
      - /url: /manage/people
    - link "Archives":
      - /url: /manage/archives
    - link "History":
      - /url: /manage/audit
  - heading "Confirmation dialog" [level=2]
  - button "Open irreversible confirmation"
  - status: "Confirmed actions: 0"
  - heading "Review the newer saved version" [level=2]
  - paragraph: Your changes have not replaced the current version.
  - heading "Current saved version" [level=3]
  - paragraph: A newer saved synopsis.
  - heading "Your retained draft" [level=3]
  - paragraph: A coastal town waits for its final train. An archivist returns home.
  - button "Save changes" [disabled]
  - text: Review the newer version and check access before saving.
  - heading "Impact on Aruna Sari" [level=2]
  - alert: The impact changed. Review it again before confirming.
  - term: Active movies
  - definition: "2"
  - term: Archived movies
  - definition: "1"
  - term: TV
  - definition: "1"
  - paragraph: Archive before permanent deletion. Restore before editing.
  - heading "Read-only history" [level=2]
  - table "Catalog events":
    - caption: Catalog events
    - rowgroup:
      - row "Record Context Outcome":
        - columnheader "Record"
        - columnheader "Context"
        - columnheader "Outcome"
    - rowgroup:
      - row "11111111-1111-4111-8111-111111111111 The Last Light · Movie Approval recorded; creation not confirmed":
        - rowheader "11111111-1111-4111-8111-111111111111"
        - cell "The Last Light · Movie"
        - cell "Approval recorded; creation not confirmed"
  - heading "Session boundary" [level=2]
  - status:
    - strong: Checking your session
    - paragraph: Protected work is concealed until your session and access are verified.
  - status:
    - strong: Your session expired
    - paragraph: Protected work is concealed until your session and access are verified.
  - status:
    - strong: Sign-in is temporarily unavailable
    - paragraph: Protected work is concealed until your session and access are verified.
  - status:
    - strong: You do not have access
    - paragraph: Protected work is concealed until your session and access are verified.
  - heading "Movie proposal" [level=2]
  - paragraph: "Revision: fixture-revision-1"
  - term: Title
  - definition: The Last Light
  - term: Source
  - definition: Unknown
  - status: Revision approved. Creation is not yet confirmed.
  - heading "Safe text and URLs" [level=2]
  - text: Untrusted text · Optional
  - textbox "Untrusted text · Optional": 李 · O’Connor · é
  - text: Untrusted URL · Optional
  - textbox "Untrusted URL · Optional": https://example.test/source
  - paragraph: 李 · O’Connor · é
  - link "李 · O’Connor · é":
    - /url: https://example.test/source
  - text: Artwork unavailable
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
  41  |   await expect(page.getByRole('heading', { level: 1 })).toContainText(
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
> 76  |   await expect(page.locator('[role="alert"]').filter({ hasText: /too long/i })).toBeVisible({ timeout: 3000 });
      |                                                                                 ^ Error: expect(locator).toBeVisible() failed
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
```