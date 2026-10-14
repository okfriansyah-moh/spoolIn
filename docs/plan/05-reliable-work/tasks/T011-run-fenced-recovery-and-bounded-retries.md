---
kind: task
id: T011
group: G06
depends_on:
- T010
- T008
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T011 — Run fenced recovery and bounded retries

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-057 | both |
| ATDD-058 | both |
| ATDD-059 | both |
| ATDD-060 | both |
| ATDD-066 | both |
| ATDD-067 | implements |
| ATDD-154 | both |
| ATDD-155 | both |
| ATDD-156 | both |
| ATDD-157 | both |
| ATDD-158 | implements |
| ATDD-225 | implements |
| ATDD-225-T1-R01 | implements |
| ATDD-225-T1-R02 | implements |
| ATDD-225-T1-R03 | implements |
| ATDD-225-T1-R04 | implements |
| ATDD-225-T1-R05 | implements |
| ATDD-225-T1-R06 | implements |
| ATDD-225-T1-R07 | implements |
| ATDD-225-T1-R08 | implements |
| ATDD-225-T1-R09 | implements |
| ATDD-225-T1-R10 | implements |
| ATDD-225-T1-R11 | implements |
| ATDD-225-T1-R12 | implements |
| ATDD-225-T1-R13 | implements |
| ATDD-225-T1-R14 | implements |
| ATDD-225-T1-R15 | implements |
| ATDD-225-T1-R16 | implements |
| ATDD-225-T1-R17 | implements |
| ATDD-225-T1-R18 | implements |
| ATDD-225-T1-R19 | implements |
| ATDD-225-T1-R20 | implements |
| ATDD-285 | both |
| ATDD-286 | both |
| ATDD-287 | both |
| ATDD-309 | both |
| FR-014 | both |
| NFR-004 | both |
| NFR-008 | both |
| A04 | both |
| D19 | both |
| D20 | both |
| D21 | both |
| D22 | both |
| AC-072 | both |
| AC-074 | both |
| AC-087 | both |
| G-04 | both |
| AC-057 | both |
| AC-057-C01 | both |
| AC-058 | both |
| AC-058-C01 | both |
| AC-059 | both |
| AC-059-C01 | both |
| AC-060 | both |
| AC-060-C01 | both |
| AC-073 | both |
| AC-073-C01 | both |
| OD-012 | both |
| PRD-FEEDBACK-06 | both |
| TD-C-028 | both |
| TD-C-029 | both |
| TD-C-042 | both |
| TD-C-046 | both |
| ADR-05 | both |
| V-WORK | both |
| TD-ROW-L3559 | both |
| TD-ROW-L3743 | both |
| TD-ROW-L3744 | both |
| TD-ROW-L3745 | both |
| TD-ROW-L3746 | both |
| TD-ROW-L3747 | both |
| TD-ROW-L3748 | both |
| TD-ROW-L3749 | both |
| TD-ROW-L3750 | both |
| TD-ROW-L3751 | both |
| TD-ROW-L3752 | both |
| TD-ROW-L3753 | both |
| TD-ROW-L3754 | both |
| TD-ROW-L3755 | both |
| TD-ROW-L3756 | both |
| TD-ROW-L3757 | both |
| TD-ROW-L3758 | both |
| TD-ROW-L3759 | both |
| TD-ROW-L3760 | both |
| TD-ROW-L3761 | both |
| TD-ROW-L3762 | both |
| TD-ROW-L3763 | both |
| TD-ROW-L3764 | both |
| TD-ROW-L3765 | both |
| TD-ROW-L3766 | both |
| TD-ROW-L3767 | both |
| TD-ROW-L3857 | both |
| TD-ROW-L3858 | both |
| TD-ROW-L3859 | both |
| TD-ROW-L3860 | both |
| TD-ROW-L3861 | both |
| TD-ROW-L3862 | both |
| TD-ROW-L3863 | both |
| TD-ROW-L3864 | both |
| TD-ROW-L3865 | both |
| TD-ROW-L3866 | both |
| TD-ROW-L3867 | both |
| TD-ROW-L3868 | both |
| TD-ROW-L3869 | both |
| TD-ROW-L3870 | both |
| TD-ROW-L3871 | both |
| TD-ROW-L3872 | both |
| TD-ROW-L3873 | both |
| TD-ROW-L3874 | both |
| TD-ROW-L3875 | both |
| TD-ROW-L5450 | both |
| TD-ROW-L5532 | both |
| F-LEASE | both |
| BRAIN-A04 | both |
| BRAIN-D19 | both |
| BRAIN-D20 | both |
| BRAIN-D21 | both |
| BRAIN-D22 | both |

## Prerequisites

