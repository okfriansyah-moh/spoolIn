---
kind: task
id: T036
group: G22
depends_on:
- T011
- T022
- T023
- T028
- T034
- T035
readiness: Ready
status: In progress
risk: high
test_strategy: alongside
visual_review: not-applicable
---

# T036 — Expose logical metrics, dashboards and alert episodes

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-166 | verifies |
| ATDD-195 | verifies |
| ATDD-206 | both |
| ATDD-207 | both |
| ATDD-300 | both |
| ATDD-312 | both |
| SC-06 | both |
| FR-004 | both |
| FR-015 | both |
| NFR-006 | both |
| NFR-008 | both |
| A07 | both |
| D21 | both |
| D23 | both |
| AC-019 | both |
| AC-019-C01 | both |
| AC-019-C02 | both |
| AC-079 | both |
| AC-080 | both |
| AC-081 | both |
| AC-082 | both |
| G-03 | both |
| G-05 | both |
| AC-063 | both |
| AC-063-C01 | both |
| OD-004 | both |
| OD-008 | both |
| OD-014 | both |
| US-003 | both |
| TD-C-062 | both |
| TD-ROW-L3593 | both |
| TD-ROW-L6347 | both |
| TD-ROW-L6348 | both |
| TD-ROW-L6349 | both |
| TD-ROW-L6350 | both |
| TD-ROW-L6351 | both |
| TD-ROW-L6352 | both |
| TD-ROW-L6353 | both |
| TD-ROW-L6354 | both |
| TD-ROW-L6355 | both |
| TD-ROW-L6356 | both |
| TD-ROW-L6357 | both |
| TD-ROW-L6358 | both |
| TD-ROW-L6364 | both |
| TD-ROW-L6365 | both |
| TD-ROW-L6366 | both |
| TD-ROW-L6367 | both |
| TD-ROW-L6368 | both |
| TD-ROW-L6369 | both |
| TD-ROW-L6370 | both |
| TD-ROW-L6371 | both |
| TD-ROW-L6372 | both |
| BRAIN-A07 | both |
| BRAIN-D21 | both |
| BRAIN-D23 | both |

## Prerequisites

