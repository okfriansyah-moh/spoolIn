# Movie poster sourcing investigation

> Historical research: the user superseded the open-license-only sourcing restriction on 18 October 2026. The findings below describe the earlier snapshot and criteria. The current `public-pages-2026-10-v2` package retains 100 movies and 100 TV series with 200 matched posters sourced from public TMDB HTML pages. Source URLs and supplied rights information are recorded in `data/seed/manifest.json` and `data/seed/sources/tmdb-pages.json`; no open-license claim is inferred. The earlier shortfall does not describe the replacement package.

Research date: 18 October 2026. This report covers movies only. It is candidate research, not an approved seed replacement.

## Result and acceptance status

The current manifest contains 100 movies, including many recent commercial films. This pass identified **three retained-title poster candidates with detailed source-page evidence**, one retained-title low-resolution candidate, and four replacement leads. **Zero pairs have completed the full acceptance process**: no image bytes were acquired, no visual review was performed, and no complete field-level metadata audit was completed. The approved requirement of 100 verified movie/poster pairs remains unmet.

The table below records claims made by the individual file pages, rather than claiming worldwide clearance. Several public-domain rationales are expressly limited by jurisdiction. The poster artwork is a separate work from the movie; the movie being public domain does not establish poster reuse rights. [Citizen Kane file evidence](https://commons.wikimedia.org/wiki/File:Citizen_Kane_poster,_1941_(Style_A).jpg)

## Retained-title candidates

| Movie | Specific source file | File-page evidence | Remaining work |
|---|---|---|---|
| Citizen Kane (1941), Q24815 | [Style A theatrical poster](https://commons.wikimedia.org/wiki/File:Citizen_Kane_poster,_1941_(Style_A).jpg) | RKO Radio Pictures; 1950×2937 JPEG; scan via Heritage Auctions; US public domain because poster copyright was not renewed; file page links renewal records. | Visual review, source hash, applicable reuse assessment, frozen evidence and field audit. |
| Rebecca (1940), Q204212 | [1939 advance theatrical poster](https://commons.wikimedia.org/wiki/File:Rebecca_(1939_poster).jpeg) | United Artists Corporation; 2013×3000 JPEG; Heritage Auctions scan; US public domain, nonrenewal rationale. Poster date is 1939; film date is 1940. | Visual review and full asset/metadata evidence. Do not change film year to poster year. |
| Roman Holiday (1953), Q201293 | [1953 theatrical poster](https://commons.wikimedia.org/wiki/File:Roman_Holiday_(1953_poster).jpg) | Designer unknown, Paramount Pictures; 1940×2936 JPEG; Heritage Auctions scan; US public domain, nonrenewal rationale. | Visual review and full asset/metadata evidence. |
| 20,000 Leagues Under the Sea (1916), Q111655 | [1916 poster](https://commons.wikimedia.org/wiki/File:Poster_-_20,000_Leagues_under_the_Sea_(1916).jpg) | Universal Film Manufacturing Company employees; 219×300 JPEG; file page states US public domain. | Source is unusually small; inspect readability and seek a better scan before acceptance. |

The alternative [Rebecca Poster.jpg](https://commons.wikimedia.org/wiki/File:Rebecca_Poster.jpg) is described as a Photoplay poster/advertisement and measures 1596×2102. Prefer the specifically described theatrical poster above; an advertisement should not be silently treated as an original release poster.

## Replacement leads

| Movie | Specific source file | Evidence and limitation |
|---|---|---|
| Big Buck Bunny (2008), Q282456 | [Official poster](https://commons.wikimedia.org/wiki/File:Big_buck_bunny_poster_big.jpg) | 1500×2122 JPEG; Blender Foundation; CC BY 3.0; file page gives required attribution and links the production press source. This is a short film, not a TV series. |
| Sintel (2010), Q925587 | [Poster](https://commons.wikimedia.org/wiki/File:Sintel_poster.jpg) | A specific portrait poster lead; complete license/creator extraction and visual review still required. Do not infer poster terms solely from film terms. |
| Night of the Living Dead (1968), Q623051 | [Specific regional poster](https://commons.wikimedia.org/wiki/File:Night_Of_The_Living_Dead_(1968)_-_Poster.jpg) | 917×1319 JPEG; unknown author; source-page rationale is US publication without notice. It expressly distinguishes the common poster, which has a notice. Do not substitute another design under this rationale. |
| One Exciting Night (1922) | [Lobby poster](https://commons.wikimedia.org/wiki/File:One_Exciting_Night_1922_lobby_poster.jpg) | 960×1440 JPEG; unknown author; pre-1931 US public-domain rationale. Weak original provenance (Google image proxy); needs stronger archive corroboration before acceptance. |

## Rejections and cautions

- [Seven Samurai poster2.jpg](https://commons.wikimedia.org/wiki/File:Seven_Samurai_poster2.jpg) explicitly records US copyright despite its Japanese public-domain tag. Reject for this package without a separately adequate basis. Being hosted on Commons is not sufficient proof.
- [Nosferatu logo.png](https://commons.wikimedia.org/wiki/File:Nosferatu_logo.png) is a logo, not a poster, and fails the agreed artwork rule. Two requested Nosferatu poster pages returned internal errors; no acceptance claim is made for them.
- Official archival possession is insufficient by itself. The [Library of Congress motion-picture image guidance](https://guides.loc.gov/cold-war-images/selected-collections) places its motion-picture poster collection among material that may have restrictions. Require an individual item's evidence.

## Feasibility and bounded acquisition

The [1925 US film-poster category](https://commons.wikimedia.org/wiki/Category:1925_film_posters_of_the_United_States) supplies further early-film leads. A direct Commons API category request succeeded, returning files including Ben-Hur, The Big Parade and Body and Soul. A subsequent batched metadata query returned **HTTP 429**. The batch did not produce an inventory artifact; category membership is not a verified-pair count. No repeated repair attempts or mass media downloads followed.

Obtaining 100 movie pairs may be feasible by replacing much of the recent commercial selection with older films and open films. This remains an inference, not a verified inventory. A complete 100-pair movie inventory and the separate 100-pair TV inventory must both pass before seed replacement. Continue later with a rate-limited source queue, page-specific evidence, title identity matching, and contact-sheet review; do not build the replacement from category counts.