- [T010 — Implement owner receipts and atomic mutation evidence](T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T008 — Enforce scoped service identity and shared admission](../../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/core/src/main/kotlin/com/spoolin/core/operations/work/ | T011 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/operations/work/ | T011 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/operations/work/ | T011 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/recover-work.sh | T011 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/work/leases/ | T011 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/operations/work/ | T011 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/operations/work/ | T011 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/operations/work/ | T011 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [7.3 Idempotency and native worker claims](../../../04-spoolin-techdoc.md#73-idempotency-and-native-worker-claims): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.7 Durable work, retries and notifications](../../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.3 Retention, purge and backup runbooks](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-225-T1-R01 · L8938](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-225-T1-R02 · L8939](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `operation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `REFERENCES operation_receipt(id) ON DELETE RESTRICT; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-225-T1-R03 · L8940](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `kind` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-225-T1-R04 · L8941](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `payload` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; measure PostgreSQL jsonb::text bytes at 16384 and ±1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (jsonb_typeof(payload)='object' AND octet_length(payload::text)<=16384); no SQL default; owner supplies value` |
| [ATDD-225-T1-R05 · L8942](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `state` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission supplies 'READY'; explicit NULL fails23502; `NOT NULL DEFAULT 'READY' CHECK (state IN ('READY','LEASED','DONE','EXHAUSTED','CANCELLED')); NOT NULL (explicit or PK)` |
| [ATDD-225-T1-R06 · L8943](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `generation` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (generation >= 0); NOT NULL (explicit or PK)` |
| [ATDD-225-T1-R07 · L8944](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `attempt_count` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK >= 0; <= max_attempts` |
| [ATDD-225-T1-R08 · L8945](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `max_attempts` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes 0, 1, 10, 11; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (max_attempts BETWEEN 1 AND 10); no SQL default; owner supplies value` |
| [ATDD-225-T1-R09 · L8946](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `next_attempt_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-225-T1-R10 · L8947](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `deadline_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-225-T1-R11 · L8948](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `lease_owner` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-225-T1-R12 · L8949](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `lease_until` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-225-T1-R13 · L8950](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `lease_epoch` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (lease_epoch >= 0); NOT NULL (explicit or PK)` |
| [ATDD-225-T1-R14 · L8951](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `last_error` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-225-T1-R15 · L8952](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `correlation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-225-T1-R16 · L8953](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-225-T1-R17 · L8954](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-225-T1-R18 · L8955](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `base_delay_ms` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes 0, 1, 60000, 60001; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (base_delay_ms BETWEEN 1 AND 60000); no SQL default; owner supplies value` |
| [ATDD-225-T1-R19 · L8956](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `cap_delay_ms` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes 0, 1, 3600000, 3600001; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (cap_delay_ms BETWEEN 1 AND 3600000); no SQL default; owner supplies value` |
| [ATDD-225-T1-R20 · L8957](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling) | `backoff_multiplier` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes 0, 1, 10, 11; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (backoff_multiplier BETWEEN 1 AND 10); no SQL default; owner supplies value` |
| [FR-014 · L250](../../../02-PRD.md#61-functional-requirements) | Preserve required follow-up through interruptions and make exhausted work inspectable/recoverable \| D20/D21 Committed \| AC-057 through AC-060 |
| [NFR-004 · L264](../../../02-PRD.md#62-quality-and-delivery-requirements) | Safe recovery is bounded and respects cancellation; uncertain outcomes cannot multiply effects \| D21 Committed; D22 defaults Recommended \| AC-072 through AC-075 |
| [NFR-008 · L268](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [A04 · L285](../../../02-PRD.md#63-assessment-coverage) | Real service-to-service gRPC \| NFR-001/NFR-008 / AC-065, AC-087 |
| [D19 · L322](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [D20 · L323](../../../02-PRD.md#64-project-decision-register) | Atomic successful audit, durable follow-up, saga only when needed \| FR-013/FR-014 / AC-053, AC-054, AC-057 through AC-060; techdoc mechanics |
| [D21 · L324](../../../02-PRD.md#64-project-decision-register) | Bounded safe retries and one configured exhaustion channel \| NFR-004/NFR-005, FR-015 / AC-061 through AC-063, AC-072 through AC-078 |
| [D22 · L325](../../../02-PRD.md#64-project-decision-register) | Initial retry settings: 3 retries after the initial attempt (at most 4 total application attempts), initial interval 1,000 ms, multiplier 2, maximum interval 30,000 ms \| These are proposed starting values, not finalized per-operation policies. NFR-004 / AC-072, AC-074; section 10.1 |
| [AC-072 · L863](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Prove bounded safe exponential/jitter retry with one owner and deadline/library-attempt accounting \| NFR-004; technical design |
| [AC-074 · L864](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Validate retry overrides/units/ranges; zero disables retries; D22 numerical defaults remain Recommended \| NFR-004/NFR-005; technical design |
| [AC-087 · L872](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration \| NFR-008; technical design |
| [G-04 · L927](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
| [AC-057 · L973](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Required follow-up \| A confirmed change requires later work \| The application stops before follow-up completes \| The obligation remains visible and recoverable; an uncommitted change does not leave a false successful follow-up obligation. |
| [AC-057-C01 · L973](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | The obligation remains visible and recoverable; an uncommitted change does not leave a false successful follow-up obligation. |
| [AC-058 · L974](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Repeated/interrupted work \| Follow-up is delivered twice or interrupted after completing its effect \| Recovery runs \| Completed effects are not unintentionally duplicated; completion status is reconciled with what actually happened. |
| [AC-058-C01 · L974](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Completed effects are not unintentionally duplicated; completion status is reconciled with what actually happened. |
| [AC-059 · L975](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Recovery budget \| Work already has failed attempts and a scheduled retry \| The application restarts \| Its prior attempts and failure state remain; restarting does not grant unlimited retries. |
| [AC-059-C01 · L975](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Its prior attempts and failure state remain; restarting does not grant unlimited retries. |
| [AC-060 · L976](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Exhausted work \| Recovery attempts are exhausted \| An authorized operations owner inspects it \| See operation, owner, attempts, sanitized cause, and safe recovery guidance; controlled recovery preserves the original evidence and checks current conditions. |
| [AC-060-C01 · L976](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | See operation, owner, attempts, sanitized cause, and safe recovery guidance; controlled recovery preserves the original evidence and checks current conditions. |
| [AC-073 · L987](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Permanent/uncertain failure \| Input is invalid, access denied, an edit stale, or a prior outcome uncertain \| Recovery is considered \| Ask for correction/review for permanent failures; resolve uncertain effects before repeating a change. |
| [AC-073-C01 · L987](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Ask for correction/review for permanent failures; resolve uncertain effects before repeating a change. |
| [OD-012 · L1051](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Bounded safe recovery fixed; per-operation values, deadlines, jitter, claiming, retry ownership, and controlled-recovery mechanics remain for techdoc \| FR-014/NFR-004 \| Technical design; before workers/retries |
| [PRD-FEEDBACK-06 · L802](../../../02-PRD.md#810-shared-feedback-rules) | Failure / uncertainty \| Explain what is known; offer safe retry for confirmed failure and outcome checking before repeating uncertain changes |
| [TD-C-028 · L3731](../../../04-spoolin-techdoc.md#durable_work) | Owner durable_work persisted schedule/budget/generation/lease; notification-specific columns conditional and unique event key. Database notification DONE/RECORDED zero attempts/sends; uncertain external delivery never blindly resent. |
| [TD-C-029 · L3845](../../../04-spoolin-techdoc.md#operational_event) | Owner operational_event append-only phase/recovery types; claim+STARTED before effect, unique phase tuple, fenced FINISHED+state atomicity, watchdog outcome reconciliation, no technical events in business history; unresolved evidence retained. |
| [TD-C-042 · L5278](../../../04-spoolin-techdoc.md#worker-claim-and-fenced-completion) | Real worker SKIP LOCKED claims, &lt;=10 batch/concurrency2, lease30s heartbeat10s effect&lt;=15s, durable STARTED before send, fenced completion and watchdog reconciliation under original attempt tuple; initialization replay distinct. |
| [TD-C-046 · L5434](../../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications) | Bounded retries/deadline/jitter persist; no hidden SDK retries, every send counted, permanent vs transient vs uncertain separated. Controlled operator recovery requires reason/new generation/original effect identity. One selected notification mode and truthful real-provider evidence. |
| [ADR-05 · L6552](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-05 \| Atomic outbox/work tables and explicit uncertainty. No blanket automatic retries \| FR-014/015/NFR-004/014; provider limitations stay visible |
| [V-WORK · L6393](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-WORK \| Crash at every commit/send/receipt boundary, two claiming workers, lease expiry/heartbeat loss/stale epoch, restart persisted backoff,0 retries, exhaustion/cancel/control recovery; assert one logical effect, bounded sends, retained evidence and true uncertainty |
| [TD-ROW-L3559 · L3559](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | work_item \| durable_work: existing scheduling, payload, lease, generation, deadline and budget columns \| Transactional outbox, bounded claims and fenced completion |
| [TD-ROW-L3743 · L3743](../../../04-spoolin-techdoc.md#durable_work) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L3744 · L3744](../../../04-spoolin-techdoc.md#durable_work) | `operation_id` \| `uuid` \| `REFERENCES operation_receipt(id) ON DELETE RESTRICT; NULL allowed; no SQL default; owner supplies value` \| Originating operation. internal; presence/clearing §5 \| `UUID?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3745 · L3745](../../../04-spoolin-techdoc.md#durable_work) | `kind` \| `varchar(64)` \| `NOT NULL; no SQL default; owner supplies value` \| Typed handler name, never executable code. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3746 · L3746](../../../04-spoolin-techdoc.md#durable_work) | `payload` \| `jsonb` \| `NOT NULL CHECK (jsonb_typeof(payload)='object' AND octet_length(payload::text)<=16384); no SQL default; owner supplies value` \| Versioned IDs-only message; no account PII or credential. internal; presence/clearing §5 \| `TypedJsonDTO` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3747 · L3747](../../../04-spoolin-techdoc.md#durable_work) | `state` \| `varchar(16)` \| `NOT NULL DEFAULT 'READY' CHECK (state IN ('READY','LEASED','DONE','EXHAUSTED','CANCELLED')); NOT NULL (explicit or PK)` \| Durable work lifecycle. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3748 · L3748](../../../04-spoolin-techdoc.md#durable_work) | `generation` \| `integer` \| `NOT NULL DEFAULT 0 CHECK (generation >= 0); NOT NULL (explicit or PK)` \| Explicit operator recovery increments; restart never changes. internal; presence/clearing §5 \| `Int` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3749 · L3749](../../../04-spoolin-techdoc.md#durable_work) | `attempt_count` \| `integer` \| NOT NULL DEFAULT 0 CHECK &gt;= 0; &lt;= max_attempts \| Attempts started in this generation; STARTED operational events preserve evidence; never routine telemetry; audit only under §6.1.1 allowlist \| `Int` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3750 · L3750](../../../04-spoolin-techdoc.md#durable_work) | `max_attempts` \| `integer` \| `NOT NULL CHECK (max_attempts BETWEEN 1 AND 10); no SQL default; owner supplies value` \| Policy snapshot, default 3 at creation. internal; presence/clearing §5 \| `Int` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3751 · L3751](../../../04-spoolin-techdoc.md#durable_work) | `next_attempt_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Eligible scheduling time. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3752 · L3752](../../../04-spoolin-techdoc.md#durable_work) | `deadline_at` \| `timestamptz(6)` \| `NOT NULL; no SQL default; owner supplies value` \| Persisted absolute generation deadline. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3753 · L3753](../../../04-spoolin-techdoc.md#durable_work) | `lease_owner` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Random worker incarnation; NULL when not leased. internal; presence/clearing §5 \| `UUID?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3754 · L3754](../../../04-spoolin-techdoc.md#durable_work) | `lease_until` \| `timestamptz(6)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Lease expiry instant. internal; presence/clearing §5 \| `Instant?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3755 · L3755](../../../04-spoolin-techdoc.md#durable_work) | `lease_epoch` \| `bigint` \| `NOT NULL DEFAULT 0 CHECK (lease_epoch >= 0); NOT NULL (explicit or PK)` \| Fencing generation increases every claim. internal; presence/clearing §5 \| `Long` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3756 · L3756](../../../04-spoolin-techdoc.md#durable_work) | `last_error` \| `varchar(64)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Sanitized category. internal; presence/clearing §5 \| `String?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3757 · L3757](../../../04-spoolin-techdoc.md#durable_work) | `correlation_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Origin correlation. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3758 · L3758](../../../04-spoolin-techdoc.md#durable_work) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L3759 · L3759](../../../04-spoolin-techdoc.md#durable_work) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L3760 · L3760](../../../04-spoolin-techdoc.md#durable_work) | `base_delay_ms` \| `integer` \| `NOT NULL CHECK (base_delay_ms BETWEEN 1 AND 60000); no SQL default; owner supplies value` \| Persisted retry base milliseconds. internal; presence/clearing §5 \| `Int` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3761 · L3761](../../../04-spoolin-techdoc.md#durable_work) | `cap_delay_ms` \| `integer` \| `NOT NULL CHECK (cap_delay_ms BETWEEN 1 AND 3600000); no SQL default; owner supplies value` \| Persisted maximum backoff milliseconds. internal; presence/clearing §5 \| `Int` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3762 · L3762](../../../04-spoolin-techdoc.md#durable_work) | `backoff_multiplier` \| `integer` \| `NOT NULL CHECK (backoff_multiplier BETWEEN 1 AND 10); no SQL default; owner supplies value` \| Persisted exponential multiplier. internal; presence/clearing §5 \| `Int` → not exposed; no GraphQL/proto field |
| [TD-ROW-L3763 · L3763](../../../04-spoolin-techdoc.md#durable_work) | `notification_key` \| `varchar(160)` \| NULL for non-notification; UNIQUE when present \| Service/work/generation or alert fingerprint plus episode deduplication identity; never routine telemetry; audit only under §6.1.1 allowlist \| `String?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3764 · L3764](../../../04-spoolin-techdoc.md#durable_work) | `notification_mode` \| `varchar(16)` \| NULL for non-notification; database/smtp/slack/google_chat \| Selected mode snapshot; changing configuration does not reinterpret an existing record; never routine telemetry; audit only under §6.1.1 allowlist \| `NotificationMode?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3765 · L3765](../../../04-spoolin-techdoc.md#durable_work) | `notification_state` \| `varchar(24)` \| NULL for non-notification; RECORDED/PENDING/ACCEPTED/DELIVERY_CONFIRMED/UNKNOWN/FAILED \| Provider evidence independent of work scheduler state; never routine telemetry; audit only under §6.1.1 allowlist \| `NotificationState?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3766 · L3766](../../../04-spoolin-techdoc.md#durable_work) | `notification_summary` \| `jsonb` \| NULL for non-notification; object &lt;=8192 bytes \| Allowlisted IDs, severity, safe cause and runbook reference; no private payload; never routine telemetry; audit only under §6.1.1 allowlist \| `NotificationSummary?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3767 · L3767](../../../04-spoolin-techdoc.md#durable_work) | `provider_reference` \| `varchar(255)` \| NULL allowed; no default \| Nonsecret provider receipt; NULL when none exists; never routine telemetry; audit only under §6.1.1 allowlist \| `String?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3857 · L3857](../../../04-spoolin-techdoc.md#operational_event) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. internal; never exposed \| `UUID` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3858 · L3858](../../../04-spoolin-techdoc.md#operational_event) | `operation_id` \| `uuid` \| `REFERENCES operation_receipt(id) ON DELETE RESTRICT; NULL allowed; no SQL default; owner supplies value` \| Local operation when applicable. internal; never exposed \| `UUID?` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3859 · L3859](../../../04-spoolin-techdoc.md#operational_event) | `actor_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Opaque verified account ID; NULL for unauthenticated attempt. internal; never exposed \| `UUID?` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3860 · L3860](../../../04-spoolin-techdoc.md#operational_event) | `actor_kind` \| `varchar(16)` \| `NOT NULL CHECK (actor_kind IN ('HUMAN','SERVICE','ANONYMOUS')); no SQL default; owner supplies value` \| Never infer human identity from a supplied display name. internal; presence/clearing §5 \| `String` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3861 · L3861](../../../04-spoolin-techdoc.md#operational_event) | `action` \| `varchar(64)` \| `NOT NULL; no SQL default; owner supplies value` \| Allowlisted event action. internal; never exposed \| `String` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3862 · L3862](../../../04-spoolin-techdoc.md#operational_event) | `subject_kind` \| `varchar(24)` \| `NOT NULL; no SQL default; owner supplies value` \| Catalog title/person/credit/proposal/artwork subject; security/work events use operational_event. internal; never exposed \| `String` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3863 · L3863](../../../04-spoolin-techdoc.md#operational_event) | `subject_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Logical subject retained after deletion; deliberately no FK. internal; never exposed \| `UUID?` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3864 · L3864](../../../04-spoolin-techdoc.md#operational_event) | `outcome` \| `varchar(16)` \| NULL on attempt start; activity/finish/recovery constraints below \| Sanitized activity outcome; attempt classification uses attempt_outcome separately; never routine telemetry; audit only under §6.1.1 allowlist \| `String?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3865 · L3865](../../../04-spoolin-techdoc.md#operational_event) | `changes` \| `jsonb` \| NOT NULL DEFAULT '{}'; object &lt;=65536 bytes \| Allowlisted technical/security metadata and controlled recovery reason; never raw business snapshots or secret values; never routine telemetry; audit only under §6.1.1 allowlist \| `OperationalMetadata` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3866 · L3866](../../../04-spoolin-techdoc.md#operational_event) | `correlation_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Sanitized correlation. internal; presence/clearing §5 \| `UUID` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3867 · L3867](../../../04-spoolin-techdoc.md#operational_event) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. internal; never exposed \| `Instant` → internal only; no GraphQL/protobuf field |
| [TD-ROW-L3868 · L3868](../../../04-spoolin-techdoc.md#operational_event) | `event_type` \| `varchar(24)` \| NOT NULL; ACTIVITY/ATTEMPT_STARTED/ATTEMPT_FINISHED/RECOVERY \| Selects the constrained record shape; never routine telemetry; audit only under §6.1.1 allowlist \| `OperationalEventType` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3869 · L3869](../../../04-spoolin-techdoc.md#operational_event) | `work_id` \| `uuid` \| NULL only for ACTIVITY; FK durable_work(id) ON DELETE RESTRICT \| Local work identity; never routine telemetry; audit only under §6.1.1 allowlist \| `UUID?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3870 · L3870](../../../04-spoolin-techdoc.md#operational_event) | `generation` \| `integer` \| NULL on ACTIVITY; &gt;=0 otherwise \| Work recovery generation; never routine telemetry; audit only under §6.1.1 allowlist \| `Int?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3871 · L3871](../../../04-spoolin-techdoc.md#operational_event) | `attempt_no` \| `integer` \| NULL on ACTIVITY/RECOVERY; 1..10 for attempts \| Attempt identity inside the generation; never routine telemetry; audit only under §6.1.1 allowlist \| `Int?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3872 · L3872](../../../04-spoolin-techdoc.md#operational_event) | `lease_epoch` \| `bigint` \| NULL on ACTIVITY; &gt;0 for attempts; &gt;=0 for RECOVERY \| Original attempt fence or current recovery fence; not an account authentication epoch; never routine telemetry; audit only under §6.1.1 allowlist \| `Long?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3873 · L3873](../../../04-spoolin-techdoc.md#operational_event) | `attempt_outcome` \| `varchar(24)` \| NULL except FINISHED; SUCCEEDED/TRANSIENT/PERMANENT/UNCERTAIN/CANCELLED \| Original worker-attempt outcome; no public AuditOutcome expansion; never routine telemetry; audit only under §6.1.1 allowlist \| `AttemptOutcome?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3874 · L3874](../../../04-spoolin-techdoc.md#operational_event) | `error_code` \| `varchar(64)` \| NULL allowed; no default \| Sanitized cause, never exception text; never routine telemetry; audit only under §6.1.1 allowlist \| `String?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L3875 · L3875](../../../04-spoolin-techdoc.md#operational_event) | `operator_id` \| `varchar(96)` \| NULL allowed; required for controlled RECOVERY \| Verified local operator identity, never arbitrary input identity; never routine telemetry; audit only under §6.1.1 allowlist \| `String?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L5450 · L5450](../../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications) | slack \| Secret fixed webhook or token/channel; verified HTTPS \| API success/provider reference→ACCEPTED; explicit rejection→FAILED/retryable as classified. Do not claim human receipt |
| [TD-ROW-L5532 · L5532](../../../04-spoolin-techdoc.md#83-api-security-enforcement) | Durable workers / owning service \| Persisted allowed effect tuple, budget/deadline and current lease owner/epoch \| Stable destination child ID, fencing and outcome reconciliation; no blind resend after uncertain effect \| Lease loss, double claim, late completion and crash after send cannot multiply logical effects or erase unresolved evidence |
| [BRAIN-A04 · L51](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A04 \| Service-to-service communication via gRPC \| Real RPC calls supporting application features; protobuf contracts and integration checks |
| [BRAIN-D19 · L90](../../../01-brainstorming-result.md#4-decision-register) | D19 \| Committed \| Unit and integration tests, including real gRPC communication and persistence against the selected database engine |
| [BRAIN-D20 · L91](../../../01-brainstorming-result.md#4-decision-register) | D20 \| Committed \| Durable outbox or equivalent work table for reliable cross-service follow-up; saga coordination only when the finalized workflow needs it |
| [BRAIN-D21 · L92](../../../01-brainstorming-result.md#4-decision-register) | D21 \| Committed \| Bounded exponential backoff with jitter for safe transient failures; environment-configurable retry policy and exhaustion notification through one configured channel |
| [BRAIN-D22 · L93](../../../01-brainstorming-result.md#4-decision-register) | D22 \| Recommended \| Initial retry defaults in section 9.2; exact operation policies must respect deadlines and idempotency |

## Implementation

1. Implement real PostgreSQL SKIP LOCKED claims with batch≤10/concurrency2, 30s lease, 10s heartbeat and bounded effect duration. Commit claim plus STARTED before any send; release DB resources before remote calls.
2. Fence renewal and terminal commit by generation/lease/attempt tuple, then commit FINISHED plus state atomically. Watchdog recovery preserves the original tuple and queries destination receipt before another effect.
3. Persist sampled jitter, attempt count, next attempt and total deadline across restarts. Apply three total attempts, 500ms base, factor2 and 5,000ms cap where selected; count hidden library sends or disable retries at those layers.
4. Separate permanent/authorization/validation/conflict failures from safe transient retries and uncertain effects. Cancellation cannot retract a committed remote action. Exhaustion remains inspectable and creates the specified notification obligation for T036.
5. Implement restricted recover-work with explicit reason, new recovery generation, current-state checks and original effect ID. Keep failed original evidence; administrative recovery is not a browser feature.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T011-A1:** Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.
- **T011-A2:** Budget: crash/restart preserves attempts and jitter/deadline; permanent failures never auto-retry and uncertainty never becomes presumed rollback.
- **T011-A3:** Recovery: inspected exhaustion shows sanitized owner/operation/attempt/cause, and authorized recovery preserves original evidence and destination identity.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-057](../../../05-atdd.md#case-atdd-057-required-follow-up) / System | Commitrequiredwork then kill owner before effect; separately rollback domain transaction before commit; restart originalID. | Then The obligation remains visible and recoverable; an uncommitted change does not leave a false successful follow-up obligation. Observation: Kill after required work commit and restart, separately roll back before commit; inspect durable queued follow-up only for the successful domain mutation. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-058](../../../05-atdd.md#case-atdd-058-repeatedinterrupted-work) / System | Destinationcommit then kill worker before receipt save; second worker reconciles samechildID; no doubled remote effect. | Then Completed effects are not unintentionally duplicated; completion status is reconciled with what actually happened. Observation: Crash after destination commit/before receipt save and let second worker reconcile child identity; inspect one logical effect and retained interrupted attempt. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-059](../../../05-atdd.md#case-atdd-059-recovery-budget) / System | Savedattemptcount2, max 3 andfuture next attempt/deadline; restart; original values unchanged and only remaining budget used. | Then Its prior attempts and failure state remain; restarting does not grant unlimited retries. Observation: Record saved attempt 2/max 3/next-time/deadline, restart and count only remaining sends; process defaults cannot reset this generation’s budget. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-060](../../../05-atdd.md#case-atdd-060-exhausted-work) / System | EXHAUSTEDworkwith3attempts; authorized operator inspects and recovers with reason; old attempts remain new generation bounded same effect IDs. | Then See operation, owner, attempts, sanitized cause, and safe recovery guidance; controlled recovery preserves the original evidence and checks current conditions. Observation: Inspect exhausted attempts and authenticated reasoned operator recovery; previous evidence remains with a new bounded generation and stable effect identity. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-066](../../../05-atdd.md#case-atdd-066-permanentuncertain-failure) / System | Invalid/denied/stale/notfound outcomes; unknown remote commit; safe eligible transient failure. Noautomaticpermanentretry. | Then Ask for correction/review for permanent failures; resolve uncertain effects before repeating a change. Observation: Record actual failure class and receipt reconciliation before any retry; permanent invalid/denied/stale/not-found outcomes cannot enter automatic transient retry. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-154](../../../05-atdd.md#case-atdd-154-commit-work-ownership-before-sending-effects) / Integration | F-WORK: original parent/child IDs, generation 0, attempts0..3, controlled deadline/scheduled instant; workers W1/W2 synchronized before and after destination commit. | Then each claim commits its attempt and increased epoch/count before the effect And due ordering, batch≤10 and concurrency2 per service hold And 30 s leases, 10 s heartbeats and effect≤15 s bound ownership; stale owner/epoch completion changes no row Observation: Observe real work claim transaction and ATTEMPT_STARTED operational_event before permitting a network send. With two workers, compare owner/lease epoch/order/batch/attempt counts, persisted deadline and exact sends. Use F-LEASE for renewal and watchdog CAS; mock send order cannot prove commit-before-send. Observe durable_work claim/count/fence and ATTEMPT_STARTED operational_event together from a separate committed reader before permitting any outbound effect. A failed phase insert rolls back the claim. The event retains original work/generation/attempt/lease identity; duplicate phase is refused, and another worker’s stale completion appends no FINISHED event. |
| [ATDD-155](../../../05-atdd.md#case-atdd-155-reconcile-a-late-effect-after-losing-its-lease) / Integration | F-WORK: original parent/child IDs, generation 0, attempts0..3, controlled deadline/scheduled instant; workers W1/W2 synchronized before and after destination commit. | Then the interrupted attempt is marked UNCERTAIN and destination receipt is checked using the same child identity And the old epoch cannot mark local completion or start new effects And a late remote success is reconciled without a second logical effect Observation: Let W1’s destination commit after lease loss while W2 takes recovery ownership. Inspect W1 fenced local writes, UNCERTAIN attempt, destination child receipt and W2 reconciliation before any new send. Compare one destination effect and preserved attempts using F-LEASE barriers. Inspect one original ATTEMPT_STARTED and at most one matching FINISHED with retained UNCERTAIN classification/original fence; watchdog’s current locked ownership performs destination reconciliation. Later safe resolution does not erase old evidence or let W1 append stale success/new sends. |
| [ATDD-156](../../../05-atdd.md#case-atdd-156-persist-the-sampled-retry-budget-across-restarts) / Integration | F-WORK: original parent/child IDs, generation 0, attempts0..3, controlled deadline/scheduled instant; workers W1/W2 synchronized before and after destination commit. | Then defaults allow three total sends with500 ms base, multiplier2, 5 s cap and full jitter within the current bound And normal60 s/artwork5 min absolute deadlines, attempt counts and sampled nextAttempt survive restart And zero retries permits one initial send and exactly one retry owner accounts for every actual outbound send Observation: Record sampled next_attempt_at, max_attempts and deadline_at before terminating the worker. Restart and compare stored values byte-for-byte and the remaining allowed sends; changing process defaults must not recalculate the persisted generation’s budget. |
| [ATDD-157](../../../05-atdd.md#case-atdd-157-retry-only-safe-classified-transient-failures) / Integration | F-WORK: original parent/child IDs, generation 0, attempts0..3, controlled deadline/scheduled instant; workers W1/W2 synchronized before and after destination commit. | Then permanent outcomes request correction or review without automatic resubmission And eligible transient failures retry only safely within attempt/deadline/server guidance And an uncertain mutation checks its destination outcome before another send Observation: For every explicit failure class, capture destination receipt and actual outbound sends. Permanent failures send once; transient safe failures honor sampled schedule and bounded Retry-After; ambiguous post-send failures reconcile before any permitted replay. |
| [ATDD-285](../../../05-atdd.md#case-atdd-285-classify-retryable-failure-separately-from-uncertain-effect) / Unit | INVALID_ARGUMENT, PERMISSION_DENIED, NOT_FOUND, STALE_VERSION; UNAVAILABLE before send; 429 with Retry-After; timeout after send; queryable/not-queryable provider. | Then permanent failures stop; safe transient failures may schedule only within saved budget And possible post-send commit selects receipt reconciliation before replay And an unqueryable ambiguous provider remains UNKNOWN for operator review Observation: Compare fixed action enums and no-send decisions. A fake provider response cannot establish actual acceptance or durable attempts. |
| [ATDD-286](../../../05-atdd.md#case-atdd-286-bound-jitter-and-remaining-retry-budget-without-resetting-it) / Unit | Base500 ms, factor2, cap5000 ms; attempt indices1/2/5 give upper bounds500/1000/5000; draws0/0.5/1; retries 0→maxAttempts1; persisted count 2/max 3/deadlineT. | Then sampled delays are 0/250/500, 0/500/1000 and 0/2500/5000 ms for the listed rows And zero retries permits only the initial send; count3 or now at deadline permits no new send And a changed process default cannot alter supplied saved budget Observation: Use fixed draws/clock and literal expected arithmetic. Persisting sampled time, restart and actual outbound counts remain integration obligations. |
| [ATDD-287](../../../05-atdd.md#case-atdd-287-reject-stale-lease-owners-before-renewal-or-sending) / Unit | LEASED ownerW1 epoch7 untilT; W1/7, W1/6 and W2/7 requests atT−1 microsecond/T/T+1; terminal rows and original deadlineD. | Then only the current live owner/epoch on a live nonterminal lease can renew or start an eligible effect And stale/expired/terminal ownership cannot resurrect work or reset its deadline And reaching D forbids new effects even if lease metadata remains present Observation: Assert explicit eligibility booleans using fixed values; real claim/renew/watchdog CAS and late remote effects are proved independently. |
| [ATDD-309](../../../05-atdd.md#case-atdd-309-fence-actual-heartbeat-renewal-against-stale-recovery) / Integration | F-LEASE W1epoch7/W2epoch8, lease 30 s/heartbeat 10 s/effect≤15 s; renewal-before-expiry, exactexpiry, expiredreclaim, originaldeadlineD and late destinationcommit. | Then only the current live owner/epoch renews or writes completion And recovery records interrupted effect UNCERTAIN and reconciles before another send And stale renewal cannot resurrect ownership or reset saved deadline; after D no new send starts Observation: Inspect actual conditional affected-row results, lease owner/epoch/attempt/deadline, network sends and original destination child receipt before/after recovery. A SQL text inspection or mock scheduler is insufficient. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-LEASE · L216](../../../05-atdd.md#f-lease-and-f-alert-retention-parameterized-operational-boundaries) | F-LEASE uses lease 30 s/heartbeat 10 s/effect≤15 s, current W1/epoch7 and reclaimed W2/epoch8. Renewal requires the same live owner/epoch/nonterminal state and cannot resurrect an expired lease, extend/reset the persisted work deadline or make a stale completion authoritative. Synchronize heartbeat just before/at expiry with watchdog claim; losing owner’s later remote commit is reconciled by the original destination child receipt. No new effect starts after loss/cancel/deadline. Retaining lease metadata while an already-started effect settles does not grant permission for another send. F-ALERT-RETENTION records an explicit positive isolated retention R from actual resolved Alertmanager configuration, plus retry/group/dedup settings. Use R−1 s and R+1 s outage/recovery rows; R is a test parameter, not a new production default. Within retained retry scope, recovery records one deduplicated Core event; during DB outage no durable Core recording can be promised. Beyond retention report actual state/loss without inventing guaranteed eventual delivery. Alertmanager UI and independent startup/status/operator dashboard remain the detection path. Report the actual release retention value before claiming release behavior; secrets/destinations remain protected. |

Partial implementation contributions: [ATDD-067](../../../05-atdd.md#case-atdd-067-cancel-or-exhaust), [ATDD-158](../../../05-atdd.md#case-atdd-158-preserve-original-evidence-during-controlled-recovery), [ATDD-225](../../../05-atdd.md#case-atdd-225-enforce-durable-task-storage-and-committed-scheduling). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T011` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T011` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
