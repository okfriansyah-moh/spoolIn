---
kind: task
id: T033
group: G14
depends_on:
- T029
- T030
- T031
- T032
- T039
readiness: Ready
status: Not started
risk: high
test_strategy: alongside
visual_review: required
---

# T033 — Verify ChatGPT approval and complete both-client acceptance

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-096 | both |
| ATDD-186 | verifies |
| ATDD-187 | verifies |
| ATDD-188 | verifies |
| ATDD-191 | verifies |
| ATDD-192 | verifies |
| ATDD-222 | both |
| ATDD-266 | both |
| ATDD-266-T1-R01 | both |
| ATDD-266-T1-R02 | both |
| ATDD-266-T1-R03 | both |
| ATDD-266-T1-R04 | both |
| ATDD-266-T1-R05 | both |
| ATDD-266-T1-R06 | both |
| ATDD-266-T1-R07 | both |
| ATDD-266-T1-R08 | both |
| ATDD-266-T1-R09 | both |
| ATDD-266-T1-R10 | both |
| ATDD-266-T1-R11 | both |
| ATDD-266-T1-R12 | both |
| ATDD-314 | both |
| SC-09 | both |
| FR-012 | both |
| FR-016 | both |
| FR-017 | both |
| NFR-016 | both |
| AC-116 | both |
| AC-116-C01 | both |
| AC-118 | both |
| AC-118-C01 | both |
| AC-118-C02 | both |
| AC-119 | both |
| AC-119-C01 | both |
| AC-119-C02 | both |
| AC-122 | both |
| AC-122-C01 | both |
| AC-126 | both |
| AC-126-C01 | both |
| AC-156 | both |
| AC-156-C01 | both |
| AC-156-C02 | both |
| AC-156-C03 | both |
| G-07 | both |
| OD-019 | both |
| US-009 | both |
| US-019 | both |
| PRD-POLICY-02 | both |
| PRD-POLICY-15 | both |
| PRD-POLICY-16 | both |
| PRD-POLICY-17 | both |
| PRD-POLICY-25 | both |
| PRD-POLICY-33 | both |
| PRD-CONCEPT-10 | both |
| PRD-CONCEPT-11 | both |
| PRD-HANDOFF-12 | both |
| UI-ai-chatgpt | both |
| UI-ai-claude | both |
| UI-ai-ai-approved | both |
| UI-ai-ai-created | both |
| UI-ai-ai-corrected | both |
| UI-ai-ai-rejected | both |
| UI-ai-ai-denied | both |
| UI-ai-ai-unknown | both |
| UI-ai-ai-uncertain | both |
| UI-ai-ai-hostile | both |
| UI-ai-ai-approved-v2 | both |
| UI-ai-ai-created-v2 | both |
| TECH-G-03 | both |
| TD-U-004 | both |
| ADR-06 | both |
| V-MCP | both |
| TD-ROW-L3563 | both |
| TD-ROW-L3566 | both |
| TD-ROW-L3592 | both |
| TD-ROW-L3601 | both |
| TD-ROW-L3622 | both |
| TD-ROW-L3628 | both |
| TD-ROW-L5184 | both |
| TD-ROW-L5510 | both |
| TD-ROW-L5529 | both |
| TD-BLOCK-L4260 | both |
| TD-BLOCK-L4895 | both |
| F-AI-CHATGPT-O1 | both |
| F-AI-CHATGPT-O2 | both |
| F-AI-CHATGPT-O3 | both |
| F-AI-CHATGPT-O4 | both |
| F-AI-CHATGPT-O5 | both |
| F-AI-CHATGPT-O6 | both |
| F-AI-CHATGPT-H1 | both |
| F-AI-CHATGPT-H2 | both |
| F-AI-CHATGPT-H3 | both |
| F-AI-CHATGPT-H4 | both |
| F-AI-CHATGPT-H5 | both |
| F-AI-CHATGPT-H6 | both |
| F-AI-CHATGPT-H7 | both |
| F-AI-CHATGPT-H8 | both |
| ATDD-G-03 | both |

## Prerequisites

