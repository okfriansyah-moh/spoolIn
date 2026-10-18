---
kind: task
id: T029
group: G18
depends_on:
- T009
- T006
- T007
- T012
- T016
- T028
readiness: Ready
status: In progress
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T029 — Commit immutable proposals and exact approval once

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-090 | implements |
| ATDD-091 | implements |
| ATDD-092 | implements |
| ATDD-093 | implements |
| ATDD-094 | implements |
| ATDD-095 | implements |
| ATDD-096 | implements |
| ATDD-100 | implements |
| ATDD-130 | implements |
| ATDD-145 | implements |
| ATDD-186 | implements |
| ATDD-187 | implements |
| ATDD-188 | implements |
| ATDD-189 | both |
| ATDD-190 | both |
| ATDD-191 | implements |
| ATDD-192 | implements |
| ATDD-221 | implements |
| ATDD-222 | implements |
| ATDD-246 | implements |
| ATDD-246-T1-R01 | implements |
| ATDD-246-T1-R02 | implements |
| ATDD-246-T1-R03 | implements |
| ATDD-246-T1-R04 | implements |
| ATDD-246-T1-R05 | implements |
| ATDD-246-T1-R06 | implements |
| ATDD-246-T1-R07 | implements |
| ATDD-246-T1-R08 | implements |
| ATDD-246-T1-R09 | implements |
| ATDD-246-T1-R10 | implements |
| ATDD-246-T1-R11 | implements |
| ATDD-246-T1-R12 | implements |
| ATDD-246-T1-R13 | implements |
| ATDD-247 | implements |
| ATDD-247-T1-R01 | implements |
| ATDD-247-T1-R02 | implements |
| ATDD-247-T1-R03 | implements |
| ATDD-247-T1-R04 | implements |
| ATDD-247-T1-R05 | implements |
| ATDD-247-T1-R06 | implements |
| ATDD-253-T1-R28 | implements |
| ATDD-253-T1-R29 | implements |
| ATDD-253-T1-R30 | implements |
| ATDD-255-T1-R03 | implements |
| ATDD-255-T1-R04 | implements |
| ATDD-255-T1-R05 | implements |
| ATDD-255-T1-R06 | implements |
| ATDD-266 | implements |
| ATDD-283 | both |
| ATDD-294 | both |
| ATDD-308 | both |
| ATDD-313 | implements |
| ATDD-314 | implements |
| J-10 | both |
| SC-09 | implements |
| FR-012 | implements |
| FR-016 | both |
| FR-017 | implements |
| NFR-008 | both |
| NFR-016 | implements |
| AC-116 | implements |
| AC-116-C01 | implements |
| AC-117 | both |
| AC-117-C01 | both |
| AC-117-C02 | both |
| AC-118 | both |
| AC-118-C01 | both |
| AC-118-C02 | both |
| AC-119 | implements |
| AC-119-C01 | implements |
| AC-119-C02 | implements |
| AC-120 | implements |
| AC-120-C01 | implements |
| AC-120-C02 | implements |
| AC-121 | both |
| AC-121-C01 | both |
| AC-122 | implements |
| AC-122-C01 | implements |
| AC-126 | implements |
| AC-126-C01 | implements |
| AC-156 | implements |
| AC-156-C01 | implements |
| AC-156-C02 | implements |
| AC-156-C03 | implements |
| G-07 | both |
| OD-019 | implements |
| US-009 | implements |
| US-019 | implements |
| PRD-POLICY-02 | implements |
| PRD-POLICY-15 | implements |
| PRD-POLICY-16 | implements |
| PRD-POLICY-17 | implements |
| PRD-POLICY-25 | implements |
| PRD-POLICY-33 | implements |
| PRD-CONCEPT-10 | implements |
| PRD-CONCEPT-11 | implements |
| PRD-HANDOFF-12 | implements |
| TD-C-022 | both |
| TD-C-034 | both |
| TD-C-053 | both |
| TD-ROW-L4868 | both |
| TD-ROW-L4869 | both |
| TD-ROW-L4870 | both |
| TD-ROW-L4871 | both |
| TD-ROW-L4872 | both |
| TD-ROW-L4873 | both |
| TD-ROW-L4874 | both |
| TD-ROW-L4875 | both |
| TD-ROW-L4876 | both |
| TD-ROW-L4877 | both |
| TD-ROW-L4878 | both |
| TD-ROW-L4879 | both |
| TD-ROW-L4880 | both |
| TD-ROW-L4881 | both |
| TD-ROW-L4882 | both |
| TD-ROW-L4883 | both |
| TD-ROW-L4884 | both |
| TD-ROW-L4885 | both |
| TD-ROW-L4886 | both |
| TD-BLOCK-L2778 | both |
| TD-BLOCK-L2791 | both |
| TD-BLOCK-L2949 | both |
| TD-BLOCK-L3103 | both |
| TD-BLOCK-L3211 | both |
| TD-BLOCK-L5683 | both |
| TD-BLOCK-L5713 | both |
| TD-BLOCK-L5853 | both |
| TD-BLOCK-L5878 | both |
| F-DETERMINISTIC-CONTENT | both |
| ATDD-G-03 | implements |

