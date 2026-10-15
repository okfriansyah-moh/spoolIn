---
kind: task
id: T036
group: G16
depends_on:
- T003
- T009
- T010
- T011
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T036 — Record actionable alerts through one selected mode

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Default: exhausted eligible work yields one inspectable sanitized recording and zero sends; database mode needs no external provider credential. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-061 | both |
| ATDD-062 | both |
| ATDD-063 | both |
| ATDD-067 | verifies |
| ATDD-158 | verifies |
| ATDD-193 | both |
| ATDD-194 | both |
| ATDD-195 | implements |
| ATDD-227 | implements |
| ATDD-227-T1-R01 | implements |
| ATDD-227-T1-R02 | implements |
| ATDD-227-T1-R03 | implements |
| ATDD-227-T1-R04 | implements |
| ATDD-227-T1-R05 | implements |
| ATDD-227-T1-R06 | implements |
| ATDD-227-T1-R07 | implements |
| ATDD-227-T1-R08 | implements |
| ATDD-227-T1-R09 | implements |
| ATDD-227-T1-R10 | implements |
| ATDD-299 | both |
| SC-06 | both |
| FR-014 | both |
| FR-015 | both |
| NFR-004 | both |
| NFR-006 | both |
| NFR-008 | both |
| A04 | both |
| D19 | both |
| D20 | both |
| D21 | both |
| D23 | both |
| AC-082 | both |
| AC-087 | both |
| G-04 | both |
| J-06 | both |
| AC-060 | both |
| AC-060-C01 | both |
| AC-061 | both |
| AC-061-C01 | both |
| AC-062 | both |
| AC-062-C01 | both |
| AC-063 | both |
| AC-063-C01 | both |
| AC-075 | both |
| AC-075-C01 | both |
| AC-075-C02 | both |
| OD-013 | both |
| US-010 | both |
| PRD-HANDOFF-06 | both |
| TD-U-005 | both |
| ADR-07 | both |
| V-NOTIFY | both |
| TD-ROW-L3560 | both |
| TD-ROW-L3587 | both |
| TD-ROW-L5448 | both |
| TD-ROW-L5506 | both |
| BRAIN-A04 | both |
| BRAIN-D19 | both |
| BRAIN-D20 | both |
| BRAIN-D21 | both |
| BRAIN-D23 | both |

## Prerequisites

- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T009 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T010 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T011 — Run fenced recovery and bounded retries](../../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/alerts/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/operations/notifications/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/operations/notifications/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/operations/notifications/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/notifications/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/alerts/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/operations/notifications/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/operations/notifications/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/operations/notifications/ | T036 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [6.2.2 Common operational tables](../../../04-spoolin-techdoc.md#622-common-operational-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.7 Durable work, retries and notifications](../../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [11.1 Configuration contract](../../../04-spoolin-techdoc.md#111-configuration-contract): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.4 Metric contracts and alert expressions](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-227-T1-R01 · L9067](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `kind` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-227-T1-R02 · L9068](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `state` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission supplies 'READY'; explicit NULL fails23502; `NOT NULL DEFAULT 'READY' CHECK (state IN ('READY','LEASED','DONE','EXHAUSTED','CANCELLED')); NOT NULL (explicit or PK)` |
| [ATDD-227-T1-R03 · L9069](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `generation` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (generation >= 0); NOT NULL (explicit or PK)` |
| [ATDD-227-T1-R04 · L9070](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `attempt_count` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK >= 0; <= max_attempts` |
| [ATDD-227-T1-R05 · L9071](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `lease_epoch` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (lease_epoch >= 0); NOT NULL (explicit or PK)` |
| [ATDD-227-T1-R06 · L9072](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `notification_key` (`varchar(160)`); Source-valid technical token; 160 versus 161 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL for non-notification; UNIQUE when present` |
| [ATDD-227-T1-R07 · L9073](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `notification_mode` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL for non-notification; database/smtp/slack/google_chat` |
| [ATDD-227-T1-R08 · L9074](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `notification_state` (`varchar(24)`); Source-valid technical token; 24 versus 25 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL for non-notification; RECORDED/PENDING/ACCEPTED/DELIVERY_CONFIRMED/UNKNOWN/FAILED` |
| [ATDD-227-T1-R09 · L9075](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `notification_summary` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; apply only declared byte checks; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL for non-notification; object <=8192 bytes` |
| [ATDD-227-T1-R10 · L9076](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence) | `provider_reference` (`varchar(255)`); Source-valid technical token; 255 versus 256 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL allowed; no default` |
| [SC-06 · L105](../../../02-PRD.md#4-goals-and-success-criteria) | Make failures understandable and recoverable \| Users see accurate outcomes; operations owners can inspect retained failures, receive alerts, and recover eligible work \| Unverified; Committed |
| [FR-014 · L248](../../../02-PRD.md#61-functional-requirements) | Preserve required follow-up through interruptions and make exhausted work inspectable/recoverable \| D20/D21 Committed \| AC-057 through AC-060 |
| [FR-015 · L249](../../../02-PRD.md#61-functional-requirements) | Notify an operations owner through one configured channel and expose delivery failure without alert storms \| D21 Committed \| AC-061 through AC-063 |
| [NFR-004 · L262](../../../02-PRD.md#62-quality-and-delivery-requirements) | Safe recovery is bounded and respects cancellation; uncertain outcomes cannot multiply effects \| D21 Committed; D22 defaults Recommended \| AC-072 through AC-075 |
| [NFR-006 · L264](../../../02-PRD.md#62-quality-and-delivery-requirements) | Operations owners can assess readiness, diagnose user failures, and act on alerts \| D23 Committed \| AC-079 through AC-082 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [A04 · L283](../../../02-PRD.md#63-project-requirements-coverage) | Real service-to-service gRPC \| NFR-001/NFR-008 / AC-065, AC-087 |
| [D19 · L320](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [D20 · L321](../../../02-PRD.md#64-project-decision-register) | Atomic successful audit, durable follow-up, saga only when needed \| FR-013/FR-014 / AC-053, AC-054, AC-057 through AC-060; techdoc mechanics |
| [D21 · L322](../../../02-PRD.md#64-project-decision-register) | Bounded safe retries and one configured exhaustion channel \| NFR-004/NFR-005, FR-015 / AC-061 through AC-063, AC-072 through AC-078 |
| [D23 · L324](../../../02-PRD.md#64-project-decision-register) | Health/readiness, logs, metrics, dashboards, actionable alerts \| NFR-006 / AC-079 through AC-082 |
| [AC-082 · L868](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise actionable alerts, routing, deduplication, and channel failure under selected thresholds \| NFR-006/FR-015; technical design |
| [AC-087 · L870](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration \| NFR-008; technical design |
| [G-04 · L925](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
| [J-06 · L946](../../../02-PRD.md#123-delivery-responsibilities-and-journeys) | Operations owner diagnoses failed work \| Restricted operational access \| Identify failed follow-up, receive configured notification, and recover safely without losing the original failure evidence |
| [AC-060 · L974](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Exhausted work \| Recovery attempts are exhausted \| An authorized operations owner inspects it \| See operation, owner, attempts, sanitized cause, and safe recovery guidance; controlled recovery preserves the original evidence and checks current conditions. |
| [AC-060-C01 · L974](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | See operation, owner, attempts, sanitized cause, and safe recovery guidance; controlled recovery preserves the original evidence and checks current conditions. |
| [AC-061 · L975](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Exhaustion notification \| An eligible operation exhausts recovery and one channel is configured \| Notification delivery succeeds \| The operations owner receives actionable failure/operation context and recovery guidance, with delivery evidence. |
| [AC-061-C01 · L975](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | The operations owner receives actionable failure/operation context and recovery guidance, with delivery evidence. |
| [AC-062 · L976](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Notification failure \| The configured channel is unavailable or rejects delivery \| Delivery fails \| Failure stays inspectable without reversing business success or recursively creating more notifications. |
| [AC-062-C01 · L976](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Failure stays inspectable without reversing business success or recursively creating more notifications. |
| [AC-063 · L977](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Repeated exhaustion \| The same exhausted operation is observed repeatedly \| Alerts are considered \| Duplicate signals do not create an uncontrolled alert storm; delivery status remains visible. |
| [AC-063-C01 · L977](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Duplicate signals do not create an uncontrolled alert storm; delivery status remains visible. |
| [AC-075 · L986](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Cancel or exhaust \| The user cancels or the allowed recovery period/attempt budget ends \| Recovery stops \| Show cancellation or retained failure accurately; do not launch unlimited new attempts. Notify exhaustion where applicable. |
| [AC-075-C01 · L986](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Show cancellation or retained failure accurately; do not launch unlimited new attempts. |
| [AC-075-C02 · L986](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Notify exhaustion where applicable. |
| [OD-013 · L1050](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | One channel and visible delivery/exhaustion behavior fixed; provider/destination/credentials, deduplication, limits, and delivery inspection remain for techdoc \| FR-015 \| Technical design; before notification verification |
| [US-010 · L965](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | #### US-010: Recover follow-up and notify failures As an operations owner, I want interrupted work to remain inspectable so I can recover it without repeating completed effects. Requirements: FR-014, FR-015. Operational inspection is restricted; its tooling belongs to the techdoc. |
| [PRD-HANDOFF-06 · L839](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D21/D22 \| Committed policy; Recommended D22 defaults \| Bounded exponential backoff with jitter for safe transient failures, environment configuration, one retry owner, deadlines/cancellation, retained attempt budget, exhaustion inspection, and one configured notification provider. D22 states the recommended starting values; the techdoc selects operation-specific policies. |
| [TD-U-005 · L6538](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes) | TD-U-005 \| Exactly one notification mode: database (default), SMTP, Slack or Google Chat \| FR-015, D21, OD-013, AC-061/062/063/082; external delivery is conditional on selected external mode. Database mode records only and satisfies the revised local release obligation, not the old delivery claim |
| [ADR-07 · L6554](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-07 \| Default DB recording, one optional external adapter. No mandatory paid notification infrastructure \| TD-U-005 supersedes external-only release interpretation |
| [V-NOTIFY · L6394](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-NOTIFY \| Independently configure database/smtp/slack/google_chat, inject explicit rejection/ambiguous timeout/repeated alert/failed channel; assert one selected adapter, durable dedup, RECORDED≠ACCEPTED≠DELIVERY_CONFIRMED and nonrecursive failures; external test needs provider evidence |
| [TD-ROW-L3560 · L3560](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | notification \| durable_work kind NOTIFICATION: event_key/mode/state/summary→notification_*; provider_reference/correlation/timestamps retained; ID becomes work identity \| Unique event, truthful delivery evidence, 90-day summary and unresolved retention; former work_id is unnecessary self-linkage |
| [TD-ROW-L3587 · L3587](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `durable_work` \| Each service: `operations/work` and `operations/notifications` share one repository contract \| One durable scheduling/recording mechanism; typed notification evidence remains distinct |
| [TD-ROW-L5448 · L5448](../../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications) | database (default) \| None external \| Durable durable_work.notification_state=RECORDED with kind=NOTIFICATION and state=DONE; visible in restricted operator query/status. Never say sent/delivered |
| [TD-ROW-L5506 · L5506](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-04 restore \| Nonce reuse breaks GCM confidentiality/integrity \| Committed nonce allocator + new key after restore \| Crash allocation, counter exhaustion, restore old counter cannot encrypt with old key; key custody operational; Core/operator |
| [BRAIN-A04 · L49](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A04 \| Service-to-service communication via gRPC \| Real RPC calls supporting application features; protobuf contracts and integration checks |
| [BRAIN-D19 · L88](../../../01-brainstorming-result.md#4-decision-register) | D19 \| Committed \| Unit and integration tests, including real gRPC communication and persistence against the selected database engine |
| [BRAIN-D20 · L89](../../../01-brainstorming-result.md#4-decision-register) | D20 \| Committed \| Durable outbox or equivalent work table for reliable cross-service follow-up; saga coordination only when the finalized workflow needs it |
| [BRAIN-D21 · L90](../../../01-brainstorming-result.md#4-decision-register) | D21 \| Committed \| Bounded exponential backoff with jitter for safe transient failures; environment-configurable retry policy and exhaustion notification through one configured channel |
| [BRAIN-D23 · L92](../../../01-brainstorming-result.md#4-decision-register) | D23 \| Committed \| Health/readiness checks, correlated logs, metrics, dashboards, and actionable alerts; numerical SLOs and any SLA remain open |

## Implementation

1. Implement exactly one configured database/default, SMTP, Slack or Google Chat mode using the selected typed durable_work notification extension; invalid/multiple/missing-mode dependencies block readiness.
2. For database mode atomically record DONE/RECORDED with zero attempts/network sends. Do not label it delivered. External adapters use fixed allowlisted destination and protected credential files with bounded payloads/timeouts.
3. Implement authenticated private Alertmanager v4 ingress, payload cap, sanitized deduplication and durable receipt before 2xx acknowledgment. Repeated episodes/exhaustion do not recurse or cause alert storms.
4. Distinguish scheduler DONE from RECORDED/ACCEPTED/DELIVERED/FAILED/UNKNOWN evidence. Ambiguous external acceptance remains UNKNOWN and requires reconciliation without blind resend.
5. Test all mode/config/fault contracts locally with explicit doubles where appropriate; require actual provider acceptance evidence only for external modes selected for a release claim, with authorized destination/access as prerequisite.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T036-A1:** Default: exhausted eligible work yields one inspectable sanitized recording and zero sends; database mode needs no external provider credential.
- **T036-A2:** Failure: provider/receiver outage remains visible without rolling back successful business action or recursively notifying itself.
- **T036-A3:** Ambiguity: lost provider response never becomes confirmed delivery or automatic duplicate send; only genuine selected-provider receipt supports external acceptance claims.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-061](../../../05-atdd.md#case-atdd-061-exhaustion-notification) / System | Defaultdatabase then independently select each external mode with required credentials. RECORDED versus actual provider acceptance/delivery evidence perTD-U-005. | Then The selected mode records one actionable, deduplicated event with accurate receipt evidence And database mode says RECORDED without claiming external sending or delivery And external mode distinguishes provider acceptance, confirmed delivery where evidenced, uncertainty and failure Observation: Compare selected databaseRECORDED versus real independently configured provider evidence; one mode dispatches and no recording-only row claims sending/delivery. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-062](../../../05-atdd.md#case-atdd-062-notification-failure) / System | Selectedprovider explicit rejection/unavailable/ambiguous post send; ledger failure retained; no recursive alert or rollback business success. | Then The selected mode records one actionable, deduplicated event with accurate receipt evidence And database mode says RECORDED without claiming external sending or delivery And external mode distinguishes provider acceptance, confirmed delivery where evidenced, uncertainty and failure Observation: Inspect notification explicit rejection/unavailable/ambiguous acceptance, ledger/attempt state and nonrecursive failure; successful business mutation stays committed. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-063](../../../05-atdd.md#case-atdd-063-repeated-exhaustion) / System | Repeat same work generation exhaustion tenobservations and duplicate alert firing/resolved episode; oneevent/transition and bounded dispatch. | Then The selected mode records one actionable, deduplicated event with accurate receipt evidence And database mode says RECORDED without claiming external sending or delivery And external mode distinguishes provider acceptance, confirmed delivery where evidenced, uncertainty and failure Observation: Repeat the exhaustion/alert episode and inspect one deduplicated transition/event and bounded work, with distinct firing/resolved identities. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-067](../../../05-atdd.md#case-atdd-067-cancel-or-exhaust) / System | Cancelbeforeeffect, deadline expired READYrow, attempt limit row, in-flight after commit; no new sends after cancel/deadline; actual commit notundone. | Then Show cancellation or retained failure accurately; do not launch unlimited new attempts. Notify exhaustion where applicable. Observation: Cancel/expire/exhaust work at each pre-send/in-flight barrier; inspect no new sends while preserving already committed effects and truthful uncertainty. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-158](../../../05-atdd.md#case-atdd-158-preserve-original-evidence-during-controlled-recovery) / Integration | F-WORK: original parent/child IDs, generation 0, attempts0..3, controlled deadline/scheduled instant; workers W1/W2 synchronized before and after destination commit. | Then only authorized recovery with current conditions checked and an explicit reason creates a new bounded generation And previous attempts/audit and original effect IDs remain intact And uncertain provider delivery lacking reconciliation support cannot be blindly resent Observation: Use authenticated operator recovery with recorded reason and original outcome lookup. Compare generations, original child effect IDs, old attempts and new bounded budget; unknown provider acceptance cannot become a blind resend or lose its evidence. RECOVERY operational_event is unique for work/new generation, requires verified operator/reason and retains old STARTED/FINISHED phases. Failure while writing recovery cannot reset the durable budget or change original child effect identity. |
| [ATDD-193](../../../05-atdd.md#case-atdd-193-dispatch-exactly-one-selected-notification-mode) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.7 TD-U-005 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then database mode stores RECORDED with no delivery attempt or outbound send or claim of sending And external mode uses one fixed verifiedTLS destination and preserves provider reference as ACCEPTED And DELIVERY_CONFIRMED requires stronger authenticated evidence and never implies human reading; event keys deduplicate each transition Observation: Run each selected mode independently and inspect typed notification durable_work record plus actual fixed destination acceptance where claimed. Database mode is RECORDED with no provider send. Distinguish ACCEPTED, DELIVERY_CONFIRMED and human reading; duplicate event transitions create no extra recording or delivery effect. Inspect one typed NOTIFICATION durable_work row per deduplicated event. Database mode is DONE/RECORDED with zero attempts/leases/provider reference and no sends; completion is recording only. External notification_state/provider evidence is independent of scheduler state, and no second notification ledger or recursive failure task is created. |
| [ATDD-194](../../../05-atdd.md#case-atdd-194-keep-ambiguous-provider-acceptance-visible) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.7 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then queryable receipt/idempotency resolves the same effect where supported And unsupported reconciliation leaves UNKNOWN for operator review without blind resend And delivery failure remains inspectable, nonrecursive and cannot reverse business success Observation: Use real selected adapter with controlled provider fault boundary, label simulation, and obtain actual provider receipt where acceptance is claimed. Inspect UNKNOWN versus resolved outcome and retained attempts; unavailable reconciliation cannot authorize blind resend or reverse the successful business operation. UNKNOWN/FAILED typed notification evidence remains until controlled disposition even when work is exhausted. Preserve provider_reference and attempt events; unsupported reconciliation never turns READY or restart into permission for blind retransmission. |
| [ATDD-299](../../../05-atdd.md#case-atdd-299-distinguish-notification-recording-from-external-acceptance) / Unit | database, smtp, slack, google_chat; durable recording, explicit rejection, authenticated provider acceptance, stronger delivery evidence, ambiguous timeout. | Then database mode is RECORDED with no send action And ACCEPTED and DELIVERY_CONFIRMED require their respective evidence and never mean human read And ambiguous unqueryable acceptance stays UNKNOWN without blind resend Observation: Inspect returned state/action only. Durable event dedup and actual provider acceptance are integration evidence. |

Partial implementation contributions: [ATDD-195](../../../05-atdd.md#case-atdd-195-acknowledge-alerts-only-after-durable-sanitized-recording), [ATDD-227](../../../05-atdd.md#case-atdd-227-enforce-typed-notification-recording-and-delivery-evidence). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

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

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize permission/secret boundaries, races, rollback/replay, lifetime evidence and the real owner boundary specified in this task. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

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
- Reach Ready instructions and Passed evidence for every required check, including self-review, fresh independent review and applicable visual/interaction checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
