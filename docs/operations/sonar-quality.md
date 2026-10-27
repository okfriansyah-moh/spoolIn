# Sonar quality verification

Task `SONAR-ZERO` extends draft PR #17 from base `5e44a778`. The six projects now
have zero reliability issues, zero security issues and exactly zero duplicated lines
and blocks. Native line coverage and both Sonar coverage measures exceed 80% for
every component. Analyzer scope, rules, duplication thresholds and existing coverage
checks remain unchanged.

## Verified results — 26 October 2026

Clean tested source: `1d3f0bd85e41385aaab257c799dc220fcbe2e112`.
The complete Docker run passed **4,328 unit tests**, with no failures, errors or skips,
from `2026-10-26T05:59:17.298Z` to `2026-10-26T06:04:06.205Z`.
All six analyses completed and passed their gates at `2026-10-26T06:16:33.863Z`;
the publication verifier accepted the matching input fingerprint and exact raw counters.

| Component | Native covered / total | Native lines | Sonar Coverage | Sonar lines | Security / reliability / duplicate lines / blocks | Gate |
|---|---:|---:|---:|---:|---:|---|
| frontend | 4,975 / 5,909 | 84.19% | 82.2% | 86.3% | 0 / 0 / 0 / 0 | OK |
| core | 5,704 / 6,523 | 87.44% | 81.4% | 87.5% | 0 / 0 / 0 / 0 | OK |
| catalog | 5,083 / 5,845 | 86.96% | 81.4% | 87.0% | 0 / 0 / 0 / 0 | OK |
| media | 1,893 / 2,179 | 86.87% | 81.3% | 86.9% | 0 / 0 / 0 / 0 | OK |
| backend-support | 3,180 / 3,624 | 87.75% | 81.6% | 87.8% | 0 / 0 / 0 / 0 | OK |
| mcp | 1,127 / 1,360 | 82.87% | 81.6% | 83.1% | 0 / 0 / 0 / 0 | OK |

Changed executable-line coverage against `5e44a778`: frontend 93.776%, Core 99.293%,
Catalog 91.228%, MCP 100%; no changed production files were missing from reports.
Media and backend support have no production-source changes in this selection.
Kover imports resolved 164 Core, 91 Catalog, 48 Media and 79 support source mappings;
scanner logs contain no unresolved-report or out-of-range coverage warnings.
Frontend native coverage includes Svelte; its Sonar view covers the supported TypeScript
sources, as described in [the measurement boundaries](unit-coverage.md#measurement-boundaries).

[HTML summary](http://localhost:8090/) and [Sonar projects](http://localhost:9000/projects)
were opened, and every generated component HTML report was inspected. Raw values are
retained in `.local/unit-coverage/summary.json` and `sonar-results.json`.

## Implementation and behavior checks

Shared frontend draft synchronization, deliberate fresh-start guards, submission/recovery,
list loading and outcome decoding replace repeated movie/person/credit behavior.
Domain adapters retain their fields, endpoints and movie-specific title state. Shared
Core response/request projections and Catalog receipt, lifecycle and SQL projections
retain authorization, locks, transaction boundaries, sparse updates and idempotency.
Explicit UTF-16 comparators preserve valid protocol ordering. Linear Unicode whitespace
trimming preserves normalization. Static shell and route titles retain one title element.
The completed identity migration, public APIs and database schemas are unchanged by
this selection.

- All 4,328 unit tests and six report-enforcement regression checks passed. Enforcement
  rejects insufficient coverage, failed or empty suites, missing reports and nonzero or
  malformed raw quality counts; failure reports remain available.
- Frontend lint, type and dependency checks passed; one existing `resetKey` capture
  warning remains. Changed MCP lint, type/dependency checks and 97 focused tests passed.
- Real PostgreSQL/mTLS title projections, archived visibility, null/array/date mapping,
  row locks, lifecycle operations, proposal expiry and concurrent receipt replay passed:
  166 cases across four nonempty selections, no failures or skips.
- Eight final browser cases passed against normal frontend routes, including the initial
  shell title and Archives → People → back navigation. Browser GraphQL fixtures remain
  distinct from the real database/transport checks.
- An outdated proposal test now advances to the returned expiry instead of assuming the
  superseded ten-minute duration; production expiry did not change.

The independent combined reviewer accepted this source and matching final evidence
on 26 October 2026 with no outstanding findings, including the dashboard screenshots,
browser/database proof, approved classifications and disclosed baseline failures.
Local raw evidence is under `.ai/state/sonar-zero/`; superseded diagnostic coverage and
wrong-mode/interrupted browser attempts are excluded from acceptance.

## MCP network-range findings

Fourteen `typescript:S1313` findings concern network classification constants. The
independent reviewer traced direct literals, every DNS answer, pinned connections,
redirect revalidation and TLS verification, and approved these exact reported ranges:

| Use | Reported ranges |
|---|---|
| IPv4 denylist | 10.0.0.0/8, 100.64.0.0/10, 169.254.0.0/16, 172.16.0.0/12, 192.0.0.0/24, 192.88.99.0/24, 192.168.0.0/16, 198.18.0.0/15, 224.0.0.0/4, 240.0.0.0/4 |
| IPv6 global-unicast classifier | 2000::/3 |
| IPv6 denylist | 2001::/23, 2002::/16, 3fff::/20 |

The constants configure `BlockList.addSubnet`; they are never fixed request destinations.
Range definitions and private-network protection remain intact. Protective tests cover
denied endpoints, permitted adjacent addresses, mapped IPv6, mixed DNS answers,
redirects, connection pinning, unsafe URLs, cancellation, concurrency and byte limits.
See `mcp/test/tools/poster-download.test.ts` and
[OWASP SSRF prevention guidance](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html).

As explicitly selected by the user, only these fourteen independently verified findings
received explanatory evidence comments and `FALSE_POSITIVE` status. They persist in
the local dashboard database. A new dashboard requires review of its own findings;
publication never automatically dismisses future findings.

## Separate baseline failures

Broad Core/Catalog lint and complexity/depth checks still report unrelated baseline
issues. Newly introduced audit-object and projection-layout findings were corrected;
remaining findings in touched files belong to unchanged functions. MCP broad lint
retains eighteen existing findings. No quality threshold or suppression was relaxed.

Configured Semgrep passed with no findings or errors. The broad directory secret scan
returned 635 findings in ignored runtime credentials and historical/evidence metadata,
including issue IDs; none refer to changed source. The final frontend delta also passed
its affected-file secret scan. These results do not establish repository-wide green
checks. Dependency inventories did not change. Existing integration acceptance and
remote CI remain separate from this local verification.