## Prerequisites

- [T009 — Enforce scoped service identity and shared admission](../../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T006 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T007 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T012 — Serve precise public catalog and people reads](../../10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.
- [T016 — Implement authoritative movie creation and edits](../../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.
- [T028 — Implement isolated MCP transport and connector identity](T028-implement-isolated-mcp-transport-and-connector-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/proposal/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/resources/db/migration/V005__movie_proposals.sql | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/proposal/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/catalog/proposals/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [5.10 MCP output schema registry](../../../04-spoolin-techdoc.md#510-mcp-output-schema-registry): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.11 JSON projections and draft schemas](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [9.2 Proposal, approval and uncertain publication](../../../04-spoolin-techdoc.md#92-proposal-approval-and-uncertain-publication): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-246-T1-R01 · L10074](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-246-T1-R02 · L10075](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `revision` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (revision > 0); no SQL default; owner supplies value` |
| [ATDD-246-T1-R03 · L10076](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `actor_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-246-T1-R04 · L10077](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `connector_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-246-T1-R05 · L10078](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `client_kind` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (client_kind IN ('CLAUDE','CHATGPT')); no SQL default; owner supplies value` |
| [ATDD-246-T1-R06 · L10079](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `content` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; measure PostgreSQL jsonb::text bytes at 131072 and ±1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (jsonb_typeof(content)='object' AND octet_length(content::text)<=131072); no SQL default; owner supplies value` |
| [ATDD-246-T1-R07 · L10080](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `content_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(content_hash)=32); no SQL default; owner supplies value` |
| [ATDD-246-T1-R08 · L10081](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `challenge_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(challenge_hash)=32); no SQL default; owner supplies value` |
| [ATDD-246-T1-R09 · L10082](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `state` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (state IN ('REVIEW','SUPERSEDED','REJECTED','APPROVED','CREATED','EXPIRED','FAILED')); no SQL default; owner supplies value` |
| [ATDD-246-T1-R10 · L10083](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `operation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES operation_receipt(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-246-T1-R11 · L10084](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `movie_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-246-T1-R12 · L10085](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `expires_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-246-T1-R13 · L10086](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-247-T1-R01 · L10132](../../../05-atdd.md#case-atdd-247-enforce-embedded-exact-approval-and-consumption-evidence) | `approval_actor_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL until committed approval; immutable afterwards` |
| [ATDD-247-T1-R02 · L10133](../../../05-atdd.md#case-atdd-247-enforce-embedded-exact-approval-and-consumption-evidence) | `approval_connector_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL until committed approval; immutable afterwards` |
| [ATDD-247-T1-R03 · L10134](../../../05-atdd.md#case-atdd-247-enforce-embedded-exact-approval-and-consumption-evidence) | `approval_content_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL until approved; otherwise exactly 32 bytes` |
| [ATDD-247-T1-R04 · L10135](../../../05-atdd.md#case-atdd-247-enforce-embedded-exact-approval-and-consumption-evidence) | `approved_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL until approved; no SQL default` |
| [ATDD-247-T1-R05 · L10136](../../../05-atdd.md#case-atdd-247-enforce-embedded-exact-approval-and-consumption-evidence) | `approval_expires_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL until approved; > approved_at and <= proposal expires_at` |
| [ATDD-247-T1-R06 · L10137](../../../05-atdd.md#case-atdd-247-enforce-embedded-exact-approval-and-consumption-evidence) | `approval_consumed_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `NULL until publication; within approved lifetime` |
| [ATDD-253-T1-R28 · L10492](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ProposeMovie`; ProposeMovie(Proposal) → ProposalRevision |
| [ATDD-253-T1-R29 · L10493](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ApproveAndCreate`; ApproveAndCreate(ApprovalInput) → Outcome |
| [ATDD-253-T1-R30 · L10494](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `RejectProposal`; RejectProposal(ApprovalInput) → Outcome |
| [ATDD-255-T1-R03 · L10586](../../../05-atdd.md#case-atdd-255-verify-mcpgatewayservice-typed-service-and-actor-boundary) | `ProposeMovie`; ProposeMovie(Proposal) → ProposalRevision |
| [ATDD-255-T1-R04 · L10587](../../../05-atdd.md#case-atdd-255-verify-mcpgatewayservice-typed-service-and-actor-boundary) | `ApproveMovie`; ApproveMovie(ApprovalInput) → Outcome |
| [ATDD-255-T1-R05 · L10588](../../../05-atdd.md#case-atdd-255-verify-mcpgatewayservice-typed-service-and-actor-boundary) | `RejectMovie`; RejectMovie(ApprovalInput) → Outcome |
| [ATDD-255-T1-R06 · L10589](../../../05-atdd.md#case-atdd-255-verify-mcpgatewayservice-typed-service-and-actor-boundary) | `GetMovieOperation`; GetMovieOperation(Id) → Outcome |
| [J-10 · L93](../../../02-PRD.md#32-application-journeys) | Maintainer uses ChatGPT/Claude to add a movie \| Connected assistant acting under Maintainer permission \| Review proposed metadata and sources in chat; explicitly approve the exact content; receive a confirmed creation result |
| [SC-09 · L108](../../../02-PRD.md#4-goals-and-success-criteria) | Keep humans in control of AI creation \| Both supported chat clients demonstrate exact-content human approval, correction/rejection, safe retry, and denied unauthorized creation \| Unverified; Committed |
| [FR-012 · L246](../../../02-PRD.md#61-functional-requirements) | Enforce the public/Viewer/Maintainer capability matrix, including private movie/people archives and connected tools \| D05 Committed; product policy Committed (section 13.1) \| AC-050 through AC-052, AC-113, AC-119, AC-133, AC-134 |
| [FR-016 · L250](../../../02-PRD.md#61-functional-requirements) | Create movies from MCP proposals only after exact-content human approval in ChatGPT/Claude \| Committed product policy (section 13.1) \| AC-116 through AC-122, AC-126, AC-148, AC-156 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-016 · L274](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [AC-116 · L501](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Proposal review \| A connected Maintainer requests a movie proposal \| The assistant presents it \| Show all proposed publishable values, required core metadata, source references, and explicit unknowns in chat before creation; no proposed record is public yet. |
| [AC-116-C01 · L501](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Show all proposed publishable values, required core metadata, source references, and explicit unknowns in chat before creation; no proposed record is public yet. |
| [AC-117 · L502](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Exact-content approval \| A complete valid proposal is visible to its Maintainer \| That human explicitly approves the exact proposal \| Create one movie only if permission and validation still hold; show confirmed result and attributable approval/creation history. Self-approval by the proposing Maintainer is allowed. |
| [AC-117-C01 · L502](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Create one movie only if permission and validation still hold; show confirmed result and attributable approval/creation history. |
| [AC-117-C02 · L502](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Self-approval by the proposing Maintainer is allowed. |
| [AC-118 · L503](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Edit/reject \| A proposal is awaiting approval or changes after approval \| The Maintainer edits or rejects it \| Editing requires a new review and explicit approval of revised content. Rejection publishes nothing; silence or a general earlier request is not approval. |
| [AC-118-C01 · L503](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Editing requires a new review and explicit approval of revised content. |
| [AC-118-C02 · L503](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Rejection publishes nothing; silence or a general earlier request is not approval. |
| [AC-119 · L504](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Forged/unauthorized approval \| A Viewer, disconnected/revoked identity, or assistant claiming human approval attempts creation \| The action is evaluated \| Deny creation unless actual authorized human approval for the exact content is established. The assistant cannot promote itself or bypass access through MCP. |
| [AC-119-C01 · L504](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Deny creation unless actual authorized human approval for the exact content is established. |
| [AC-119-C02 · L504](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | The assistant cannot promote itself or bypass access through MCP. |
| [AC-120 · L505](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Invalid/unsupported information \| Core data are missing or outside effective input limits, sources disagree, or claims cannot be substantiated \| A proposal is reviewed or submitted \| Mark uncertainty and request correction/clarification; do not invent required metadata to force acceptance. Missing or invalid required data prevent publication even after a generic approval; corrected publishable content requires fresh approval under AC-156. |
| [AC-120-C01 · L505](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Mark uncertainty and request correction/clarification; do not invent required metadata to force acceptance. |
| [AC-120-C02 · L505](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Missing or invalid required data prevent publication even after a generic approval; corrected publishable content requires fresh approval under AC-156. |
| [AC-121 · L506](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Duplicate/uncertain creation \| Approved creation loses its response or is repeated \| The Maintainer checks/retries \| Resolve the original outcome and report one created movie or a recoverable failure; no blind duplication, silent content change, or unconfirmed success. |
| [AC-121-C01 · L506](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Resolve the original outcome and report one created movie or a recoverable failure; no blind duplication, silent content change, or unconfirmed success. |
| [AC-122 · L507](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Both chat clients \| Authorized ChatGPT and Claude connections are configured \| The proposal/review/correct/approve/reject journey is exercised in each \| Both demonstrate the same product permissions and exact-content approval rule; unsupported client enforcement is a release blocker, not permission to accept model-generated approval claims. |
| [AC-122-C01 · L507](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Both demonstrate the same product permissions and exact-content approval rule; unsupported client enforcement is a release blocker, not permission to accept model-generated approval claims. |
| [AC-126 · L508](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Hostile source/instruction \| Retrieved metadata or model output contains instructions to bypass approval, reveal PII, or perform excluded actions \| The proposal flow handles it \| Treat it as untrusted content, refuse the unauthorized action, expose no private information, and leave the catalog unchanged until a valid permitted proposal receives human approval. |
| [AC-126-C01 · L508](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Treat it as untrusted content, refuse the unauthorized action, expose no private information, and leave the catalog unchanged until a valid permitted proposal receives human approval. |
| [AC-156 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | MCP correction changes reviewed content \| A Maintainer reviewed a proposal and a publishable value violates a limit or requires normalization/correction \| The proposal is submitted or corrected \| Invalid values publish nothing. Show the revised exact publishable proposal and obtain fresh explicit human approval before creating it. Neither prior approval nor an assistant claim authorizes changed content; retries/uncertain outcomes still obey AC-121. |
| [AC-156-C01 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid values publish nothing. |
| [AC-156-C02 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show the revised exact publishable proposal and obtain fresh explicit human approval before creating it. |
| [AC-156-C03 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Neither prior approval nor an assistant claim authorizes changed content; retries/uncertain outcomes still obey AC-121. |
| [G-07 · L928](../../../02-PRD.md#122-release-gates-remain-planned) | All Required/Committed outcomes, including both MCP clients and human approval, have evidence; repository/README and code ownership are verified \| NFR-011; FR-016 / AC-116 through AC-122, AC-126, AC-156; AC-095 through AC-097 \| Planned; release access requires verification |
| [OD-019 · L1055](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Product discovery/feedback and section 5.5 input/artwork policy settled; GraphQL/gRPC and MCP contracts, human-approval enforcement, consistent counting/validation, safe display/data handling, compatibility, remaining request/query/page and decoding resource bounds, ordering ties/navigation, responsive support matrix, and accessibility verification remain for techdoc \| NFR-007/NFR-013/NFR-016, FR-016/FR-017 \| Technical design; before dependent implementation |
| [US-009 · L445](../../../02-PRD.md#us-009-review-accountable-history) | ### US-009: Review accountable history As a Maintainer, I want read-only history so I can understand catalog changes and AI approvals. Requirements: FR-013, NFR-015. Audit viewing is a privileged product capability, not a restoration feature. |
| [US-019 · L495](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | ### US-019: Approve AI-assisted movie creation in chat As a connected Maintainer, I want to review suggested metadata in ChatGPT or Claude before it becomes public. Requirements: FR-016, FR-012, FR-007, FR-013, NFR-014, NFR-015. The assistant proposes content as a connected tool; the Maintainer's explicit approval and current permission authorize creation. |
| [PRD-POLICY-02 · L116](../../../02-PRD.md#51-included-behavior) | Manual and AI-assisted movie creation require a usable title, synopsis, at least one genre, and release year. A Maintainer can correct optional information without supplying invented values. Creation publishes an active movie only after validation and a confirmed save. An AI proposal is not a public catalog entry. |
| [PRD-POLICY-15 · L146](../../../02-PRD.md#53-accounts-audit-and-ai-boundaries) | Maintainers have a read-only audit screen for catalog changes, movie and person lifecycle actions, credit changes, artwork changes, and AI approvals/creation outcomes. It shows actor, action, affected record, time, outcome, and relevant changes. Approval and actual movie creation are distinguishable events; approval does not prove creation succeeded. Technical security events remain available to authorized operational inspection without requiring a separate user-management or security-console product. |
| [PRD-POLICY-16 · L148](../../../02-PRD.md#53-accounts-audit-and-ai-boundaries) | MCP creation is limited to one proposed new movie per approval. The connected Maintainer reviews the complete proposed values and source references in ChatGPT or Claude, corrects or rejects them, and explicitly approves the exact content. The proposing Maintainer may approve it; no second person is required. Any change to publishable content requires fresh approval. The assistant cannot infer approval from an earlier request, manufacture it, or use source text as authorization. Creation still requires current Maintainer permission and valid data. Chat approval enforcement and compatibility are techdoc responsibilities, not permission to weaken this rule. |
| [PRD-POLICY-17 · L160](../../../02-PRD.md#55-input-validation-and-artwork-policy) | Visitors registering or searching, and Maintainers creating or editing movies, people, and credits, need to know what input is accepted before submitting it. Current permissions and the active-record editing rules still apply. Validation cannot grant access, resolve a stale edit, or authorize an AI proposal. FR-017 and NFR-016 govern these outcomes; US-021 provides acceptance criteria. |
| [PRD-POLICY-25 · L198](../../../02-PRD.md#validation-safe-handling-and-feedback) | The same effective validation policy applies to creation and editing through forms and direct requests, and to approved MCP creation. A caller cannot bypass it by omitting browser checks. For MCP proposals, prepare normalized publishable values before review; if validation or correction changes those values afterward, obtain fresh approval for the revised proposal. Never alter approved content silently to make it fit a limit. |
| [PRD-POLICY-33 · L225](../../../02-PRD.md#configuration-and-existing-content) | If a limit changes while a draft or upload is in progress, validate changed/new input against the effective policy at submission or attachment. Show updated guidance and the reason for rejection, preserve permitted drafts and prior artwork, and let the user correct or retry safely. A policy change must not permit a stale overwrite, bypass approval, or convert an uncertain save into a confirmed failure. Configuration controls are documented deployment facilities, not a new Maintainer settings screen. |
| [PRD-CONCEPT-10 · L818](../../../02-PRD.md#9-conceptual-information-and-privacy) | AI proposal \| Unpublished movie values and source references presented for exact-content human approval; corrected proposals need renewed approval. A proposal is not a review/rating. |
| [PRD-CONCEPT-11 · L819](../../../02-PRD.md#9-conceptual-information-and-privacy) | Audit record \| Attributable action, affected record, time, outcome, and relevant changes. It distinguishes approval, creation, failures, movie/person archive/restore/permanent deletion, and credit changes. |
| [PRD-HANDOFF-12 · L845](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | MCP creation policy; FR-016 \| Committed product constraint; mechanism Open \| Real ChatGPT/Claude compatibility; authenticated Maintainer scope; trustworthy evidence of explicit human approval for exact content; validation, safe replay, provenance, and audit. Technical feasibility must be demonstrated before this feature is called complete. |
| [TD-C-022 · L2762](../../../04-spoolin-techdoc.md#510-mcp-output-schema-registry) | Merge and resolve output registry local references; exact additionalProperties-false context, proposalReview and operationReceipt results shared by tool bindings; never expose challenge in model outputs. |
| [TD-C-034 · L4856](../../../04-spoolin-techdoc.md#movie_proposal) | Immutable proposal composite revision with complete same-row approval binding, one current REVIEW/CREATED partial unique indexes, original publish receipt; exact approval and creation audit atomicity. |
| [TD-C-053 · L5661](../../../04-spoolin-techdoc.md#9-mcp-integration-and-exact-content-approval) | Private per-human client-bound seven-day connector, exact MCP input shapes/effective bounds, immutable normalized proposal/hash/revision/source review, UI-only challenge, atomic one-use approval/create and reject; all model-forged paths denied. |
| [TD-ROW-L4868 · L4868](../../../04-spoolin-techdoc.md#movie_proposal) | `id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Proposal identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4869 · L4869](../../../04-spoolin-techdoc.md#movie_proposal) | `revision` \| `integer` \| `NOT NULL CHECK (revision > 0); no SQL default; owner supplies value` \| Monotonic immutable revision. API; presence/clearing §5 \| `Int` → Int / int32 |
| [TD-ROW-L4870 · L4870](../../../04-spoolin-techdoc.md#movie_proposal) | `actor_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Core account logical reference. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4871 · L4871](../../../04-spoolin-techdoc.md#movie_proposal) | `connector_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Core credential logical reference. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4872 · L4872](../../../04-spoolin-techdoc.md#movie_proposal) | `client_kind` \| `varchar(16)` \| `NOT NULL CHECK (client_kind IN ('CLAUDE','CHATGPT')); no SQL default; owner supplies value` \| Trusted adapter route. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4873 · L4873](../../../04-spoolin-techdoc.md#movie_proposal) | `content` \| `jsonb` \| `NOT NULL CHECK (jsonb_typeof(content)='object' AND octet_length(content::text)<=131072); no SQL default; owner supplies value` \| Exact normalized CreateMovieInput+sources; no account PII. API; presence/clearing §5 \| `TypedJsonDTO` → typed projection/JSON / typed message |
| [TD-ROW-L4874 · L4874](../../../04-spoolin-techdoc.md#movie_proposal) | `content_hash` \| `bytea` \| `NOT NULL CHECK (octet_length(content_hash)=32); no SQL default; owner supplies value` \| SHA256 deterministically serialized JSON bytes. API; presence/clearing §5 \| `ByteArray` → Base64/bytes / bytes |
| [TD-ROW-L4875 · L4875](../../../04-spoolin-techdoc.md#movie_proposal) | `challenge_hash` \| `bytea` \| `NOT NULL CHECK (octet_length(challenge_hash)=32); no SQL default; owner supplies value` \| Digest of random UI-only capability; raw value only tool _meta. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4876 · L4876](../../../04-spoolin-techdoc.md#movie_proposal) | `state` \| `varchar(16)` \| `NOT NULL CHECK (state IN ('REVIEW','SUPERSEDED','REJECTED','APPROVED','CREATED','EXPIRED','FAILED')); no SQL default; owner supplies value` \| Revision state. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4877 · L4877](../../../04-spoolin-techdoc.md#movie_proposal) | `operation_id` \| `uuid` \| `NOT NULL REFERENCES operation_receipt(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Original creation operation, cannot be replaced on timeout. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4878 · L4878](../../../04-spoolin-techdoc.md#movie_proposal) | `movie_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Logical result retained after future movie deletion. API; presence/clearing §5 \| `UUID?` → UUID / optional string |
| [TD-ROW-L4879 · L4879](../../../04-spoolin-techdoc.md#movie_proposal) | `expires_at` \| `timestamptz(6)` \| `NOT NULL; no SQL default; owner supplies value` \| Approval challenge expires 10 min after creation. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4880 · L4880](../../../04-spoolin-techdoc.md#movie_proposal) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4881 · L4881](../../../04-spoolin-techdoc.md#movie_proposal) | `approval_actor_id` \| `uuid` \| NULL until committed approval; immutable afterwards \| Exact verified approver; must equal proposal actor_id; never routine telemetry; audit only under §6.1.1 allowlist \| `UUID?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L4882 · L4882](../../../04-spoolin-techdoc.md#movie_proposal) | `approval_connector_id` \| `uuid` \| NULL until committed approval; immutable afterwards \| Exact connector; must equal connector_id; never routine telemetry; audit only under §6.1.1 allowlist \| `UUID?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L4883 · L4883](../../../04-spoolin-techdoc.md#movie_proposal) | `approval_content_hash` \| `bytea` \| NULL until approved; otherwise exactly 32 bytes \| Exact approved content hash; must equal immutable content_hash; never routine telemetry; audit only under §6.1.1 allowlist \| `ByteArray?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L4884 · L4884](../../../04-spoolin-techdoc.md#movie_proposal) | `approved_at` \| `timestamptz(6)` \| NULL until approved; no SQL default \| Actual verified human-approval instant; never routine telemetry; audit only under §6.1.1 allowlist \| `Instant?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L4885 · L4885](../../../04-spoolin-techdoc.md#movie_proposal) | `approval_expires_at` \| `timestamptz(6)` \| NULL until approved; &gt; approved_at and &lt;= proposal expires_at \| min(approved_at + two minutes, proposal expires_at); historical evidence remains after expiry; never routine telemetry; audit only under §6.1.1 allowlist \| `Instant?` → Internal only; no GraphQL/protobuf field |
| [TD-ROW-L4886 · L4886](../../../04-spoolin-techdoc.md#movie_proposal) | `approval_consumed_at` \| `timestamptz(6)` \| NULL until publication; within approved lifetime \| One-use publication consumption, committed with title/receipt/audit; never routine telemetry; audit only under §6.1.1 allowlist \| `Instant?` → Internal only; no GraphQL/protobuf field |
| [TD-BLOCK-L2778 · L2778](../../../04-spoolin-techdoc.md#5101-registry-header) | 5.10.1 Registry header — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L2791 · L2791](../../../04-spoolin-techdoc.md#5102-movie_context-output) | 5.10.2 movie_context output — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L2949 · L2949](../../../04-spoolin-techdoc.md#5103-propose_movie-output) | 5.10.3 propose_movie output — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L3103 · L3103](../../../04-spoolin-techdoc.md#5104-shared-operation-receipt-output) | 5.10.4 Shared operation receipt output — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L3211 · L3211](../../../04-spoolin-techdoc.md#5105-tool-output-bindings) | 5.10.5 Tool output bindings — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L5683 · L5683](../../../04-spoolin-techdoc.md#911-movie_context-input-schema) | 9.1.1 movie_context input schema — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L5713 · L5713](../../../04-spoolin-techdoc.md#912-propose_movie-input-schema) | 9.1.2 propose_movie input schema — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L5853 · L5853](../../../04-spoolin-techdoc.md#913-movie_operation-input-schema) | 9.1.3 movie_operation input schema — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L5878 · L5878](../../../04-spoolin-techdoc.md#914-trusted-review-approval-input) | 9.1.4 Trusted review approval input — complete json contract, all members, constraints and field numbers |
| [ATDD-G-03 · L14187](../../../05-atdd.md#required-execution-and-release-prerequisites) | G-03 each actual chat client \| Independent ClaudeDesktop and ChatGPT registration/versions, UI/model isolation and trustworthy UI-origin verification; publication remains disabled until supported proof |

## Implementation

1. Implement immutable normalized proposal revisions, deterministic serialization/hash and field-level source/unknown representation. Validate data before review; missing required metadata never becomes invented content.
2. Store same-row exact approval binding and one-use consumption evidence under Catalog lock, with current actor/client/credential/epoch/hash/revision/policy/challenge/expiry checks. Enforce pending-proposal quota20 in Catalog, resolving §5.4's adapter wording by owner authority.
3. Commit valid exact approval, publication, safe receipt and distinct audit events atomically. Correction creates a new review requiring new human approval; reject publishes nothing and cannot undo an already created movie.
4. Apply independent provenance storage and MCP result byte bounds, escaped/untrusted instructions, no arbitrary source URL fetch or source-identity inference from citation URLs.
5. Test replay, concurrent approval/correction/rejection, expiration, revoked credentials and commit response loss through actual Core/Catalog with an explicitly controlled trusted-host fixture. That fixture cannot close real client enforcement.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T029-A1:** Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing.
- **T029-A2:** Revision: the owner returns every complete revised value/source/unknown and invalidates prior approval; rejected proposal stays nonpublic. T030 renders these values and T039 owns complete ATDD-188 correction/redraw proof.
- **T029-A3:** Durability: lost response returns original movie result, late reject never deletes it, and bounded provenance/result rejection causes no partial publication.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-189](../../../05-atdd.md#case-atdd-189-consume-exact-approval-and-publication-atomically) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §9 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then only valid live actor/current policy/exact revision/hash and unexpired10min challenge can create And the two-minute approval deadline and one-use binding apply without allowing a later changed payload And one transaction commits movie/genres/provenance/approval consumption/audit/work/original receipt; repeats resolve one creation Observation: Use actual Catalog transaction barriers before commit and after commit/before response. Inspect one consumed current challenge/approval, movie/genres/provenance/audit/work and original receipt; duplicate clicks create one movie. F-EXPIRY covers exact challenge/approval cutoff and policy/actor rechecks. ApprovalEvidence is embedded on immutable movie_proposal revision, not a separate table: verify exact approval_actor_id/connector_id/content_hash and consumed timestamp with the original binding. All consumption/domain/receipt/catalog_audit_event/work writes share the actual transaction; partial/mismatched bindings reject and a second CREATED revision cannot publish. |
| [ATDD-190](../../../05-atdd.md#case-atdd-190-reject-a-proposal-without-undoing-an-existing-movie) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §9 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then rejection produces no movie and records original FAILED/REJECTED receipt And repeated rejection is stable And post creation rejection reports original success without deletion; silence/general requests/model claims never consent Observation: Observe trusted rejection against current REVIEW revision and repeat it, then reject an already-created revision. Inspect proposal/original operation and zero new movie on rejection; later rejection reports the committed creation without delete/rollback. |
| [ATDD-283](../../../05-atdd.md#case-atdd-283-freeze-deterministically-serialized-content-with-reviewed-byte-vectors) / Unit | F-DETERMINISTIC-CONTENT vectors A/B/C/D: key reordering, genre permutation, composed/decomposed title, ordered character reversal, explicit null versus omitted; expected bytes and digest in the fixture. | Then object-key order and set-like genre order do not change deterministically serialized bytes And changed exact strings, ordered characters, explicit clearing, expectedVersion or policyRevision change the appropriate fingerprint And the owner-produced reviewed bytes hash to their listed SHA-256 value Observation: Compare with the fixture’s fixed byte strings/digests, not a second call to the implementation. The adapter displays the returned owner hash and need not independently reserialize. |
| [ATDD-294](../../../05-atdd.md#case-atdd-294-require-exact-current-human-review-binding) / Unit | Current proposal revision 2/hashH/actorA/clientCLAUDE/challengeC/policyP; change one field, old revision 1, replayed invocation, expiryT and model approved=true. | Then only exact current trusted binding before expiry is eligible And model claims, changed actor/client/content/policy, stale revision and reused challenge are denied And the rule alone cannot establish that a real host attests UI origin Observation: Assert deny/eligible reasons with one independently changed input per row; actual host, MAC and atomic challenge consumption are separate proofs. |
| [ATDD-308](../../../05-atdd.md#case-atdd-308-enforce-provenance-storage-and-mcp-result-bounds-independently) / Integration | F-SOURCES: validfieldSources whose actual Postgresjsonb::text is16383/16384/16385 bytes; duplicatefieldsame source record; multiple sources; overallrequest/result131071/131072/131073 bytes. | Then valid within-bound evidence is preserved completely with review/hash binding And persisted-map overflow rejects without partial proposal/publication or dropped provenance And oversized MCP input/result fails with safe capacity feedback and no truncated reviewed payload Observation: Measure octet_length(field_sources::text) in PostgreSQL for persisted fixtures, separately capture wire byte size and inspect source/proposal/movie/receipt rollback. Label field shape invalidity INVALID_ARGUMENT; capacity envelopes use RESOURCE_EXHAUSTED. Do not infer jsonb size from compact wire JSON. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-DETERMINISTIC-CONTENT · L178](../../../05-atdd.md#f-deterministic-content-owner-vectors-and-exact-review) | Vectors exercise the owner serializer, not independent adapter serialization. Compact sorted-key UTF-8 vector A is `{"genres":["a","b"],"synopsis":"A\nB","title":"Café","unknown":null}` (the escaped newline is JSON bytes, not two literal content characters). Vector A SHA-256 is `6c35f24215047d8053eaeba11848dc0b1bd44144067e952c5ad5c4fc34240903`. Vector B provides reversed key and genre input order and must produce A. Vector C replaces é with e+U+0301 and must differ. Vector D uses ordered characters ["Maya","Maya / Echo"] versus their reversal and must differ. Serializer-only vectors are not complete valid movie proposals. For real publication use full F-MCP values, normalized source evidence and owner-returned bytes/hash; object keys sort, genre sets sort, character/source sequences retain selected order, optional unknowns are explicit and exact post-trim strings are not NFC-normalized. Operation fingerprints additionally bind clear/presence masks, expected versions and policy revision; private inputs use keyed HMAC. The adapters display owner-returned hashes. Reviewed complete vectors and their bytes are captured with expectation revision before executable checks are implemented. |

Partial implementation contributions: [ATDD-090](../../../05-atdd.md#case-atdd-090-proposal-review), [ATDD-091](../../../05-atdd.md#case-atdd-091-exact-content-approval), [ATDD-092](../../../05-atdd.md#case-atdd-092-editreject), [ATDD-093](../../../05-atdd.md#case-atdd-093-forgedunauthorized-approval), [ATDD-094](../../../05-atdd.md#case-atdd-094-invalidunsupported-information), [ATDD-095](../../../05-atdd.md#case-atdd-095-duplicateuncertain-creation), [ATDD-096](../../../05-atdd.md#case-atdd-096-both-chat-clients), [ATDD-100](../../../05-atdd.md#case-atdd-100-hostile-sourceinstruction), [ATDD-130](../../../05-atdd.md#case-atdd-130-mcp-correction-changes-reviewed-content), [ATDD-145](../../../05-atdd.md#case-atdd-145-validate-exact-recipient-output-schemas), [ATDD-186](../../../05-atdd.md#case-atdd-186-require-independent-real-client-proof-of-human-review), [ATDD-187](../../../05-atdd.md#case-atdd-187-bind-review-authority-to-one-human-and-client), [ATDD-188](../../../05-atdd.md#case-atdd-188-freeze-and-redraw-every-reviewed-revision), [ATDD-191](../../../05-atdd.md#case-atdd-191-treat-sources-and-model-output-as-untrusted-facts), [ATDD-192](../../../05-atdd.md#case-atdd-192-reconnect-mcp-without-repeating-a-publication), [ATDD-221](../../../05-atdd.md#case-atdd-221-prove-exact-human-approval-in-claude-desktop), [ATDD-222](../../../05-atdd.md#case-atdd-222-prove-exact-human-approval-in-chatgpt), [ATDD-246](../../../05-atdd.md#case-atdd-246-enforce-immutable-proposal-revision-storage-and-authorized-review), [ATDD-247](../../../05-atdd.md#case-atdd-247-enforce-embedded-exact-approval-and-consumption-evidence), [ATDD-266](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback), [ATDD-313](../../../05-atdd.md#case-atdd-313-evaluate-ordinary-and-held-out-proposal-outcomes-in-claude-desktop), [ATDD-314](../../../05-atdd.md#case-atdd-314-evaluate-ordinary-and-held-out-proposal-outcomes-in-chatgpt). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T029` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T029` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- Hand the owned outputs to [T039 — Verify ChatGPT approval and complete both-client acceptance](../../24-assisted-movie-creation-client-acceptance/tasks/T039-verify-chatgpt-approval-and-complete-both-client-acceptance.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
