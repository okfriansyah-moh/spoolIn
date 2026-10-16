---
kind: task
id: T007
group: G05
depends_on:
- T002
- T003
- T006
readiness: Ready
status: In progress
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T007 — Implement owner receipts and atomic mutation evidence

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-151 | implements |
| ATDD-152 | implements |
| ATDD-153 | implements |
| ATDD-223 | implements |
| ATDD-223-T1-R01 | implements |
| ATDD-223-T1-R02 | implements |
| ATDD-223-T1-R03 | implements |
| ATDD-223-T1-R04 | implements |
| ATDD-223-T1-R05 | implements |
| ATDD-223-T1-R06 | implements |
| ATDD-223-T1-R07 | implements |
| ATDD-223-T1-R08 | implements |
| ATDD-223-T1-R09 | implements |
| ATDD-223-T1-R10 | implements |
| ATDD-223-T1-R11 | implements |
| ATDD-223-T1-R12 | implements |
| ATDD-226 | implements |
| ATDD-226-T1-R01 | implements |
| ATDD-226-T1-R02 | implements |
| ATDD-226-T1-R03 | implements |
| ATDD-226-T1-R04 | implements |
| ATDD-226-T1-R05 | implements |
| ATDD-226-T1-R06 | implements |
| ATDD-226-T1-R07 | implements |
| ATDD-226-T1-R08 | implements |
| ATDD-226-T1-R09 | implements |
| ATDD-226-T1-R10 | implements |
| ATDD-226-T1-R11 | implements |
| ATDD-226-T1-R12 | implements |
| ATDD-226-T1-R13 | implements |
| ATDD-226-T1-R14 | implements |
| ATDD-226-T1-R15 | implements |
| ATDD-226-T1-R16 | implements |
| ATDD-226-T1-R17 | implements |
| ATDD-226-T1-R18 | implements |
| ATDD-226-T1-R19 | implements |
| ATDD-251-T1-R17 | implements |
| ATDD-253-T1-R23 | implements |
| ATDD-254-T1-R06 | implements |
| ATDD-284 | both |
| ATDD-288 | both |
| NFR-008 | both |
| TD-C-027 | both |
| TD-C-040 | both |
| TD-ROW-L2740 | both |
| TD-ROW-L3680 | both |
| TD-ROW-L3681 | both |
| TD-ROW-L3682 | both |
| TD-ROW-L3683 | both |
| TD-ROW-L3684 | both |
| TD-ROW-L3685 | both |
| TD-ROW-L3686 | both |
| TD-ROW-L3687 | both |
| TD-ROW-L3688 | both |
| TD-ROW-L3689 | both |
| TD-ROW-L3690 | both |
| TD-ROW-L3691 | both |

## Prerequisites

