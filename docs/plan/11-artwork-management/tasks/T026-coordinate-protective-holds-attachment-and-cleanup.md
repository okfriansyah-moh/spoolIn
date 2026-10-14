---
kind: task
id: T026
group: G12
depends_on:
- T008
- T010
- T011
- T017
- T025
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T026 — Coordinate protective holds, attachment and cleanup

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-064 | implements |
| ATDD-038 | implements |
| ATDD-039 | implements |
| ATDD-040 | implements |
| ATDD-041 | implements |
| ATDD-042 | implements |
| ATDD-043 | implements |
| ATDD-044 | implements |
| ATDD-045 | implements |
| ATDD-046 | implements |
| ATDD-123 | implements |
| ATDD-124 | implements |
| ATDD-125 | implements |
| ATDD-126 | implements |
| ATDD-159 | implements |
| ATDD-160 | implements |
| ATDD-161 | implements |
| ATDD-162 | implements |
| ATDD-163 | implements |
| ATDD-164 | both |
| ATDD-165 | both |
| ATDD-166 | implements |
| ATDD-167 | both |
| ATDD-169 | implements |
| ATDD-170 | implements |
| ATDD-245 | implements |
| ATDD-245-T1-R01 | implements |
| ATDD-245-T1-R02 | implements |
| ATDD-245-T1-R03 | implements |
| ATDD-245-T1-R04 | implements |
| ATDD-245-T1-R05 | implements |
| ATDD-245-T1-R06 | implements |
| ATDD-245-T1-R07 | implements |
| ATDD-245-T1-R08 | implements |
| ATDD-245-T1-R09 | implements |
| ATDD-245-T1-R10 | implements |
| ATDD-249 | implements |
| ATDD-249-T1-R01 | implements |
| ATDD-249-T1-R02 | implements |
| ATDD-249-T1-R03 | implements |
| ATDD-249-T1-R04 | implements |
| ATDD-249-T1-R05 | implements |
| ATDD-249-T1-R06 | implements |
| ATDD-249-T1-R07 | implements |
| ATDD-251-T1-R33 | implements |
| ATDD-251-T1-R34 | implements |
| ATDD-253-T1-R24 | implements |
| ATDD-253-T1-R25 | implements |
| ATDD-253-T1-R26 | implements |
| ATDD-253-T1-R27 | implements |
| ATDD-254-T1-R02 | implements |
| ATDD-254-T1-R03 | implements |
| ATDD-254-T1-R04 | implements |
| ATDD-254-T1-R05 | implements |
| ATDD-254-T1-R07 | implements |
| ATDD-263 | implements |
| ATDD-315 | implements |
| ATDD-305 | implements |
| J-04 | implements |
| FR-002 | implements |
| FR-004 | both |
| FR-009 | implements |
| FR-010 | both |
| FR-017 | implements |
| NFR-001 | implements |
| NFR-002 | implements |
| NFR-004 | both |
| NFR-005 | implements |
| NFR-007 | implements |
| NFR-008 | implements |
| NFR-013 | implements |
| NFR-016 | implements |
| A03 | implements |
| A04 | implements |
| A05 | implements |
| A07 | both |
| A08 | both |
| A14 | implements |
| D03 | implements |
| D10 | implements |
| D11 | both |
| D19 | implements |
| D21 | both |
| AC-019 | both |
| AC-019-C01 | both |
| AC-019-C02 | both |
| AC-038 | implements |
| AC-038-C01 | implements |
| AC-038-C02 | implements |
| AC-039 | implements |
| AC-039-C01 | implements |
| AC-039-C02 | implements |
| AC-039-C03 | implements |
| AC-040 | implements |
| AC-040-C01 | implements |
| AC-041 | implements |
| AC-041-C01 | implements |
| AC-041-C02 | implements |
| AC-042 | implements |
| AC-042-C01 | implements |
| AC-043 | implements |
| AC-043-C01 | implements |
| AC-043-C02 | implements |
| AC-044 | implements |
| AC-044-C01 | implements |
| AC-044-C02 | implements |
| AC-045 | implements |
| AC-045-C01 | implements |
| AC-045-C02 | implements |
| AC-046 | both |
| AC-046-C01 | both |
| AC-068 | implements |
| AC-068-C01 | implements |
| AC-147 | implements |
| AC-147-C01 | implements |
| AC-147-C02 | implements |
| AC-147-C03 | implements |
| AC-149 | implements |
| AC-149-C01 | implements |
| AC-149-C02 | implements |
| AC-149-C03 | implements |
| AC-150 | implements |
| AC-150-C01 | implements |
| AC-150-C02 | implements |
| AC-150-C03 | implements |
| AC-151 | implements |
| AC-151-C01 | implements |
| AC-151-C02 | implements |
| AC-151-C03 | implements |
| AC-152 | implements |
| AC-152-C01 | implements |
| AC-152-C02 | implements |
| AC-152-C03 | implements |
| AC-157 | implements |
| AC-157-C01 | implements |
| AC-157-C02 | implements |
| AC-065 | implements |
| AC-066 | implements |
| AC-087 | implements |
| AC-103 | implements |
| G-02 | implements |
| G-03 | both |
| G-04 | both |
| G-05 | implements |
| AC-075 | both |
| AC-075-C01 | both |
| AC-075-C02 | both |
| OD-004 | both |
| OD-007 | implements |
| OD-008 | both |
| OD-019 | implements |
| US-001 | implements |
| US-007 | implements |
| US-011 | implements |
| US-021 | implements |
| PRD-PERM-03 | implements |
| PRD-POLICY-06 | implements |
| PRD-POLICY-13 | implements |
| PRD-POLICY-27 | implements |
| PRD-POLICY-28 | implements |
| PRD-POLICY-29 | implements |
| PRD-POLICY-30 | implements |
| PRD-POLICY-31 | implements |
| PRD-POLICY-32 | implements |
| PRD-FEEDBACK-02 | implements |
| PRD-CONCEPT-01 | implements |
| PRD-CONCEPT-03 | implements |
| PRD-HANDOFF-01 | implements |
| PRD-HANDOFF-02 | implements |
| PRD-HANDOFF-08 | implements |
| PRD-HANDOFF-13 | implements |
| TD-C-033 | both |
| TD-C-036 | verifies |
| TD-C-045 | both |
| TD-ROW-L2756 | both |
| TD-ROW-L2757 | both |
| TD-ROW-L4814 | both |
| TD-ROW-L4815 | both |
| TD-ROW-L4816 | both |
| TD-ROW-L4817 | both |
| TD-ROW-L4818 | both |
| TD-ROW-L4819 | both |
| TD-ROW-L4820 | both |
| TD-ROW-L4821 | both |
| TD-ROW-L4822 | both |
| TD-ROW-L4823 | both |
| TD-ROW-L5096 | both |
| TD-ROW-L5097 | both |
| TD-ROW-L5098 | both |
| TD-ROW-L5099 | both |
| TD-ROW-L5100 | both |
| TD-ROW-L5101 | both |
| TD-ROW-L5102 | both |
| BRAIN-A07 | both |
| BRAIN-A08 | both |
| BRAIN-D11 | both |
| BRAIN-D21 | both |