- [T011 — Run fenced recovery and bounded retries](../../09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.
- [T022 — Accept and process bounded artwork streams](../../14-artwork-management-processing-and-attachment/tasks/T022-accept-and-process-bounded-artwork-streams.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing.
- [T023 — Coordinate protective holds, attachment and cleanup](../../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.
- [T028 — Implement isolated MCP transport and connector identity](../../18-assisted-movie-creation-transport-and-review/tasks/T028-implement-isolated-mcp-transport-and-connector-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.
- [T034 — Record actionable alerts through one selected mode](../../21-operational-notifications/tasks/T034-record-actionable-alerts-through-one-selected-mode.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Default: exhausted eligible work yields one inspectable sanitized recording and zero sends; database mode needs no external provider credential.
- [T035 — Emit bounded private-safe JSON diagnostics](T035-emit-bounded-private-safe-json-diagnostics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | ops/monitoring/prometheus.yaml | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/monitoring/alerts.yaml | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/monitoring/alertmanager.yaml | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/monitoring/grafana/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/operations/metrics/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/operations/metrics/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/operations/metrics/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/operations/monitoring/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/operations/metrics/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/operations/metrics/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/operations/metrics/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [12.2 Metrics, alerts and privacy](../../../04-spoolin-techdoc.md#122-metrics-alerts-and-privacy): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.4 Metric contracts and alert expressions](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [13.3 Verification locations and command selection](../../../04-spoolin-techdoc.md#133-verification-locations-and-command-selection): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [SC-06 · L105](../../../02-PRD.md#4-goals-and-success-criteria) | Make failures understandable and recoverable \| Users see accurate outcomes; operations owners can inspect retained failures, receive alerts, and recover eligible work \| Unverified; Committed |
| [FR-004 · L238](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [FR-015 · L249](../../../02-PRD.md#61-functional-requirements) | Notify an operations owner through one configured channel and expose delivery failure without alert storms \| D21 Committed \| AC-061 through AC-063 |
| [NFR-006 · L264](../../../02-PRD.md#62-quality-and-delivery-requirements) | Operations owners can assess readiness, diagnose user failures, and act on alerts \| D23 Committed \| AC-079 through AC-082 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [A07 · L286](../../../02-PRD.md#63-project-requirements-coverage) | Add, update, delete movies \| FR-004 / AC-016 through AC-021, AC-108 through AC-110, AC-130 |
| [D21 · L322](../../../02-PRD.md#64-project-decision-register) | Bounded safe retries and one configured exhaustion channel \| NFR-004/NFR-005, FR-015 / AC-061 through AC-063, AC-072 through AC-078 |
| [D23 · L324](../../../02-PRD.md#64-project-decision-register) | Health/readiness, logs, metrics, dashboards, actionable alerts \| NFR-006 / AC-079 through AC-082 |
| [AC-019 · L371](../../../02-PRD.md#us-003-maintain-movies) | Permanent deletion \| A movie is archived and a Maintainer sees its deletion consequences \| They separately confirm permanent deletion \| Remove it from the archive and catalog permanently, remove its credits, preserve shared people and other titles' artwork, and retain an attributable deletion record. No restore action remains. |
| [AC-019-C01 · L371](../../../02-PRD.md#us-003-maintain-movies) | Remove it from the archive and catalog permanently, remove its credits, preserve shared people and other titles' artwork, and retain an attributable deletion record. |
| [AC-019-C02 · L371](../../../02-PRD.md#us-003-maintain-movies) | No restore action remains. |
| [AC-079 · L865](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Distinguish process health, readiness, and actual operation success \| NFR-006; technical design |
| [AC-080 · L866](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Preserve sanitized correlation across frontend requests, internal calls, and durable work \| NFR-006; technical design |
| [AC-081 · L867](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify outcome/latency/retry/pending-age/dependency metrics and dashboards, including application errors under HTTP 200 \| NFR-006; technical design |
| [AC-082 · L868](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise actionable alerts, routing, deduplication, and channel failure under selected thresholds \| NFR-006/FR-015; technical design |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-05 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [AC-063 · L977](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Repeated exhaustion \| The same exhausted operation is observed repeatedly \| Alerts are considered \| Duplicate signals do not create an uncontrolled alert storm; delivery status remains visible. |
| [AC-063-C01 · L977](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Duplicate signals do not create an uncontrolled alert storm; delivery status remains visible. |
| [OD-004 · L1020](../../../02-PRD.md#131-resolved-product-decisions) | Movies and people require archiving before separately confirmed permanent deletion. Person archiving retains credits privately; restoration reveals only surviving credits on active titles. Deletion preserves associated titles and movie deletion preserves shared people. \| FR-004/FR-005/FR-006; AC-019/AC-024/AC-108 through AC-110, AC-129 through AC-138 |
| [OD-008 · L1046](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Replacement/recovery outcomes fixed; attachment/reference/cleanup/retention and compensation mechanics remain for techdoc \| FR-010/FR-014 \| Technical design; before lifecycle implementation |
| [OD-014 · L1051](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Operational outcomes committed; telemetry tools, readiness rules, definitions/windows/thresholds, retention/access, and response owner remain for techdoc \| NFR-006 \| Technical design; before monitoring implementation; deployment SLO/SLA only with justified assumptions |
| [US-003 · L362](../../../02-PRD.md#us-003-maintain-movies) | ### US-003: Maintain movies As a Maintainer, I want accurate movie information and deliberate removal controls so I can manage the catalog without losing work unexpectedly. Requirements: FR-004, FR-013, NFR-014. Writes require current Maintainer access; public/Viewer reads remain unchanged. |
| [TD-C-062 · L6341](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | All named metric types/labels/boundaries and fixed histogram buckets, no identifiers in labels; selected alert/rule/dashboard expressions tested for reset/missing/low traffic/errors/duplicates/receiver failure; thresholds measured targets. |
| [TD-ROW-L3593 · L3593](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `form_draft` \| Core `draft/persistence` \| Private recoverable edits expire independently of catalog records |
| [TD-ROW-L6347 · L6347](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_operation_results_total` \| Counter; service/operation/boundary/outcome/code \| One logical owner result or ingress result per completed request; SUCCESS, DOMAIN_REJECTED, SYSTEM_FAILED, UNCERTAIN separated, including GraphQL HTTP 200 errors |
| [TD-ROW-L6348 · L6348](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_operation_duration_seconds` \| Histogram; service/operation/boundary \| Entry to reply/termination at owner or ingress, identified separately; includes waits, excludes later durable effects |
| [TD-ROW-L6349 · L6349](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_work_items` \| Gauge; service/state \| Current scheduled durable_work row counts by state; exclude database-only NOTIFICATION recording rows, which have no worker lifecycle |
| [TD-ROW-L6350 · L6350](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_oldest_due_work_seconds` \| Gauge; service \| Age since oldest READY work became due; zero when none, collection failure leaves scrape unhealthy rather than zero |
| [TD-ROW-L6351 · L6351](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_notification_events` \| Gauge; service/mode/state \| durable_work rows WHERE kind=NOTIFICATION grouped by notification_mode/notification_state; RECORDED/ACCEPTED/UNKNOWN/FAILED and confirmed delivery remain separate |
| [TD-ROW-L6352 · L6352](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_pool_active_connections`, `spoolin_pool_max_connections` \| Gauges; service \| Bounded runtime pool usage/capacity; migration pool excluded |
| [TD-ROW-L6353 · L6353](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_disk_available_bytes`, `spoolin_disk_capacity_bytes` \| Gauges; service/volume \| Owned database/artwork/backup volume headroom, configured finite volume labels |
| [TD-ROW-L6354 · L6354](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_crypto_failures_total` \| Counter; service/code \| Authentication/key/unavailable/nonce-exhaustion classes; no secret or row ID label |
| [TD-ROW-L6355 · L6355](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_media_processing_total` \| Counter; outcome \| Completed processors: SUCCESS or FAILED; rejected file validation separately in operation results |
| [TD-ROW-L6356 · L6356](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_tunnel_connected`, `spoolin_tunnel_enabled` \| Gauges; client \| Runner connection/configuration for CHATGPT; connection is distinct from successful client review |
| [TD-ROW-L6357 · L6357](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_log_dropped_total` \| Counter; service/level/reason \| Discarded diagnostic records; fixed reasons queue_full, sink_failed or contract_invalid; no payload/identity label |
| [TD-ROW-L6358 · L6358](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | `spoolin_service_ready` \| Gauge; service \| Dependency readiness; complements Prometheus `up` process/exporter reachability |
| [TD-ROW-L6364 · L6364](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | Service unavailable \| `up == 0` or `spoolin_service_ready == 0`, for 1 min \| Critical; clears on healthy scrape/readiness |
| [TD-ROW-L6365 · L6365](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | System mutation failure \| `sum by(service)(rate(spoolin_operation_results_total{boundary="owner",outcome="SYSTEM_FAILED"}[5m])) / clamp_min(sum by(service)(rate(spoolin_operation_results_total{boundary="owner"}[5m])),0.001) > 0.05` with at least 20 total results in 5 min; restrict operation allowlist to mutations \| Warning; validation/denial/conflict are separate expected outcomes |
| [TD-ROW-L6366 · L6366](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | Exhausted work / delayed work \| `spoolin_work_items{state="EXHAUSTED"} > 0`, for 1 min; or `spoolin_oldest_due_work_seconds > 120` \| Warning; clears after controlled recovery/eligible completion |
| [TD-ROW-L6367 · L6367](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | Notification failure/uncertainty \| `spoolin_notification_events{state=~"FAILED\|UNKNOWN"} > 0` \| Warning; operator resolves evidence/state; record locally without recursively dispatching this alert through the failed adapter |
| [TD-ROW-L6368 · L6368](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | Pool pressure \| `spoolin_pool_active_connections / spoolin_pool_max_connections > 0.8`, for 5 min \| Warning; clears when sustained usage falls |
| [TD-ROW-L6369 · L6369](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | Low disk \| `spoolin_disk_available_bytes / spoolin_disk_capacity_bytes < 0.2`; critical at `< 0.1` \| Warning/critical; restrict new uploads at critical headroom and verify safe space recovery |
| [TD-ROW-L6370 · L6370](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | Crypto fault \| `increase(spoolin_crypto_failures_total[5m]) >= 1` \| Critical; inspect keys/AAD/nonce safety; recovery requires canary verification |
| [TD-ROW-L6371 · L6371](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | Processing failures \| FAILED rate / total processor rate &gt; 0.1 over 5 min with at least 10 jobs \| Warning; clears after bounded processor recovery |
| [TD-ROW-L6372 · L6372](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions) | Tunnel unavailable \| `spoolin_tunnel_connected == 0` and enabled==1, for 2 min \| Warning; clears on reconnection; client approval acceptance still independently required |
| [BRAIN-A07 · L52](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A07 \| Add, delete, and update movies \| Working operations with validation and understandable outcomes |
| [BRAIN-D21 · L90](../../../01-brainstorming-result.md#4-decision-register) | D21 \| Committed \| Bounded exponential backoff with jitter for safe transient failures; environment-configurable retry policy and exhaustion notification through one configured channel |
| [BRAIN-D23 · L92](../../../01-brainstorming-result.md#4-decision-register) | D23 \| Committed \| Health/readiness checks, correlated logs, metrics, dashboards, and actionable alerts; numerical SLOs and any SLA remain open |

## Implementation

1. Implement every named metric type/fixed label/bucket and private authenticated scrape endpoint, including domain errors under HTTP200, gRPC deadlines, stages, worker pending/exhaustion, keys, storage and notification evidence.
2. Provision Prometheus/Alertmanager/Grafana as selected local dependencies, fixed safe dashboards and source-defined alert expressions/windows/reset/deduplication rules. Do not use raw IDs/input/private fields as labels.
3. Test low/no traffic, missing series, counter reset, process/dependency failure and recovery separately; health, readiness, business outcome and external integration evidence remain distinct.
4. Exercise authenticated alert ingress and configured retention with receiver outage within/beyond retention; make independent outage monitoring truthful and actionable without recursive notification.
5. Keep numeric thresholds as prospective local targets until T045 measures the workload; dashboards/logs never substitute for durable evidence.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T036-A1:** Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard.
- **T036-A2:** Alert: one episode routes one sanitized durable event, repeated signals deduplicate, recovery resets appropriately, and receiver outage cannot be labeled delivered.
- **T036-A3:** Privacy: no secret/account/operation/high-cardinality input label appears and private exporters are not browser/MCP tools.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Use checks alongside implementation because build/visual/runtime composition must be exercised as its interfaces appear; acceptance still governs before completion. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-166](../../../05-atdd.md#case-atdd-166-retain-unresolved-holds-and-reject-stale-messages) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.6 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then unknown reservations remain protective and become operationally visible after24 h And only conclusive Catalog state permits confirm or release And tuple/version checks and terminal RELEASED prevent stale commands from affecting current or other-title references Observation: Age actual RESERVED hold beyond 24 hours with Catalog unavailable, then send conclusive and stale tuple/version messages. Inspect hold state/protection and alert evidence; RELEASED cannot reactivate and contradictory references never authorize release. |
| [ATDD-195](../../../05-atdd.md#case-atdd-195-acknowledge-alerts-only-after-durable-sanitized-recording) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §5.5 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then only private mTLS ingress accepts the bounded20alert/64 KiB payload with64labels/annotations and1024charvalues And valid16lowerhex fingerprint/timestamps and allowlisted fields form dedup events without fetched URLs Observation: Send real authenticated private webhook payload rows and interrupt the Core transaction after event/before dispatch insert. Inspect 202 only after committed dedup/event/work, 503 on rollback and safe 400/413 for malformed bounds; callback URLs are never fetched. Commit one bounded deduplicated NOTIFICATION durable_work record before acknowledging Alertmanager. Database mode has DONE/RECORDED without an outbound attempt; external dispatch follows the commit. Core/database outage remains truthful unsent evidence in Alertmanager within its configured retention, not a fabricated durable application record. |
| [ATDD-206](../../../05-atdd.md#case-atdd-206-observe-logical-results-instead-of-http-success-alone) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §12.2/12.4 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then owner and ingress result/duration measurements have separate documented boundaries and seconds buckets And HTTP 200domain failures, gRPCdeadline, queueage, retries, media/crypto/storage/tunnel and notification states remain distinguishable And bounded labels excludeUUID/private input; failed collection is unhealthy rather than fabricated zero Observation: Scrape actual metrics after named owner/ingress outcomes and inspect exact classification/buckets/allowlisted labels. Compare HTTP 200 domain failures to recorded failure series, queue/notification state to persisted rows and failed scrape to unhealthy rather than invented zero. |
| [ATDD-207](../../../05-atdd.md#case-atdd-207-evaluate-alert-episodes-with-actionable-recovery) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §12.4 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then rules use source-proposed expressions, minimum traffic and expected failure classification rather than treating domain rejection asoutage And receiver failure remains visible inAlertmanager/status without recursive dispatch or claiming durableDBrecording whileDBunavailable Observation: Evaluate the selected rule fixtures at threshold/window/minimum-traffic boundaries and inspect actual Alertmanager grouping/dedup/firing/resolved state. Run F-ALERT-RETENTION receiver outage rows and compare Core ledger versus retained/unsent local alert state without recursive delivery. |
| [ATDD-300](../../../05-atdd.md#case-atdd-300-classify-operational-outcomes-and-bounded-metric-labels) / Unit | SUCCESS, VALIDATION, DENIED, CONFLICT, SYSTEM_FAILED underHTTP200; unknown label value, UUID/query/providerURL; counter resets and minimum20-result rule fixtures. | Then domain failures remain distinguishable from transport success And unbounded/private labels are rejected or excluded according to the fixed schema And expected denial/conflict do not become system-failure alert traffic Observation: Compare exact outcome/label sets and independent threshold fixture values; real exporter/scrape and Alertmanager timing remain system integration. |
| [ATDD-312](../../../05-atdd.md#case-atdd-312-observe-retained-alert-retry-and-outage-beyond-retention-truthfully) / Integration | F-ALERT-RETENTION configuredR&gt;0, receiverApp/DB unavailable atR−1 s and beyondR; repeatedfiring/resolvedepisode; actualstatus/dashboard. | Then inside configured retention the alert remains visible/retryable and recovery records one deduplicated event And during database outage no DB recording or external delivery is falsely claimed And beyond retention evidence reports actual retained/unsent/lost status without an invented delivery guarantee; independent status remains usable Observation: Capture actual resolved retention value, timestamps, Alertmanager UI/state, receiver responses, unique Core event/work rows and operator status. Controlled shorter test retention is a fixture, not a new production default or SLA. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T036` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T036` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| boundary-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
