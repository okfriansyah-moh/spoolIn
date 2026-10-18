> Current continuation (18 October 2026): source review is not runtime acceptance. All 46 tasks remain In progress for remote closure. The historical matrix below is retained; see [the complete task/obligation assessment](remote-acceptance-assessment.json) and the retained findings ledger for later corrections, actual remote failures and unresolved requirements.

# G01–G26 source closure matrix (T001–T046)

Revision: `1c0f196` on `codex/g17-g26-delivery`. Grok 4.7 re-checked F-001–F-011; required findings are resolved in source. Per-task review cells below stay “pending Grok” for the full 46-task pass except where a row names a finding. Runtime stays pending.

| Task | Required outcome | Source locations | FE / API / BE integration | Tests / evidence + revision | Source gaps | Review | Runtime |
| --- | --- | --- | --- | --- | --- | --- | --- |
| T001 | Reproducible build + verification harness | `build-tools/`, root `package.json`, Gradle settings | Harness scripts wired; no full suite run in lane | G-01 contract scripts; Gradle not executed here | — | pending Grok | pending |
| T002 | Shared wire contracts | `contracts/grpc`, `contracts/graphql`, `contracts/json` | Generated consumers referenced from FE/MCP/tests | `tests/contracts/`; `validate_manifest.py` ok @ lane | MCP TS proto gen gitignored (`mcp/src/generated/`) | pending Grok | pending |
| T003 | DB ownership + migrations | `services/*/db/migration`, `ops/bin/migrate.sh` | Per-service Flyway owners | Integration tests exist; not run in lane | — | pending Grok | pending |
| T004 | Themed accessible shell | `frontend/src/routes`, design tokens | SSR static via Core `200.html` | Frontend unit tests; browser not run | — | pending Grok | pending |
| T005 | Protected account storage | `services/core/.../account`, crypto config | GraphQL register/signIn | Core tests; DB not run | — | pending Grok | pending |
| T006 | Effective policy + input semantics | `libraries/backend-support/.../policy`, Core policy GraphQL | Browser `policy` query | Policy rule tests | — | pending Grok | pending |
| T007 | Owner receipts + atomic evidence | `OwnerReceipts.kt` (core/catalog/media), `ReceiptActivity` | Mutations wrap domain+audit in `committed` lambda | Receipt boundary tests; not run in lane | Catalog receipts omit `executeAudited`; audit via `AuditInsert` inside mutation (same connection) | pending Grok | pending |
| T008 | Registration + sessions | `services/core/.../session` | GraphQL session bootstrap + CSRF | Session tests | — | pending Grok | pending |
| T009 | mTLS + admission | `services/core/Application.kt` allowlists, interceptors | gRPC SAN/method map | Foundation/owner ITs | — | pending Grok | pending |
| T010 | Account browser journeys | `frontend/src/routes/account`, session controllers | `/graphql` session mutations | DOM/unit tests | E2E not run | pending Grok | pending |
| T011 | Fenced recovery + retries | `services/*/operations/maintenance`, retry config | Worker receipt paths | Recovery ITs | Runtime recovery not exercised | pending Grok | pending |
| T012 | Public catalog reads | Catalog gRPC reads, Core catalog view | Public GraphQL `titles`/`title`/`people` | Catalog/core tests | — | pending Grok | pending |
| T013 | Discovery UI | `frontend/src/lib/features/catalog`, `person/read` | GraphQL → Core → Catalog RPC | FE unit tests | — | pending Grok | pending |
| T014 | Encrypted drafts + receipts | `ProtectedDrafts`, Core draft storage | GraphQL draft mutations | Draft ITs | — | pending Grok | pending |
| T015 | Browser draft controllers | `DraftGraphql`, FE draft helpers | Account-isolated draft API | Draft shape tests | — | pending Grok | pending |
| T016 | Authoritative movie writes | `MovieWrites.kt`, catalog RPC | `writeMovie` gRPC | Movie write tests | — | pending Grok | pending |
| T017 | Movie editors connected | `frontend/.../movie/*`, Managed GraphQL | `fetchManagedMovie` / create / update | Editor controller tests | Runtime save not proven | pending Grok | pending |
| T018 | Person writes | `PersonWrites.kt` | gRPC writePerson | Person tests | — | pending Grok | pending |
| T019 | People management UI | `frontend/routes/manage/people` | Managed people GraphQL | FE tests | — | pending Grok | pending |
| T020 | Credits + roles | `CreditWrites`, `RoleWrites` | Catalog credit RPC | Credit tests | — | pending Grok | pending |
| T021 | Credit editing UI | `frontend/.../credit` | GraphQL credit mutations | draft-sync tests | — | pending Grok | pending |
| T022 | Artwork upload streams | `ArtworkRpc`, Media service | Core proxy + Media gRPC | Artwork tests | — | pending Grok | pending |
| T023 | Holds + attachment | Catalog/Media artwork coordination | BeginArtwork / Reserve paths | Integration tests | — | pending Grok | pending |
| T024 | Archive / restore / delete | Lifecycle RPC handlers | GraphQL lifecycle | Tests present | — | pending Grok | pending |
| T025 | Archive UI + confirmations | `frontend/routes/manage/archives` | GraphQL → catalog | FE wiring | — | pending Grok | pending |
| T026 | Artwork progress UI | `frontend/.../artwork` | Upload GraphQL + proxy | Unit tests | — | pending Grok | pending |
| T027 | Draft recovery in editors | Credit/movie draft sync | Draft GraphQL + controllers | draft-sync tests | Full editor recovery not runtime-proven | pending Grok | pending |
| T028 | MCP transport + connector | `mcp/src`, `McpGatewayRpc`, connector store | gRPC gateway + bearer; streamable HTTP stub extended | MCP meta unit tests added; no vitest project config in lane | Full MCP SDK session, live gRPC client, stdio bridge not wired | pending Grok | pending |
| T029 | Immutable proposals + exact approval | `ProposalWrites`, `ExactApprovalBinding`, `McpGatewayRpc` | Propose clears `ui_challenge`; binding token on gateway proto for adapter only | Proposal unit tests | `MCP_UI_ASSERTION` signing not implemented | pending Grok | pending |
| T030 | Trusted review resource | `mcp/src/review/*`, `McpReviewUiGateway` | Review fetches challenge via binding token; approve/reject POST added @ lane | review-state vitest (not executed) | Host must `postMessage` `connectorToken` for approve bearer | pending Grok | pending |
| T031 | Read-only audit | `AuditReads`, `ManagedCatalogGraphql.audit` | `manage/audit` → GraphQL `audit` | audit api test | — | pending Grok | pending |
| T032 | Frozen 100+100 dataset | `data/seed/manifest.json`, validator | Manifest validator pass @ lane | `validate_manifest.py` ok | — | pending Grok | pending |
| T033 | Seed init + replay | `CatalogSeedLoader`, `ops/bin/seed.sh` | PENDING→SUCCEEDED receipt; `synopsis_language` on insert | Loader source review | DB seed not executed in lane | pending Grok | pending |
| T034 | Actionable alerts | Core alerts tables + ingress | Alert recording path | Schema/tests | Delivery mode not runtime-proven | pending Grok | pending |
| T035 | Safe JSON diagnostics | `SafeJsonDiagnostics`, `LogLevelNormalization` | Redacts binding tokens | `JsonLogMessageRulesTest` (not run) | — | pending Grok | pending |
| T036 | Metrics + dashboards | Prometheus/Grafana ops assets | Scrape configs | Ops docs | Not started in lane | pending Grok | pending |
| T037 | Lifecycle commands | `ops/bin/*.sh`, `Makefile` `start` → `start.sh` | start fails closed without `SPOOLIN_LOCK_DIR` | Script inspection | Compose not run | pending Grok | pending |
| T038 | Claude Desktop approval | Client harness `tests/clients/claude` | Requires live MCP+host | Harness source | No host evidence | pending Grok | pending |
| T039 | ChatGPT acceptance | Client harness | Tunnel + host | Harness source | No host evidence | pending Grok | pending |
| T040 | Key rotation | `docs`/ops rotation scripts | Crypto key settings | Config tests | Not runtime-proven | pending Grok | pending |
| T041 | Migrations + restore | `migrate.sh`, recovery docs | Owner migrators | ITs | Not run | pending Grok | pending |
| T042 | API/RPC boundary reconciliation | Allowlists in `Application.kt` (core/catalog/media) | Method map matches proto | ATDD rows; Gradle ITs not run | — | pending Grok | pending |
| T043 | Policy + injection flows | Policy provider, GraphQL rules | Cross-boundary tests skeleton | Security tests | Full flow proof pending | pending Grok | pending |
| T044 | Visual/a11y evidence | Design system, browser tests | Playwright flows | Deferred per lane | Not run | pending Grok | pending |
| T045 | Capacity + recovery limits | Ops docs, receipt expiry | Maintenance jobs | — | Not measured | pending Grok | pending |
| T046 | Release handoff | `docs/release/*`, README | Points to matrix + open gaps | This matrix @ lane | Independent review + runtime acceptance open | pending Grok | pending |

## Lane fixes applied (this revision)

- MCP `buildProposeMovieCallToolResult` / `composeProposeMovieFromGatewayPayload` strip `ui_binding_token` from model structured content; binding token only in `_meta.ui`.
- Streamable HTTP POST `tools/call` + `propose_movie` uses composer (gateway revision in params; live gRPC forward still outstanding).
- Core HTTP `/gateway/approve_movie` and `/gateway/reject_movie` with connector bearer + bound challenge verification.
- Review UI sends connector bearer when host supplies `connectorToken` via `postMessage`.
