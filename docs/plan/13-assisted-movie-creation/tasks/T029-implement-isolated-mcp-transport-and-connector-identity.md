---
kind: task
id: T029
group: G14
depends_on:
- T001
- T002
- T006
- T008
- T009
- T010
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T029 — Implement isolated MCP transport and connector identity

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

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
| ATDD-186 | implements |
| ATDD-187 | implements |
| ATDD-188 | implements |
| ATDD-191 | implements |
| ATDD-192 | implements |
| ATDD-221 | implements |
| ATDD-222 | implements |
| ATDD-232 | implements |
| ATDD-232-T1-R01 | implements |
| ATDD-232-T1-R02 | implements |
| ATDD-232-T1-R03 | implements |
| ATDD-232-T1-R04 | implements |
| ATDD-232-T1-R05 | implements |
| ATDD-232-T1-R06 | implements |
| ATDD-232-T1-R07 | implements |
| ATDD-232-T1-R08 | implements |
| ATDD-232-T1-R09 | implements |
| ATDD-255-T1-R02 | implements |
| ATDD-266 | implements |
| ATDD-313 | implements |
| ATDD-314 | implements |
| SC-09 | implements |
| FR-012 | implements |
| FR-016 | implements |
| FR-017 | implements |
| NFR-016 | implements |
| AC-116 | implements |
| AC-116-C01 | implements |
| AC-117 | implements |
| AC-117-C01 | implements |
| AC-117-C02 | implements |
| AC-118 | implements |
| AC-118-C01 | implements |
| AC-118-C02 | implements |
| AC-119 | implements |
| AC-119-C01 | implements |
| AC-119-C02 | implements |
| AC-120 | implements |
| AC-120-C01 | implements |
| AC-120-C02 | implements |
| AC-121 | implements |
| AC-121-C01 | implements |
| AC-122 | implements |
| AC-122-C01 | implements |
| AC-126 | implements |
| AC-126-C01 | implements |
| AC-156 | implements |
| AC-156-C01 | implements |
| AC-156-C02 | implements |
| AC-156-C03 | implements |
| G-07 | implements |
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
| TD-C-021 | both |
| TD-C-054 | both |
| TD-U-003 | both |
| TD-ROW-L2670 | both |
| TD-ROW-L2671 | both |
| TD-ROW-L2672 | both |
| TD-ROW-L2673 | both |
| TD-ROW-L2674 | both |
| TD-ROW-L4243 | both |
| TD-ROW-L4244 | both |
| TD-ROW-L4245 | both |
| TD-ROW-L4246 | both |
| TD-ROW-L4247 | both |
| TD-ROW-L4248 | both |
| TD-ROW-L4249 | both |
| TD-ROW-L4250 | both |
| TD-ROW-L4251 | both |
| ATDD-G-03 | implements |

## Prerequisites

