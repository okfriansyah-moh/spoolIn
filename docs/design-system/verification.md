# SpoolIn branding verification

Date: 11 October 2026, Asia/Jakarta. Scope: documentation and static HTML prototype rebrand.

## Structural checks

The baseline artifact checker stopped because this package had no `verification.md`. This record supplies that missing artifact and separates the current checks from the earlier design review recorded in [the specification](../03-design-system.md#validation-and-handoff-boundaries).

- All JSON artifacts under `docs/` parse successfully.
- Local Markdown file links resolve, including the renamed `04-spoolin-techdoc.md` and new [brand history](brand-history.md).
- All 192 PRD FR, NFR, and AC definition rows are byte-for-byte unchanged from the pre-rebrand documents.
- Product-name scanning found no placeholder brand references or incorrect display capitalization. Genuine external repository references and the actual workspace path retain their original names; technical identifiers retain their existing spelling. Lowercase `spoolin` appears only in filesystem slugs.
- PRD source fingerprints and the technical document’s file hashes and design-subtree composite digest are refreshed against the final artifact bytes.

Document paths below use the current numbered filenames; the recorded results describe the original validation runs. The installed checker also assumes the unnumbered specification filename, so filename-aware validation is required for this layout.

Artifact-checker command:

```sh
python3 /Users/mekari/.codex/skills/design-system/scripts/check_artifacts.py docs/design-system --source docs/02-PRD.md
```

The unmodified installed checker reports missing package-root `index.html`, `components.html`, and `tokens.css`. This package already keeps those artifacts in `mockup/`; its portable index also cites the actual PRD outside the package. A temporary, layout-aware copy of the installed checker passed structural source coverage, artifact links, requirement mappings, token aliases, and CSS export consistency. Its only adaptations were the three existing `mockup/` entrypoint locations, HTML-gallery/component reachability through those locations, and permission for the portable index to cite the declared primary source. All other checks were retained. The installed skill and the established package layout were preserved.

## Browser checks and preview

A local headless Chromium browser rendered all 14 HTML templates in both themes at 1440px. Discovery, the component gallery, and the design-system index were also checked at 390px and 320px in both themes: 40 page/theme/width combinations in total. Checks confirmed exact SpoolIn wordmarks and page titles, visible wordmarks within the viewport, no horizontal page overflow, no broken image elements, and no placeholder brand text. The discovery page also matched the approved headline and supporting copy at each width. No JavaScript page errors occurred.

Theme switching, People navigation, wordmark navigation back to the catalog, and navigation from the gallery to the brand history passed. After the verification link was added, all six gallery theme/width combinations and both documentation links were checked again and passed. Screenshots of the homepage in both themes at all three widths were captured and visually reviewed. The wordmark and headline remain readable, navigation wraps, and the supporting copy fits at all inspected widths. The [two-theme preview](mockup/assets/design-preview.jpg) was regenerated from the actual updated desktop HTML renders, preserving the themes, typography, artwork, and paired-frame symbol.

## Boundaries

This pass verifies the branding and inspected static prototype layouts. It does not rerun the earlier 582 state/theme/width combinations, measure new contrast ratios, or establish production behavior. No application, API, database, authentication, or integration changes were made.

[Design specification](../03-design-system.md) · [Portable index](index.md) · [UI gallery](mockup/index.html)


## Catalog follow-up · 28 October 2026

The six-sort select and Previous/Next demonstration supersede the earlier discovery mockup behavior. Prior browser/visual results above describe the earlier branding revision and do not verify this follow-up. Initial generation and delivery attempts were blocked by elevated memory pressure. Following explicit approval to continue, sequential pinned-toolchain Docker runs capped at 1 GB and one CPU completed contract generation, production builds, changed frontend lint, frontend type checking, 76 focused frontend tests and 10 focused Catalog unit tests. The type checker retains one unrelated existing RoleCreator warning. Extended coverage/security/browser suites were not run in this bounded follow-up; visual and manual browser acceptance remain pending. Current source fingerprints are refreshed in the technical document; those fingerprints establish document identity, not passing software.


The local PostgreSQL/Core/Catalog/Media/MCP images were rebuilt, Catalog migration V027 was applied, and the four application containers were restarted. Catalog’s runtime schema range was aligned to 27 after the first readiness attempt exposed the mismatch. Final application readiness passed. A bounded read-only live GraphQL check passed all six sort modes across two pages, default 24-title size/order, year/type filtering and cross-sort cursor rejection. This establishes those API observations; manual browser layout, focus and history acceptance remain pending.

### Catalog recovery correction · 28 October 2026

A live browser reproduced artwork and catalog HTTP 429 responses during repeated paging. The correction bounds visible poster work and retries transient reads without changing quotas. Focused frontend regressions: 111 passed; Core frontend-policy test passed; frontend type check passed with one existing RoleCreator warning. Independent correction source review accepted the visibility cancellation and download deadline fixes. Extended acceptance and quality measurements remain pending; the rebuilt local app passed live page traversal 1→4→1 and rapid six-sort changes. Two real poster 429 responses recovered automatically, with eight visible posters loaded and no unavailable artwork or error alerts. A fault-injected page failure retained rows and recovered on retry. Desktop 1280px and mobile 390px checks measured 16px action gaps without horizontal overflow. Evidence: `.ai/reports/recovery-live.log`, `recovery-layout.log` and control screenshots in the same local report directory.

### Read budget refinement · 28 October 2026

Manual feedback confirmed the former 120-read default still interrupted human browsing because posters share that quota. The new READ default is 2400 per 60 seconds; other profiles and explicit overrides are preserved. Title details use bounded read recovery. Selection-4e29d3f4f3c34875 records 159 backend configuration cases, 30 frontend catalog/detail cases, 19 MCP admission cases, frontend/MCP type checking, changed-source lint and both production builds passing. Existing RoleCreator warning remains. Core and MCP restarted after refreshing their protected policy agreement without replacing keys or clearing counters; readiness passed. Live browser paging 1→8→1, title detail/return and rapid six-sort changes produced 348 read responses with zero failures, error alerts, waiting messages or unavailable artwork (`.ai/reports/read-budget-live.log`). Independent source review accepted. Broad acceptance remains pending.
