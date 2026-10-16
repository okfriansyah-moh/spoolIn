---
kind: task
id: T014
group: G11
depends_on:
- T005
- T008
- T009
- T006
- T007
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T014 — Persist encrypted drafts with durable save and discard receipts

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-048 | implements |
| ATDD-178 | both |
| ATDD-179 | implements |
| ATDD-180 | verifies |
| ATDD-233 | implements |
| ATDD-233-T1-R01 | implements |
| ATDD-233-T1-R02 | implements |
| ATDD-233-T1-R03 | implements |
| ATDD-233-T1-R04 | implements |
| ATDD-233-T1-R05 | implements |
| ATDD-233-T1-R06 | implements |
| ATDD-233-T1-R07 | implements |
| ATDD-233-T1-R08 | implements |
| ATDD-233-T1-R09 | implements |
| ATDD-233-T1-R10 | implements |
| ATDD-233-T1-R11 | implements |
| ATDD-233-T1-R12 | implements |
| ATDD-233-T1-R13 | implements |
| ATDD-251-T1-R18 | implements |
| ATDD-251-T1-R35 | implements |
| ATDD-251-T1-R36 | implements |
| ATDD-259 | implements |
| ATDD-293 | both |
| ATDD-319 | both |
| ATDD-320 | both |
| ATDD-329 | both |
| ATDD-330 | both |
| ATDD-331 | both |
| ATDD-332 | both |
| ATDD-333 | implements |
| FR-011 | implements |
| NFR-008 | both |
| NFR-015 | both |
| D05 | implements |
| D12 | implements |
| AC-048 | implements |
| AC-048-C01 | implements |
| AC-048-C02 | implements |
| AC-123 | both |
| AC-123-C01 | both |
| AC-128 | both |
| AC-128-C01 | both |
| G-05 | both |
| TD-C-023 | implements |
| TD-C-041 | both |
| TD-ROW-L2741 | both |
| TD-ROW-L2758 | both |
| TD-ROW-L2759 | both |
| TD-ROW-L3242 | both |
| TD-ROW-L3243 | both |
| TD-ROW-L3244 | both |
| TD-ROW-L3245 | both |
| TD-ROW-L3246 | both |
| TD-ROW-L3247 | both |
| TD-ROW-L3248 | both |
| TD-ROW-L3249 | both |
| TD-ROW-L4292 | both |
| TD-ROW-L4293 | both |
| TD-ROW-L4294 | both |
| TD-ROW-L4295 | both |
| TD-ROW-L4296 | both |
| TD-ROW-L4297 | both |
| TD-ROW-L4298 | both |
| TD-ROW-L4299 | both |
| TD-ROW-L4300 | both |
| TD-ROW-L4301 | both |
| TD-ROW-L4302 | both |
| TD-ROW-L4303 | both |
| TD-ROW-L4304 | both |
| F-DRAFT-REPLAY | both |

## Prerequisites