## Prerequisites

- [T008 — Enforce scoped service identity and shared admission](../../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T010 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T011 — Run fenced recovery and bounded retries](../../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.
- [T017 — Implement authoritative movie creation and edits](../../09-movie-management/tasks/T017-implement-authoritative-movie-creation-and-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.
- [T025 — Accept and process bounded artwork streams](T025-accept-and-process-bounded-artwork-streams.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/artwork/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/resources/db/migration/V004__artwork_attachment.sql | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/features/hold/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/features/cleanup/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/artworkproxy/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/artwork/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/artwork/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/features/hold/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/features/cleanup/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/artworkproxy/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [5.5 Browser HTTP and operational endpoints](../../../04-spoolin-techdoc.md#55-browser-http-and-operational-endpoints): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.6 Media tables](../../../04-spoolin-techdoc.md#626-media-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.6 Artwork transfer and reference-safe attachment](../../../04-spoolin-techdoc.md#76-artwork-transfer-and-reference-safe-attachment): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.7 Durable work, retries and notifications](../../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-245-T1-R01 · L10019](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY REFERENCES operation_receipt(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-245-T1-R02 · L10020](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `title_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-245-T1-R03 · L10021](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `actor_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-245-T1-R04 · L10022](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `expected_version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (expected_version >= 0); no SQL default; owner supplies value` |
| [ATDD-245-T1-R05 · L10023](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `asset_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-245-T1-R06 · L10024](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `old_hold_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-245-T1-R07 · L10025](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `state` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (state IN ('PREPARING','ATTACHED','ABORTED')); no SQL default; owner supplies value` |
| [ATDD-245-T1-R08 · L10026](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `policy_revision` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-245-T1-R09 · L10027](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-245-T1-R10 · L10028](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-249-T1-R01 · L10244](../../../05-atdd.md#case-atdd-249-enforce-multiple-protected-artwork-reference-promises) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-249-T1-R02 · L10245](../../../05-atdd.md#case-atdd-249-enforce-multiple-protected-artwork-reference-promises) | `asset_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES asset(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-249-T1-R03 · L10246](../../../05-atdd.md#case-atdd-249-enforce-multiple-protected-artwork-reference-promises) | `title_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-249-T1-R04 · L10247](../../../05-atdd.md#case-atdd-249-enforce-multiple-protected-artwork-reference-promises) | `state` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (state IN ('RESERVED','ACTIVE','RELEASED')); no SQL default; owner supplies value` |
| [ATDD-249-T1-R05 · L10248](../../../05-atdd.md#case-atdd-249-enforce-multiple-protected-artwork-reference-promises) | `catalog_version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (catalog_version >= 0); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-249-T1-R06 · L10249](../../../05-atdd.md#case-atdd-249-enforce-multiple-protected-artwork-reference-promises) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-249-T1-R07 · L10250](../../../05-atdd.md#case-atdd-249-enforce-multiple-protected-artwork-reference-promises) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-251-T1-R33 · L10379](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `uploadArtwork`; Maintainer / Catalog intent + Core/Media stream \| Active movie/current version/policy; exact bounded base64; PENDING until safe attachment; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R34 · L10380](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `cancelArtwork`; Current Maintainer owning upload / Catalog \| Separate cancellation UUID; atomically abort preparing intent, preserve attached result if race lost; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-253-T1-R24 · L10488](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `BeginArtwork`; BeginArtwork(Hold) → Outcome |
| [ATDD-253-T1-R25 · L10489](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `GetAttachment`; GetAttachment(Id) → Attachment |
| [ATDD-253-T1-R26 · L10490](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `CancelArtwork`; CancelArtwork(CancelArtworkRequest) → ArtworkCancelOutcome |
| [ATDD-253-T1-R27 · L10491](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ResolveArtworkAccess`; ResolveArtworkAccess(ArtworkAccessRequest) → ArtworkGrant |
| [ATDD-254-T1-R02 · L10537](../../../05-atdd.md#case-atdd-254-verify-mediaservice-typed-service-and-actor-boundary) | `GetAsset`; GetAsset(Id) → Asset |
| [ATDD-254-T1-R03 · L10538](../../../05-atdd.md#case-atdd-254-verify-mediaservice-typed-service-and-actor-boundary) | `Reserve`; Reserve(Hold) → Attachment |
| [ATDD-254-T1-R04 · L10539](../../../05-atdd.md#case-atdd-254-verify-mediaservice-typed-service-and-actor-boundary) | `Confirm`; Confirm(Hold) → Attachment |
| [ATDD-254-T1-R05 · L10540](../../../05-atdd.md#case-atdd-254-verify-mediaservice-typed-service-and-actor-boundary) | `Release`; Release(Hold) → Attachment |
| [ATDD-254-T1-R07 · L10542](../../../05-atdd.md#case-atdd-254-verify-mediaservice-typed-service-and-actor-boundary) | `ReadAsset`; ReadAsset(AssetReadRequest) → stream AssetReadChunk |
| [J-04 · L92](../../../02-PRD.md#32-application-journeys) | Maintainer uploads/replaces artwork \| Active movie exists; authorized Maintainer selects a file \| See transfer, processing, and confirmed attachment; preserve existing artwork on replacement failure |
| [FR-002 · L238](../../../02-PRD.md#61-functional-requirements) | Search movie titles and cast/creator names with the approved matching and visibility rules \| A11/A14 Required; product policy Committed (section 13.1) \| AC-006 through AC-010, AC-022, AC-134, AC-147, AC-159 |
| [FR-004 · L240](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [FR-009 · L245](../../../02-PRD.md#61-functional-requirements) | Upload movie artwork with validation and distinct transfer/processing/saved feedback \| A08/A14 Required \| AC-038 through AC-042, AC-149 through AC-151 |
| [FR-010 · L246](../../../02-PRD.md#61-functional-requirements) | Recover failed/uncertain artwork changes while protecting prior and still-used artwork \| D20/D21 Committed; product policy Committed (section 13.1); D11 mechanics Open \| AC-043 through AC-046, AC-152, AC-155, AC-157 |
| [FR-017 · L253](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-001 · L261](../../../02-PRD.md#62-quality-and-delivery-requirements) | Required assessment stack and real frontend/backend protocols \| A01 through A05, D03 Required \| AC-064 through AC-066; section 10 |
| [NFR-002 · L262](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preferred App/Catalog/Media ownership with unrelated catalog work usable during artwork outage \| D02/D10 Working design \| AC-067, AC-068; section 10 |
| [NFR-004 · L264](../../../02-PRD.md#62-quality-and-delivery-requirements) | Safe recovery is bounded and respects cancellation; uncertain outcomes cannot multiply effects \| D21 Committed; D22 defaults Recommended \| AC-072 through AC-075 |
| [NFR-005 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Setup rejects unusable configuration and protects secrets \| D13/D21; configuration and secret protection Committed \| AC-076 through AC-078, AC-153 through AC-155, AC-157 |
| [NFR-007 · L267](../../../02-PRD.md#62-quality-and-delivery-requirements) | Responsive, keyboard-usable journeys with accurate feedback and retained recoverable work \| A13/A14 Required; keyboard use Committed \| AC-083 through AC-085, AC-139, AC-147, AC-159 |
| [NFR-008 · L268](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-013 · L273](../../../02-PRD.md#62-quality-and-delivery-requirements) | Interfaces implement the product's validation, optional data, access, and failure behavior \| A04/A05/A14 Required support; detailed contracts Open \| AC-100 through AC-103, AC-142/AC-143, AC-148 |
| [NFR-016 · L276](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [A03 · L284](../../../02-PRD.md#63-assessment-coverage) | At least two meaningful Spring Boot services \| NFR-001 / AC-065 |
| [A04 · L285](../../../02-PRD.md#63-assessment-coverage) | Real service-to-service gRPC \| NFR-001/NFR-008 / AC-065, AC-087 |
| [A05 · L286](../../../02-PRD.md#63-assessment-coverage) | Frontend application communication through GraphQL \| NFR-001/NFR-013 / AC-066, AC-100 |
| [A07 · L288](../../../02-PRD.md#63-assessment-coverage) | Add, update, delete movies \| FR-004 / AC-016 through AC-021, AC-108 through AC-110, AC-130 |
| [A08 · L289](../../../02-PRD.md#63-assessment-coverage) | Upload movie artwork \| FR-009/FR-010 / AC-038 through AC-046 |
| [A14 · L295](../../../02-PRD.md#63-assessment-coverage) | Sufficient operation feedback \| NFR-007; FR-001/FR-002/FR-004/FR-009 / AC-002, AC-009, AC-018, AC-038, AC-085 |
| [D03 · L306](../../../02-PRD.md#64-project-decision-register) | GraphQL frontend and gRPC backend boundaries \| NFR-001/NFR-013 / AC-065, AC-066, AC-100 |
| [D10 · L313](../../../02-PRD.md#64-project-decision-register) | Catalog owns titles/people/credits, Media owns assets \| NFR-002 / AC-067, AC-068 |
| [D11 · L314](../../../02-PRD.md#64-project-decision-register) | Artwork transport and lifecycle protocol \| Product outcomes settled; mechanics Open in techdoc. FR-009/FR-010 / AC-038 through AC-046 |
| [D19 · L322](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [D21 · L324](../../../02-PRD.md#64-project-decision-register) | Bounded safe retries and one configured exhaustion channel \| NFR-004/NFR-005, FR-015 / AC-061 through AC-063, AC-072 through AC-078 |
| [AC-019 · L373](../../../02-PRD.md#us-003-maintain-movies) | Permanent deletion \| A movie is archived and a Maintainer sees its deletion consequences \| They separately confirm permanent deletion \| Remove it from the archive and catalog permanently, remove its credits, preserve shared people and other titles' artwork, and retain an attributable deletion record. No restore action remains. |
| [AC-019-C01 · L373](../../../02-PRD.md#us-003-maintain-movies) | Remove it from the archive and catalog permanently, remove its credits, preserve shared people and other titles' artwork, and retain an attributable deletion record. |
| [AC-019-C02 · L373](../../../02-PRD.md#us-003-maintain-movies) | No restore action remains. |
| [AC-038 · L421](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Progress \| A Maintainer selects a supported file \| Transfer and processing run \| Show measured transfer progress, separate processing, then saved only after the movie displays the confirmed attachment. Transfer at 100% is not a saved result. |
| [AC-038-C01 · L421](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show measured transfer progress, separate processing, then saved only after the movie displays the confirmed attachment. |
| [AC-038-C02 · L421](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Transfer at 100% is not a saved result. |
| [AC-039 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Invalid file \| Content is empty, malformed, animated, disguised, unsupported, unsafe, or outside displayed limits \| Upload is attempted \| Explain the applicable format/size/content/processing-limit issue and attach nothing; preserve previous artwork. Enforce section 5.5 against actual content, not filename or declared type alone. The techdoc defines validation mechanisms and processing limits. |
| [AC-039-C01 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Explain the applicable format/size/content/processing-limit issue and attach nothing; preserve previous artwork. |
| [AC-039-C02 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Enforce section 5.5 against actual content, not filename or declared type alone. |
| [AC-039-C03 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | The techdoc defines validation mechanisms and processing limits. |
| [AC-040 · L423](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Permission/target changes \| Permission expires or the movie is archived/deleted during upload \| Attachment is attempted \| Do not attach to an unauthorized/unavailable target; explain the outcome and preserve the previous valid artwork. |
| [AC-040-C01 · L423](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not attach to an unauthorized/unavailable target; explain the outcome and preserve the previous valid artwork. |
| [AC-041 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Interrupted upload \| Transfer is cancelled/interrupted or storage fails \| The operation ends \| Show cancellation/failure without claiming success, preserve the previous artwork, and allow a fresh attempt once the outcome is known. Resume-from-byte support is not required. |
| [AC-041-C01 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show cancellation/failure without claiming success, preserve the previous artwork, and allow a fresh attempt once the outcome is known. |
| [AC-041-C02 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Resume-from-byte support is not required. |
| [AC-042 · L425](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Repeated upload \| A completed/pending upload is repeated \| Requests are processed \| Do not create unintended duplicate attachments; show the known result or pending outcome instead of treating each repeat as a new success. |
| [AC-042-C01 · L425](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not create unintended duplicate attachments; show the known result or pending outcome instead of treating each repeat as a new success. |
| [AC-043 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Upload succeeded, attachment failed \| File transfer/processing completed but the movie was not updated \| The Maintainer sees the result \| Clearly report that artwork was not attached; keep prior artwork, and offer a safe retry after confirming the failed outcome. No unattached image becomes public. |
| [AC-043-C01 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Clearly report that artwork was not attached; keep prior artwork, and offer a safe retry after confirming the failed outcome. |
| [AC-043-C02 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | No unattached image becomes public. |
| [AC-044 · L427](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Replacement \| A movie already has valid artwork \| Replacement succeeds or fails \| Success displays the new confirmed image; failure preserves the previous image. Later cleanup cannot remove another title's valid artwork. |
| [AC-044-C01 · L427](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Success displays the new confirmed image; failure preserves the previous image. |
| [AC-044-C02 · L427](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Later cleanup cannot remove another title's valid artwork. |
| [AC-045 · L428](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Uncertain attachment \| Confirmation is lost after submission \| The Maintainer checks status \| Show uncertainty while resolving the result; display success or a recoverable failure only when known. Do not ask for a blind repeat or delete possibly attached artwork. |
| [AC-045-C01 · L428](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show uncertainty while resolving the result; display success or a recoverable failure only when known. |
| [AC-045-C02 · L428](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not ask for a blind repeat or delete possibly attached artwork. |
| [AC-046 · L429](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Interrupted cleanup \| Cleanup/recovery overlaps another valid use or a restart \| Work resumes \| Existing attachments stay usable; recovery does not remove referenced artwork or report missing output as completed. |
| [AC-046-C01 · L429](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Existing attachments stay usable; recovery does not remove referenced artwork or report missing output as completed. |
| [AC-068 · L467](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | Artwork outage \| Artwork handling is unavailable but catalog work is available \| A Maintainer edits movie metadata \| Unrelated metadata work can succeed; image-dependent work explains its failure without claiming attachment success. |
| [AC-068-C01 · L467](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | Unrelated metadata work can succeed; image-dependent work explains its failure without claiming attachment success. |
| [AC-147 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Plain-text safety \| Entered catalog text, names, search text, or changed values contain markup, apostrophes, or instruction-like content within applicable limits \| They appear in forms, catalog/detail pages, search results, previews, or audit history \| Treat the content as text and execute no embedded code or instructions. Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. Private registration data retain their access boundaries. |
| [AC-147-C01 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Treat the content as text and execute no embedded code or instructions. |
| [AC-147-C02 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. |
| [AC-147-C03 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Private registration data retain their access boundaries. |
| [AC-149 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Artwork byte boundaries \| Recommended file bounds of 1 to 5,242,880 bytes are configured \| A Maintainer submits an empty file, a one-byte invalid file, a valid supported image exactly at the maximum, or a file one byte above it \| Reject empty/undecodable content and the oversized file with the specific reason; the exact maximum passes the size check subject to all content/processing checks. Attach no rejected image and preserve previous artwork. MiB is displayed with its exact byte interpretation. |
| [AC-149-C01 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject empty/undecodable content and the oversized file with the specific reason; the exact maximum passes the size check subject to all content/processing checks. |
| [AC-149-C02 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Attach no rejected image and preserve previous artwork. |
| [AC-149-C03 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | MiB is displayed with its exact byte interpretation. |
| [AC-150 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Static supported formats \| JPEG, PNG, and WebP are enabled and an active movie may lack artwork \| A Maintainer uploads valid static examples or attempts GIF, SVG, animated PNG/WebP, or another unsupported format \| Supported static files proceed through upload/processing to confirmed attachment if other checks pass. Reject unsupported or animated content with correction feedback and no attachment; do not silently convert animation into an accepted static upload. Missing artwork remains a placeholder on failure. |
| [AC-150-C01 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Supported static files proceed through upload/processing to confirmed attachment if other checks pass. |
| [AC-150-C02 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject unsupported or animated content with correction feedback and no attachment; do not silently convert animation into an accepted static upload. |
| [AC-150-C03 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Missing artwork remains a placeholder on failure. |
| [AC-151 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Disguised, malformed, or unsafe image \| A file has a permitted extension or declared type but invalid actual content, or a small compressed image exceeds the adopted processing bounds \| Artwork is validated or processed \| Reject it without attachment or unbounded processing; preserve prior artwork and report a safe content/processing-limit failure. Filename, declared type, or signature alone cannot bypass validation. Numerical processing bounds are established under OD-007/OD-019 before implementation. |
| [AC-151-C01 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject it without attachment or unbounded processing; preserve prior artwork and report a safe content/processing-limit failure. |
| [AC-151-C02 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Filename, declared type, or signature alone cannot bypass validation. |
| [AC-151-C03 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Numerical processing bounds are established under OD-007/OD-019 before implementation. |
| [AC-152 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Replacement failure and correction \| A movie has valid artwork and a replacement is rejected or fails processing/attachment \| The Maintainer inspects the result and selects a corrected image after the failed outcome is known \| Existing artwork and unrelated movie information remain available; show the failure and effective rules, then allow a safe fresh attempt. Confirm a replacement only after attachment. Uncertain outcomes still require reconciliation under AC-045 before repeat submission. |
| [AC-152-C01 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing artwork and unrelated movie information remain available; show the failure and effective rules, then allow a safe fresh attempt. |
| [AC-152-C02 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Confirm a replacement only after attachment. |
| [AC-152-C03 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Uncertain outcomes still require reconciliation under AC-045 before repeat submission. |
| [AC-157 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Policy changes during a draft/upload \| Input or artwork met earlier limits, and the policy changes before submission/attachment \| The operation is evaluated against the effective policy \| If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-157-C01 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. |
| [AC-157-C02 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-065 · L858](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise meaningful real gRPC features across at least two running Spring Boot applications \| NFR-001; technical design |
| [AC-066 · L859](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify frontend GraphQL and documented upload/retrieval/auth exceptions against A05; reconcile the MCP boundary \| NFR-001/NFR-013; technical design |
| [AC-087 · L872](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration \| NFR-008; technical design |
| [AC-103 · L881](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify effective input/file policies from section 5.5; establish remaining request/query/page and decoded-image resource bounds with understandable outcomes \| NFR-013/NFR-016; technical design |
| [G-02 · L925](../../../02-PRD.md#122-release-gates-remain-planned) | Mandated stack/protocols and chosen integration approach work \| NFR-001/NFR-002/NFR-013; AC-064 through AC-067, AC-100 through AC-103 \| Planned in techdoc |
| [G-03 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-04 · L927](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
| [G-05 · L928](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [AC-075 · L988](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Cancel or exhaust \| The user cancels or the allowed recovery period/attempt budget ends \| Recovery stops \| Show cancellation or retained failure accurately; do not launch unlimited new attempts. Notify exhaustion where applicable. |
| [AC-075-C01 · L988](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Show cancellation or retained failure accurately; do not launch unlimited new attempts. |
| [AC-075-C02 · L988](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Notify exhaustion where applicable. |
| [OD-004 · L1022](../../../02-PRD.md#131-resolved-product-decisions) | Movies and people require archiving before separately confirmed permanent deletion. Person archiving retains credits privately; restoration reveals only surviving credits on active titles. Deletion preserves associated titles and movie deletion preserves shared people. \| FR-004/FR-005/FR-006; AC-019/AC-024/AC-108 through AC-110, AC-129 through AC-138 |
| [OD-007 · L1047](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Upload UX, supported static formats, configurable size policy, and rejection/recovery outcomes fixed in section 5.5; adoption of Recommended byte bounds, transport/A05 interpretation, content checks, decoded-dimension/resource bounds, processing, storage/retrieval remain for techdoc \| FR-009/FR-010/FR-017, NFR-001/NFR-013/NFR-016 \| Technical design; before artwork implementation |
| [OD-008 · L1048](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Replacement/recovery outcomes fixed; attachment/reference/cleanup/retention and compensation mechanics remain for techdoc \| FR-010/FR-014 \| Technical design; before lifecycle implementation |
| [OD-019 · L1057](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Product discovery/feedback and section 5.5 input/artwork policy settled; GraphQL/gRPC and MCP contracts, human-approval enforcement, consistent counting/validation, safe display/data handling, compatibility, remaining request/query/page and decoding resource bounds, ordering ties/navigation, responsive support matrix, and accessibility verification remain for techdoc \| NFR-007/NFR-013/NFR-016, FR-016/FR-017 \| Technical design; before dependent implementation |
| [US-001 · L335](../../../02-PRD.md#us-001-browse-titles-and-people) | ### US-001: Browse titles and people As a visitor or Viewer, I want to inspect titles, artwork, and credits so I can identify the work and its people. Requirements: FR-001, NFR-007. Reading needs no account and changes no catalog information. |
| [US-007 · L415](../../../02-PRD.md#us-007-upload-and-recover-artwork) | ### US-007: Upload and recover artwork As a Maintainer, I want to know which image a movie actually uses and recover from failed uploads. Requirements: FR-009, FR-010. Artwork management applies to active movies; current permissions are required throughout the operation. |
| [US-011 · L461](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | ### US-011: Continue metadata work during an artwork outage As a Maintainer, I want to edit movie metadata when artwork handling is unavailable so I can continue work that does not need an image change. Requirement: NFR-002. Related assessment technology constraints NFR-001 and AC-064 through AC-067 remain in section 10. |
| [US-021 · L525](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | ### US-021: Submit accepted input and correct validation failures As a Visitor registering or searching, or a Maintainer entering catalog information or artwork, I want visible limits and specific correction feedback so I can complete a permitted action without losing valid work. Requirements: FR-017, NFR-016, NFR-005, NFR-007; related capabilities FR-002/FR-004/FR-005/FR-006/FR-009/FR-010/FR-011/FR-016. Section 5.5 defines the policies and their statuses. Selected name defaults are Committed; other numerical defaults remain Recommended. Scenarios using recommended numbers assume those values have been configured. Configuration verification concerns restricted deployment facilities, not another application role or settings screen. |
| [PRD-PERM-03 · L72](../../../02-PRD.md#31-permission-matrix) | Create/update movies and manage their credits/artwork \| No \| No \| Yes |
| [PRD-POLICY-06 · L128](../../../02-PRD.md#52-removal-and-editing-rules) | Archiving hides a movie from public/Viewer listings, search, filters, direct detail access, and public credit views. It preserves the movie's remaining metadata, artwork, and credits in the Maintainer-only archive. A Maintainer may restore it or permanently delete it with a separate confirmation that identifies the movie and irreversibility. There is no direct permanent-delete action for active movies and no restore after permanent deletion. Archive editing is limited to restore/permanent delete; restore first to edit movie content. |
| [PRD-POLICY-13 · L142](../../../02-PRD.md#52-removal-and-editing-rules) | A failed artwork replacement keeps the current valid image. A confirmed replacement changes the visible image; any later cleanup must not undo it. Transfer completion alone is not attachment success. Unattached or uncertain artwork remains outside public display until resolved. |
| [PRD-POLICY-27 · L206](../../../02-PRD.md#artwork-policy) | Artwork upload and replacement are Maintainer capabilities for active movies. Existing progress, permissions, attachment, and recovery rules remain binding. |
| [PRD-POLICY-28 · L215](../../../02-PRD.md#artwork-policy) | The maximum is an app recommendation informed by Supabase's standard-upload guidance, which recommends that method for files up to 6 MB. The recommendation does not select a storage provider or upload transport, and it is not a universal poster standard. A one-byte minimum merely rejects empty content; every accepted file must also be a valid, decodable supported image. |
| [PRD-POLICY-29 · L217](../../../02-PRD.md#artwork-policy) | File-size bounds are configurable in bytes and inclusive. Display the effective size limits and enabled formats before file selection. Validate actual content and decodability; an extension, filename, declared content type, or signature alone cannot establish validity. An unsupported, animated, malformed, disguised, or unsafe file attaches nothing. Explain the applicable format, size, content, or processing-limit issue and let the Maintainer select a corrected file. A failed or rejected replacement preserves current artwork and movie information. Distinguish uploading, processing, saved, failed, and uncertain outcomes; transfer completion does not bypass processing or confirm attachment. |
| [PRD-POLICY-30 · L219](../../../02-PRD.md#artwork-policy) | Require bounded image processing even when compressed file size is within its limit. Numerical limits on decoded dimensions and resource consumption, content-handling safeguards, and image-processing mechanisms remain OD-007/OD-019 technical-design matters before artwork implementation. OWASP file-upload guidance supports content checks and processing safeguards; a byte limit alone does not establish upload safety. |
| [PRD-POLICY-31 · L223](../../../02-PRD.md#configuration-and-existing-content) | Configuration must supply finite whole-number bounds in the appropriate units, with nonnegative text bounds, positive file-size bounds, and minimum no greater than maximum. A required field cannot have a zero effective minimum. Enabled artwork formats must be a nonempty subset of the supported static formats. An unset setting follows its deliberately documented default; an explicitly empty, invalid, or unsupported setting is not treated as unset. Negative values, reversed bounds, and unsupported formats prevent affected functionality from becoming ready, with actionable redacted diagnostics and no silent unlimited or permissive fallback. OD-020 owns configuration keys, sources, precedence, application/reload timing, and distribution of the effective policy. |
| [PRD-POLICY-32 · L225](../../../02-PRD.md#configuration-and-existing-content) | Changes to limits do not automatically truncate stored content, remove artwork, or invalidate existing password credentials. Existing records and artwork remain readable under their permissions. An unchanged stored field may be retained while other fields are edited; newly entered or changed values must meet current limits. Explicitly clearing an optional field remains allowed. Existing credentials must not be rejected solely because a new-password minimum changed. This compatibility rule does not exempt malicious new submissions or permit unsupported new artwork. |
| [PRD-FEEDBACK-02 · L798](../../../02-PRD.md#810-shared-feedback-rules) | Empty / unknown \| Distinguish empty catalog, no search matches, no audit events, unknown optional metadata, and missing artwork |
| [PRD-CONCEPT-01 · L811](../../../02-PRD.md#9-conceptual-information-and-privacy) | Movie / TV series \| Stable identity, separate title type, display title, original title where different, descriptive metadata, optional artwork, and public credits. Names alone do not identify a work. |
| [PRD-CONCEPT-03 · L813](../../../02-PRD.md#9-conceptual-information-and-privacy) | Optional context \| Original language, production countries, movie runtime, TV status/end context where applicable and known, source-scoped external identifiers, and attributed artwork where available. TV duration means episode runtime. |
| [PRD-HANDOFF-01 · L836](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | A01 through A05; D03 \| Required \| Svelte/TypeScript, Kotlin/Spring Boot, at least two meaningful services, frontend application GraphQL, and real backend gRPC. Document any artwork/auth transport interpretation. |
| [PRD-HANDOFF-02 · L837](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D02/D10 \| Working design \| Preferred App/Catalog/Media ownership, domain authorization, separate data ownership, and unrelated catalog work independent of artwork availability. It is not an external three-service requirement. |
| [PRD-HANDOFF-08 · L843](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D13 \| Committed \| Docker Compose and `make start`, `make stop`, `make restart`, `make status`, `make logs`, `make test-unit`, `make test-integration`, with documented command behavior, isolated tests, readiness checks, failing exits, and persistent data/artwork. |
| [PRD-HANDOFF-13 · L848](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | Input policy; FR-017/NFR-016 \| Committed behavior and name defaults; other numerical defaults Recommended \| Implement section 5.5 with authoritative validation across entry paths, Unicode-code-point counting, plain-text display, safe data handling, inclusive configurable limits, and compatibility for retained data. Establish adopted email/URL/password rules, remaining field/collection bounds, upload content checks and bounded decoding, configuration distribution/application timing, and meaningful boundary/security verification. Configuration/library/transport choices remain open; do not introduce extra UI or turn recommended values into external obligations. |
| [TD-C-033 · L4804](../../../04-spoolin-techdoc.md#artwork_attachment_request) | Catalog artwork_attachment_request durable PREPARING/ATTACHED/ABORTED receipt-linked intent and outcome tombstone, title/actor/version/asset/policy references and recovery indexes. |
| [TD-C-036 · L5002](../../../04-spoolin-techdoc.md#626-media-tables) | Media asset single processing metadata group/unique work and upload identities plus independent asset_reference holds; READY required metadata, all-or-none processing, RESERVED/ACTIVE protection, lifetime deletion/reference tombstones. |
| [TD-C-045 · L5414](../../../04-spoolin-techdoc.md#artwork-upload-and-attachment-sequence) | Reserve protective READY hold before Catalog attach; actor/version/policy recheck and atomic reference/audit/confirm/old-release; cancellation race retains outcome; no time-only hold expiry, safe DB/filesystem gap recovery and authorized current-tuple read. |
| [TD-ROW-L2756 · L2756](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `uploadArtwork` \| Maintainer / Catalog intent + Core/Media stream \| Active movie/current version/policy; exact bounded base64; PENDING until safe attachment |
| [TD-ROW-L2757 · L2757](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `cancelArtwork` \| Current Maintainer owning upload / Catalog \| Separate cancellation UUID; atomically abort preparing intent, preserve attached result if race lost |
| [TD-ROW-L4814 · L4814](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `id` \| `uuid` \| `PRIMARY KEY REFERENCES operation_receipt(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Same UUID as initiating operation/hold. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4815 · L4815](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `title_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Logical ID retained after title deletion. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4816 · L4816](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `actor_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Authorized account. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4817 · L4817](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `expected_version` \| `bigint` \| `NOT NULL CHECK (expected_version >= 0); no SQL default; owner supplies value` \| Version at selection. internal; presence/clearing §5 \| `Long` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4818 · L4818](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `asset_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| New logical Media asset. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4819 · L4819](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `old_hold_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Previous attachment to release only after new commit. internal; presence/clearing §5 \| `UUID?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4820 · L4820](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `state` \| `varchar(16)` \| `NOT NULL CHECK (state IN ('PREPARING','ATTACHED','ABORTED')); no SQL default; owner supplies value` \| Terminal decision forbids later attach after ABORTED. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4821 · L4821](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `policy_revision` \| `varchar(64)` \| `NOT NULL; no SQL default; owner supplies value` \| Effective media validation policy at commit. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4822 · L4822](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4823 · L4823](../../../04-spoolin-techdoc.md#artwork_attachment_request) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L5096 · L5096](../../../04-spoolin-techdoc.md#asset_reference) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Same UUID as Catalog artwork_attachment_request, immutable. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5097 · L5097](../../../04-spoolin-techdoc.md#asset_reference) | `asset_id` \| `uuid` \| `NOT NULL REFERENCES asset(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Protected object. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5098 · L5098](../../../04-spoolin-techdoc.md#asset_reference) | `title_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Logical Catalog title. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5099 · L5099](../../../04-spoolin-techdoc.md#asset_reference) | `state` \| `varchar(16)` \| `NOT NULL CHECK (state IN ('RESERVED','ACTIVE','RELEASED')); no SQL default; owner supplies value` \| RELEASED is terminal; no reactivation. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5100 · L5100](../../../04-spoolin-techdoc.md#asset_reference) | `catalog_version` \| `bigint` \| `CHECK (catalog_version >= 0); NULL allowed; no SQL default; owner supplies value` \| Attachment commit version when active. internal; presence/clearing §5 \| `Long?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5101 · L5101](../../../04-spoolin-techdoc.md#asset_reference) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L5102 · L5102](../../../04-spoolin-techdoc.md#asset_reference) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [BRAIN-A07 · L54](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A07 \| Add, delete, and update movies \| Working operations with validation and understandable outcomes |
| [BRAIN-A08 · L55](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A08 \| Upload movie artwork \| Valid upload, progress, processing/success feedback, and failure handling |
| [BRAIN-D11 · L82](../../../01-brainstorming-result.md#4-decision-register) | D11 \| Open \| Select artwork transport, attachment/replacement protocol, and recovery/cleanup policy |
| [BRAIN-D21 · L92](../../../01-brainstorming-result.md#4-decision-register) | D21 \| Committed \| Bounded exponential backoff with jitter for safe transient failures; environment-configurable retry policy and exhaustion notification through one configured channel |

## Implementation

1. Implement Catalog PREPARING/ATTACHED/ABORTED attachment intent tied to actor/title/version/policy and original operation. Core performs bounded GraphQL Base64 spool→gRPC transfer and never owns final attachment truth.
2. Reserve a protective READY asset hold before Catalog selects it. Recheck live actor/target/current policy, then commit reference/audit/receipt/confirm-new/release-old work atomically; no DB lock spans Media call.
3. Implement confirm/release/idempotent hold outcomes with exact asset/title/operation tuple and no time-only expiration of unresolved reservations. Cleanup checks all reserved/active references and destination truth.
4. Serialize cancellation against the attachment decision with a separate cancellation UUID; a race-winning committed attachment remains attached. Response loss resolves original Catalog receipt instead of deleting uncertain artwork.
5. Serve processed bytes only via Core after current Catalog authorization/tuple recheck with prescribed no-store/content headers; independently reconcile files, holds and database state across faults.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T026-A1:** Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.
- **T026-A2:** Cancel: either abort wins with no new attachment or attach wins truthfully; a stale/delayed release cannot undo the winner.
- **T026-A3:** Retrieval: public/Viewer cannot fetch private/archived/unattached assets by guessed ID or stale URL; Media outage leaves unrelated metadata usable.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-164](../../../05-atdd.md#case-atdd-164-preserve-a-committed-attachment-during-finalization-outage) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.6 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then NEW reference/version/audit/confirm and old-release obligations survive atomically And the saved receipt remains successful even while bytes are temporarily unavailable And recovery never undoes the selected hold or deletes artwork used by M-B Observation: Pause Media confirmation after actual Catalog reference commit. Compare title version, attachment ID, selected asset and durable confirmation/release work across restart; a missing acknowledgment cannot erase the committed reference or release its protective hold. |
| [ATDD-165](../../../05-atdd.md#case-atdd-165-resolve-cancel-and-attachment-under-one-decision-lock) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §5.6/7.7 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then cancel winning PREPARING commits ABORTED and original FAILED/CANCELLED with audit/reconciliation work And attachment winning returns cancelled=false and original success without undo or selected-hold release And unknown intent returns NOT_FOUND without fabricating an abort and repeats resolve their original outcomes Observation: Coordinate CancelArtwork and attachment transactions in both winning orders. Inspect cancellation receipt, original upload outcome, Catalog intent/work and selected hold/bytes. Repeat and unknown-intent rows return their specified stable outcomes without extra undo. |
| [ATDD-167](../../../05-atdd.md#case-atdd-167-exclude-protected-artwork-from-deletion-races) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.6 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then any RESERVED or ACTIVE hold prevents deletion And committed DELETING rejects future reserve, unlink is idempotent and DELETED retains a tombstone And orphan cleanup cross-checks accepted jobs/intents before removing bytes; referenced assets survive Observation: Synchronize actual reservation and cleanup under Media asset/hold locks, kill immediately before/after unlink and restart. Inspect bytes and DELETING/DELETED tombstones; any protective hold preserves bytes and future reserve cannot revive a DELETING asset. |

Partial implementation contributions: [ATDD-064](../../../05-atdd.md#case-atdd-064-artwork-outage), [ATDD-038](../../../05-atdd.md#case-atdd-038-progress), [ATDD-039](../../../05-atdd.md#case-atdd-039-invalid-file), [ATDD-040](../../../05-atdd.md#case-atdd-040-permissiontarget-changes), [ATDD-041](../../../05-atdd.md#case-atdd-041-interrupted-upload), [ATDD-042](../../../05-atdd.md#case-atdd-042-repeated-upload), [ATDD-043](../../../05-atdd.md#case-atdd-043-upload-succeeded-attachment-failed), [ATDD-044](../../../05-atdd.md#case-atdd-044-replacement), [ATDD-045](../../../05-atdd.md#case-atdd-045-uncertain-attachment), [ATDD-046](../../../05-atdd.md#case-atdd-046-interrupted-cleanup), [ATDD-123](../../../05-atdd.md#case-atdd-123-artwork-byte-boundaries), [ATDD-124](../../../05-atdd.md#case-atdd-124-static-supported-formats), [ATDD-125](../../../05-atdd.md#case-atdd-125-disguised-malformed-or-unsafe-image), [ATDD-126](../../../05-atdd.md#case-atdd-126-replacement-failure-and-correction), [ATDD-159](../../../05-atdd.md#case-atdd-159-validate-the-complete-artwork-stream-boundary), [ATDD-160](../../../05-atdd.md#case-atdd-160-bound-full-decoding-independently-of-compressed-size), [ATDD-161](../../../05-atdd.md#case-atdd-161-keep-file-content-away-from-execution-and-paths), [ATDD-162](../../../05-atdd.md#case-atdd-162-recover-crashes-between-intent-and-byte-acceptance), [ATDD-163](../../../05-atdd.md#case-atdd-163-protect-a-candidate-before-selecting-it), [ATDD-166](../../../05-atdd.md#case-atdd-166-retain-unresolved-holds-and-reject-stale-messages), [ATDD-169](../../../05-atdd.md#case-atdd-169-serve-only-currently-authorized-processed-artwork), [ATDD-170](../../../05-atdd.md#case-atdd-170-show-measured-stages-and-resume-the-original-outcome), [ATDD-245](../../../05-atdd.md#case-atdd-245-enforce-durable-artwork-attachment-decision-storage), [ATDD-249](../../../05-atdd.md#case-atdd-249-enforce-multiple-protected-artwork-reference-promises), [ATDD-263](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback), [ATDD-315](../../../05-atdd.md#case-atdd-315-check-browser-file-policy-before-transfer-without-trusting-mime), [ATDD-305](../../../05-atdd.md#case-atdd-305-separate-real-transfer-events-from-processing-display). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T026` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T026` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- Hand the owned outputs to [T027 — Connect measured progress and recover attachment outcomes](T027-connect-measured-progress-and-recover-attachment-outcomes.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
