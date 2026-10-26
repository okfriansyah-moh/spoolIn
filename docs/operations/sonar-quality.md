# Sonar quality verification

Task `SONAR-ZERO` extends draft PR #17 from base `5e44a778`. Every frontend,
Core, Catalog, Media, backend support and MCP project must report zero reliability
issues, zero security issues and exactly zero duplicated lines and blocks. Native
line coverage and both Sonar coverage measures must remain at least 80% per component.
Analyzer scope, rule activation, duplication thresholds and existing coverage gates
remain unchanged.

## Implementation and current evidence

Shared frontend draft/editor/list behavior and Core/Catalog mapping, receipt and SQL
projection behavior replace the reported duplication. Explicit UTF-16 comparators
preserve protocol ordering. Linear Unicode whitespace trimming retains existing
normalization. A static shell title and route updates retain one title element.

Changed MCP lint, type/dependency checks and 97 focused tests passed. Core and
Catalog complete unit suites passed. Frontend lint/type/dependency checks and 612
unit tests passed before the added title case; that case subsequently passed separately.
Real PostgreSQL/mTLS title projections, archived visibility, null/array/date mapping,
row locks, lifecycle operations, proposal expiry and concurrent receipt replay passed.
An outdated proposal test now advances to the service-returned expiry rather than
assuming the superseded ten-minute duration; production expiry did not change.

Independent source review accepted revision `c11c9bf3`. A fresh clean Docker run
passed all 4,324 unit tests with no failures, errors or skips. Changed executable-line
coverage passed: frontend 94.068%, Core 99.293%, Catalog 91.228%, MCP 100%.
Final PostgreSQL projection, lifecycle, proposal and receipt suites passed after the
refactors. Frontend lint, type and dependency checks passed. Final browser replay,
refreshed coverage after the Core layout correction, Sonar publication and review of
that evidence remain pending.
Existing broad Core/Catalog lint and complexity/depth findings remain separate from
affected-source results; the newly introduced audit-object and projection-layout
findings were corrected. MCP broad lint retains eighteen existing findings. The
configured Semgrep scan passed with no findings or errors. The broad directory secret
scan returned 635 findings in ignored runtime credentials and historical evidence,
including issue-ID metadata; none refer to changed source. No scanner suppressions
or rule changes were introduced. Dependency inventories did not change.

## MCP network-range findings

The fourteen `typescript:S1313` findings concern literal network classification ranges,
not fixed request destinations. The IPv4 and IPv6 exclusions prevent requests to
protected networks; the IPv6 global-unicast range bounds permitted address families.
The downloader checks every DNS answer, pins its selected connection address and
revalidates redirect targets. Range definitions remain unchanged.

Focused tests cover denied range endpoints, permitted adjacent addresses, mapped IPv6,
mixed DNS answers, redirects, connection pinning, unsafe URL shapes, cancellation,
concurrency and size limits. See `mcp/test/tools/poster-download.test.ts`.
The review also considers [OWASP SSRF prevention guidance](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html).
Only independently verified safe findings may be classified as false positives in
Sonar, as explicitly selected by the user. On 26 October 2026, the independent reviewer approved all fourteen reported constants.
Each finding now has an evidence comment and `FALSE_POSITIVE` status in the local
Sonar dashboard. These are reviewed classifications, not removed protections or rule
suppressions. They persist in the dashboard database; a new dashboard requires review
and classification of its own findings.