- [T005 — Protect account identities and credential storage](../../03-account-access-credential-storage/tasks/T005-protect-account-identities-and-credential-storage.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged.
- [T008 — Implement registration and authoritative sessions](../../06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.
- [T009 — Enforce scoped service identity and shared admission](../../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T006 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T007 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/draft/ | T014 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/db/migration/V005__protected_drafts.sql | T014 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/draft/ | T014 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/integrationTest/kotlin/com/spoolin/core/recovery/drafts/ | T014 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.4 Forms, drafts and conflict recovery](../../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.6.5 Code example: save and reconcile a draft](../../../04-spoolin-techdoc.md#565-code-example-save-and-reconcile-a-draft): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.11 JSON projections and draft schemas](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.4 Core tables](../../../04-spoolin-techdoc.md#624-core-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.3.1 Durable draft receipts](../../../04-spoolin-techdoc.md#731-durable-draft-receipts): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-233-T1-R01 · L9385](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-233-T1-R02 · L9386](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `account_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-233-T1-R03 · L9387](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `target_kind` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (target_kind IN ('MOVIE','PERSON','CREDIT')); no SQL default; owner supplies value` |
| [ATDD-233-T1-R04 · L9388](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `target_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-233-T1-R05 · L9389](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `base_version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (base_version >= 0); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-233-T1-R06 · L9390](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `operation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-233-T1-R07 · L9391](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `policy_revision` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-233-T1-R08 · L9392](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `body_key_id` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-233-T1-R09 · L9393](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `body_nonce` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 12 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(body_nonce)=12); no SQL default; owner supplies value` |
| [ATDD-233-T1-R10 · L9394](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `body_cipher` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 16, 65552 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(body_cipher) BETWEEN 16 AND 65552); no SQL default; owner supplies value` |
| [ATDD-233-T1-R11 · L9395](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` |
| [ATDD-233-T1-R12 · L9396](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-233-T1-R13 · L9397](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage) | `expires_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-251-T1-R18 · L10364](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `drafts`; Authenticated owner / Core \| At most 20 eligible decrypted drafts, TTL 24 h; stale target/operation reviewed before restore; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R35 · L10381](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `saveDraft`; Authenticated owner / Core \| Target-specific non-secret schema, 64 KiB plaintext, max 20; create requires absent expectedVersion, update requires matching version; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R36 · L10382](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `discardDraft`; Authenticated owner / Core \| Draft UUID/version CAS; delete own draft only, unknown own draft false; stale existing version conflicts; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [FR-011 · L245](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-015 · L273](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [D05 · L306](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D12 · L313](../../../02-PRD.md#64-project-decision-register) | Authentication mechanism and verified internal identity \| Product access settled; mechanics Open in techdoc. FR-011/FR-012 / AC-047 through AC-052 |
| [AC-048 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Expiry/sign-out \| A user signs out or their sign-in expires during work \| They attempt another restricted action \| Require sign-in again; public browsing remains available. Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-048-C01 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Require sign-in again; public browsing remains available. |
| [AC-048-C02 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-123 · L518](../../../02-PRD.md#us-020-protect-private-account-information) | Private data boundaries \| Accounts contain names/emails and password credentials \| Public pages, catalog search, other users' access, diagnostics, or MCP proposals are inspected \| Private account information is not exposed outside its authorized purpose; email and private name data receive the selected encrypted-at-rest protection. |
| [AC-123-C01 · L518](../../../02-PRD.md#us-020-protect-private-account-information) | Private account information is not exposed outside its authorized purpose; email and private name data receive the selected encrypted-at-rest protection. |
| [AC-128 · L521](../../../02-PRD.md#us-020-protect-private-account-information) | Protection failure \| Encryption/decryption or required key access fails \| A protected operation is attempted \| Fail with non-sensitive feedback and operational evidence; never fall back to plaintext, invent missing account values, or overwrite valid protected data with an empty value. |
| [AC-128-C01 · L521](../../../02-PRD.md#us-020-protect-private-account-information) | Fail with non-sensitive feedback and operational evidence; never fall back to plaintext, invent missing account values, or overwrite valid protected data with an empty value. |
| [G-05 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [TD-C-023 · L3234](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | Validate exact policy/draft/audit/provenance JSON projections. Drafts preserve unnormalized correction text within 64KiB; movie/person/credit typed shapes reject unknown/secret keys. Reviewed citation identity does not infer provider entity identity or server fetching. |
| [TD-C-041 · L5262](../../../04-spoolin-techdoc.md#731-durable-draft-receipts) | Draft receipts replay before mutable version/quota/policy, operation→account→draft lock, account quota20 and 24h TTL; save/discard original safe result after later edit/expiry/discard, no nonce/TTL/audit duplication or resurrection. |
| [TD-ROW-L2741 · L2741](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `drafts` \| Authenticated owner / Core \| At most 20 eligible decrypted drafts, TTL 24 h; stale target/operation reviewed before restore |
| [TD-ROW-L2758 · L2758](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `saveDraft` \| Authenticated owner / Core \| Required top-level operationId; MutationResult with original safe ID/version receipt; target-specific schema, 64 KiB plaintext, max 20; create omits expectedVersion, update matches version; replay never saves again |
| [TD-ROW-L2759 · L2759](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `discardDraft` \| Authenticated owner / Core \| Required top-level operationId; DraftDiscardResult with original discarded flag/receipt; delete own draft only, absent/hidden draft false without disclosure; stale existing owned version conflicts; replay never deletes a newer draft |
| [TD-ROW-L3242 · L3242](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | `Policy.textBounds` \| Object keyed by registrationName, personName, title, originalTitle, synopsis, creditRole, characterName, biography, email, password, search, sourceUrl and reuseBasis. Values `{minCodePoints:Int,maxCodePoints:Int,required:Boolean,normalization:"TRIM_WHITE_SPACE" or "EXACT"}` \| All keys present; password EXACT, ordinary text TRIM_WHITE_SPACE. Effective configured bounds follow §11.1; sourceUrl max 2048 and reuseBasis max 4000 are request-envelope bounds |
| [TD-ROW-L3243 · L3243](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | `Policy.artwork` \| `{maxInputBytes:LongString,formats:["JPEG","PNG","WEBP"],maxDimensionPx:Int,maxPixels:LongString,maxDecodedBytes:LongString,maxOutputBytes:LongString}` \| Enabled nonempty subset of formats; physical/processing envelopes from §7.6; decimal strings preserve bigint values |
| [TD-ROW-L3244 · L3244](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | `Policy.requestLimits` \| `{metadataBytes:Int,uploadEnvelopeBytes:Int,graphqlQueryBytes:Int,maxDepth:Int,maxFields:Int,maxAliases:Int,maxCost:Int,defaultPage:Int,maxPage:Int}` \| Positive effective values from §5.3; defaults 131072/7100000/16384/8/200/20/1000/24/100 respectively |
| [TD-ROW-L3245 · L3245](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | `Draft.values` for MOVIE \| Partial object using only MovieInput fields \| Draft may be incomplete/blank or exceed current field policy while being corrected; retain exact non-secret values within 65,536 plaintext bytes. Check types, UUID/date component syntax, Unicode validity and safe envelope, then apply full current business validation only at commit |
| [TD-ROW-L3246 · L3246](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | `Draft.values` for PERSON \| Partial `{name:String,biography:String or null}` \| Same safe draft envelope; targetId refers to edited person or is absent for a new person |
| [TD-ROW-L3247 · L3247](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | `Draft.values` for CREDIT \| Partial `{titleId:UUID,personId:UUID,roleId:UUID,characters:[String]}` \| titleId identifies parent movie for a new credit; targetId identifies an existing credit for editing. Array entries contain no null; no invented character-count limit; recovery rechecks endpoints and role |
| [TD-ROW-L3248 · L3248](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | `AuditEvent.changes` \| Object of allowlisted catalog field paths → `{before:scalar/array/object/null,after:scalar/array/object/null}` \| Owner constructs bounded public-field diffs, opaque IDs and lifecycle/version changes; never arbitrary user JSON, account PII or cryptographic material |
| [TD-ROW-L3249 · L3249](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas) | `Provenance.fieldSources` \| Object keyed by supported movie/person/credit field → `{url:String,retrievedAt:DateTime,reuseBasis:String,precision:String or null}` \| HTTP/HTTPS references and factual precision; unknown precision null; each source record has at most one evidence item per field, multiple source records can support a field |
| [TD-ROW-L4292 · L4292](../../../04-spoolin-techdoc.md#form_draft) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → `Draft.id` / no draft RPC |
| [TD-ROW-L4293 · L4293](../../../04-spoolin-techdoc.md#form_draft) | `account_id` \| `uuid` \| `NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Only this account may retrieve/update. internal; presence/clearing §5 \| `UUID` → owner predicate only; no Draft output field / no draft RPC |
| [TD-ROW-L4294 · L4294](../../../04-spoolin-techdoc.md#form_draft) | `target_kind` \| `varchar(16)` \| `NOT NULL CHECK (target_kind IN ('MOVIE','PERSON','CREDIT')); no SQL default; owner supplies value` \| Allowlisted form type. protected owner API; presence/clearing §5 \| `String` → `DraftInput.targetKind`, `Draft.targetKind` (String) / no draft RPC |
| [TD-ROW-L4295 · L4295](../../../04-spoolin-techdoc.md#form_draft) | `target_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Logical Catalog ID; NULL for creation. protected owner API; presence/clearing §5 \| `UUID?` → `DraftInput.targetId`, `Draft.targetId` (nullable UUID) / no draft RPC |
| [TD-ROW-L4296 · L4296](../../../04-spoolin-techdoc.md#form_draft) | `base_version` \| `bigint` \| `CHECK (base_version >= 0); NULL allowed; no SQL default; owner supplies value` \| NULL for new entity. protected owner API; presence/clearing §5 \| `Long?` → `DraftInput.baseVersion`, `Draft.baseVersion` (nullable Long string) / no draft RPC |
| [TD-ROW-L4297 · L4297](../../../04-spoolin-techdoc.md#form_draft) | `operation_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Logical original Catalog submission ID to reconcile before resuming; distinct from the top-level Core save/discard receipt ID. protected owner API; presence/clearing §5 \| `UUID?` → `DraftInput.operationId`, `Draft.operationId` (nullable Catalog UUID) / no draft RPC |
| [TD-ROW-L4298 · L4298](../../../04-spoolin-techdoc.md#form_draft) | `policy_revision` \| `varchar(64)` \| `NOT NULL; no SQL default; owner supplies value` \| SHA-256 policy version in hex. internal; presence/clearing §5 \| `String` → `DraftInput.policyRevision` only; no Draft output field / no draft RPC |
| [TD-ROW-L4299 · L4299](../../../04-spoolin-techdoc.md#form_draft) | `body_key_id` \| `varchar(64)` \| `NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Encryption key ID, no secret key material. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4300 · L4300](../../../04-spoolin-techdoc.md#form_draft) | `body_nonce` \| `bytea` \| `NOT NULL CHECK (octet_length(body_nonce)=12); no SQL default; owner supplies value` \| 96-bit unique per-key nonce. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4301 · L4301](../../../04-spoolin-techdoc.md#form_draft) | `body_cipher` \| `bytea` \| `NOT NULL CHECK (octet_length(body_cipher) BETWEEN 16 AND 65552); no SQL default; owner supplies value` \| UTF8 JSON ciphertext with appended 16-byte GCM tag; max plaintext 65,536 bytes. internal; presence/clearing §5 \| `ByteArray` → ciphertext not exposed; authorized decrypt supplies `Draft.values: JSON!` / no draft RPC |
| [TD-ROW-L4302 · L4302](../../../04-spoolin-techdoc.md#form_draft) | `version` \| `bigint` \| `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` \| Optimistic row version; increment on every domain change. API; presence/clearing §5 \| `Long` (`@Version`) → `Draft.version`, safe save `Operation.version` (Long string) / no draft RPC |
| [TD-ROW-L4303 · L4303](../../../04-spoolin-techdoc.md#form_draft) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. internal; presence/clearing §5 \| `Instant` → no Draft output field; internal ordering/expiry basis / no draft RPC |
| [TD-ROW-L4304 · L4304](../../../04-spoolin-techdoc.md#form_draft) | `expires_at` \| `timestamptz(6)` \| `NOT NULL; no SQL default; owner supplies value` \| 24 h after last update. protected owner API; presence/clearing §5 \| `Instant` → `Draft.expiresAt: DateTime!` / no draft RPC |

## Implementation

1. Implement typed movie/person/credit draft schemas, encrypted owner-only form_draft storage, quota20, 24-hour eligibility and ≤64KiB shape capacity. Incomplete/over-current-policy correction content is allowed where safe; passwords/raw files/private unknown keys are not.
2. Treat top-level Core saveDraft/discardDraft operation UUID independently of nested original Catalog submission UUID. Lock operation→account→draft; verify ownership and identical retained receipt before mutable version/quota/policy checks.
3. Commit save/discard, safe receipt and redacted operational audit atomically. Return historical safe outcome metadata, never rebuild old plaintext from the current draft row; later edit/discard/expiry must not resurrect state or refresh TTL.
4. Implement expected draft versions and changed-content reuse conflicts, same-key concurrent replay and distinct-key stale write handling. Consume crypto nonces according to separate allocation guarantees.
5. Exercise real PostgreSQL response loss, concurrent saves/discards, fault rollback and exact expiry before purge; frontend integration remains T027.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T014-A1:** Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation.
- **T014-A2:** Discard replay: deleted draft content is never reread or rebuilt; later state remains absent even when the old request repeats.
- **T014-A3:** Isolation: B cannot read/replay A's recovery state, quota/policy changes cannot break identical retained replay, and distinct stale edits require review.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-178](../../../05-atdd.md#case-atdd-178-protect-recoverable-data-with-owner-and-version-checks) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §3.4/5.11 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then only the authenticated owner can inspect/change a draft using create-absent/update-matching version semantics And incomplete or over policy non-secret values remain correct able within the safe encrypted envelope And passwords/files/account registration/tokens/challenges/unknown keys reject; stale CAS never overwrites another tab Observation: Exercise actual saveDraft/discardDraft with top-level Core operationId, distinct DraftInput Catalog operationId, owned encrypted payload and CAS. Inspect Core receipt/fingerprint/redacted audit atomicity, safe result/version/discarded state and current draft query separately. Replays cannot bump TTL/version or resurrect an expired/discarded draft. Owner-only reads, incomplete-draft policy and source-default bounds still apply; exact after-expiry/receipt-retention behavior follows F-DRAFT-REPLAY. |
| [ATDD-180](../../../05-atdd.md#case-atdd-180-reject-tampered-protected-envelopes-without-data-loss) / Integration | F-CRYPTO: independent synthetic PII/DRAFT/EMAIL_LOOKUP epochs old/new, 100+1 eligible rows, distinct canaries, committed nonce counters and isolated backup checkpoint. Never capture raw key material. | Then valid account/draft data decrypt only for its authorized purpose under independent32-byte AES256GCM keys And nonce12/tag16 and AAD/version binding detect tampering And failure returns safe unavailable evidence, keeps original protected values and never writes plaintext or invented empty data Observation: Tamper each actual encrypted envelope and AAD binding in isolated persisted rows, then request authorized reads. Compare safe unavailable result and unchanged original ciphertext; only valid owner/purpose data decrypts and no empty/plaintext fallback is written. |
| [ATDD-293](../../../05-atdd.md#case-atdd-293-validate-safe-draft-shape-independently-of-publication-completeness) / Unit | Allowed incomplete movie/person/credit draft; blank/over-current-field-limit text within65536-byte payload; unknown key, password, file, account email, challenge; invalid Unicode/UUID; payload bound±1. | Then permitted incomplete values remain recoverable within the draft envelope And unknown/private/file keys, invalid types/Unicode/IDs and oversize payload reject And publication validation is not silently applied as draft completeness Observation: Inspect accepted shape or safe error without encrypting/storing; owner CAS/encryption/isolation remains real integration. |
| [ATDD-319](../../../05-atdd.md#case-atdd-319-separate-draft-mutation-identity-from-catalog-recovery-identity) / Unit | Core operationUUID D1, draftUUID X, Catalog submissionUUID C1; accountA/B, save/discard kind, expectedVersion omitted/null/5, exact values/policy; changed one field. | Then top-level D1 binds the Core mutation while C1 remains Catalog recovery data And account/kind/target/version/presence/values/policy differences change binding or conflict And the keyed fingerprint never persists raw draft values, secrets or passwords Observation: Compare serialized presence/scope and HMAC selection with fixed examples; actual atomic receipt/payload storage and racing writes remain separate proof. |
| [ATDD-320](../../../05-atdd.md#case-atdd-320-replay-safe-draft-outcomes-without-current-row-reconstruction) / Unit | Retained safe receipt D1→draftX/version7; later currentversion8, expired/discarded row, foreign accountB, changedfingerprint; receipt resultExpired after selected retention. | Then a matching retained receipt returns its original safe ID/version/discarded outcome without a draft-body write And current-row changes/absence cannot turn replay into creation or refresh And foreign/mismatched/expired-result behavior follows the scoped receipt contract without inventing missing historical payload Observation: Inspect replay result and no-mutation decision; no fake repository lookup proves durable recovery or cross-account server enforcement. |
| [ATDD-329](../../../05-atdd.md#case-atdd-329-replay-a-committed-draft-save-after-losing-its-response) / Integration | CoreD1 save draftX expectedVersionabsent/create then matching update; nested CatalogC1; drop aftercommit/beforeresponse; restartCore and repeat exactD1. | Then saveDraft returns the selected MutationResult with original Core operation/resultId/version And replay returns the original safe outcome without a second version/TTL/audit change And nested C1 remains the separate Catalog submission; current plaintext is obtained only from authorized current drafts Observation: Use real Core/PostgreSQL commit/reply barriers, restart and operation(owner:CORE). Inspect ciphertext, one mutation audit and safe ID/version receipt/HMAC with no plaintext. Capture distinct Core/Catalog UUIDs and current query separately. The atomic Core boundary is form_draft + REQUEST operation_receipt + redacted operational_event ACTIVITY. Historical safe replay remains scoped and performs no second encrypted draft/audit effect; it exposes no initialization variant or decrypted old payload. |
| [ATDD-330](../../../05-atdd.md#case-atdd-330-replay-a-committed-discard-without-reading-deleted-draft-content) / Integration | DraftX/version5; CorediscardD2; drop committedreply then replay; newop absentdraft and foreignaccount variants follow F-DRAFT-REPLAY. | Then DraftDiscardResult returns original operation/discarded outcome and safe problem projection And matching replay does not recreate or reread deleted content And fresh-missing/foreign/stale variants follow the explicit current contract without an identity oracle Observation: Inspect actual owner predicate/CAS/delete/operation/audit transaction and replay after restart; no Boolean-only legacy return or old plaintext is emitted. Absence after earlier successful discard cannot rewrite its original true outcome. Discard atomically deletes owned form_draft and records safe REQUEST receipt plus redacted operational_event ACTIVITY. Original discarded flag replays without querying deleted values or deleting a newer UUID reuse; internal initialization fields are never returned. |
| [ATDD-331](../../../05-atdd.md#case-atdd-331-separate-concurrent-duplicate-draft-intents-from-stale-independent-edits) / Integration | TwoexactD1 requests, changedvaluesunderD1, distinctD3/D4 withsameexpectedVersion5, foreignB reading/reusingA receipt and operationretention rows. | Then same exact intent has one logical version/TTL change and original result And mismatched content conflicts without replacing receipt or draft And independent stale edits cannot overwrite the winner; another account cannot inspect the receipt Observation: Inspect actual unique operation insertion/keyed fingerprint and draft CAS under concurrent transactions. Record winning state and separately attributable failures; a wasted committed nonce is permitted, but no duplicate business effect/receipt payload or successful audit. Real Core locking keeps form_draft mutation/version/REQUEST receipt/ACTIVITY evidence consistent. Distinct stale intents cannot overwrite, and duplicate winners emit one successful activity despite concurrent unused nonce reservations. |
| [ATDD-332](../../../05-atdd.md#case-atdd-332-roll-back-the-whole-draft-mutation-receipt-and-redacted-audit-boundary) / Integration | Injectafterdraftchange/beforereceipt, afterreceipt/beforeaudit and afteraudit/beforecommit; separately loseack aftercommit; actualallocatednonce. | Then precommit failures leave no partial successful draft change, receipt or audit And postcommit loss is reconciled to the committed original safe outcome And protected draft body stays encrypted and absent from receipts/telemetry; consumed nonce is not reused Observation: Reread every involved table from a separate connection after actual rollback/restart; nonce reservation is its separate committed transaction and may remain consumed. Verify no schema/table addition or plaintext fallback is needed. Inject rollback before and after operational_event ACTIVITY/operation_receipt writes with the form_draft change. A failed local transaction commits none of those effects; separate safe failure evidence cannot replace a committed winner or expose private draft/initializer values. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-DRAFT-REPLAY · L250](../../../05-atdd.md#f-draft-replay-core-mutation-intent-versus-catalog-submission) | Required top-level saveDraft/discardDraft operationId is a fresh UUID per Core user intent, retained for exact retry. DraftInput.operationId is optional original Catalog submission identity and remains separate. saveDraft returns MutationResult!; discardDraft returns DraftDiscardResult! containing operation:Operation!, discarded:Boolean! and problem:Problem. Existing Core operation rows contain owner/kind/keyed private request hash and safe IDs/version/discarded outcomes, not decrypted body. Draft changes, original safe receipt and redacted audit are one local transaction; nonce allocation remains separately committed. Matching replay returns its original retained result even if current draft changed, expired or was discarded, without TTL/version refresh or resurrection. Changed body/identity conflicts and foreign account cannot inspect the receipt. Current draft plaintext is fetched separately only when currently authorized/eligible, after reconciliation of a known original Core receipt. Create omits expectedVersion (explicit null is invalid); update supplies current version. Fresh missing/hidden discard succeeds with discarded=false, owned stale returns STALE_VERSION, successful deletion returns true. Safe full results last7days and then RESULT_EXPIRED/resultExpired terminal tombstones prevent repeat effects; seven-day receipt results never become a promise of indefinite draft recovery. These are prospective oracles, not performed mutations. |

Partial implementation contributions: [ATDD-048](../../../05-atdd.md#case-atdd-048-expirysign-out), [ATDD-179](../../../05-atdd.md#case-atdd-179-conceal-drafts-until-the-same-account-can-resume), [ATDD-233](../../../05-atdd.md#case-atdd-233-enforce-encrypted-owned-draft-storage), [ATDD-259](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback), [ATDD-333](../../../05-atdd.md#case-atdd-333-retain-safe-draft-replay-without-resurrecting-later-expired-or-discarded-state). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T014` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T014` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- Hand the owned outputs to [T027 — Prove recovery through real movie, person and credit editors](../../17-draft-recovery-editor-integration/tasks/T027-prove-recovery-through-real-movie-person-and-credit-editors.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
