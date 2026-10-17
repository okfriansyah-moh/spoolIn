# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: discovery/discovery.spec.ts >> T013-back-restores-criteria: browser Back restores filter state
- Location: flows/discovery/discovery.spec.ts:273:1

# Error details

```
Test timeout of 60000ms exceeded.
```

```
Error: page.waitForURL: Test timeout of 60000ms exceeded.
=========================== logs ===========================
waiting for navigation until "load"
  navigated to "http://127.0.0.1:43104/"
  navigated to "http://127.0.0.1:43104/"
============================================================
```

# Page snapshot

```yaml
- generic [ref=f2e2]:
  - link "Skip to content" [ref=f2e3]:
    - /url: "#main-content"
  - banner [ref=f2e4]:
    - link "SpoolIn home" [ref=f2e5]:
      - /url: /
      - generic [ref=f2e6]: SpoolIn
    - navigation "Main navigation" [ref=f2e7]:
      - link "Catalog" [ref=f2e8]:
        - /url: /
      - link "People" [ref=f2e9]:
        - /url: /people
    - button "Switch to light theme" [ref=f2e10] [cursor=pointer]: Switch theme
  - main [ref=f2e11]:
    - heading "One component set." [active] [level=1] [ref=f2e12]
    - paragraph [ref=f2e13]: Shared component specimens. Isolated fixtures; no business writes.
    - paragraph [ref=f2e14]:
      - link "Navigation fixture" [ref=f2e15]:
        - /url: /next
    - generic [ref=f2e16]:
      - generic [ref=f2e17]:
        - heading "Wordmark and browser mark" [level=2] [ref=f2e18]
        - generic [ref=f2e19]: SpoolIn
        - generic [ref=f2e20]:
          - img "SpoolIn" [ref=f2e21]
          - img "SpoolIn" [ref=f2e22]
          - img "SpoolIn" [ref=f2e23]
      - generic [ref=f2e24]:
        - heading "Type in real content" [level=2] [ref=f2e25]
        - heading "The Space Between Every Place We Have Ever Called Home" [level=3] [ref=f2e26]
        - paragraph [ref=f2e27]: V · Aruna Sari · 李 · O’Connor · é
        - paragraph [ref=f2e28]: After fifteen years away, a railway archivist returns to her coastal hometown to record its last overnight train.
      - generic [ref=f2e29]:
        - heading "Buttons and links" [level=2] [ref=f2e30]
        - generic [ref=f2e31]:
          - button "Save changes" [ref=f2e33] [cursor=pointer]
          - button "Cancel" [ref=f2e35] [cursor=pointer]
          - button "Delete archived movie" [ref=f2e37] [cursor=pointer]
          - generic [ref=f2e38]:
            - button "Save draft" [disabled] [ref=f2e39]
            - generic [ref=f2e40]: Review the newer saved version before saving.
          - button "Saving…" [disabled] [ref=f2e42]
          - button "Selected" [pressed] [ref=f2e44] [cursor=pointer]
        - status [ref=f2e45]:
          - strong [ref=f2e46]: Changes saved
          - paragraph [ref=f2e48]: "Record: fixture-movie-1"
      - generic [ref=f2e49]:
        - heading "Field anatomy and error" [level=2] [ref=f2e50]
        - generic [ref=f2e51]:
          - generic [ref=f2e52]: Saved title · Optional
          - textbox "Saved title · Optional" [disabled] [ref=f2e53]: The Last Light
          - generic [ref=f2e54]: Review the current version before editing.
        - generic [ref=f2e55]:
          - generic [ref=f2e56]:
            - generic [ref=f2e57]: Title · Required
            - textbox "Title · Required" [ref=f2e58]: The Last Light
            - generic [ref=f2e59]: Use the full title. Do not truncate retained input.
          - generic [ref=f2e60]:
            - generic [ref=f2e61]: Synopsis · Optional
            - textbox "Synopsis · Optional" [ref=f2e62]: A coastal town waits for its final train. An archivist returns home.
          - button "Validate fields" [ref=f2e64] [cursor=pointer]
      - generic [ref=f2e65]:
        - heading "Selection and date controls" [level=2] [ref=f2e66]
        - generic [ref=f2e67]:
          - generic [ref=f2e68]:
            - generic [ref=f2e69]: Search titles
            - searchbox "Search titles" [ref=f2e70]
            - button "Clear search text" [ref=f2e71] [cursor=pointer]
          - generic [ref=f2e72]: Title type
          - combobox "Title type" [ref=f2e73]:
            - option "All titles" [selected]
            - option "Movies"
            - option "TV"
          - group "Genres · match any selected" [ref=f2e74]:
            - generic [ref=f2e76]:
              - checkbox "Drama" [checked] [ref=f2e77]
              - text: Drama
            - generic [ref=f2e78]:
              - checkbox "Documentary" [ref=f2e79]
              - text: Documentary
          - generic [ref=f2e80]: Release year
          - combobox "Release year" [ref=f2e81]:
            - option "All years" [selected]
            - option "2025"
            - option "2026"
          - generic [ref=f2e82]: Month
          - combobox "Month" [disabled] [ref=f2e83]:
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
          - generic [ref=f2e84]: Choose a year first. Clearing it also clears month. Other categories combine with AND.
        - status "Current filters" [ref=f2e85]: "{\"text\":\"\",\"kind\":\"\",\"genres\":[\"drama\"],\"year\":\"\",\"month\":\"\"}"
      - generic [ref=f2e86]:
        - heading "Consequential feedback" [level=2] [ref=f2e87]
        - status [ref=f2e88]:
          - strong [ref=f2e89]: Changes saved
          - paragraph [ref=f2e91]: "Record: fixture-movie-1"
        - status [ref=f2e92]:
          - strong [ref=f2e93]: Changes were not saved
          - paragraph [ref=f2e95]: Your permitted draft remains. Correct the title.
        - status [ref=f2e96]:
          - strong [ref=f2e97]: Save result not confirmed
          - paragraph [ref=f2e99]: "Check the original operation before submitting again. Operation: fixture-operation-1"
        - status [ref=f2e100]:
          - strong [ref=f2e101]: Action not permitted
          - paragraph [ref=f2e103]: Your access changed. Review your session.
        - status [ref=f2e104]:
          - strong [ref=f2e105]: Loading current results
          - paragraph [ref=f2e107]: Please wait for this search.
      - generic [ref=f2e108]:
        - heading "Loading and upload stages" [level=2] [ref=f2e109]
        - status [ref=f2e110]: Loading the current search…
        - status [ref=f2e112]:
          - text: Transfer · 63%
          - progressbar "Transfer · 63%" [ref=f2e113]
        - status [ref=f2e114]:
          - text: Transfer · 100%
          - progressbar "Transfer · 100%" [ref=f2e115]
          - paragraph [ref=f2e116]: Transfer complete. Waiting for processing and attachment.
        - status [ref=f2e117]: Processing artwork
      - generic [ref=f2e119]:
        - heading "Poster and missing artwork" [level=2] [ref=f2e120]
        - generic [ref=f2e121]: Artwork unavailable
        - article [ref=f2e123]:
          - generic [ref=f2e124]: Artwork unavailable
          - heading [level=3] [ref=f2e126]:
            - link "The Space Between Every Place We Have Ever Called Home · 李" [ref=f2e127]:
              - /url: /titles/11111111-1111-4111-8111-111111111111
          - paragraph [ref=f2e128]: Movie · Release date unknown
        - generic [ref=f2e129]: Artwork unavailable
      - generic [ref=f2e131]:
        - heading "Navigation and metadata" [level=2] [ref=f2e132]
        - generic [ref=f2e133]:
          - generic [ref=f2e134]:
            - term [ref=f2e135]: Original release
            - definition [ref=f2e136]: October 2025 · day unknown
          - generic [ref=f2e137]:
            - term [ref=f2e138]: Runtime
            - definition [ref=f2e139]: Unknown
          - generic [ref=f2e140]:
            - term [ref=f2e141]: Credit count
            - definition [ref=f2e142]: "0"
        - group [ref=f2e143]:
          - generic "Source context" [ref=f2e144] [cursor=pointer]
        - navigation "Verified Maintainer navigation fixture" [ref=f2e145]:
          - link "Catalog" [ref=f2e146]:
            - /url: /
          - link "People" [ref=f2e147]:
            - /url: /people
          - link "Movies" [ref=f2e148]:
            - /url: /manage/movies
          - link "Manage people" [ref=f2e149]:
            - /url: /manage/people
          - link "Archives" [ref=f2e150]:
            - /url: /manage/archives
          - link "History" [ref=f2e151]:
            - /url: /manage/audit
      - generic [ref=f2e152]:
        - heading "Confirmation dialog" [level=2] [ref=f2e153]
        - button "Open irreversible confirmation" [ref=f2e155] [cursor=pointer]
        - status [ref=f2e156]: "Confirmed actions: 0"
      - generic [ref=f2e158]:
        - heading "Review the newer saved version" [level=2] [ref=f2e159]
        - paragraph [ref=f2e160]: Your changes have not replaced the current version.
        - generic [ref=f2e161]:
          - generic [ref=f2e162]:
            - heading "Current saved version" [level=3] [ref=f2e163]
            - paragraph [ref=f2e164]: A newer saved synopsis.
          - generic [ref=f2e165]:
            - heading "Your retained draft" [level=3] [ref=f2e166]
            - paragraph [ref=f2e167]: A coastal town waits for its final train. An archivist returns home.
        - generic [ref=f2e168]:
          - button "Save changes" [disabled] [ref=f2e169]
          - generic [ref=f2e170]: Review the newer version and check access before saving.
      - generic [ref=f2e172]:
        - heading "Impact on Aruna Sari" [level=2] [ref=f2e173]
        - alert [ref=f2e174]: The impact changed. Review it again before confirming.
        - generic [ref=f2e175]:
          - generic [ref=f2e176]:
            - term [ref=f2e177]: Active movies
            - definition [ref=f2e178]: "2"
          - generic [ref=f2e179]:
            - term [ref=f2e180]: Archived movies
            - definition [ref=f2e181]: "1"
          - generic [ref=f2e182]:
            - term [ref=f2e183]: TV
            - definition [ref=f2e184]: "1"
        - paragraph [ref=f2e185]: Archive before permanent deletion. Restore before editing.
      - generic [ref=f2e186]:
        - heading "Read-only history" [level=2] [ref=f2e187]
        - table [ref=f2e188]:
          - caption [ref=f2e189]: Catalog events
          - rowgroup [ref=f2e190]:
            - row [ref=f2e191]:
              - columnheader "Record" [ref=f2e192]
              - columnheader "Context" [ref=f2e193]
              - columnheader "Outcome" [ref=f2e194]
          - rowgroup [ref=f2e195]:
            - row [ref=f2e196]:
              - rowheader "11111111-1111-4111-8111-111111111111" [ref=f2e197]
              - cell "The Last Light · Movie" [ref=f2e198]
              - cell "Approval recorded; creation not confirmed" [ref=f2e199]
      - generic [ref=f2e200]:
        - heading "Session boundary" [level=2] [ref=f2e201]
        - status [ref=f2e202]:
          - strong [ref=f2e203]: Checking your session
          - paragraph [ref=f2e204]: Protected work is concealed until your session and access are verified.
        - status [ref=f2e205]:
          - strong [ref=f2e206]: Your session expired
          - paragraph [ref=f2e207]: Protected work is concealed until your session and access are verified.
        - status [ref=f2e208]:
          - strong [ref=f2e209]: Sign-in is temporarily unavailable
          - paragraph [ref=f2e210]: Protected work is concealed until your session and access are verified.
        - status [ref=f2e211]:
          - strong [ref=f2e212]: You do not have access
          - paragraph [ref=f2e213]: Protected work is concealed until your session and access are verified.
      - generic [ref=f2e215]:
        - heading "Movie proposal" [level=2] [ref=f2e216]
        - paragraph [ref=f2e217]: "Revision: fixture-revision-1"
        - generic [ref=f2e218]:
          - generic [ref=f2e219]:
            - term [ref=f2e220]: Title
            - definition [ref=f2e221]: The Last Light
          - generic [ref=f2e222]:
            - term [ref=f2e223]: Source
            - definition [ref=f2e224]: Unknown
        - status [ref=f2e225]: Revision approved. Creation is not yet confirmed.
      - generic [ref=f2e226]:
        - heading "Safe text and URLs" [level=2] [ref=f2e227]
        - generic [ref=f2e228]:
          - generic [ref=f2e229]: Untrusted text · Optional
          - textbox "Untrusted text · Optional" [ref=f2e230]: 李 · O’Connor · é
        - generic [ref=f2e231]:
          - generic [ref=f2e232]: Untrusted URL · Optional
          - textbox "Untrusted URL · Optional" [ref=f2e233]: https://example.test/source
        - paragraph [ref=f2e234]: 李 · O’Connor · é
        - link "李 · O’Connor · é" [ref=f2e236]:
          - /url: https://example.test/source
        - generic [ref=f2e237]: Artwork unavailable
```

# Test source

```ts
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
  213 |   ).toBeVisible({ timeout: 5000 });
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
> 293 |   await page.waitForURL(/^\//);
      |              ^ Error: page.waitForURL: Test timeout of 60000ms exceeded.
  294 |   await page.waitForLoadState('networkidle');
  295 | 
  296 |   // Kind filter should be restored from URL
  297 |   const restoredKind = await page.getByLabel(/title type/i).inputValue();
  298 |   expect(restoredKind).toBe('MOVIE');
  299 | });
  300 | 
```