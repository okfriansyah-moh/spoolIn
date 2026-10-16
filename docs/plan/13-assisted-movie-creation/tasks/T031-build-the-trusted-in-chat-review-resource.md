---
kind: task
id: T031
group: G14
depends_on:
- T004
- T029
- T030
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: required
---

# T031 — Build the trusted in-chat review resource

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

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
| ATDD-266 | implements |
| ATDD-306 | both |
| ATDD-313 | implements |
| ATDD-314 | implements |
| SC-09 | implements |
| FR-012 | implements |
| FR-016 | implements |
| FR-017 | implements |
| NFR-008 | both |
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
| DS-POLICY-16 | implements |
| DS-POLICY-20 | implements |
| DS-POLICY-21 | implements |
| ATDD-G-03 | implements |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T029 — Implement isolated MCP transport and connector identity](T029-implement-isolated-mcp-transport-and-connector-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.
- [T030 — Commit immutable proposals and exact approval once](T030-commit-immutable-proposals-and-exact-approval-once.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | mcp/src/review/index.html | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/review/main.ts | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/review/review-state.ts | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/review/review.css | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/test/review/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/mcp/reviewresource/ | T031 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [2.4.5 MCP adapter and review UI](../../../04-spoolin-techdoc.md#245-mcp-adapter-and-review-ui): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.1.1 UI reference and implementation map](../../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.10 MCP output schema registry](../../../04-spoolin-techdoc.md#510-mcp-output-schema-registry): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [9.2 Proposal, approval and uncertain publication](../../../04-spoolin-techdoc.md#92-proposal-approval-and-uncertain-publication): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
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
| [DS-POLICY-16 · L94](../../../03-design-system.md#visible-interaction-contracts) | Login uses `autocomplete="username"` and `current-password`; registration uses name/email/new-password. Enter submits and pending feedback prevents duplicate UI submission. Generic credential failure reveals no account details. Registration creates only Viewer, then directs to sign-in. Existing-password login has no registration-length rule. Passwords are never copied into recovery data, diagnostics, or audit. Cookies/tokens, CSRF, session expiry/storage, provisioning, verified MCP identity and security enforcement are technical-design responsibilities. |
| [DS-POLICY-20 · L102](../../../03-design-system.md#visible-interaction-contracts) | History is read-only and shows actor/action/record/time/outcome/changes without account emails, credentials, raw files or unrelated profile data. Success, failure, approval and creation remain distinct. Restricted security/operations inspection is not a new app role or screen. |
| [DS-POLICY-21 · L104](../../../03-design-system.md#visible-interaction-contracts) | ChatGPT and Claude examples are explicitly external-client storyboards. All exact publishable values, sources and unknowns appear before human approval. Correction requires fresh approval of the revised exact content; rejection publishes nothing. Permission and validation still apply after approval. No assistant claim substitutes for authorized human approval. Unknown creation is reconciled without blind duplication. Client compatibility/enforcement requires real integration testing; neither a storyboard nor model text proves it. |
| [ATDD-G-03 · L14187](../../../05-atdd.md#required-execution-and-release-prerequisites) | G-03 each actual chat client \| Independent ClaudeDesktop and ChatGPT registration/versions, UI/model isolation and trustworthy UI-origin verification; publication remains disabled until supported proof |

## Implementation

1. Build one framework-neutral TypeScript MCP Apps resource using adopted tokens and exact proposal view models. Emit self-contained mcp/dist/review/index.html with approved script/style/CSP and no separate asset server.
2. Render every publishable field, sources and explicit unknowns, hash/revision/current review context and human correction/reject/approve actions. Escape all model/source data and validate citation navigation without arbitrary fetches.
3. Keep UI-only challenge and trusted invocation material out of model text, logs, generated bundles and persistent browser state. Never set UI-origin assertion solely from a model-supplied parameter.
4. Handle revision updates, pending/invalid/denied/expired/rejected/uncertain/created states with complete redraw and fresh approval after changes. Uncertain outcome queries original operation.
5. Run component/resource-package interaction and visual comparisons for all AI storyboards; actual host permissions and consent proof remain exclusively T032/T033.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T031-A1:** Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action.
- **T031-A2:** Isolation: synthetic markup/instructions render as data, challenge is absent from model output and resource bundle, and component tests cannot manufacture trusted host proof.
- **T031-A3:** Resource: packaged self-contained artifact loads under selected CSP with accessible actions in both themes and supported host size contexts.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-306](../../../05-atdd.md#case-atdd-306-render-every-reviewed-field-without-exposing-challenge-to-the-model) / Component | ControlledMCPApps component: revision 1/2, all publishablefields/nullunknowns/sourceevidence; challengeC in UI-only input; changedhash; missing trusted host input. | Then the component shows all exact current values and invalidates stale review binding And model-visible output excludes challenge; unsupported host state disables publication Observation: Inspect actual component DOM and separate model/UI projections. Controlled host inputs prove neither installed-client isolation nor an authentic UI-origin attestation. |

Partial implementation contributions: [ATDD-090](../../../05-atdd.md#case-atdd-090-proposal-review), [ATDD-091](../../../05-atdd.md#case-atdd-091-exact-content-approval), [ATDD-092](../../../05-atdd.md#case-atdd-092-editreject), [ATDD-093](../../../05-atdd.md#case-atdd-093-forgedunauthorized-approval), [ATDD-094](../../../05-atdd.md#case-atdd-094-invalidunsupported-information), [ATDD-095](../../../05-atdd.md#case-atdd-095-duplicateuncertain-creation), [ATDD-096](../../../05-atdd.md#case-atdd-096-both-chat-clients), [ATDD-100](../../../05-atdd.md#case-atdd-100-hostile-sourceinstruction), [ATDD-130](../../../05-atdd.md#case-atdd-130-mcp-correction-changes-reviewed-content), [ATDD-186](../../../05-atdd.md#case-atdd-186-require-independent-real-client-proof-of-human-review), [ATDD-187](../../../05-atdd.md#case-atdd-187-bind-review-authority-to-one-human-and-client), [ATDD-188](../../../05-atdd.md#case-atdd-188-freeze-and-redraw-every-reviewed-revision), [ATDD-191](../../../05-atdd.md#case-atdd-191-treat-sources-and-model-output-as-untrusted-facts), [ATDD-192](../../../05-atdd.md#case-atdd-192-reconnect-mcp-without-repeating-a-publication), [ATDD-221](../../../05-atdd.md#case-atdd-221-prove-exact-human-approval-in-claude-desktop), [ATDD-222](../../../05-atdd.md#case-atdd-222-prove-exact-human-approval-in-chatgpt), [ATDD-266](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback), [ATDD-313](../../../05-atdd.md#case-atdd-313-evaluate-ordinary-and-held-out-proposal-outcomes-in-claude-desktop), [ATDD-314](../../../05-atdd.md#case-atdd-314-evaluate-ordinary-and-held-out-proposal-outcomes-in-chatgpt). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

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

Check changed keyboard/focus/labels/errors/announcements and relevant zoom, motion, touch and browser behavior. Preserve known regressions and actual reader observations required to prove speech; exhaustive compatibility sweeps remain release-owned. For AI states include actual Claude/ChatGPT host context at T032/T033; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

| Flow/state reference | Actor and fixture condition | Trigger and expected comparison |
|---|---|---|
| [ai / chatgpt](../../../design-system/mockup/ai.html#chatgpt) · [ATDD-266-T1-R01 · L11126](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Bound Maintainer; open actual ChatGPT `propose_movie` review resource; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show every exact normalized publishable field, source evidence and explicit unknown, plus exact approve/correct/reject controls. Proposal is not public; model cannot access approval challenge/app-only methods. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / claude](../../../design-system/mockup/ai.html#claude) · [ATDD-266-T1-R02 · L11127](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Bound Maintainer; open actual Claude review resource; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show the same complete exact review and explicit unknowns with approve/correct/reject. Independently prove this host's human origin enforcement; a storyboard or ChatGPT pass cannot enable publication. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-approved](../../../design-system/mockup/ai.html#ai-approved) · [ATDD-266-T1-R03 · L11128](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; send exact current human approval while result is pending; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show approval received/creation pending for revision 1, with original-result lookup. This is UI intent/in-flight feedback, not proof of a separately committed approval-before-create transaction or published movie. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-created](../../../design-system/mockup/ai.html#ai-created) · [ATDD-266-T1-R04 · L11129](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; recover confirmed successful original publication; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show one actual movie identity and approved revision values with separate attributable APPROVED/SUCCEEDED events. Creation confirmation requires receipt; no earlier intent label suffices. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-corrected](../../../design-system/mockup/ai.html#ai-corrected) · [ATDD-266-T1-R05 · L11130](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; request a changed synopsis through paired proposal ID/revision; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Redraw every exact revision-2 value, provenance/unknowns and fresh approve control; prior review/challenge cannot authorize changed content. Keep correction/rejection available. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-rejected](../../../design-system/mockup/ai.html#ai-rejected) · [ATDD-266-T1-R06 · L11131](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; receive confirmed rejection of unpublished current revision; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Proposal rejected and nothing published; repeated rejection stays stable. Silence/general request/model claims are not consent; rejecting an already-created proposal cannot delete its movie. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-denied](../../../design-system/mockup/ai.html#ai-denied) · [ATDD-266-T1-R07 · L11132](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Viewer/revoked or untrusted origin; attempt approval/publication; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Creation not authorized, no success or restricted data. Fabricated roles/model approval cannot enable create; unsupported host isolation disables publication. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-unknown](../../../design-system/mockup/ai.html#ai-unknown) · [ATDD-266-T1-R08 · L11133](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; review conflicting/unsupported required release facts; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Identify unsupported required information and disagreement; block publication without invented defaults. Correct the complete proposal and obtain fresh approval before submission. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-uncertain](../../../design-system/mockup/ai.html#ai-uncertain) · [ATDD-266-T1-R09 · L11134](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; lose publication response; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Creation result not confirmed and resolve original proposal/revision/publish UUID. Do not change content or create blindly; report one actual success or confirmed recoverable failure when known. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-hostile](../../../design-system/mockup/ai.html#ai-hostile) · [ATDD-266-T1-R10 · L11135](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; include source instructions to expose PII or bypass approval; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show permitted catalog facts and refusal/ignored-instruction feedback without exposing PII or obeying excluded actions. Only valid exact human review may progress; citations do not trigger arbitrary fetches. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-approved-v2](../../../design-system/mockup/ai.html#ai-approved-v2) · [ATDD-266-T1-R11 · L11136](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; approve exact corrected revision 2 with fresh trusted binding; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show revision-2 approval intent/creation pending and original-result lookup; recheck current policy/permission. No revision-1 consent reuse or premature committed success appears. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [ai / ai-created-v2](../../../design-system/mockup/ai.html#ai-created-v2) · [ATDD-266-T1-R12 · L11137](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | Maintainer; reconcile confirmed successful revision-2 publication; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show one created identity with corrected synopsis and separate approval/creation outcomes for revision 2. No stale revision-1 value or second movie appears. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

These captures inspect this task’s controlled policy/draft/review component with source fixtures. They do not require later domain editors or real host integration to exist: T016/T043/T044 and T032/T033 respectively retain the full production/host state proof. Record that boundary explicitly with each capture.

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
- Hand the owned outputs to [T033 — Verify ChatGPT approval and complete both-client acceptance](T033-verify-chatgpt-approval-and-complete-both-client-acceptance.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