- [T001 — Lock the build and isolated verification harness](../../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted.
- [T002 — Generate and validate the shared wire contracts](../../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- [T006 — Implement registration and authoritative sessions](../../03-account-access/tasks/T006-implement-registration-and-authoritative-sessions.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.
- [T008 — Enforce scoped service identity and shared admission](../../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T009 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T010 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | mcp/src/server.ts | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/transport/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/gateway/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/identity/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/tools/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/resources/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/operations/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/test/transport/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/test/gateway/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/connector/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/mcpgateway/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/db/migration/V006__connector_credentials.sql | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/connector.sh | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/tunnel.sh | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/connector/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/mcpgateway/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/integrationTest/kotlin/com/spoolin/core/boundary/connector/ | T029 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [2.4.5 MCP adapter and review UI](../../../04-spoolin-techdoc.md#245-mcp-adapter-and-review-ui): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [4.5 MCP adapter](../../../04-spoolin-techdoc.md#45-mcp-adapter): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.8 MCP tools, resources and gateway mapping](../../../04-spoolin-techdoc.md#58-mcp-tools-resources-and-gateway-mapping): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.10 MCP output schema registry](../../../04-spoolin-techdoc.md#510-mcp-output-schema-registry): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [9.1 MCP tool input schemas](../../../04-spoolin-techdoc.md#91-mcp-tool-input-schemas): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-232-T1-R01 · L9331](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-232-T1-R02 · L9332](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `token_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL UNIQUE CHECK (octet_length(token_hash)=32); no SQL default; owner supplies value` |
| [ATDD-232-T1-R03 · L9333](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `account_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-232-T1-R04 · L9334](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `client_kind` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (client_kind IN ('CLAUDE','CHATGPT')); no SQL default; owner supplies value` |
| [ATDD-232-T1-R05 · L9335](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `auth_epoch` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (auth_epoch >= 0); no SQL default; owner supplies value` |
| [ATDD-232-T1-R06 · L9336](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `scope` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (scope='movie:propose movie:approve'); no SQL default; owner supplies value` |
| [ATDD-232-T1-R07 · L9337](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-232-T1-R08 · L9338](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `expires_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-232-T1-R09 · L9339](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage) | `revoked_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-255-T1-R02 · L10585](../../../05-atdd.md#case-atdd-255-verify-mcpgatewayservice-typed-service-and-actor-boundary) | `GetMovieContext`; GetMovieContext(MovieContextRequest) → MovieContext |
| [SC-09 · L108](../../../02-PRD.md#4-goals-and-success-criteria) | Keep humans in control of AI creation \| Both supported chat clients demonstrate exact-content human approval, correction/rejection, safe retry, and denied unauthorized creation \| Unverified; Committed |
| [FR-012 · L246](../../../02-PRD.md#61-functional-requirements) | Enforce the public/Viewer/Maintainer capability matrix, including private movie/people archives and connected tools \| D05 Committed; product policy Committed (section 13.1) \| AC-050 through AC-052, AC-113, AC-119, AC-133, AC-134 |
| [FR-016 · L250](../../../02-PRD.md#61-functional-requirements) | Create movies from MCP proposals only after exact-content human approval in ChatGPT/Claude \| Committed product policy (section 13.1) \| AC-116 through AC-122, AC-126, AC-148, AC-156 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
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
| [TD-C-021 · L2662](../../../04-spoolin-techdoc.md#58-mcp-tools-resources-and-gateway-mapping) | MCP model context/proposal/status and app-only approval/rejection route through Core gateway, actor-bound resource reads, effective policy schemas, 128KiB envelopes and UI-only challenge metadata. Publication assertion disabled until actual host enforcement. |
| [TD-C-054 · L5919](../../../04-spoolin-techdoc.md#91-secure-mcp-tunnel-and-registration-prerequisites) | Private outbound ChatGPT tunnel plus separate Claude stdio bridge, runtime/control-plane versus connector secret separation, installed runner/flags and client/version/discovery/trusted approval evidence independently; absence blocks client completion without weaker approval. |
| [TD-U-003 · L6536](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes) | TD-U-003 \| Independent top-level `mcp/` TypeScript adapter, outside `services/`, no DB access \| D02/10, OD-015/016; three Spring domain/application services remain; adapter is an additional protocol process, not a fourth domain/identity service |
| [TD-ROW-L2670 · L2670](../../../04-spoolin-techdoc.md#58-mcp-tools-resources-and-gateway-mapping) | `movie_context` / model \| GetMovieContext → Catalog SearchTitles/ListGenres/ListRoles \| `{titles:[{id,title,release,genreIds}],genres:[{id,label}],roles:[{id,label,category,allowsCharacters}],policyRevision}`; active metadata only, at most requested limit 1..20; unknown dates explicit |
| [TD-ROW-L2671 · L2671](../../../04-spoolin-techdoc.md#58-mcp-tools-resources-and-gateway-mapping) | `propose_movie` / model \| ProposeMovie → Catalog ProposeMovie \| `{proposalId,revision,contentHash,expiresAt,normalizedMovie,policyRevision,status,reviewResourceUri}`; REVIEW only; normalized complete movie values and bounded source evidence; no raw challenge |
| [TD-ROW-L2672 · L2672](../../../04-spoolin-techdoc.md#58-mcp-tools-resources-and-gateway-mapping) | `movie_operation` / model \| GetMovieOperation → Catalog GetOperation \| `{operationId,state,resultId?,version?,problem?,resultExpired}`; bound connector/actor only; decimal string version; terminal receipt survives response loss |
| [TD-ROW-L2673 · L2673](../../../04-spoolin-techdoc.md#58-mcp-tools-resources-and-gateway-mapping) | `approve_movie` / trusted review UI only \| ApproveMovie → Catalog ApproveAndCreate \| Same sanitized operation receipt; verify exact current revision/hash/actor/client/challenge and UI origin; successful result identifies one created movie |
| [TD-ROW-L2674 · L2674](../../../04-spoolin-techdoc.md#58-mcp-tools-resources-and-gateway-mapping) | `reject_movie` / trusted review UI only \| RejectMovie → Catalog RejectProposal \| Same receipt with original publish FAILED/REJECTED; repeat rejection stable; committed creation returns original successful receipt |
| [TD-ROW-L4243 · L4243](../../../04-spoolin-techdoc.md#mcp_credential) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4244 · L4244](../../../04-spoolin-techdoc.md#mcp_credential) | `token_hash` \| `bytea` \| `NOT NULL UNIQUE CHECK (octet_length(token_hash)=32); no SQL default; owner supplies value` \| 256-bit random credential digest; raw secret only local secret file. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4245 · L4245](../../../04-spoolin-techdoc.md#mcp_credential) | `account_id` \| `uuid` \| `NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Grant owner. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4246 · L4246](../../../04-spoolin-techdoc.md#mcp_credential) | `client_kind` \| `varchar(16)` \| `NOT NULL CHECK (client_kind IN ('CLAUDE','CHATGPT')); no SQL default; owner supplies value` \| Bound client route. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4247 · L4247](../../../04-spoolin-techdoc.md#mcp_credential) | `auth_epoch` \| `bigint` \| `NOT NULL CHECK (auth_epoch >= 0); no SQL default; owner supplies value` \| Revocation check. internal; presence/clearing §5 \| `Long` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4248 · L4248](../../../04-spoolin-techdoc.md#mcp_credential) | `scope` \| `varchar(64)` \| `NOT NULL CHECK (scope='movie:propose movie:approve'); no SQL default; owner supplies value` \| No edit/delete/artwork/audit/account-management tools. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4249 · L4249](../../../04-spoolin-techdoc.md#mcp_credential) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4250 · L4250](../../../04-spoolin-techdoc.md#mcp_credential) | `expires_at` \| `timestamptz(6)` \| `NOT NULL; no SQL default; owner supplies value` \| Hard credential expiry. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4251 · L4251](../../../04-spoolin-techdoc.md#mcp_credential) | `revoked_at` \| `timestamptz(6)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Explicit revocation. internal; presence/clearing §5 \| `Instant?` → not exposed; no GraphQL/proto field |
| [ATDD-G-03 · L14187](../../../05-atdd.md#required-execution-and-release-prerequisites) | G-03 each actual chat client \| Independent ClaudeDesktop and ChatGPT registration/versions, UI/model isolation and trustworthy UI-origin verification; publication remains disabled until supported proof |

## Implementation

1. Run an early installed-client capability preflight for both hosts after the transport exists; record any unavailable registration or app-only protection before investing in publication integration. Preflight is not the final real-client approval proof.
2. Implement the separate Node adapter with Streamable HTTP /mcp, Claude stdio bridge and private health/metrics8788 listener. Deliberately emit the documented mcp/dist/stdio-bridge.js entry; no protocol logs on stdout and no database client/business cache.
3. Provision per-account/client seven-day credentials through the authenticated local operator path, with separate tunnel control-plane/runtime credentials; scopes allow context/propose/status only for model tools.
4. Map exact input/output schemas to typed McpGatewayService clients, bounded deadlines/errors/results and actor-bound resources. UI-only approve/reject registrations never become model-visible routes or user-supplied consent flags.
5. Enforce adapter-local resource counters separately from shared Core quotas. Reconnect/reconcile original operation before repeating a write, and restrict tunnel forwarding to MCP alone.
6. Leave publication disabled for any host without T032/T033 real enforcement proof; local protocol tests establish adapter behavior only.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T029-A1:** Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.
- **T029-A2:** Transport: stdio carries protocol frames only, reconnect preserves original operation identity, and private exporter cannot be discovered as a tool.
- **T029-A3:** Projection: exact schema rejects undeclared private fields/challenges in model output; adapter has no DB privileges or secret-bearing resource bundle.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| Local contract contribution | Owned source clauses and selected section fixtures | Verify generated/owner behavior locally; full assigned case proof remains with the downstream owner identified in the root register |

Partial implementation contributions: [ATDD-090](../../../05-atdd.md#case-atdd-090-proposal-review), [ATDD-091](../../../05-atdd.md#case-atdd-091-exact-content-approval), [ATDD-092](../../../05-atdd.md#case-atdd-092-editreject), [ATDD-093](../../../05-atdd.md#case-atdd-093-forgedunauthorized-approval), [ATDD-094](../../../05-atdd.md#case-atdd-094-invalidunsupported-information), [ATDD-095](../../../05-atdd.md#case-atdd-095-duplicateuncertain-creation), [ATDD-096](../../../05-atdd.md#case-atdd-096-both-chat-clients), [ATDD-100](../../../05-atdd.md#case-atdd-100-hostile-sourceinstruction), [ATDD-130](../../../05-atdd.md#case-atdd-130-mcp-correction-changes-reviewed-content), [ATDD-186](../../../05-atdd.md#case-atdd-186-require-independent-real-client-proof-of-human-review), [ATDD-187](../../../05-atdd.md#case-atdd-187-bind-review-authority-to-one-human-and-client), [ATDD-188](../../../05-atdd.md#case-atdd-188-freeze-and-redraw-every-reviewed-revision), [ATDD-191](../../../05-atdd.md#case-atdd-191-treat-sources-and-model-output-as-untrusted-facts), [ATDD-192](../../../05-atdd.md#case-atdd-192-reconnect-mcp-without-repeating-a-publication), [ATDD-221](../../../05-atdd.md#case-atdd-221-prove-exact-human-approval-in-claude-desktop), [ATDD-222](../../../05-atdd.md#case-atdd-222-prove-exact-human-approval-in-chatgpt), [ATDD-232](../../../05-atdd.md#case-atdd-232-enforce-scoped-mcp-credential-storage), [ATDD-266](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback), [ATDD-313](../../../05-atdd.md#case-atdd-313-evaluate-ordinary-and-held-out-proposal-outcomes-in-claude-desktop), [ATDD-314](../../../05-atdd.md#case-atdd-314-evaluate-ordinary-and-held-out-proposal-outcomes-in-chatgpt). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

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
- Hand the owned outputs to [T033 — Verify ChatGPT approval and complete both-client acceptance](T033-verify-chatgpt-approval-and-complete-both-client-acceptance.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