- [T002 — Generate and validate the shared wire contracts](../../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T006 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/core/src/main/kotlin/com/spoolin/core/operations/receipts/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/operations/receipts/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/operations/receipts/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/operations/events/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/operations/events/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/operations/events/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/db/migration/V004__operation_records.sql | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/resources/db/migration/V001__operation_records.sql | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/resources/db/migration/V001__operation_records.sql | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/work/receipts/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/operations/receipts/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/operations/receipts/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/operations/receipts/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/operations/events/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/operations/events/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/operations/events/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [6.2.2 Common operational tables](../../../04-spoolin-techdoc.md#622-common-operational-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.3 Initialization receipt extensions](../../../04-spoolin-techdoc.md#623-initialization-receipt-extensions): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.1 Transaction inventory](../../../04-spoolin-techdoc.md#71-transaction-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.3 Idempotency and native worker claims](../../../04-spoolin-techdoc.md#73-idempotency-and-native-worker-claims): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-223-T1-R01 · L8823](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-223-T1-R02 · L8824](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `actor_scope` (`varchar(96)`); Source-valid technical token; 96 versus 97 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-223-T1-R03 · L8825](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `kind` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-223-T1-R04 · L8826](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `receipt_type` (`varchar(24)`); Source-valid technical token; 24 versus 25 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission supplies 'REQUEST'; explicit NULL fails23502; `NOT NULL DEFAULT 'REQUEST'; checked owner-specific allowlist` |
| [ATDD-223-T1-R05 · L8827](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `request_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(request_hash)=32); no SQL default; owner supplies value` |
| [ATDD-223-T1-R06 · L8828](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `hash_key_id` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-223-T1-R07 · L8829](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `state` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (state IN ('PENDING','SUCCEEDED','FAILED','EXPIRED')); no SQL default; owner supplies value` |
| [ATDD-223-T1-R08 · L8830](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `result` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; measure PostgreSQL jsonb::text bytes at 16384 and ±1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (result IS NULL OR (jsonb_typeof(result)='object' AND octet_length(result::text)<=16384)); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-223-T1-R09 · L8831](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `error_code` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-223-T1-R10 · L8832](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `correlation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-223-T1-R11 · L8833](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-223-T1-R12 · L8834](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection) | `expires_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL only for initialization; REQUEST requires a finite expiry; no default` |
| [ATDD-226-T1-R01 · L9003](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-226-T1-R02 · L9004](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `operation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `REFERENCES operation_receipt(id) ON DELETE RESTRICT; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-226-T1-R03 · L9005](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `actor_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-226-T1-R04 · L9006](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `actor_kind` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (actor_kind IN ('HUMAN','SERVICE','ANONYMOUS')); no SQL default; owner supplies value` |
| [ATDD-226-T1-R05 · L9007](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `action` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-226-T1-R06 · L9008](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `subject_kind` (`varchar(24)`); Source-valid technical token; 24 versus 25 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-226-T1-R07 · L9009](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `subject_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-226-T1-R08 · L9010](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `outcome` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL on attempt start; activity/finish/recovery constraints below` |
| [ATDD-226-T1-R09 · L9011](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `changes` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; measure PostgreSQL jsonb::text bytes at 65536 and ±1; Omission supplies CAST('{}' AS jsonb); explicit NULL fails23502; `NOT NULL DEFAULT '{}'; object <=65536 bytes` |
| [ATDD-226-T1-R10 · L9012](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `correlation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-226-T1-R11 · L9013](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-226-T1-R12 · L9014](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `event_type` (`varchar(24)`); Source-valid technical token; 24 versus 25 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; ACTIVITY/ATTEMPT_STARTED/ATTEMPT_FINISHED/RECOVERY` |
| [ATDD-226-T1-R13 · L9015](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `work_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL only for ACTIVITY; FK durable_work(id) ON DELETE RESTRICT` |
| [ATDD-226-T1-R14 · L9016](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `generation` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL on ACTIVITY; >=0 otherwise` |
| [ATDD-226-T1-R15 · L9017](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `attempt_no` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes 0, 1, 10, 11; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL on ACTIVITY/RECOVERY; 1..10 for attempts` |
| [ATDD-226-T1-R16 · L9018](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `lease_epoch` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL on ACTIVITY; >0 for attempts; >=0 for RECOVERY` |
| [ATDD-226-T1-R17 · L9019](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `attempt_outcome` (`varchar(24)`); Source-valid technical token; 24 versus 25 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL except FINISHED; SUCCEEDED/TRANSIENT/PERMANENT/UNCERTAIN/CANCELLED` |
| [ATDD-226-T1-R18 · L9020](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `error_code` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL allowed; no default` |
| [ATDD-226-T1-R19 · L9021](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage) | `operator_id` (`varchar(96)`); Source-valid technical token; 96 versus 97 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL allowed; required for controlled RECOVERY` |
| [ATDD-251-T1-R17 · L10363](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `operation`; Authorized submitting actor / receipt owner \| Original UUID and optional explicit owner; hidden/unknown null, expired tombstone retained; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-253-T1-R23 · L10487](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `GetOperation`; GetOperation(Id) → Outcome |
| [ATDD-254-T1-R06 · L10541](../../../05-atdd.md#case-atdd-254-verify-mediaservice-typed-service-and-actor-boundary) | `GetOperation`; GetOperation(Id) → Outcome |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [TD-C-027 · L3666](../../../04-spoolin-techdoc.md#622-common-operational-tables) | Owner REQUEST receipts: exact actor/kind/hash, 16KiB result, seven-day body then lifetime tombstone; internal initialization variants permanent and hidden. UUIDv5 initializer identity and owner restrictions. |
| [TD-C-040 · L5252](../../../04-spoolin-techdoc.md#73-idempotency-and-native-worker-claims) | One submitted UUID, content/presence/version/policy hash, private HMAC, actor isolation, atomic receipt/domain/audit/work; stable derived child IDs; expired terminal body never permission to rerun. |
| [TD-ROW-L2740 · L2740](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `operation` \| Authorized submitting actor / receipt owner \| Original UUID and optional explicit owner; hidden/unknown null, expired tombstone retained |
| [TD-ROW-L3680 · L3680](../../../04-spoolin-techdoc.md#operation_receipt) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L3681 · L3681](../../../04-spoolin-techdoc.md#operation_receipt) | `actor_scope` \| `varchar(96)` \| `NOT NULL; no SQL default; owner supplies value` \| Authenticated account UUID + scope, or server-issued anonymous registration scope. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3682 · L3682](../../../04-spoolin-techdoc.md#operation_receipt) | `kind` \| `varchar(64)` \| `NOT NULL; no SQL default; owner supplies value` \| Allowlisted operation name. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3683 · L3683](../../../04-spoolin-techdoc.md#operation_receipt) | `receipt_type` \| `varchar(24)` \| NOT NULL DEFAULT 'REQUEST'; checked owner-specific allowlist \| REQUEST, ACCOUNT_INITIALIZATION or CATALOG_INITIALIZATION; internal startup types cannot be selected by clients; never routine telemetry; audit only under §6.1.1 allowlist \| `ReceiptType` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3684 · L3684](../../../04-spoolin-techdoc.md#operation_receipt) | `request_hash` \| `bytea` \| `NOT NULL CHECK (octet_length(request_hash)=32); no SQL default; owner supplies value` \| SHA-256 deterministically serialized nonsecret input; keyed HMAC for credential/PII-bearing input. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3685 · L3685](../../../04-spoolin-techdoc.md#operation_receipt) | `hash_key_id` \| `varchar(64)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| HMAC secret version for private-input fingerprint, including protected draft mutation input; NULL for plain digest. internal; presence/clearing §5 \| `String?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3686 · L3686](../../../04-spoolin-techdoc.md#operation_receipt) | `state` \| `varchar(16)` \| `NOT NULL CHECK (state IN ('PENDING','SUCCEEDED','FAILED','EXPIRED')); no SQL default; owner supplies value` \| Durable outcome, not transport result. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L3687 · L3687](../../../04-spoolin-techdoc.md#operation_receipt) | `result` \| `jsonb` \| `CHECK (result IS NULL OR (jsonb_typeof(result)='object' AND octet_length(result::text)<=16384)); NULL allowed; no SQL default; owner supplies value` \| Sanitized typed result IDs/version/error and draft-discard boolean; no tokens/password/name/email or draft values. Core save/discard receipts use the shapes in §7.3.1. API; presence/clearing §5 \| `TypedJsonDTO?` → typed projection/JSON / typed message |
| [TD-ROW-L3688 · L3688](../../../04-spoolin-techdoc.md#operation_receipt) | `error_code` \| `varchar(64)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Allowlisted stable failure category. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L3689 · L3689](../../../04-spoolin-techdoc.md#operation_receipt) | `correlation_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Validated root request ID. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L3690 · L3690](../../../04-spoolin-techdoc.md#operation_receipt) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L3691 · L3691](../../../04-spoolin-techdoc.md#operation_receipt) | `expires_at` \| `timestamptz(6)` \| NULL only for initialization; REQUEST requires a finite expiry; no default \| Detailed REQUEST-result expiry; initialization evidence never expires; never routine telemetry; audit only under §6.1.1 allowlist \| `Instant?` → Internal only; no GraphQL/protobuf field |

## Implementation

1. Create each owner's local operation_receipt, durable_work and operational_event structures with the current typed extensions and constraints; retain three physical copies without shared repositories. Catalog business-audit insert helpers target the separately owned T016 table.
2. Implement stable per-intent UUID, owner/actor/kind/content fingerprint and private HMAC binding, same-content concurrent replay, changed-content conflict and cross-owner denial. Keep original domain ID distinct from request ID and draft-save ID.
3. Commit domain mutation, safe receipt, appropriate audit and required work atomically within the owner; rollback leaves no misleading success. Use explicit versions/native flush/clear and source lock ordering.
4. Return ≤16KiB safe historical results, purge ordinary result bodies after seven days while retaining lifetime tombstones, and retain internal initialization evidence permanently without public receipt access.
5. Implement receipt routing by typed owner through gRPC, never database fan-out. Expiry or absent current rows never authorize replaying old effects or reconstructing private payloads.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T007-A1:** Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- **T007-A2:** Crash: response loss after commit resolves the original result from a fresh reader; failure before commit leaves none of its success effects.
- **T007-A3:** Retention: expired body produces the safe expired outcome and retains no authority to recreate a later-deleted record; initializer receipt is hidden and permanent.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-284](../../../05-atdd.md#case-atdd-284-bind-operation-identity-to-content-and-actor-scope) / Unit | UUID O, actor A, kind WriteMovie, version5/policyP; same tuple, changed actor B, kind, clear mask, ordered characters, version6/policyQ; synthetic PII and ephemeral registration nonce. | Then matching intent is classified for replay and changed compatible identity content is IDEMPOTENCY_CONFLICT And another actor cannot inspect the receipt And private inputs select keyed HMAC and registration never creates a reusable password-derived request hash Observation: Inspect classification and fingerprint inputs only. Concurrent insertion, durable replay and generic registration response require real owner integration. |
| [ATDD-288](../../../05-atdd.md#case-atdd-288-end-protected-eligibility-at-exact-expiry) / Unit | Session idle/absolute, connector, draft, cursor, impact, challenge and approval cutoffs T; rows atT−1 microsecond/T/T+1; signed actor assertion uses its own2 s tolerance. | Then ordinary eligibility requires now strictly before every applicable cutoff And a retained expired record does not remain usable And actor-signature skew is evaluated by its separately specified verifier rather than applied to ordinary TTLs Observation: Compare cutoff truth table without timers or deletion; actual access at the boundary requires owner integration. |

Partial implementation contributions: [ATDD-151](../../../05-atdd.md#case-atdd-151-distinguish-repeated-intent-from-similar-data), [ATDD-152](../../../05-atdd.md#case-atdd-152-keep-expired-results-from-authorizing-another-effect), [ATDD-153](../../../05-atdd.md#case-atdd-153-keep-credentials-out-of-replay-identities), [ATDD-223](../../../05-atdd.md#case-atdd-223-enforce-request-receipt-storage-and-safe-projection), [ATDD-226](../../../05-atdd.md#case-atdd-226-enforce-restricted-activity-and-attempt-phase-storage). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T007` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T007` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- Hand the owned outputs to [T011 — Run fenced recovery and bounded retries](../../09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
