# Seed data and poster audit

Observed on 18 October 2026. The approved upgrade requires 100 real movie/poster pairs and 100 real TV/poster pairs before replacement. **That sourcing gate remains unmet. No seed or application implementation has been changed.**

## Current dataset

The frozen package contains 100 movies, 100 TV titles, 1,213 people, and 1,475 credits. It has no artwork references or bundled image files. All 200 synopses are generated one-line descriptions; all 200 records omit original title, original language, countries, and runtime. These omissions come from the current freezer, not proof that the underlying works lack that information.

The existing structural validator passed across the full manifest: identities, references, hashes, provenance shape, and declared counts. This does not verify historical truth or artwork rights. See the [machine-readable audit](seed-data-audit.json) for the exact snapshot and comparison scope.

## Live source comparison

All 200 title records were retrieved from the [Wikidata Action API](https://www.wikidata.org/w/api.php). English labels, release values selected by the existing freezer, seeded genre IDs, and included credited-person/role statements matched the current source in this pass. This compares the package with Wikidata; it is not independent corroboration from 200 official studio or broadcaster records, and it does not claim complete cast lists.

An initial query returned HTTP 429 after the first 40 records. A later rate-spaced continuation retrieved the remaining 160 without mismatches. The raw title responses are retained locally under `.ai/evidence/seed-poster-research/current-title-entities.json`; that ignored file is not included in a clone.

The referenced-name check compared 1,000 of 1,378 distinct person/genre English labels with live Wikidata and found no mismatches among those retrieved. The remaining 378 were not checked. The source first returned HTTP 429; after honoring its retry interval, a continuation retrieved another batch and then returned an API error. This partial name audit is not full people/genre acceptance. Local raw responses are retained under `.ai/evidence/seed-poster-research/current-reference-entities.json`.

Live source entities have original-title statements for 199 titles, language statements for 199, country statements for 200, and duration statements for 183. Presence alone does not establish a field's correct normalized meaning, date, country context, or duration unit. Those values have not been imported. Image statements for 57 titles and logos for 133 are not a poster inventory. Wikidata metadata licensing does not establish rights to referenced artwork.

## Poster research

- [Movie source inventory](seed-movie-poster-sourcing.md): three detailed retained-title candidates, one small-image candidate, and four replacement leads. No complete approved movie/poster package.
- [TV source inventory](seed-tv-poster-sourcing.md): all 28 files in the live Commons series-poster category reviewed as metadata; 12 distinct portrait candidates plus four outside-category leads. Category files include duplicates and excluded photographs, logos, or square artwork.
- A browser inspection displayed the Leap Year poster with a readable title and creator/VRT permission notices. Full identity, file, and package acceptance remains incomplete.

Fully approved pairs in these inventories: **0/100 movies and 0/100 TV series**. These are incomplete verification counts, not a claim that no usable posters exist or that 100 cannot exist worldwide. Candidate counts must not be presented as approved coverage.

## Outcome and next requirement

The plan explicitly requires reporting a source shortfall rather than substituting placeholders or relaxing rights requirements. Retain the current seed and its version/hash. Do not start an incomplete poster replacement, apply a database refresh, change the existing artwork permissions, or mark seed acceptance complete.

To continue the unchanged plan, establish complete title-matched, visually reviewed, per-file rights evidence for both 100-item inventories. Further rights-holder or open-collection sources can be added without changing the criteria. A commercial/provider source with different terms, a reduced catalog, or non-poster artwork would be a change to the agreed scope and is not substituted here.

Verification in this pass is limited to structural validation, source comparisons, documentation checks, and source-page inspection. No application regression suites, browser acceptance campaign, fresh installation, refresh smoke run, Media integration, or image-package validation ran because no replacement package or application change exists.
