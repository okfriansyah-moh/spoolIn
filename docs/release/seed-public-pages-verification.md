# Real seed data and posters

The `public-pages-2026-10-v2` package contains 100 movies and 100 TV series, each with a frozen title-matched poster. Preparation read public TMDB HTML and linked static JPEGs without source accounts, API credentials or provider API requests. Installation reads packaged files only. Source URLs, retrieval dates, dimensions, hashes and supplied rights information are retained in the manifest and source ledger. Unspecified rights remain unspecified.

The previous Wikidata snapshot remains the upgrade baseline. Stable record IDs are retained. Refresh compares each seed-owned field with that baseline, retains existing relationships and protects edits, uploads, artwork removal history, archives and deletion markers. Media preparation, Catalog attachment and Media confirmation have separate resumable completion records.

## Focused verification

- Complete package validation and real Media decoding covered all 200 posters. Two independent visual inspections checked all eight labeled contact sheets.
- A fresh PostgreSQL installation contained exactly 100 movies and 100 TV series with 200 attached posters. All 200 poster responses passed bounded HTTPS JPEG checks.
- An upgrade retained a changed title/synopsis, an archived title, a deleted title and previous artwork removal. Its 199 retained titles included 197 seed posters. Repeat import preserved the title-row digest and added no duplicates.
- An interrupted Media preparation stopped after 6 committed assets. Resume completed 200 assets and 200 attachments without duplicates.
- A movie and TV page displayed matched posters and TMDB source links in the real browser. TV remained read-only. Screenshots and runner outputs remain in local selection evidence.
- Focused existing/new tests covered package rejection, source projection, poster attribution and selected filter behavior. Kotlin compilation, formatting, detekt, two owner architecture checks, frontend typing/build and changed-file ESLint passed.
- Changed executable-line coverage used the existing Kover agent on those same seed scenarios and focused Vitest cases: Catalog 285/318 (89.623%), Media 188/189 (99.471%), frontend 10/10 (100%). No thresholds or exclusions changed.
- The configured Semgrep baseline reported zero findings. The first directory-wide secret scan included ignored runtime configuration and historical local evidence; all 347 findings were confined to `.local` or `.ai`. The same scanner reported zero leaks in a clean exported source tree before publication.

No theme/viewport/browser matrix, recording, load test or broad acceptance sweep was run. This evidence covers the seed upgrade and poster display; it does not establish full release acceptance. The PR remains a draft and is not merged.