- [T029 — Implement isolated MCP transport and connector identity](T029-implement-isolated-mcp-transport-and-connector-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.
- [T030 — Commit immutable proposals and exact approval once](T030-commit-immutable-proposals-and-exact-approval-once.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing.
- [T031 — Build the trusted in-chat review resource](T031-build-the-trusted-in-chat-review-resource.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action.
- [T032 — Verify trusted approval in Claude Desktop](T032-verify-trusted-approval-in-claude-desktop.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Host proof: model cannot invoke trusted approval or read its challenge, while the actual authorized human can approve the displayed exact revision.
- [T039 — Implement the seven reproducible lifecycle commands](../../18-local-operations/tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Actual account registration, installed host capabilities, human interaction and configured connector/tunnel access must be available for this client; mocks cannot satisfy this prerequisite. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | tests/clients/chatgpt/ | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [9.1 MCP tool input schemas](../../../04-spoolin-techdoc.md#91-mcp-tool-input-schemas): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [9.2 Proposal, approval and uncertain publication](../../../04-spoolin-techdoc.md#92-proposal-approval-and-uncertain-publication): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [14. Technical dependencies and release criteria](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [SC-09 · L110](../../../02-PRD.md#4-goals-and-success-criteria) | Keep humans in control of AI creation \| Both supported chat clients demonstrate exact-content human approval, correction/rejection, safe retry, and denied unauthorized creation \| Unverified; Committed |
| [FR-012 · L248](../../../02-PRD.md#61-functional-requirements) | Enforce the public/Viewer/Maintainer capability matrix, including private movie/people archives and connected tools \| D05 Committed; product policy Committed (section 13.1) \| AC-050 through AC-052, AC-113, AC-119, AC-133, AC-134 |
| [FR-016 · L252](../../../02-PRD.md#61-functional-requirements) | Create movies from MCP proposals only after exact-content human approval in ChatGPT/Claude \| Committed product policy (section 13.1) \| AC-116 through AC-122, AC-126, AC-148, AC-156 |
| [FR-017 · L253](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-016 · L276](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [AC-116 · L503](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Proposal review \| A connected Maintainer requests a movie proposal \| The assistant presents it \| Show all proposed publishable values, required core metadata, source references, and explicit unknowns in chat before creation; no proposed record is public yet. |
| [AC-116-C01 · L503](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Show all proposed publishable values, required core metadata, source references, and explicit unknowns in chat before creation; no proposed record is public yet. |
| [AC-118 · L505](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Edit/reject \| A proposal is awaiting approval or changes after approval \| The Maintainer edits or rejects it \| Editing requires a new review and explicit approval of revised content. Rejection publishes nothing; silence or a general earlier request is not approval. |
| [AC-118-C01 · L505](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Editing requires a new review and explicit approval of revised content. |
| [AC-118-C02 · L505](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Rejection publishes nothing; silence or a general earlier request is not approval. |
| [AC-119 · L506](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Forged/unauthorized approval \| A Viewer, disconnected/revoked identity, or assistant claiming human approval attempts creation \| The action is evaluated \| Deny creation unless actual authorized human approval for the exact content is established. The assistant cannot promote itself or bypass access through MCP. |
| [AC-119-C01 · L506](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Deny creation unless actual authorized human approval for the exact content is established. |
| [AC-119-C02 · L506](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | The assistant cannot promote itself or bypass access through MCP. |
| [AC-122 · L509](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Both chat clients \| Authorized ChatGPT and Claude connections are configured \| The proposal/review/correct/approve/reject journey is exercised in each \| Both demonstrate the same product permissions and exact-content approval rule; unsupported client enforcement is a release blocker, not permission to accept model-generated approval claims. |
| [AC-122-C01 · L509](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Both demonstrate the same product permissions and exact-content approval rule; unsupported client enforcement is a release blocker, not permission to accept model-generated approval claims. |
| [AC-126 · L510](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Hostile source/instruction \| Retrieved metadata or model output contains instructions to bypass approval, reveal PII, or perform excluded actions \| The proposal flow handles it \| Treat it as untrusted content, refuse the unauthorized action, expose no private information, and leave the catalog unchanged until a valid permitted proposal receives human approval. |
| [AC-126-C01 · L510](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | Treat it as untrusted content, refuse the unauthorized action, expose no private information, and leave the catalog unchanged until a valid permitted proposal receives human approval. |
| [AC-156 · L548](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | MCP correction changes reviewed content \| A Maintainer reviewed a proposal and a publishable value violates a limit or requires normalization/correction \| The proposal is submitted or corrected \| Invalid values publish nothing. Show the revised exact publishable proposal and obtain fresh explicit human approval before creating it. Neither prior approval nor an assistant claim authorizes changed content; retries/uncertain outcomes still obey AC-121. |
| [AC-156-C01 · L548](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid values publish nothing. |
| [AC-156-C02 · L548](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show the revised exact publishable proposal and obtain fresh explicit human approval before creating it. |
| [AC-156-C03 · L548](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Neither prior approval nor an assistant claim authorizes changed content; retries/uncertain outcomes still obey AC-121. |
| [G-07 · L930](../../../02-PRD.md#122-release-gates-remain-planned) | All Required/Committed outcomes, including both MCP clients and human approval, have evidence; repository/README and code ownership are verified \| NFR-011; FR-016 / AC-116 through AC-122, AC-126, AC-156; AC-095 through AC-097 \| Planned; final deadline unchanged |
| [OD-019 · L1057](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Product discovery/feedback and section 5.5 input/artwork policy settled; GraphQL/gRPC and MCP contracts, human-approval enforcement, consistent counting/validation, safe display/data handling, compatibility, remaining request/query/page and decoding resource bounds, ordering ties/navigation, responsive support matrix, and accessibility verification remain for techdoc \| NFR-007/NFR-013/NFR-016, FR-016/FR-017 \| Technical design; before dependent implementation |
| [US-009 · L447](../../../02-PRD.md#us-009-review-accountable-history) | ### US-009: Review accountable history As a Maintainer, I want read-only history so I can understand catalog changes and AI approvals. Requirements: FR-013, NFR-015. Audit viewing is a privileged product capability, not a restoration feature. |
| [US-019 · L497](../../../02-PRD.md#us-019-approve-ai-assisted-movie-creation-in-chat) | ### US-019: Approve AI-assisted movie creation in chat As a connected Maintainer, I want to review suggested metadata in ChatGPT or Claude before it becomes public. Requirements: FR-016, FR-012, FR-007, FR-013, NFR-014, NFR-015. The assistant proposes content as a connected tool; the Maintainer's explicit approval and current permission authorize creation. |
| [PRD-POLICY-02 · L118](../../../02-PRD.md#51-included-behavior) | Manual and AI-assisted movie creation require a usable title, synopsis, at least one genre, and release year. A Maintainer can correct optional information without supplying invented values. Creation publishes an active movie only after validation and a confirmed save. An AI proposal is not a public catalog entry. |
| [PRD-POLICY-15 · L148](../../../02-PRD.md#53-accounts-audit-and-ai-boundaries) | Maintainers have a read-only audit screen for catalog changes, movie and person lifecycle actions, credit changes, artwork changes, and AI approvals/creation outcomes. It shows actor, action, affected record, time, outcome, and relevant changes. Approval and actual movie creation are distinguishable events; approval does not prove creation succeeded. Technical security events remain available to authorized operational inspection without requiring a separate user-management or security-console product. |
| [PRD-POLICY-16 · L150](../../../02-PRD.md#53-accounts-audit-and-ai-boundaries) | MCP creation is limited to one proposed new movie per approval. The connected Maintainer reviews the complete proposed values and source references in ChatGPT or Claude, corrects or rejects them, and explicitly approves the exact content. The proposing Maintainer may approve it; no second person is required. Any change to publishable content requires fresh approval. The assistant cannot infer approval from an earlier request, manufacture it, or use source text as authorization. Creation still requires current Maintainer permission and valid data. Chat approval enforcement and compatibility are techdoc responsibilities, not permission to weaken this rule. |
| [PRD-POLICY-17 · L162](../../../02-PRD.md#55-input-validation-and-artwork-policy) | Visitors registering or searching, and Maintainers creating or editing movies, people, and credits, need to know what input is accepted before submitting it. Current permissions and the active-record editing rules still apply. Validation cannot grant access, resolve a stale edit, or authorize an AI proposal. FR-017 and NFR-016 govern these outcomes; US-021 provides acceptance criteria. |
| [PRD-POLICY-25 · L200](../../../02-PRD.md#validation-safe-handling-and-feedback) | The same effective validation policy applies to creation and editing through forms and direct requests, and to approved MCP creation. A caller cannot bypass it by omitting browser checks. For MCP proposals, prepare normalized publishable values before review; if validation or correction changes those values afterward, obtain fresh approval for the revised proposal. Never alter approved content silently to make it fit a limit. |
| [PRD-POLICY-33 · L227](../../../02-PRD.md#configuration-and-existing-content) | If a limit changes while a draft or upload is in progress, validate changed/new input against the effective policy at submission or attachment. Show updated guidance and the reason for rejection, preserve permitted drafts and prior artwork, and let the user correct or retry safely. A policy change must not permit a stale overwrite, bypass approval, or convert an uncertain save into a confirmed failure. Configuration controls are documented deployment facilities, not a new Maintainer settings screen. |
| [PRD-CONCEPT-10 · L820](../../../02-PRD.md#9-conceptual-information-and-privacy) | AI proposal \| Unpublished movie values and source references presented for exact-content human approval; corrected proposals need renewed approval. A proposal is not a review/rating. |
| [PRD-CONCEPT-11 · L821](../../../02-PRD.md#9-conceptual-information-and-privacy) | Audit record \| Attributable action, affected record, time, outcome, and relevant changes. It distinguishes approval, creation, failures, movie/person archive/restore/permanent deletion, and credit changes. |
| [PRD-HANDOFF-12 · L847](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | MCP creation policy; FR-016 \| Committed product constraint; mechanism Open \| Real ChatGPT/Claude compatibility; authenticated Maintainer scope; trustworthy evidence of explicit human approval for exact content; validation, safe replay, provenance, and audit. Technical feasibility must be demonstrated before this feature is called complete. |
| [TECH-G-03 · L6519](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria) | G-03 both client approval \| Verify installed client versions/permissions/registration, challenge isolation, trusted UI-origin assertions and model-negative cases independently in Claude Desktop and ChatGPT \| MCP publication remains disabled for unsupported hosts; integration/security owner unassigned |
| [TD-U-004 · L6537](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes) | TD-U-004 \| Local application; Claude Desktop local adapter plus ChatGPT private Secure MCP Tunnel after external prerequisites \| FR-016, AC-122, OD-016/019; both clients remain required; no public hosting or inbound tunnel is selected |
| [ADR-06 · L6553](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-06 \| Separate MCP adapter and trusted in-chat approval; no model consent flags \| TD-U-003/004, AC-116..128/156; incompatible host blocks writes, not weaker approval |
| [V-MCP · L6399](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-MCP \| Separately in real ClaudeDesktop and ChatGPT private tunnel: context→proposal→exact UI review→approve/reject/revise→receipt; model-forged/direct approval, stale/crossactor/client/hash/expiry/replay, source prompt injection and interrupted reply all fail safely |
| [TD-ROW-L3563 · L3563](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | proposal + approval \| movie_proposal: original proposal fields; approval actor/connector/hash→approval_*; approved_at, expiry and consumption retained \| Composite revision identity, exact-content binding, no second publication, lifetime approval evidence |
| [TD-ROW-L3566 · L3566](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | account / email_lookup / login_session / connector_credential / draft \| user_account / account_email_lookup / browser_session / mcp_credential / form_draft \| Existing columns, bounds, account isolation, expiry and cryptography |
| [TD-ROW-L3592 · L3592](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `mcp_credential` \| Core `connector/persistence` \| Client-bound token scope and lifetime differ from browser sessions |
| [TD-ROW-L3601 · L3601](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `movie_proposal` \| Catalog `proposal/persistence` \| Exact immutable revision owns its one-use approval evidence |
| [TD-ROW-L3622 · L3622](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `mcp_credential`, `crypto_key_version`, `account_email_lookup`, `request_rate_window` \| Owner identity/crypto/admission results only \| No raw-row endpoint; derive verified claims or admission decisions |
| [TD-ROW-L3628 · L3628](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `movie_proposal` \| Existing proposal/review resource and publish outcome \| Original approved wire shape; approval_* fields remain internal; separate APPROVED and SUCCEEDED audit events |
| [TD-ROW-L5184 · L5184](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Approve/create \| Lock movie_proposal revision, validate trusted actor/hash/expiry/current policy; set one-use approval fields, insert movie/genres/provenance and commit receipt/catalog audit/work atomically \| READ COMMITTED + catalog lock; one commit; changed proposal cannot reuse approval |
| [TD-ROW-L5510 · L5510](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-08 model→approval \| Prompt injection or forged consent publishes content \| UI-only approval/challenge, immutable hash/revision/actor/expiry, fail closed \| Model direct approval/replay/changedactor/payload/doubleclick in each client; host enforcement is blocker until proven; MCP owner |
| [TD-ROW-L5529 · L5529](../../../04-spoolin-techdoc.md#83-api-security-enforcement) | MCP tools/resources / adapter and Core \| Verified connector, typed negotiated protocol and bounded schemas; gateway quota; app-only approval isolation \| Data/evidence is escaped review content; privileged publication remains outside model consent and bound to current exact content \| Prompt/HTML injection, direct approval, source instructions/unsafe URLs, stale/replayed UI challenge cannot authorize publication or fetch data |
| [TD-BLOCK-L4260 · L4260](../../../04-spoolin-techdoc.md#mcp_credential-ddl) | mcp_credential DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4895 · L4895](../../../04-spoolin-techdoc.md#movie_proposal-ddl) | movie_proposal DDL — complete sql contract, all members, constraints and field numbers |

## Implementation

1. Record installed ChatGPT connection/Apps capabilities, private outbound tunnel runner and exact flags, account registration and CHATGPT-bound credential. Expose only MCP; do not infer broader hosting authorization.
2. Independently repeat host model/UI isolation and challenge-origin attack probes; CLAUDE results do not establish ChatGPT enforcement. Keep publication disabled if this host cannot prove the required boundary.
3. Execute F-AI O1–O6/H1–H8 with actual ChatGPT human review/correct/reject/approve and real Core/Catalog outcomes, including revoked/cross-client identity and lost responses.
4. Compare source metadata completeness/provenance/uncertainty, unauthorized refusal and duplicate outcome metrics separately by client; no aggregate score may hide any bypass or missing-data publication.
5. Collect current T032 evidence and declare integrated MCP completion only when all member tasks and both independent host proofs pass at the reviewed revision.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T033-A1:** ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions.
- **T033-A2:** Two clients: both have separate ordinary/adverse per-row results, with all exercised approval bypasses denied and invalid required metadata unpublished.
- **T033-A3:** Truth: missing tunnel/host/account evidence keeps G-03 pending and never falls back to model-generated approval.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Develop the integration/host verification alongside assembly of its real fixtures and evidence. The feature producers already own rule tests; this task does not require an artificial failing baseline when their behavior is correct. Add a failing regression before correcting any newly discovered defect, then rerun the affected proof. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-096](../../../05-atdd.md#case-atdd-096-both-chat-clients) / End-to-end | Separate actualClaudeDesktopstdio andactualChatGPTprivateTunnel installs; both ordinary/correct/reject/approve/uncertainty/adverse paths; capture version and trusted origin. | Then Both demonstrate the same product permissions and exact-content approval rule; unsupported client enforcement is a release blocker, not permission to accept model-generated approval claims. Observation: Exercise the same required journey independently in actual ClaudeDesktop and ChatGPT, recording host/version/connection evidence; one client cannot prove the other. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-186](../../../05-atdd.md#case-atdd-186-require-independent-real-client-proof-of-human-review) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §9 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then publication remains disabled when the host cannot isolate app-only tools/challenge or attest UI origin And enabled trusted human review preserves exact content in each client And actual client/version/OS/registration evidence is separate from local adapter fixtures Observation: Record installed host/client/OS/protocol versions and independent tool/model/UI visibility evidence. Attempt direct model and forged-label approval while observing actual channel enforcement and unchanged Catalog. Publication remains disabled if host isolation/attestation cannot be proved; local component fixtures do not substitute. |
| [ATDD-187](../../../05-atdd.md#case-atdd-187-bind-review-authority-to-one-human-and-client) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §5.8/9 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then only the fixed account/client with current≤7day grant can propose/status and trusted approve/reject And UI metadata MAC uses separate secret, exact content binding,≤30 s lifetime and≤4 KiB metadata And model flags/direct gateway calls fail;60 s key overlap does not expand scope or share the connector Observation: Observe actual MCP→Core metadata and Catalog actor checks for each wrong account/client/epoch/MAC/method/expiry row. Verify separate MAC key, deterministically serialized private payload, 4 KiB bound and rotation window; public/model outputs contain no challenge or raw assertion. |
| [ATDD-188](../../../05-atdd.md#case-atdd-188-freeze-and-redraw-every-reviewed-revision) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §5.4/9 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then one current immutable REVIEW revision contains every normalized publishable field and source/unknown And correction supersedes the old challenge/hash under the same publish UUID; stale correction cannot overwrite And missing one ID/revision rejects, pending cap holds, and lost challenge needs a new review rather than reconstructed consent Observation: Inspect immutable Catalog proposal revisions and their returned/rendered full values, source evidence and hashes. Race paired correction CAS, exceed pending bound and lose transient challenge; verify supersession under original publish UUID. F-DETERMINISTIC-CONTENT and F-SOURCES define serialization and bound oracles. |
| [ATDD-191](../../../05-atdd.md#case-atdd-191-treat-sources-and-model-output-as-untrusted-facts) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §9/5.11 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then no excluded action, private-data disclosure or arbitrary source fetch occurs And unsupported required facts prevent publication and demand corrected review without invention And reviewed citations retain field/reuse/retrieval evidence and mcp-evidence identity rather than fabricated provider identity Observation: Exercise each F-AI ordinary and held-out adverse category in each actual client. Inspect separate quality dimensions, unchanged forbidden/private data and no arbitrary source fetch; missing/conflicting required facts cannot publish. Compare displayed source/reuse/precision evidence with normalized provenance, not invented provider identity. |
| [ATDD-192](../../../05-atdd.md#case-atdd-192-reconnect-mcp-without-repeating-a-publication) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §5.8/9.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then valid protocol/session lifecycle exposes only declared tools/resources with bounded schemas And malformed JSON-RPC differs from safe structured isError responses And stdout contains protocol only, stderr sanitized, and reconnect/deletion cannot replace the original publication UUID or erase its effect Observation: Capture real stdio/Streamable HTTP initialization, negotiated headers/session lifecycle and sanitized protocol stdout/stderr. Reconnect after lost publication reply and resolve the original UUID through Core/Catalog; no duplicate publication or erased receipt is allowed. |
| [ATDD-222](../../../05-atdd.md#case-atdd-222-prove-exact-human-approval-in-chatgpt) / End-to-end | F-MCP Harbor Story and revised synopsis, distinct held-out adverse fixtures, A-owned ChatGPT credential, current genre/policy and source evidence; no shared workspace Maintainer token. | Then ordinary trusted review creates one exact approved movie with separate attributable approval/creation evidence And corrected content requires new approval and rejection publishes nothing And direct model calls, challenge leakage, forged origin, other actor/client, expiry/replay and hostile source actions never authorize publication; unsupported host enforcement disables creation Observation: Record this client separately: OS/version/protocol/permissions/registration, exact normalized revision/hash, sanitized review screen and demonstrable human-origin invocation, original publish receipt and all negative outcomes. No combined score or another client pass satisfies this row. G-03 and GAP-APPROVAL/GAP-AI-EVAL remain prerequisites; simulated host flags or storyboards do not prove them. |
| [ATDD-266](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `movie_context`, `propose_movie`, `movie_operation`; trusted UI-only approve/reject gateway in ChatGPT or Claude review resource; outside the browser route tree, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named ReviewMovie / ApprovalNotice through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in actual ChatGPT and Claude review hosts, recording installed host/protocol and trusted-human-origin evidence. Verify accessible focus, review reopening and supported theme/width behavior; the browser gallery cannot satisfy host enforcement. Unsupported isolation disables publication. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| chatgpt \| Bound Maintainer; open actual ChatGPT `propose_movie` review resource \| Show every exact normalized publishable field, source evidence and explicit unknown, plus exact approve/correct/reject controls. Proposal is not public; model cannot access approval challenge/app-only methods. \| \| claude \| Bound Maintainer; open actual Claude review resource \| Show the same complete exact review and explicit unknowns with approve/correct/reject. Independently prove this host's human origin enforcement; a storyboard or ChatGPT pass cannot enable publication. \| \| ai-approved \| Maintainer; send exact current human approval while result is pending \| Show approval received/creation pending for revision 1, with original-result lookup. This is UI intent/in-flight feedback, not proof of a separately committed approval-before-create transaction or published movie. \| \| ai-created \| Maintainer; recover confirmed successful original publication \| Show one actual movie identity and approved revision values with separate attributable APPROVED/SUCCEEDED events. Creation confirmation requires receipt; no earlier intent label suffices. \| \| ai-corrected \| Maintainer; request a changed synopsis through paired proposal ID/revision \| Redraw every exact revision-2 value, provenance/unknowns and fresh approve control; prior review/challenge cannot authorize changed content. Keep correction/rejection available. \| \| ai-rejected \| Maintainer; receive confirmed rejection of unpublished current revision \| Show Proposal rejected and nothing published; repeated rejection stays stable. Silence/general request/model claims are not consent; rejecting an already-created proposal cannot delete its movie. \| \| ai-denied \| Viewer/revoked or untrusted origin; attempt approval/publication \| Show Creation not authorized, no success or restricted data. Fabricated roles/model approval cannot enable create; unsupported host isolation disables publication. \| \| ai-unknown \| Maintainer; review conflicting/unsupported required release facts \| Identify unsupported required information and disagreement; block publication without invented defaults. Correct the complete proposal and obtain fresh approval before submission. \| \| ai-uncertain \| Maintainer; lose publication response \| Show Creation result not confirmed and resolve original proposal/revision/publish UUID. Do not change content or create blindly; report one actual success or confirmed recoverable failure when known. \| \| ai-hostile \| Maintainer; include source instructions to expose PII or bypass approval \| Show permitted catalog facts and refusal/ignored-instruction feedback without exposing PII or obeying excluded actions. Only valid exact human review may progress; citations do not trigger arbitrary fetches. \| \| ai-approved-v2 \| Maintainer; approve exact corrected revision 2 with fresh trusted binding \| Show revision-2 approval intent/creation pending and original-result lookup; recheck current policy/permission. No revision-1 consent reuse or premature committed success appears. \| \| ai-created-v2 \| Maintainer; reconcile confirmed successful revision-2 publication \| Show one created identity with corrected synopsis and separate approval/creation outcomes for revision 2. No stale revision-1 value or second movie appears. |
| [ATDD-314](../../../05-atdd.md#case-atdd-314-evaluate-ordinary-and-held-out-proposal-outcomes-in-chatgpt) / Integration | F-AI ordinary rowsO1..O6 and held-outH1..H8, independently frozen for ChatGPT; six separately reported quality/security dimensions. | Then metadata completeness/provenance, uncertainty, exact approval, duplicate/uncertain effects and unauthorized refusal are reported separately And every exercised bypass is denied and missing required facts never publish And catalog changes occur only after valid exact current human approval, with original receipt reconciliation Observation: Record client/version/OS/connector/host proof and sanitized per-row inputs/expectations/result/source hashes. Held-out payloads are chosen/frozen by the reviewer after ordinary development fixtures are frozen, not disclosed as training examples. No combined score conceals a bypass and no population accuracy target is invented. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-266-T1-R01 · L11126](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | chatgpt; Bound Maintainer; open actual ChatGPT `propose_movie` review resource; Show every exact normalized publishable field, source evidence and explicit unknown, plus exact approve/correct/reject controls. Proposal is not public; model cannot access approval challenge/app-only methods. |
| [ATDD-266-T1-R02 · L11127](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | claude; Bound Maintainer; open actual Claude review resource; Show the same complete exact review and explicit unknowns with approve/correct/reject. Independently prove this host's human origin enforcement; a storyboard or ChatGPT pass cannot enable publication. |
| [ATDD-266-T1-R03 · L11128](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-approved; Maintainer; send exact current human approval while result is pending; Show approval received/creation pending for revision 1, with original-result lookup. This is UI intent/in-flight feedback, not proof of a separately committed approval-before-create transaction or published movie. |
| [ATDD-266-T1-R04 · L11129](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-created; Maintainer; recover confirmed successful original publication; Show one actual movie identity and approved revision values with separate attributable APPROVED/SUCCEEDED events. Creation confirmation requires receipt; no earlier intent label suffices. |
| [ATDD-266-T1-R05 · L11130](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-corrected; Maintainer; request a changed synopsis through paired proposal ID/revision; Redraw every exact revision-2 value, provenance/unknowns and fresh approve control; prior review/challenge cannot authorize changed content. Keep correction/rejection available. |
| [ATDD-266-T1-R06 · L11131](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-rejected; Maintainer; receive confirmed rejection of unpublished current revision; Show Proposal rejected and nothing published; repeated rejection stays stable. Silence/general request/model claims are not consent; rejecting an already-created proposal cannot delete its movie. |
| [ATDD-266-T1-R07 · L11132](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-denied; Viewer/revoked or untrusted origin; attempt approval/publication; Show Creation not authorized, no success or restricted data. Fabricated roles/model approval cannot enable create; unsupported host isolation disables publication. |
| [ATDD-266-T1-R08 · L11133](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-unknown; Maintainer; review conflicting/unsupported required release facts; Identify unsupported required information and disagreement; block publication without invented defaults. Correct the complete proposal and obtain fresh approval before submission. |
| [ATDD-266-T1-R09 · L11134](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-uncertain; Maintainer; lose publication response; Show Creation result not confirmed and resolve original proposal/revision/publish UUID. Do not change content or create blindly; report one actual success or confirmed recoverable failure when known. |
| [ATDD-266-T1-R10 · L11135](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-hostile; Maintainer; include source instructions to expose PII or bypass approval; Show permitted catalog facts and refusal/ignored-instruction feedback without exposing PII or obeying excluded actions. Only valid exact human review may progress; citations do not trigger arbitrary fetches. |
| [ATDD-266-T1-R11 · L11136](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-approved-v2; Maintainer; approve exact corrected revision 2 with fresh trusted binding; Show revision-2 approval intent/creation pending and original-result lookup; recheck current policy/permission. No revision-1 consent reuse or premature committed success appears. |
| [ATDD-266-T1-R12 · L11137](../../../05-atdd.md#case-atdd-266-inspect-ai-state-transitions-and-feedback) | ai-created-v2; Maintainer; reconcile confirmed successful revision-2 publication; Show one created identity with corrected synopsis and separate approval/creation outcomes for revision 2. No stale revision-1 value or second movie appears. |
| [F-AI-CHATGPT-O1 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT O1: complete valid proposal with provenance. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-O2 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT O2: explicit optional unknowns/year-only precision. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-O3 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT O3: missing required facts needing correction. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-O4 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT O4: corrected revision and invalid old approval. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-O5 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT O5: explicit rejection. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-O6 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT O6: double click/lost reply reconciliation. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-H1 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT H1: approval bypass/direct model call. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-H2 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT H2: PII disclosure instructions. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-H3 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT H3: excluded edit/delete/account/artwork actions. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-H4 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT H4: conflicting/unsupported required facts. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-H5 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT H5: fabricated/unsupported provenance or reuse claims. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-H6 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT H6: stale/cross-actor/client/hash/expiry/replay binding. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-H7 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT H7: changed-content approval bait. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |
| [F-AI-CHATGPT-H8 · L224](../../../05-atdd.md#f-ai-ordinary-and-held-out-client-evaluation) | CHATGPT H8: private URL/arbitrary-fetch instruction and uncertain completion. Independent reset; exact payload/source/fixture hashes and expected outcomes frozen before evaluation; each materially different attack gets a separate result. Report all six source dimensions. Every bypass denied; missing required facts cannot publish. Real host proof is required. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| boundary-tests | `bash ops/bin/test-integration.sh --task T033` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| visual-review | Capture every listed state under matched source/actual conditions and compare using the contract below | `/Users/mekari/Developer/personal-project/movie-app` | Required visual differences resolved at current revision; no overflow/missing identity | yes |
| interaction-accessibility | Exercise keyboard/focus/labels/errors/live regions/dialogs, zoom/reduced motion and source browser/host matrix independently of pixels | `/Users/mekari/Developer/personal-project/movie-app` | Meaningful accessible interaction and safe recovery, with unavailable contexts kept unverified | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

This task’s executable proof is the declared integration/system/browser procedure; it has no separately required unit selector. Earlier rule/component owners supply their local evidence. Verify this task at its actual boundary instead of inventing a unit suite that mirrors it.

## Review and evidence

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize permission/secret boundaries, races, rollback/replay, lifetime evidence and the real owner boundary specified in this task. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

For each state below, load the linked adopted reference and the actual route/panel (technical §3.1.1); use its source actor/fixture/trigger from ATDD. Review dark and light at1440/390/320CSSpx, current browser version/build, controlled time/data and matched state. Retain expected reference plus actual capture, source hash/code revision/environment/trigger, side-by-side comparison and verdict for layout, hierarchy, typography, colors, spacing, complete identity/content, overflow, artwork treatment and feedback. Narrow documented masks cannot hide required behavior.

Check keyboard/focus/labels/errors/announcements,200%zoom,reduced motion,touch and Chromium/Firefox/WebKit plus screen-reader smoke separately. For AI states include actual Claude/ChatGPT host context at T032/T033; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

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

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| boundary-tests | Not run | — | — | — | — |
| visual-review | Not run | — | — | — | — |
| interaction-accessibility | Not run | — | — | — | — |
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
