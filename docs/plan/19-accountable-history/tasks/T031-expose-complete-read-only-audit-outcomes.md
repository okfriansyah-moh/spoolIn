---
kind: task
id: T031
group: G19
depends_on:
- T004
- T010
- T016
- T019
- T021
- T025
- T023
- T029
readiness: Ready
status: In progress
risk: high
test_strategy: test-first
visual_review: required
---

# T031 — Expose complete read-only audit outcomes

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Attribution: history contains actor/action/affected record/time/outcome/relevant changes for each included event; no absent fields are inferred from scalar compatibility. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-053 | both |
| ATDD-054 | both |
| ATDD-055 | both |
| ATDD-056 | both |
| ATDD-088 | both |
| ATDD-089 | both |
| ATDD-101 | both |
| ATDD-185 | both |
| ATDD-251-T1-R19 | implements |
| ATDD-253-T1-R15 | implements |
| ATDD-265 | both |
| ATDD-265-T1-R01 | both |
| ATDD-265-T1-R02 | both |
| ATDD-265-T1-R03 | both |
| ATDD-265-T1-R04 | both |
| ATDD-265-T1-R05 | both |
| ATDD-265-T1-R06 | both |
| ATDD-307 | both |
| J-11 | both |
| FR-013 | both |
| NFR-015 | both |
| D05 | both |
| D20 | both |
| AC-053 | both |
| AC-053-C01 | both |
| AC-053-C02 | both |
| AC-054 | both |
| AC-054-C01 | both |
| AC-055 | both |
| AC-055-C01 | both |
| AC-055-C02 | both |
| AC-056 | both |
| AC-056-C01 | both |
| AC-056-C02 | both |
| AC-056-C03 | both |
| AC-114 | both |
| AC-114-C01 | both |
| AC-114-C02 | both |
| AC-115 | both |
| AC-115-C01 | both |
| AC-127 | both |
| AC-127-C01 | both |
| AC-127-C02 | both |
| OD-011 | both |
| US-015 | both |
| PRD-PERM-06 | both |
| PRD-POLICY-24 | both |
| PRD-POLICY-26 | both |
| PRD-CONCEPT-07 | both |
| PRD-HANDOFF-05 | both |
| UI-audit-audit | both |
| UI-audit-audit-detail | both |
| UI-audit-audit-loading | both |
| UI-audit-audit-empty | both |
| UI-audit-audit-failed | both |
| UI-audit-audit-denied | both |
| DS-POLICY-01 | implements |
| TD-C-035 | both |
| V-AUDIT | both |
| TD-ROW-L2742 | both |
| TD-ROW-L3562 | both |
| TD-ROW-L3602 | both |
| TD-ROW-L3604 | both |
| TD-ROW-L3619 | both |
| TD-ROW-L3620 | both |
| TD-ROW-L4955 | both |
| TD-ROW-L4956 | both |
| TD-ROW-L4957 | both |
| TD-ROW-L4958 | both |
| TD-ROW-L4959 | both |
| TD-ROW-L4960 | both |
| TD-ROW-L4961 | both |
| TD-ROW-L4962 | both |
| TD-ROW-L4963 | both |
| TD-ROW-L4964 | both |
| TD-ROW-L4965 | both |
| TD-ROW-L5178 | both |
| TD-ROW-L5179 | both |
| TD-ROW-L5180 | both |
| TD-ROW-L5181 | both |
| TD-ROW-L5509 | both |
| TD-ROW-L5512 | both |
| TD-ROW-L5525 | both |
| TD-ROW-L5526 | both |
| TD-BLOCK-L4974 | both |
| F-AUDIT-CORRECTION | both |
| BRAIN-D05 | both |
| BRAIN-D20 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T010 — Connect account and session browser journeys](../../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T016 — Implement authoritative movie creation and edits](../../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.
- [T019 — Connect people management and retained edits](../../13-people-and-credits/tasks/T019-connect-people-management-and-retained-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied.
- [T021 — Connect credit editing and verify the people outcome](../../13-people-and-credits/tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning.
- [T025 — Connect private archives and irreversible confirmations](../../15-archive-management/tasks/T025-connect-private-archives-and-irreversible-confirmations.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content.
- [T023 — Coordinate protective holds, attachment and cleanup](../../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.
- [T029 — Commit immutable proposals and exact approval once](../../18-assisted-movie-creation-transport-and-review/tasks/T029-commit-immutable-proposals-and-exact-approval-once.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/audit/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/catalogview/audit/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/manage/audit/+page.svelte | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/features/audit/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/audit/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/audit/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/audit/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/catalogview/audit/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.1.1 UI reference and implementation map](../../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [4.3 SpoolIn Catalog service](../../../04-spoolin-techdoc.md#43-spoolin-catalog-service): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.4 Projection and presence crosswalk](../../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.3 API security enforcement](../../../04-spoolin-techdoc.md#83-api-security-enforcement): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-251-T1-R19 · L10365](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `audit`; Maintainer / Catalog \| Optional subject UUID, timestamp/UUID cursor; immutable sanitized domain changes; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-253-T1-R15 · L10479](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `Audit`; Audit(PageRequest) → AuditPage |
| [J-11 · L94](../../../02-PRD.md#32-application-journeys) | Maintainer reviews accountability \| Signed in \| Inspect read-only catalog history and AI approval/creation outcomes |
| [FR-013 · L247](../../../02-PRD.md#61-functional-requirements) | Keep attributable change/security records and expose read-only Maintainer history for movie/person lifecycle, credits, artwork, and AI approval/creation \| D05/D20 and audit UI Committed \| AC-053 through AC-056, AC-114, AC-115, AC-127, AC-131 through AC-133, AC-137 |
| [NFR-015 · L273](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [D05 · L306](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D20 · L321](../../../02-PRD.md#64-project-decision-register) | Atomic successful audit, durable follow-up, saga only when needed \| FR-013/FR-014 / AC-053, AC-054, AC-057 through AC-060; techdoc mechanics |
| [AC-053 · L451](../../../02-PRD.md#us-009-review-accountable-history) | Confirmed change \| A permitted catalog change succeeds \| History is inspected \| A matching attributable successful-change record exists; a success claim cannot stand without its associated evidence. Technical atomicity remains a handoff commitment. |
| [AC-053-C01 · L451](../../../02-PRD.md#us-009-review-accountable-history) | A matching attributable successful-change record exists; a success claim cannot stand without its associated evidence. |
| [AC-053-C02 · L451](../../../02-PRD.md#us-009-review-accountable-history) | Technical atomicity remains a handoff commitment. |
| [AC-054 · L452](../../../02-PRD.md#us-009-review-accountable-history) | Failed change \| An attempted change fails before completion \| State/history are inspected \| No partial catalog change or misleading successful-change event appears; failure evidence is distinguishable from success. |
| [AC-054-C01 · L452](../../../02-PRD.md#us-009-review-accountable-history) | No partial catalog change or misleading successful-change event appears; failure evidence is distinguishable from success. |
| [AC-055 · L453](../../../02-PRD.md#us-009-review-accountable-history) | Security/media outcomes \| Authentication is denied or artwork lifecycle actions occur \| Authorized inspection occurs \| Appropriate sanitized outcomes remain attributable and available, even when the business action failed. Security events need not appear in the Maintainer catalog-history screen. |
| [AC-055-C01 · L453](../../../02-PRD.md#us-009-review-accountable-history) | Appropriate sanitized outcomes remain attributable and available, even when the business action failed. |
| [AC-055-C02 · L453](../../../02-PRD.md#us-009-review-accountable-history) | Security events need not appear in the Maintainer catalog-history screen. |
| [AC-056 · L454](../../../02-PRD.md#us-009-review-accountable-history) | Audit privacy/access \| Audit contains permitted change details \| Someone views it \| Only permitted readers see it; credentials, tokens, raw files, and account emails never appear. History is read-only. Retention/protection mechanics belong to the techdoc. |
| [AC-056-C01 · L454](../../../02-PRD.md#us-009-review-accountable-history) | Only permitted readers see it; credentials, tokens, raw files, and account emails never appear. |
| [AC-056-C02 · L454](../../../02-PRD.md#us-009-review-accountable-history) | History is read-only. |
| [AC-056-C03 · L454](../../../02-PRD.md#us-009-review-accountable-history) | Retention/protection mechanics belong to the techdoc. |
| [AC-114 · L455](../../../02-PRD.md#us-009-review-accountable-history) | Audit screen \| A Maintainer has catalog history to inspect \| They open history and an entry \| Show actor, action, affected record, time, outcome, and relevant changes, explicitly covering movie/person archive, restoration, permanent deletion, credits, artwork, and AI approval/creation. Approval is distinguishable from publication. |
| [AC-114-C01 · L455](../../../02-PRD.md#us-009-review-accountable-history) | Show actor, action, affected record, time, outcome, and relevant changes, explicitly covering movie/person archive, restoration, permanent deletion, credits, artwork, and AI approval/creation. |
| [AC-114-C02 · L455](../../../02-PRD.md#us-009-review-accountable-history) | Approval is distinguishable from publication. |
| [AC-115 · L456](../../../02-PRD.md#us-009-review-accountable-history) | Audit feedback/denial \| History is empty, loading, unavailable, or requested without permission \| The screen/request resolves \| Show the appropriate loading/empty/failure/denied state; permit safe retry for failure, and provide no editing or arbitrary restore control. |
| [AC-115-C01 · L456](../../../02-PRD.md#us-009-review-accountable-history) | Show the appropriate loading/empty/failure/denied state; permit safe retry for failure, and provide no editing or arbitrary restore control. |
| [AC-127 · L457](../../../02-PRD.md#us-009-review-accountable-history) | Actor attribution without account exposure \| An audit event identifies its responsible actor \| A Maintainer views it \| Show the actor identity needed for accountability without exposing email, credentials, unrelated account details, or a public profile link. Public/catalog/MCP outputs do not reveal private account information. |
| [AC-127-C01 · L457](../../../02-PRD.md#us-009-review-accountable-history) | Show the actor identity needed for accountability without exposing email, credentials, unrelated account details, or a public profile link. |
| [AC-127-C02 · L457](../../../02-PRD.md#us-009-review-accountable-history) | Public/catalog/MCP outputs do not reveal private account information. |
| [OD-011 · L1048](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Audit screen/access/content fixed, including movie/person lifecycle; event schema, retention, retrieval, integrity/protection, and technical security-event inspection remain for techdoc \| FR-013 \| Technical design; before audit implementation |
| [US-015 · L475](../../../02-PRD.md#us-015-complete-tasks-across-devices) | ### US-015: Complete tasks across devices As a Visitor, Viewer, or Maintainer, I want readable controls and accurate feedback on desktop and mobile. Requirement: NFR-007. Apply these criteria to registration, archive, audit, and catalog flows. |
| [PRD-PERM-06 · L73](../../../02-PRD.md#31-permission-matrix) | View application audit history \| No \| No \| Yes, read-only |
| [PRD-POLICY-24 · L196](../../../02-PRD.md#validation-safe-handling-and-feedback) | Show effective limits in field guidance before submission and identify the affected field and correction when validation fails. Preserve permitted non-secret draft content, including an over-limit value so the user can correct it. Passwords never enter recovery drafts, logs, or audit. Invalid catalog or account submission commits no partial record change. Invalid search retains its entered text and other filters with correction feedback; do not run a truncated or silently altered search. |
| [PRD-POLICY-26 · L200](../../../02-PRD.md#validation-safe-handling-and-feedback) | Catalog text is plain text. Entered markup or instructions must not execute in forms, catalog pages, search results, previews, or audit views. Preserve legitimate names and punctuation during safe handling. OWASP input-validation guidance separates input validation from defenses against injection and unsafe output. Sanitization is not a promise that every vulnerability has been prevented; the techdoc must establish the required safe display and data-handling mechanisms. |
| [PRD-CONCEPT-07 · L815](../../../02-PRD.md#9-conceptual-information-and-privacy) | Movie lifecycle \| Active, archived, or permanently deleted. Archive preserves remaining content; restore respects person state and removed credits. Permanent deletion offers no product restoration. Audit evidence follows the eventual retention policy. |
| [PRD-HANDOFF-05 · L838](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D05/D20 \| Committed \| Successful database change and audit share a local transaction; required cross-service follow-up uses an outbox or equivalent durable work. Concurrent delivery/restart/uncertain outcomes must be safe; saga coordination only if needed. |
| [DS-POLICY-01 · L37](../../../03-design-system.md#identity-and-voice) | **SpoolIn** is named after the spool that holds a length of film. Its brand promise is **“Unspooling stories and discovering what’s behind them.”** The discovery homepage uses **“Every reel holds a story.”** as its headline and the brand promise as supporting copy. Brand history records the origin, narrative, spelling, and adopted copy. |
| [TD-C-035 · L4945](../../../04-spoolin-techdoc.md#catalog_audit_event) | Append-only lifetime Catalog business audit: all five outcomes, subject kind/ID, safe actor/reference/diffs, cursor indexes and missing-large-value disclosure; excludes Core/Media/worker/security events. |
| [V-AUDIT · L6392](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-AUDIT \| Inspect committed/failed/security/media outcomes and Maintainer audit GraphQL; assert actor pseudonym, action, subject kind/ID, time, outcome and intended public-field diffs; APPROVED/REJECTED remain distinct from committed SUCCEEDED creation; verify all five AuditOutcome values and fields 8/9 against stored Catalog events, immutable history, denial to Viewer/Visitor, no PII/hash/token/body secrets |
| [TD-ROW-L2742 · L2742](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `audit` \| Maintainer / Catalog \| Optional subject UUID, timestamp/UUID cursor; immutable sanitized domain changes, required subjectKind and outcome |
| [TD-ROW-L3562 · L3562](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | audit_event \| Catalog business rows→catalog_audit_event; private/security/technical rows→operational_event ACTIVITY \| Business fields/outcomes stay unchanged; operator evidence stays restricted with its separate retention |
| [TD-ROW-L3602 · L3602](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `catalog_audit_event` \| Catalog `audit/persistence` \| Permanent business history exposed only to authorized Maintainers |
| [TD-ROW-L3604 · L3604](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | Flyway history \| One framework-managed table per database \| Migration metadata; never an authored domain entity |
| [TD-ROW-L3619 · L3619](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `catalog_audit_event` \| GraphQL AuditEvent/AuditConnection; protobuf AuditEvent/AuditPage \| Maintainer-only Catalog history; outcome→AuditOutcome/field 8; subject_kind→subjectKind/field 9; actorRef derived without PII |
| [TD-ROW-L3620 · L3620](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `durable_work`, `operational_event` \| Restricted owner worker/operator DTOs \| No browser/model or generic row RPC; technical history cannot enter Catalog business audit |
| [TD-ROW-L4955 · L4955](../../../04-spoolin-techdoc.md#catalog_audit_event) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4956 · L4956](../../../04-spoolin-techdoc.md#catalog_audit_event) | `operation_id` \| `uuid` \| `REFERENCES operation_receipt(id) ON DELETE RESTRICT; NULL allowed; no SQL default; owner supplies value` \| Local operation when applicable. API; presence/clearing §5 \| `UUID?` → UUID / optional string |
| [TD-ROW-L4957 · L4957](../../../04-spoolin-techdoc.md#catalog_audit_event) | `actor_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Opaque verified account ID; NULL for unauthenticated attempt. API; presence/clearing §5 \| `UUID?` → derived opaque `AuditEvent.actorRef: String!` / `AuditEvent.actor_ref` (string field 2), never a raw account row |
| [TD-ROW-L4958 · L4958](../../../04-spoolin-techdoc.md#catalog_audit_event) | `actor_kind` \| `varchar(16)` \| `NOT NULL CHECK (actor_kind IN ('HUMAN','SERVICE','ANONYMOUS')); no SQL default; owner supplies value` \| Never infer human identity from a supplied display name. internal; presence/clearing §5 \| `String` → internal discriminator for actorRef / no standalone actor_kind field |
| [TD-ROW-L4959 · L4959](../../../04-spoolin-techdoc.md#catalog_audit_event) | `action` \| `varchar(64)` \| `NOT NULL; no SQL default; owner supplies value` \| Allowlisted event action. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4960 · L4960](../../../04-spoolin-techdoc.md#catalog_audit_event) | `subject_kind` \| `varchar(24)` \| `NOT NULL; no SQL default; owner supplies value` \| Catalog title/person/credit/proposal/artwork subject; security/work events use operational_event. API; presence/clearing §5 \| `String` → `AuditEvent.subjectKind: String!` / `AuditEvent.subject_kind` (string, field 9) |
| [TD-ROW-L4961 · L4961](../../../04-spoolin-techdoc.md#catalog_audit_event) | `subject_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Logical subject retained after deletion; deliberately no FK. API; presence/clearing §5 \| `UUID?` → UUID / optional string |
| [TD-ROW-L4962 · L4962](../../../04-spoolin-techdoc.md#catalog_audit_event) | `outcome` \| `varchar(16)` \| `NOT NULL CHECK (outcome IN ('SUCCEEDED','DENIED','FAILED','APPROVED','REJECTED')); no SQL default; owner supplies value` \| Approval separate from actual creation. API; presence/clearing §5 \| `AuditOutcome` with JPA `EnumType.STRING` → `AuditEvent.outcome: AuditOutcome!` / `AuditEvent.outcome` (string, field 8) |
| [TD-ROW-L4963 · L4963](../../../04-spoolin-techdoc.md#catalog_audit_event) | `changes` \| `jsonb` \| `NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(changes)='object' AND octet_length(changes::text)<=65536); NOT NULL (explicit or PK)` \| Allowlisted public metadata before/after; bounded snapshot or explicit length/hash summary for large values; no credential, raw binary or account PII. API; presence/clearing §5 \| `TypedJsonDTO` → typed projection/JSON / typed message |
| [TD-ROW-L4964 · L4964](../../../04-spoolin-techdoc.md#catalog_audit_event) | `correlation_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Sanitized correlation. internal; presence/clearing §5 \| `UUID` → internal trace/audit correlation / no AuditEvent field |
| [TD-ROW-L4965 · L4965](../../../04-spoolin-techdoc.md#catalog_audit_event) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → `AuditEvent.at: DateTime!` / `AuditEvent.at` (Timestamp field 7) |
| [TD-ROW-L5178 · L5178](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Register \| Account + all current email lookup aliases + operation + redacted audit; hash/encrypt before transaction, uniqueness authoritative \| READ COMMITTED; duplicate alias rolls everything back; generic account error |
| [TD-ROW-L5179 · L5179](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Sign in/out \| Session issuance/revocation and authentication audit; lock account to enforce five active sessions; session expiry checked on every request \| READ COMMITTED; sign-out retries same identity; lost response means browser signed-out view but server revocation uncertain |
| [TD-ROW-L5180 · L5180](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Draft save/discard \| Core operation + owner predicate/version CAS + encrypted payload/TTL or deletion + redacted audit, committed together \| READ COMMITTED; Core lock order; safe replay before checking current draft version; no overwrite or resurrection |
| [TD-ROW-L5181 · L5181](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Movie/person/credit mutation \| Operation identity, current permission, policy, expected versions, domain rows, successful audit, necessary work all commit together \| READ COMMITTED + catalog lock; rejected transaction emits no successful audit |
| [TD-ROW-L5509 · L5509](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-07 worker→services \| Duplicate effects or lost obligation \| Atomic outbox/audit, stable operation ID, lease epochs/reconciliation \| Crash before/after commit/send, stale worker complete, duplicate provider send; external exactly-once not promised; reliability owner |
| [TD-ROW-L5512 · L5512](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-10 diagnostics/provider \| Secrets leak in logs/alerts or malicious URL SSRF \| Allowlisted destinations, redaction, bounded labels, no arbitrary fetch \| Token/email/input canaries absent from log/audit/provider; fixed endpoint compromise residual; operations owner |
| [TD-ROW-L5525 · L5525](../../../04-spoolin-techdoc.md#83-api-security-enforcement) | Catalog/Core persistence \| Bound JDBC/JPA/HQL/native values; fixed table/column/order expressions; typed arrays and masks \| Own DB only; unique/check/FK safeguards plus owner invariants/CAS and atomic audit/receipt/work \| Apostrophes, comments, UNION/boolean strings and wildcard characters cannot change selected scope or query structure; real PostgreSQL rollback proves no extra rows/effects |
| [TD-ROW-L5526 · L5526](../../../04-spoolin-techdoc.md#83-api-security-enforcement) | Browser views and audit / frontend \| Escaped Svelte interpolation or text nodes for all dynamic values; validated links, fixed routes; theme-only persistence and history cleanup \| No user `{@html}`, innerHTML, script/event-handler construction, eval or Function; CSS/selectors/component names are build-owned \| Stored/reflected/DOM payloads remain text in forms, details, errors, search, audit and recovery; no script execution, exfiltration or navigation to unsafe schemes |
| [TD-BLOCK-L4974 · L4974](../../../04-spoolin-techdoc.md#catalog_audit_event-ddl) | catalog_audit_event DDL — complete sql contract, all members, constraints and field numbers |
| [BRAIN-D05 · L74](../../../01-brainstorming-result.md#4-decision-register) | D05 \| Committed \| Design for authentication, authorization, RBAC, and audit logging from the start; implement core functionality first, then complete security |
| [BRAIN-D20 · L89](../../../01-brainstorming-result.md#4-decision-register) | D20 \| Committed \| Durable outbox or equivalent work table for reliable cross-service follow-up; saga coordination only when the finalized workflow needs it |

## Implementation

1. Implement authorized filtered audit reads over Catalog's immutable business history; Core returns only explicit DTO fields including outcome, subjectKind/ID and safe actor reference.
2. Keep technical/security/Core/Media/worker events in restricted operational history. Render approval, rejection, denied/failed/success and actual creation distinctly; approval does not prove later outcome.
3. Implement bounded/sanitized change summaries, missing-large-value indication and stable cursor ordering. Preserve complete domain values in their own owner storage even when audit diff is summarized.
4. Build list/detail loading/empty/failure/denied states with safe retry and full affected-record identity; no edit/arbitrary restore/account-profile/email/credential links.
5. Verify real movie/person/lifecycle/credit/artwork/proposal events, rollback absence and unauthorized direct read, with exact wire→storage projection and markup-looking data.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T031-A1:** Attribution: history contains actor/action/affected record/time/outcome/relevant changes for each included event; no absent fields are inferred from scalar compatibility.
- **T031-A2:** Privacy: Viewer/public denied reads expose no content, technical events never appear in Maintainer history, and all text is inert.
- **T031-A3:** Truth: failed transaction has no success audit, approval remains distinct from created movie, and large diff explicitly says what was omitted without corrupting stored data.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-053](../../../05-atdd.md#case-atdd-053-confirmed-change) / End-to-end | Independently each catalog mutation/lifecycle/credit/artwork/approval creation event; compare owner commit receipt/action/actor/time/changes and sanitized audit. | Then A matching attributable successful-change record exists; a success claim cannot stand without its associated evidence. Technical atomicity remains a handoff commitment. Observation: Correlate actual committed catalog/lifecycle/credit/artwork/AI events with opaque verified actor, record/time/outcome and allowed diff; approval is distinct from creation. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-054](../../../05-atdd.md#case-atdd-054-failed-change) / End-to-end | Inject failure before commit after domain change/after audit/afterwork; rollbacks have no successful change event or partial domain rows. | Then No partial catalog change or misleading successful-change event appears; failure evidence is distinguishable from success. Observation: Inject actual transaction failure after domain/audit/work statements and reread all rows separately; no successful mutation event or partial domain/work escapes rollback. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-055](../../../05-atdd.md#case-atdd-055-securitymedia-outcomes) / End-to-end | Deniedsignin/rolewrite and Media validation/finalization failure; query restricted owner inspection, no forced catalog-history security screen. | Then Appropriate sanitized outcomes remain attributable and available, even when the business action failed. Security events need not appear in the Maintainer catalog-history screen. Observation: Inspect denied-auth and Media failure evidence at restricted owner operational boundaries; it remains sanitized and does not require a new product security-console screen. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-056](../../../05-atdd.md#case-atdd-056-audit-privacyaccess) / End-to-end | Try auditasVisitor/V; A sees only allowlisted public diffs, opaqueactorID, read-only controls. Secretcanaries must beabsent. | Then Only permitted readers see it; credentials, tokens, raw files, and account emails never appear. History is read-only. Retention/protection mechanics belong to the techdoc. Observation: Compare direct Visitor/Viewer denial and Maintainer read-only audit DOM/schema, including corrected outcome/subject fields; scan synthetic credential/PII canaries. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-088](../../../05-atdd.md#case-atdd-088-audit-screen) / End-to-end | Auditcatalogchange list and entry inclallmovie/personlifecycle, credit, artwork, AIapproval/creation; actor/action/subject/time/outcome/diffs visible. | Then Show actor, action, affected record, time, outcome, and relevant changes, explicitly covering movie/person archive, restoration, permanent deletion, credits, artwork, and AI approval/creation. Approval is distinguishable from publication. Observation: Inspect actual read-only audit list rows/pagination/outcome/affected record/changes and separate AI approval/creation events; no edit/restore controls. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-089](../../../05-atdd.md#case-atdd-089-audit-feedbackdenial) / End-to-end | Auditempty/loading/failed/denied; safe readretry andnoedit/arbitrary restore button/API. | Then Show the appropriate loading/empty/failure/denied state; permit safe retry for failure, and provide no editing or arbitrary restore control. Observation: Pause/fail/empty audit query and deny direct unauthorized requests; inspect distinct loading/empty/error/correction while no private rows escape. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-101](../../../05-atdd.md#case-atdd-101-actor-attribution-without-account-exposure) / End-to-end | CatalogactorRefopaqueaccountUUID/authorized label; no email/profile link/hash/token; public people search does not include accounts. | Then Show the actor identity needed for accountability without exposing email, credentials, unrelated account details, or a public profile link. Public/catalog/MCP outputs do not reveal private account information. Observation: Correlate attributable actor ID/action in authorized audit without exposing name/email credentials or a public user directory; supplied display names are not verified identity. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-185](../../../05-atdd.md#case-atdd-185-record-large-changes-without-inventing-their-contents) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §6.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then the full domain value remains saved without truncation And audit records changed paths, exact byte lengths, digests and valueOmitted=true And the read-only UI labels omitted content and never presents the digest as the original value Observation: Commit large public text then inspect full saved value and bounded audit JSON with changed paths, UTF-8 lengths, SHA-256 and valueOmitted=true. Query actual audit UI/projection and verify explicit omission label rather than presenting a digest as original content. |
| [ATDD-265](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `audit` in `/manage/audit`, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named AuditTable / AuditDetail through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in both themes at declared widths; verify keyboard/focus and permitted navigation without leaking protected data. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| audit \| Maintainer; complete permitted `audit` page \| Show read-only time, opaque actorRef, action, subjectKind/subjectId, stored outcome and sanitized changes. Distinguish APPROVED from SUCCEEDED creation; no account email/credentials/raw files or restore controls. \| \| audit-detail \| Maintainer; inspect a permitted event \| Show actorRef, subject kind/ID, time, exact outcome/action and allowed before/after changes (or truthful large-change summary). Approval and committed creation have distinct events even in one transaction; no arbitrary edit/restore. \| \| audit-loading \| Maintainer; hold `audit` read \| Show Loading history and announce pending retrieval; do not infer absence of changes or display restricted cached events. \| \| audit-empty \| Maintainer; receive successful empty `audit` result \| Show No history yet for current query and retain read-only navigation. Empty is distinct from outage, pending or denial. \| \| audit-failed \| Maintainer; fail the `audit` read \| Show History could not load with safe read retry. Failure cannot imply no business change or roll back a committed event. \| \| audit-denied \| Visitor/Viewer; open/request catalog history \| Show History access denied and no events/private changes; provide permitted navigation and no editing/restore control. |
| [ATDD-307](../../../05-atdd.md#case-atdd-307-expose-audit-outcome-and-affected-record-through-corrected-projections) / Integration | Actual successful movie edit, rejection, AI approval and creation events; Viewer/Visitor/A reads; F-AUDIT-CORRECTION schema and stored outcomes. | Then Maintainer receives exact stored outcome, subjectKind/subjectId, actor/action/time and allowed changes And approval and actual creation remain distinguishable And Viewer/Visitor receive no history; no account PII or secret appears Observation: Inspect actual catalog_audit_event rows and exact GraphQL/protobuf response values; verify additive fields8/9 preserve old field numbering. This verifies the selected techdoc1.7 projection; historical techdoc1.5 lacked these fields. Read only catalog_audit_event through unchanged GraphQL/protobuf fields and stored outcome allowlist; Core/Media and private Catalog ACTIVITY/attempt/recovery operational_event are excluded from Maintainer history. No storage consolidation expands public AuditOutcome or exposes startup/approval/processing metadata. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-265-T1-R01 · L11079](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | audit; Maintainer; complete permitted `audit` page; Show read-only time, opaque actorRef, action, subjectKind/subjectId, stored outcome and sanitized changes. Distinguish APPROVED from SUCCEEDED creation; no account email/credentials/raw files or restore controls. |
| [ATDD-265-T1-R02 · L11080](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | audit-detail; Maintainer; inspect a permitted event; Show actorRef, subject kind/ID, time, exact outcome/action and allowed before/after changes (or truthful large-change summary). Approval and committed creation have distinct events even in one transaction; no arbitrary edit/restore. |
| [ATDD-265-T1-R03 · L11081](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | audit-loading; Maintainer; hold `audit` read; Show Loading history and announce pending retrieval; do not infer absence of changes or display restricted cached events. |
| [ATDD-265-T1-R04 · L11082](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | audit-empty; Maintainer; receive successful empty `audit` result; Show No history yet for current query and retain read-only navigation. Empty is distinct from outage, pending or denial. |
| [ATDD-265-T1-R05 · L11083](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | audit-failed; Maintainer; fail the `audit` read; Show History could not load with safe read retry. Failure cannot imply no business change or roll back a committed event. |
| [ATDD-265-T1-R06 · L11084](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | audit-denied; Visitor/Viewer; open/request catalog history; Show History access denied and no events/private changes; provide permitted navigation and no editing/restore control. |
| [F-AUDIT-CORRECTION · L182](../../../05-atdd.md#f-audit-correction-approved-projection-reconciliation) | PRD §5.3/AC-114 requires actor, action, affected record, time, outcome and changes. Techdoc1.5 omitted outcome/subject kind despite stored columns. The coordinated techdoc1.7 source revision now resolves this mismatch: add GraphQL `outcome: AuditOutcome!` and `subjectKind: String!`; AuditOutcome values are SUCCEEDED, DENIED, FAILED, APPROVED, REJECTED. Add protobuf `string outcome = 8; string subject_kind = 9;` without changing fields1..7. The string outcome accepts only the stored allowlist on projection. Existing subjectId/subject_id remains nullable/optional. These fields are now explicit in techdoc1.7 and remain unimplemented acceptance requirements; the historical projection discrepancy is resolved. Approval and actual creation have distinct recorded events even when approve/create commits atomically. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T031` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T031` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| visual-review | Inspect relevant changed states against the adopted references; retain exhaustive matrix coverage with T044 unless required by this feature | `/Users/mekari/Developer/personal-project/movie-app` | Required visual differences resolved at current revision; no overflow/missing identity | yes |
| interaction-accessibility | Exercise keyboard/focus/labels/errors/live regions/dialogs, zoom/reduced motion and source browser/host matrix independently of pixels | `/Users/mekari/Developer/personal-project/movie-app` | Meaningful accessible interaction and safe recovery, with unavailable contexts kept unverified | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Use the linked adopted references and actual feature for relevant changed states. Browser assertions and direct inspection establish ordinary behavior; retain screenshots when visual inspection needs them. Record actual fixtures, environment and outcomes. Exhaustive theme/width/browser matrices below remain release coverage references owned by T044 unless the selected behavior needs that exact observation. Do not delete their case IDs or claim deferred proof passed.

Check changed keyboard/focus/labels/errors/announcements and relevant zoom, motion, touch and browser behavior. Preserve known regressions and actual reader observations required to prove speech; exhaustive compatibility sweeps remain release-owned. For AI states include actual Claude/ChatGPT host context at T038/T039; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

| Flow/state reference | Actor and fixture condition | Trigger and expected comparison |
|---|---|---|
| [audit / audit](../../../design-system/mockup/audit.html#audit) · [ATDD-265-T1-R01 · L11079](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | Maintainer; complete permitted `audit` page; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show read-only time, opaque actorRef, action, subjectKind/subjectId, stored outcome and sanitized changes. Distinguish APPROVED from SUCCEEDED creation; no account email/credentials/raw files or restore controls. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [audit / audit-detail](../../../design-system/mockup/audit.html#audit-detail) · [ATDD-265-T1-R02 · L11080](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | Maintainer; inspect a permitted event; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show actorRef, subject kind/ID, time, exact outcome/action and allowed before/after changes (or truthful large-change summary). Approval and committed creation have distinct events even in one transaction; no arbitrary edit/restore. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [audit / audit-loading](../../../design-system/mockup/audit.html#audit-loading) · [ATDD-265-T1-R03 · L11081](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | Maintainer; hold `audit` read; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Loading history and announce pending retrieval; do not infer absence of changes or display restricted cached events. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [audit / audit-empty](../../../design-system/mockup/audit.html#audit-empty) · [ATDD-265-T1-R04 · L11082](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | Maintainer; receive successful empty `audit` result; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show No history yet for current query and retain read-only navigation. Empty is distinct from outage, pending or denial. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [audit / audit-failed](../../../design-system/mockup/audit.html#audit-failed) · [ATDD-265-T1-R05 · L11083](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | Maintainer; fail the `audit` read; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show History could not load with safe read retry. Failure cannot imply no business change or roll back a committed event. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [audit / audit-denied](../../../design-system/mockup/audit.html#audit-denied) · [ATDD-265-T1-R06 · L11084](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback) | Visitor/Viewer; open/request catalog history; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show History access denied and no events/private changes; provide permitted navigation and no editing/restore control. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| boundary-tests | Not run | — | — | — | — |
| visual-review | Not run | — | — | — | — |
| interaction-accessibility | Not run | — | — | — | — |
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
