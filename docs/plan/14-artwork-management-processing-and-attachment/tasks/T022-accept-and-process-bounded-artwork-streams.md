---
kind: task
id: T022
group: G14
depends_on:
- T003
- T009
- T006
- T007
- T011
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T022 — Accept and process bounded artwork streams

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

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
| ATDD-168 | both |
| ATDD-169 | implements |
| ATDD-170 | implements |
| ATDD-248 | implements |
| ATDD-248-T1-R01 | implements |
| ATDD-248-T1-R02 | implements |
| ATDD-248-T1-R03 | implements |
| ATDD-248-T1-R04 | implements |
| ATDD-248-T1-R05 | implements |
| ATDD-248-T1-R06 | implements |
| ATDD-248-T1-R07 | implements |
| ATDD-248-T1-R08 | implements |
| ATDD-248-T1-R09 | implements |
| ATDD-248-T1-R10 | implements |
| ATDD-248-T1-R11 | implements |
| ATDD-248-T1-R12 | implements |
| ATDD-248-T1-R13 | implements |
| ATDD-248-T1-R14 | implements |
| ATDD-248-T1-R15 | implements |
| ATDD-248-T1-R16 | implements |
| ATDD-250 | implements |
| ATDD-250-T1-R01 | implements |
| ATDD-250-T1-R02 | implements |
| ATDD-250-T1-R03 | implements |
| ATDD-250-T1-R04 | implements |
| ATDD-254-T1-R01 | implements |
| ATDD-263 | implements |
| ATDD-315 | implements |
| ATDD-305 | implements |
| ATDD-325 | implements |
| J-04 | implements |
| FR-002 | implements |
| FR-009 | implements |
| FR-010 | both |
| FR-017 | implements |
| NFR-001 | implements |
| NFR-002 | implements |
| NFR-005 | implements |
| NFR-007 | implements |
| NFR-008 | implements |
| NFR-013 | implements |
| NFR-016 | implements |
| A03 | implements |
| A04 | implements |
| A05 | implements |
| A08 | both |
| A14 | implements |
| D03 | implements |
| D10 | implements |
| D11 | both |
| D19 | implements |
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
| G-04 | implements |
| G-05 | implements |
| OD-007 | implements |
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
| TD-C-036 | implements |
| TD-C-044 | both |
| TD-ROW-L5016 | both |
| TD-ROW-L5017 | both |
| TD-ROW-L5018 | both |
| TD-ROW-L5019 | both |
| TD-ROW-L5020 | both |
| TD-ROW-L5021 | both |
| TD-ROW-L5022 | both |
| TD-ROW-L5023 | both |
| TD-ROW-L5024 | both |
| TD-ROW-L5025 | both |
| TD-ROW-L5026 | both |
| TD-ROW-L5027 | both |
| TD-ROW-L5028 | both |
| TD-ROW-L5029 | both |
| TD-ROW-L5030 | both |
| TD-ROW-L5031 | both |
| TD-ROW-L5032 | both |
| TD-ROW-L5033 | both |
| TD-ROW-L5034 | both |
| TD-ROW-L5035 | both |
| F-ART | both |
| BRAIN-A08 | both |
| BRAIN-D11 | both |

## Prerequisites

- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T009 — Enforce scoped service identity and shared admission](../../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T006 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T007 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T011 — Run fenced recovery and bounded retries](../../09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/media/src/main/kotlin/com/spoolin/media/features/upload/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/features/processing/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/features/asset/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/resources/db/migration/V002__assets_and_references.sql | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/features/processing/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/integrationTest/kotlin/com/spoolin/media/recovery/processing/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/features/upload/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/features/asset/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | modify | ops/containers/ | Extend the artifact created by T003; serialize this shared write |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [4.4 SpoolIn Media service](../../../04-spoolin-techdoc.md#44-spoolin-media-service): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.2.5 Media messages](../../../04-spoolin-techdoc.md#525-media-messages): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.6 Media tables](../../../04-spoolin-techdoc.md#626-media-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.6 Artwork transfer and reference-safe attachment](../../../04-spoolin-techdoc.md#76-artwork-transfer-and-reference-safe-attachment): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.3.2 Output and executable-input controls](../../../04-spoolin-techdoc.md#832-output-and-executable-input-controls): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-248-T1-R01 · L10183](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-248-T1-R02 · L10184](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `upload_operation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL UNIQUE REFERENCES operation_receipt(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-248-T1-R03 · L10185](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `actor_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-248-T1-R04 · L10186](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `state` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (state IN ('RECEIVING','PROCESSING','READY','REJECTED','DELETING','DELETED')); no SQL default; owner supplies value` |
| [ATDD-248-T1-R05 · L10187](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `storage_key` (`varchar(160)`); Source-valid technical token; 160 versus 161 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `UNIQUE; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-248-T1-R06 · L10188](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `content_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (octet_length(content_hash)=32); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-248-T1-R07 · L10189](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `format` (`varchar(8)`); Source-valid technical token; 8 versus 9 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (format IN ('JPEG','PNG','WEBP')); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-248-T1-R08 · L10190](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `input_bytes` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (input_bytes > 0); no SQL default; owner supplies value` |
| [ATDD-248-T1-R09 · L10191](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `output_bytes` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (output_bytes > 0); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-248-T1-R10 · L10192](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `width` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (width > 0); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-248-T1-R11 · L10193](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `height` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (height > 0); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-248-T1-R12 · L10194](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `policy_revision` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-248-T1-R13 · L10195](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `provenance` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; measure PostgreSQL jsonb::text bytes at 8192 and ±1; Omission supplies CAST('{}' AS jsonb); explicit NULL fails23502; `NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(provenance)='object' AND octet_length(provenance::text)<=8192); NOT NULL (explicit or PK)` |
| [ATDD-248-T1-R14 · L10196](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `error_code` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-248-T1-R15 · L10197](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-248-T1-R16 · L10198](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-250-T1-R01 · L10296](../../../05-atdd.md#case-atdd-250-enforce-embedded-processing-acceptance-and-crash-recovery) | `processing_work_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL before accepted processing or after evidence purge; UNIQUE; FK durable_work DELETE RESTRICT` |
| [ATDD-250-T1-R02 · L10297](../../../05-atdd.md#case-atdd-250-enforce-embedded-processing-acceptance-and-crash-recovery) | `processing_staging_key` (`varchar(160)`); Source-valid technical token; 160 versus 161 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL with no processing evidence; required when evidence exists` |
| [ATDD-250-T1-R03 · L10298](../../../05-atdd.md#case-atdd-250-enforce-embedded-processing-acceptance-and-crash-recovery) | `processing_input_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL with no evidence; otherwise exactly 32 bytes` |
| [ATDD-250-T1-R04 · L10299](../../../05-atdd.md#case-atdd-250-enforce-embedded-processing-acceptance-and-crash-recovery) | `processing_created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL with no evidence; owner sets on acceptance` |
| [ATDD-254-T1-R01 · L10536](../../../05-atdd.md#case-atdd-254-verify-mediaservice-typed-service-and-actor-boundary) | `Upload`; Upload(stream UploadChunk) → Outcome |
| [J-04 · L90](../../../02-PRD.md#32-application-journeys) | Maintainer uploads/replaces artwork \| Active movie exists; authorized Maintainer selects a file \| See transfer, processing, and confirmed attachment; preserve existing artwork on replacement failure |
| [FR-002 · L236](../../../02-PRD.md#61-functional-requirements) | Search movie titles and cast/creator names with the approved matching and visibility rules \| A11/A14 Required; product policy Committed (section 13.1) \| AC-006 through AC-010, AC-022, AC-134, AC-147, AC-159 |
| [FR-009 · L243](../../../02-PRD.md#61-functional-requirements) | Upload movie artwork with validation and distinct transfer/processing/saved feedback \| A08/A14 Required \| AC-038 through AC-042, AC-149 through AC-151 |
| [FR-010 · L244](../../../02-PRD.md#61-functional-requirements) | Recover failed/uncertain artwork changes while protecting prior and still-used artwork \| D20/D21 Committed; product policy Committed (section 13.1); D11 mechanics Open \| AC-043 through AC-046, AC-152, AC-155, AC-157 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-001 · L259](../../../02-PRD.md#62-quality-and-delivery-requirements) | Required project stack and real frontend/backend protocols \| A01 through A05, D03 Required \| AC-064 through AC-066; section 10 |
| [NFR-002 · L260](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preferred App/Catalog/Media ownership with unrelated catalog work usable during artwork outage \| D02/D10 Working design \| AC-067, AC-068; section 10 |
| [NFR-005 · L263](../../../02-PRD.md#62-quality-and-delivery-requirements) | Setup rejects unusable configuration and protects secrets \| D13/D21; configuration and secret protection Committed \| AC-076 through AC-078, AC-153 through AC-155, AC-157 |
| [NFR-007 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Responsive, keyboard-usable journeys with accurate feedback and retained recoverable work \| A13/A14 Required; keyboard use Committed \| AC-083 through AC-085, AC-139, AC-147, AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-013 · L271](../../../02-PRD.md#62-quality-and-delivery-requirements) | Interfaces implement the product's validation, optional data, access, and failure behavior \| A04/A05/A14 Required support; detailed contracts Open \| AC-100 through AC-103, AC-142/AC-143, AC-148 |
| [NFR-016 · L274](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [A03 · L282](../../../02-PRD.md#63-project-requirements-coverage) | At least two meaningful Spring Boot services \| NFR-001 / AC-065 |
| [A04 · L283](../../../02-PRD.md#63-project-requirements-coverage) | Real service-to-service gRPC \| NFR-001/NFR-008 / AC-065, AC-087 |
| [A05 · L284](../../../02-PRD.md#63-project-requirements-coverage) | Frontend application communication through GraphQL \| NFR-001/NFR-013 / AC-066, AC-100 |
| [A08 · L287](../../../02-PRD.md#63-project-requirements-coverage) | Upload movie artwork \| FR-009/FR-010 / AC-038 through AC-046 |
| [A14 · L293](../../../02-PRD.md#63-project-requirements-coverage) | Sufficient operation feedback \| NFR-007; FR-001/FR-002/FR-004/FR-009 / AC-002, AC-009, AC-018, AC-038, AC-085 |
| [D03 · L304](../../../02-PRD.md#64-project-decision-register) | GraphQL frontend and gRPC backend boundaries \| NFR-001/NFR-013 / AC-065, AC-066, AC-100 |
| [D10 · L311](../../../02-PRD.md#64-project-decision-register) | Catalog owns titles/people/credits, Media owns assets \| NFR-002 / AC-067, AC-068 |
| [D11 · L312](../../../02-PRD.md#64-project-decision-register) | Artwork transport and lifecycle protocol \| Product outcomes settled; mechanics Open in techdoc. FR-009/FR-010 / AC-038 through AC-046 |
| [D19 · L320](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [AC-038 · L419](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Progress \| A Maintainer selects a supported file \| Transfer and processing run \| Show measured transfer progress, separate processing, then saved only after the movie displays the confirmed attachment. Transfer at 100% is not a saved result. |
| [AC-038-C01 · L419](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show measured transfer progress, separate processing, then saved only after the movie displays the confirmed attachment. |
| [AC-038-C02 · L419](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Transfer at 100% is not a saved result. |
| [AC-039 · L420](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Invalid file \| Content is empty, malformed, animated, disguised, unsupported, unsafe, or outside displayed limits \| Upload is attempted \| Explain the applicable format/size/content/processing-limit issue and attach nothing; preserve previous artwork. Enforce section 5.5 against actual content, not filename or declared type alone. The techdoc defines validation mechanisms and processing limits. |
| [AC-039-C01 · L420](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Explain the applicable format/size/content/processing-limit issue and attach nothing; preserve previous artwork. |
| [AC-039-C02 · L420](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Enforce section 5.5 against actual content, not filename or declared type alone. |
| [AC-039-C03 · L420](../../../02-PRD.md#us-007-upload-and-recover-artwork) | The techdoc defines validation mechanisms and processing limits. |
| [AC-040 · L421](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Permission/target changes \| Permission expires or the movie is archived/deleted during upload \| Attachment is attempted \| Do not attach to an unauthorized/unavailable target; explain the outcome and preserve the previous valid artwork. |
| [AC-040-C01 · L421](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not attach to an unauthorized/unavailable target; explain the outcome and preserve the previous valid artwork. |
| [AC-041 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Interrupted upload \| Transfer is cancelled/interrupted or storage fails \| The operation ends \| Show cancellation/failure without claiming success, preserve the previous artwork, and allow a fresh attempt once the outcome is known. Resume-from-byte support is not required. |
| [AC-041-C01 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show cancellation/failure without claiming success, preserve the previous artwork, and allow a fresh attempt once the outcome is known. |
| [AC-041-C02 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Resume-from-byte support is not required. |
| [AC-042 · L423](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Repeated upload \| A completed/pending upload is repeated \| Requests are processed \| Do not create unintended duplicate attachments; show the known result or pending outcome instead of treating each repeat as a new success. |
| [AC-042-C01 · L423](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not create unintended duplicate attachments; show the known result or pending outcome instead of treating each repeat as a new success. |
| [AC-043 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Upload succeeded, attachment failed \| File transfer/processing completed but the movie was not updated \| The Maintainer sees the result \| Clearly report that artwork was not attached; keep prior artwork, and offer a safe retry after confirming the failed outcome. No unattached image becomes public. |
| [AC-043-C01 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Clearly report that artwork was not attached; keep prior artwork, and offer a safe retry after confirming the failed outcome. |
| [AC-043-C02 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | No unattached image becomes public. |
| [AC-044 · L425](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Replacement \| A movie already has valid artwork \| Replacement succeeds or fails \| Success displays the new confirmed image; failure preserves the previous image. Later cleanup cannot remove another title's valid artwork. |
| [AC-044-C01 · L425](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Success displays the new confirmed image; failure preserves the previous image. |
| [AC-044-C02 · L425](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Later cleanup cannot remove another title's valid artwork. |
| [AC-045 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Uncertain attachment \| Confirmation is lost after submission \| The Maintainer checks status \| Show uncertainty while resolving the result; display success or a recoverable failure only when known. Do not ask for a blind repeat or delete possibly attached artwork. |
| [AC-045-C01 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show uncertainty while resolving the result; display success or a recoverable failure only when known. |
| [AC-045-C02 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not ask for a blind repeat or delete possibly attached artwork. |
| [AC-046 · L427](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Interrupted cleanup \| Cleanup/recovery overlaps another valid use or a restart \| Work resumes \| Existing attachments stay usable; recovery does not remove referenced artwork or report missing output as completed. |
| [AC-046-C01 · L427](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Existing attachments stay usable; recovery does not remove referenced artwork or report missing output as completed. |
| [AC-068 · L465](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | Artwork outage \| Artwork handling is unavailable but catalog work is available \| A Maintainer edits movie metadata \| Unrelated metadata work can succeed; image-dependent work explains its failure without claiming attachment success. |
| [AC-068-C01 · L465](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | Unrelated metadata work can succeed; image-dependent work explains its failure without claiming attachment success. |
| [AC-147 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Plain-text safety \| Entered catalog text, names, search text, or changed values contain markup, apostrophes, or instruction-like content within applicable limits \| They appear in forms, catalog/detail pages, search results, previews, or audit history \| Treat the content as text and execute no embedded code or instructions. Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. Private registration data retain their access boundaries. |
| [AC-147-C01 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Treat the content as text and execute no embedded code or instructions. |
| [AC-147-C02 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. |
| [AC-147-C03 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Private registration data retain their access boundaries. |
| [AC-149 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Artwork byte boundaries \| Recommended file bounds of 1 to 5,242,880 bytes are configured \| A Maintainer submits an empty file, a one-byte invalid file, a valid supported image exactly at the maximum, or a file one byte above it \| Reject empty/undecodable content and the oversized file with the specific reason; the exact maximum passes the size check subject to all content/processing checks. Attach no rejected image and preserve previous artwork. MiB is displayed with its exact byte interpretation. |
| [AC-149-C01 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject empty/undecodable content and the oversized file with the specific reason; the exact maximum passes the size check subject to all content/processing checks. |
| [AC-149-C02 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Attach no rejected image and preserve previous artwork. |
| [AC-149-C03 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | MiB is displayed with its exact byte interpretation. |
| [AC-150 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Static supported formats \| JPEG, PNG, and WebP are enabled and an active movie may lack artwork \| A Maintainer uploads valid static examples or attempts GIF, SVG, animated PNG/WebP, or another unsupported format \| Supported static files proceed through upload/processing to confirmed attachment if other checks pass. Reject unsupported or animated content with correction feedback and no attachment; do not silently convert animation into an accepted static upload. Missing artwork remains a placeholder on failure. |
| [AC-150-C01 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Supported static files proceed through upload/processing to confirmed attachment if other checks pass. |
| [AC-150-C02 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject unsupported or animated content with correction feedback and no attachment; do not silently convert animation into an accepted static upload. |
| [AC-150-C03 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Missing artwork remains a placeholder on failure. |
| [AC-151 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Disguised, malformed, or unsafe image \| A file has a permitted extension or declared type but invalid actual content, or a small compressed image exceeds the adopted processing bounds \| Artwork is validated or processed \| Reject it without attachment or unbounded processing; preserve prior artwork and report a safe content/processing-limit failure. Filename, declared type, or signature alone cannot bypass validation. Numerical processing bounds are established under OD-007/OD-019 before implementation. |
| [AC-151-C01 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject it without attachment or unbounded processing; preserve prior artwork and report a safe content/processing-limit failure. |
| [AC-151-C02 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Filename, declared type, or signature alone cannot bypass validation. |
| [AC-151-C03 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Numerical processing bounds are established under OD-007/OD-019 before implementation. |
| [AC-152 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Replacement failure and correction \| A movie has valid artwork and a replacement is rejected or fails processing/attachment \| The Maintainer inspects the result and selects a corrected image after the failed outcome is known \| Existing artwork and unrelated movie information remain available; show the failure and effective rules, then allow a safe fresh attempt. Confirm a replacement only after attachment. Uncertain outcomes still require reconciliation under AC-045 before repeat submission. |
| [AC-152-C01 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing artwork and unrelated movie information remain available; show the failure and effective rules, then allow a safe fresh attempt. |
| [AC-152-C02 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Confirm a replacement only after attachment. |
| [AC-152-C03 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Uncertain outcomes still require reconciliation under AC-045 before repeat submission. |
| [AC-157 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Policy changes during a draft/upload \| Input or artwork met earlier limits, and the policy changes before submission/attachment \| The operation is evaluated against the effective policy \| If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-157-C01 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. |
| [AC-157-C02 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-065 · L856](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise meaningful real gRPC features across at least two running Spring Boot applications \| NFR-001; technical design |
| [AC-066 · L857](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify frontend GraphQL and documented upload/retrieval/auth exceptions against A05; reconcile the MCP boundary \| NFR-001/NFR-013; technical design |
| [AC-087 · L870](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration \| NFR-008; technical design |
| [AC-103 · L879](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify effective input/file policies from section 5.5; establish remaining request/query/page and decoded-image resource bounds with understandable outcomes \| NFR-013/NFR-016; technical design |
| [G-02 · L923](../../../02-PRD.md#122-release-gates-remain-planned) | Mandated stack/protocols and chosen integration approach work \| NFR-001/NFR-002/NFR-013; AC-064 through AC-067, AC-100 through AC-103 \| Planned in techdoc |
| [G-04 · L925](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
| [G-05 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [OD-007 · L1045](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Upload UX, supported static formats, configurable size policy, and rejection/recovery outcomes fixed in section 5.5; adoption of Recommended byte bounds, transport/A05 interpretation, content checks, decoded-dimension/resource bounds, processing, storage/retrieval remain for techdoc \| FR-009/FR-010/FR-017, NFR-001/NFR-013/NFR-016 \| Technical design; before artwork implementation |
| [OD-019 · L1055](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Product discovery/feedback and section 5.5 input/artwork policy settled; GraphQL/gRPC and MCP contracts, human-approval enforcement, consistent counting/validation, safe display/data handling, compatibility, remaining request/query/page and decoding resource bounds, ordering ties/navigation, responsive support matrix, and accessibility verification remain for techdoc \| NFR-007/NFR-013/NFR-016, FR-016/FR-017 \| Technical design; before dependent implementation |
| [US-001 · L333](../../../02-PRD.md#us-001-browse-titles-and-people) | ### US-001: Browse titles and people As a visitor or Viewer, I want to inspect titles, artwork, and credits so I can identify the work and its people. Requirements: FR-001, NFR-007. Reading needs no account and changes no catalog information. |
| [US-007 · L413](../../../02-PRD.md#us-007-upload-and-recover-artwork) | ### US-007: Upload and recover artwork As a Maintainer, I want to know which image a movie actually uses and recover from failed uploads. Requirements: FR-009, FR-010. Artwork management applies to active movies; current permissions are required throughout the operation. |
| [US-011 · L459](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | ### US-011: Continue metadata work during an artwork outage As a Maintainer, I want to edit movie metadata when artwork handling is unavailable so I can continue work that does not need an image change. Requirement: NFR-002. Related project technology constraints NFR-001 and AC-064 through AC-067 remain in section 10. |
| [US-021 · L523](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | ### US-021: Submit accepted input and correct validation failures As a Visitor registering or searching, or a Maintainer entering catalog information or artwork, I want visible limits and specific correction feedback so I can complete a permitted action without losing valid work. Requirements: FR-017, NFR-016, NFR-005, NFR-007; related capabilities FR-002/FR-004/FR-005/FR-006/FR-009/FR-010/FR-011/FR-016. Section 5.5 defines the policies and their statuses. Selected name defaults are Committed; other numerical defaults remain Recommended. Scenarios using recommended numbers assume those values have been configured. Configuration verification concerns restricted deployment facilities, not another application role or settings screen. |
| [PRD-PERM-03 · L70](../../../02-PRD.md#31-permission-matrix) | Create/update movies and manage their credits/artwork \| No \| No \| Yes |
| [PRD-POLICY-06 · L126](../../../02-PRD.md#52-removal-and-editing-rules) | Archiving hides a movie from public/Viewer listings, search, filters, direct detail access, and public credit views. It preserves the movie's remaining metadata, artwork, and credits in the Maintainer-only archive. A Maintainer may restore it or permanently delete it with a separate confirmation that identifies the movie and irreversibility. There is no direct permanent-delete action for active movies and no restore after permanent deletion. Archive editing is limited to restore/permanent delete; restore first to edit movie content. |
| [PRD-POLICY-13 · L140](../../../02-PRD.md#52-removal-and-editing-rules) | A failed artwork replacement keeps the current valid image. A confirmed replacement changes the visible image; any later cleanup must not undo it. Transfer completion alone is not attachment success. Unattached or uncertain artwork remains outside public display until resolved. |
| [PRD-POLICY-27 · L204](../../../02-PRD.md#artwork-policy) | Artwork upload and replacement are Maintainer capabilities for active movies. Existing progress, permissions, attachment, and recovery rules remain binding. |
| [PRD-POLICY-28 · L213](../../../02-PRD.md#artwork-policy) | The maximum is an app recommendation informed by Supabase's standard-upload guidance, which recommends that method for files up to 6 MB. The recommendation does not select a storage provider or upload transport, and it is not a universal poster standard. A one-byte minimum merely rejects empty content; every accepted file must also be a valid, decodable supported image. |
| [PRD-POLICY-29 · L215](../../../02-PRD.md#artwork-policy) | File-size bounds are configurable in bytes and inclusive. Display the effective size limits and enabled formats before file selection. Validate actual content and decodability; an extension, filename, declared content type, or signature alone cannot establish validity. An unsupported, animated, malformed, disguised, or unsafe file attaches nothing. Explain the applicable format, size, content, or processing-limit issue and let the Maintainer select a corrected file. A failed or rejected replacement preserves current artwork and movie information. Distinguish uploading, processing, saved, failed, and uncertain outcomes; transfer completion does not bypass processing or confirm attachment. |
| [PRD-POLICY-30 · L217](../../../02-PRD.md#artwork-policy) | Require bounded image processing even when compressed file size is within its limit. Numerical limits on decoded dimensions and resource consumption, content-handling safeguards, and image-processing mechanisms remain OD-007/OD-019 technical-design matters before artwork implementation. OWASP file-upload guidance supports content checks and processing safeguards; a byte limit alone does not establish upload safety. |
| [PRD-POLICY-31 · L221](../../../02-PRD.md#configuration-and-existing-content) | Configuration must supply finite whole-number bounds in the appropriate units, with nonnegative text bounds, positive file-size bounds, and minimum no greater than maximum. A required field cannot have a zero effective minimum. Enabled artwork formats must be a nonempty subset of the supported static formats. An unset setting follows its deliberately documented default; an explicitly empty, invalid, or unsupported setting is not treated as unset. Negative values, reversed bounds, and unsupported formats prevent affected functionality from becoming ready, with actionable redacted diagnostics and no silent unlimited or permissive fallback. OD-020 owns configuration keys, sources, precedence, application/reload timing, and distribution of the effective policy. |
| [PRD-POLICY-32 · L223](../../../02-PRD.md#configuration-and-existing-content) | Changes to limits do not automatically truncate stored content, remove artwork, or invalidate existing password credentials. Existing records and artwork remain readable under their permissions. An unchanged stored field may be retained while other fields are edited; newly entered or changed values must meet current limits. Explicitly clearing an optional field remains allowed. Existing credentials must not be rejected solely because a new-password minimum changed. This compatibility rule does not exempt malicious new submissions or permit unsupported new artwork. |
| [PRD-FEEDBACK-02 · L796](../../../02-PRD.md#810-shared-feedback-rules) | Empty / unknown \| Distinguish empty catalog, no search matches, no audit events, unknown optional metadata, and missing artwork |
| [PRD-CONCEPT-01 · L809](../../../02-PRD.md#9-conceptual-information-and-privacy) | Movie / TV series \| Stable identity, separate title type, display title, original title where different, descriptive metadata, optional artwork, and public credits. Names alone do not identify a work. |
| [PRD-CONCEPT-03 · L811](../../../02-PRD.md#9-conceptual-information-and-privacy) | Optional context \| Original language, production countries, movie runtime, TV status/end context where applicable and known, source-scoped external identifiers, and attributed artwork where available. TV duration means episode runtime. |
| [PRD-HANDOFF-01 · L834](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | A01 through A05; D03 \| Required \| Svelte/TypeScript, Kotlin/Spring Boot, at least two meaningful services, frontend application GraphQL, and real backend gRPC. Document any artwork/auth transport interpretation. |
| [PRD-HANDOFF-02 · L835](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D02/D10 \| Working design \| Preferred App/Catalog/Media ownership, domain authorization, separate data ownership, and unrelated catalog work independent of artwork availability. It is not an external three-service requirement. |
| [PRD-HANDOFF-08 · L841](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D13 \| Committed \| Docker Compose and `make start`, `make stop`, `make restart`, `make status`, `make logs`, `make test-unit`, `make test-integration`, with documented command behavior, isolated tests, readiness checks, failing exits, and persistent data/artwork. |
| [PRD-HANDOFF-13 · L846](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | Input policy; FR-017/NFR-016 \| Committed behavior and name defaults; other numerical defaults Recommended \| Implement section 5.5 with authoritative validation across entry paths, Unicode-code-point counting, plain-text display, safe data handling, inclusive configurable limits, and compatibility for retained data. Establish adopted email/URL/password rules, remaining field/collection bounds, upload content checks and bounded decoding, configuration distribution/application timing, and meaningful boundary/security verification. Configuration/library/transport choices remain open; do not introduce extra UI or turn recommended values into external obligations. |
| [TD-C-036 · L5002](../../../04-spoolin-techdoc.md#626-media-tables) | Media asset single processing metadata group/unique work and upload identities plus independent asset_reference holds; READY required metadata, all-or-none processing, RESERVED/ACTIVE protection, lifetime deletion/reference tombstones. |
| [TD-C-044 · L5382](../../../04-spoolin-techdoc.md#76-artwork-transfer-and-reference-safe-attachment) | Real bounded GraphQL Base64→Core spool→gRPC stream→Media quarantine pipeline; exact bytes/checksum, actual JPEG/PNG/WebP static decode/re-encode, reject malformed/animation/polyglot/bombs; fixed nonroot/no-network process resource limits. |
| [TD-ROW-L5016 · L5016](../../../04-spoolin-techdoc.md#asset) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L5017 · L5017](../../../04-spoolin-techdoc.md#asset) | `upload_operation_id` \| `uuid` \| `NOT NULL UNIQUE REFERENCES operation_receipt(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Idempotent upload owner. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5018 · L5018](../../../04-spoolin-techdoc.md#asset) | `actor_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Logical uploader account. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5019 · L5019](../../../04-spoolin-techdoc.md#asset) | `state` \| `varchar(16)` \| `NOT NULL CHECK (state IN ('RECEIVING','PROCESSING','READY','REJECTED','DELETING','DELETED')); no SQL default; owner supplies value` \| Media lifecycle. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L5020 · L5020](../../../04-spoolin-techdoc.md#asset) | `storage_key` \| `varchar(160)` \| `UNIQUE; NULL allowed; no SQL default; owner supplies value` \| Generated UUID-only relative key, NULL before output publication. internal; presence/clearing §5 \| `String?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5021 · L5021](../../../04-spoolin-techdoc.md#asset) | `content_hash` \| `bytea` \| `CHECK (octet_length(content_hash)=32); NULL allowed; no SQL default; owner supplies value` \| SHA256 re-encoded content; informational, no global dedup. internal; presence/clearing §5 \| `ByteArray?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5022 · L5022](../../../04-spoolin-techdoc.md#asset) | `format` \| `varchar(8)` \| `CHECK (format IN ('JPEG','PNG','WEBP')); NULL allowed; no SQL default; owner supplies value` \| Actual static decoded input format. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L5023 · L5023](../../../04-spoolin-techdoc.md#asset) | `input_bytes` \| `bigint` \| `NOT NULL CHECK (input_bytes > 0); no SQL default; owner supplies value` \| Original exact transferred content bytes. API; presence/clearing §5 \| `Long` → Long(decimal string) / int64 |
| [TD-ROW-L5024 · L5024](../../../04-spoolin-techdoc.md#asset) | `output_bytes` \| `bigint` \| `CHECK (output_bytes > 0); NULL allowed; no SQL default; owner supplies value` \| Processed stored length. API; presence/clearing §5 \| `Long?` → Long(decimal string) / optional int64 |
| [TD-ROW-L5025 · L5025](../../../04-spoolin-techdoc.md#asset) | `width` \| `integer` \| `CHECK (width > 0); NULL allowed; no SQL default; owner supplies value` \| Decoded/display output pixel width. API; presence/clearing §5 \| `Int?` → Int / optional int32 |
| [TD-ROW-L5026 · L5026](../../../04-spoolin-techdoc.md#asset) | `height` \| `integer` \| `CHECK (height > 0); NULL allowed; no SQL default; owner supplies value` \| Decoded/display output pixel height. API; presence/clearing §5 \| `Int?` → Int / optional int32 |
| [TD-ROW-L5027 · L5027](../../../04-spoolin-techdoc.md#asset) | `policy_revision` \| `varchar(64)` \| `NOT NULL; no SQL default; owner supplies value` \| Validation rule revision. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5028 · L5028](../../../04-spoolin-techdoc.md#asset) | `provenance` \| `jsonb` \| `NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(provenance)='object' AND octet_length(provenance::text)<=8192); NOT NULL (explicit or PK)` \| Licensed source/attribution or uploader assertion, never infer rights. API; presence/clearing §5 \| `TypedJsonDTO` → typed projection/JSON / typed message |
| [TD-ROW-L5029 · L5029](../../../04-spoolin-techdoc.md#asset) | `error_code` \| `varchar(64)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Safe rejection category. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L5030 · L5030](../../../04-spoolin-techdoc.md#asset) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L5031 · L5031](../../../04-spoolin-techdoc.md#asset) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L5032 · L5032](../../../04-spoolin-techdoc.md#asset) | `processing_work_id` \| `uuid` \| NULL before accepted processing or after evidence purge; UNIQUE; FK durable_work DELETE RESTRICT \| One processing job per asset; never a public field; never routine telemetry; audit only under §6.1.1 allowlist \| `UUID?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L5033 · L5033](../../../04-spoolin-techdoc.md#asset) | `processing_staging_key` \| `varchar(160)` \| NULL with no processing evidence; required when evidence exists \| Generated quarantine-relative UUID path; file may already be removed after terminal result; never routine telemetry; audit only under §6.1.1 allowlist \| `String?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L5034 · L5034](../../../04-spoolin-techdoc.md#asset) | `processing_input_hash` \| `bytea` \| NULL with no evidence; otherwise exactly 32 bytes \| Hash of original received bytes; separate from output content_hash; never routine telemetry; audit only under §6.1.1 allowlist \| `ByteArray?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L5035 · L5035](../../../04-spoolin-techdoc.md#asset) | `processing_created_at` \| `timestamptz(6)` \| NULL with no evidence; owner sets on acceptance \| Original media_job creation instant; never routine telemetry; audit only under §6.1.1 allowlist \| `Instant?` → Internal only; no GraphQL/protobuf field |
| [BRAIN-A08 · L53](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A08 \| Upload movie artwork \| Valid upload, progress, processing/success feedback, and failure handling |
| [BRAIN-D11 · L80](../../../01-brainstorming-result.md#4-decision-register) | D11 \| Open \| Select artwork transport, attachment/replacement protocol, and recovery/cleanup policy |

## Implementation

1. Implement asset and asset_reference DDL including typed embedded processing evidence, accepted-work uniqueness and lifetime tombstones; actual reference protocol is T023.
2. Accept ordered bounded chunks to generated quarantine paths with exact byte count/checksum, actor/intent binding and streaming cancellation. Commit accepted processing metadata/work atomically before acknowledging durable acceptance.
3. Validate actual static JPEG/PNG/WebP by full bounded decoding/re-encoding; reject empty/oversized/malformed/disguised/animated/polyglot/resource-excess input. Apply byte, dimensions, pixels, CPU, memory, output and time limits independently.
4. Invoke only the selected fixed libvips executable/options with structured arguments and generated paths, nonroot/no-network sandbox. Filename/metadata never become executable flags or paths.
5. Recover database/file crash gaps and orphan temporary state conservatively. Expose READY only when durable valid output exists; no raw upload becomes publicly served.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T022-A1:** Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing.
- **T022-A2:** Execution: hostile path/option metadata executes no extra command, fetch or write outside quarantine/output boundaries.
- **T022-A3:** Durability: crash before/after accepted processing leaves truthful resumable state, one work binding and no false READY or lost protected output.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-168](../../../05-atdd.md#case-atdd-168-reconcile-file-and-database-crash-gaps) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.6 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then unreferenced renamed files reconcile only after the documented grace and ownership checks And accepted unfinished jobs remain recoverable And a committed job with missing output reports failure rather than completion Observation: Inspect actual renamed/fsynced files and job metadata at each crash barrier. After restart, distinguish eligible unreferenced files from accepted unfinished jobs; deleting a committed file yields visible failed receipt, not invented successful output. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-ART · L107](../../../05-atdd.md#f-art-and-f-images-bounded-independent-files) | OLD/NEW are different static licensed or generated test images with independently measured SHA-256, dimensions, format and byte count. Supply valid staticJPEG/PNG/WebP;0-byte, random1-byte, exact5, 242, 880 and one-above files; malformed/disguised/animatedAPNG/WebP/GIF/SVG/polyglot fixtures; dimension8191/8192/8193, pixel39, 999, 999/40, 000, 000/40, 000, 001 and resource-bound examples. A size-exact supported file may use a valid independently verified bounded ancillary segment; it still must fully decode and satisfy every other limit. Separate byte acceptance from content acceptance. Source limits independently cap decoded160Mbytes, RSS256 MiB, CPU5 s, wall10 s, temp32 MiB, output8 MiB. Never use real private user files. Reserve/attach/cancel/cleanup cases record immutable asset/hold/title/operation tuples and actual selected bytes before/after. |

Partial implementation contributions: [ATDD-064](../../../05-atdd.md#case-atdd-064-artwork-outage), [ATDD-038](../../../05-atdd.md#case-atdd-038-progress), [ATDD-039](../../../05-atdd.md#case-atdd-039-invalid-file), [ATDD-040](../../../05-atdd.md#case-atdd-040-permissiontarget-changes), [ATDD-041](../../../05-atdd.md#case-atdd-041-interrupted-upload), [ATDD-042](../../../05-atdd.md#case-atdd-042-repeated-upload), [ATDD-043](../../../05-atdd.md#case-atdd-043-upload-succeeded-attachment-failed), [ATDD-044](../../../05-atdd.md#case-atdd-044-replacement), [ATDD-045](../../../05-atdd.md#case-atdd-045-uncertain-attachment), [ATDD-046](../../../05-atdd.md#case-atdd-046-interrupted-cleanup), [ATDD-123](../../../05-atdd.md#case-atdd-123-artwork-byte-boundaries), [ATDD-124](../../../05-atdd.md#case-atdd-124-static-supported-formats), [ATDD-125](../../../05-atdd.md#case-atdd-125-disguised-malformed-or-unsafe-image), [ATDD-126](../../../05-atdd.md#case-atdd-126-replacement-failure-and-correction), [ATDD-159](../../../05-atdd.md#case-atdd-159-validate-the-complete-artwork-stream-boundary), [ATDD-160](../../../05-atdd.md#case-atdd-160-bound-full-decoding-independently-of-compressed-size), [ATDD-161](../../../05-atdd.md#case-atdd-161-keep-file-content-away-from-execution-and-paths), [ATDD-162](../../../05-atdd.md#case-atdd-162-recover-crashes-between-intent-and-byte-acceptance), [ATDD-163](../../../05-atdd.md#case-atdd-163-protect-a-candidate-before-selecting-it), [ATDD-169](../../../05-atdd.md#case-atdd-169-serve-only-currently-authorized-processed-artwork), [ATDD-170](../../../05-atdd.md#case-atdd-170-show-measured-stages-and-resume-the-original-outcome), [ATDD-248](../../../05-atdd.md#case-atdd-248-enforce-artwork-asset-output-storage-and-private-projection), [ATDD-250](../../../05-atdd.md#case-atdd-250-enforce-embedded-processing-acceptance-and-crash-recovery), [ATDD-263](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback), [ATDD-315](../../../05-atdd.md#case-atdd-315-check-browser-file-policy-before-transfer-without-trusting-mime), [ATDD-305](../../../05-atdd.md#case-atdd-305-separate-real-transfer-events-from-processing-display), [ATDD-325](../../../05-atdd.md#case-atdd-325-keep-filename-and-metadata-payloads-outside-processor-execution). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T022` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T022` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- Hand the owned outputs to [T026 — Connect measured progress and recover attachment outcomes](../../16-artwork-management-browser-journeys/tasks/T026-connect-measured-progress-and-recover-attachment-outcomes.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
