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

Final source convergence, full fresh reports, Sonar verification and independent
combined review are still pending. Existing unrelated MCP broad lint findings remain
separate from changed-source checks.

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
Sonar, as explicitly selected by the user. Classification has not yet occurred.
