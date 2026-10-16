---
kind: task
id: T046
group: G20
depends_on:
- T032
- T033
- T035
- T036
- T037
- T038
- T039
- T040
- T041
- T042
- T043
- T044
- T045
readiness: Ready
status: Not started
risk: medium
test_strategy: checks-only
visual_review: not-applicable
---

# T046 — Prepare the reviewed repository and release handoff

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Reproduction: a fresh reader follows README to the reviewed usable100/100 installation, correct role access and truthful test/blocked results without private authoring context. This task owns the change surface and local evidence below. Risk is medium: incorrect composition, build assumptions or delivery claims can hide missing behavior. Verify the actual declared boundary. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-076 | both |
| ATDD-077 | both |
| ATDD-078 | both |
| ATDD-213 | both |
| ATDD-275 | both |
| SC-01 | both |
| SC-05 | both |
| SC-08 | both |
| NFR-008 | both |
| NFR-009 | both |
| NFR-010 | both |
| NFR-011 | both |
| NFR-012 | both |
| A12 | both |
| A15 | both |
| A16 | both |
| A17 | both |
| D01 | both |
| D04 | both |
| D06 | both |
| D13 | both |
| D17 | both |
| D19 | both |
| AC-086 | both |
| AC-088 | both |
| AC-089 | both |
| AC-090 | both |
| AC-099 | both |
| G-01 | both |
| G-06 | both |
| G-07 | both |
| J-07 | both |
| AC-093 | both |
| AC-093-C01 | both |
| AC-095 | both |
| AC-095-C01 | both |
| AC-096 | both |
| AC-096-C01 | both |
| AC-096-C02 | both |
| AC-097 | both |
| AC-097-C01 | both |
| AC-097-C02 | both |
| US-018 | both |
| US-013 | both |
| US-014 | both |
| US-017 | both |
| PRD-PERM-02 | both |
| PRD-PERM-08 | both |
| PRD-POLICY-12 | both |
| PRD-BOUND-06 | both |
| PRD-BOUND-10 | both |
| PRD-BOUND-12 | both |
| PRD-BOUND-13 | both |
| PRD-BOUND-14 | both |
| PRD-FEEDBACK-01 | both |
| PRD-FEEDBACK-03 | both |
| PRD-FEEDBACK-05 | both |
| PRD-FEEDBACK-07 | both |
| PRD-HANDOFF-03 | both |
| TD-C-063 | verifies |
| TD-C-064 | both |
| TECH-G-06 | both |
| ADR-10 | both |
| V-HANDOFF | both |
| TD-ROW-L3556 | both |
| TD-ROW-L3567 | both |
| TD-ROW-L3588 | both |
| TD-ROW-L5186 | both |
| TD-ROW-L5188 | both |
| TD-BLOCK-L4035 | both |
| ATDD-G-06 | both |
| BRAIN-A12 | both |
| BRAIN-A15 | both |
| BRAIN-A16 | both |
| BRAIN-A17 | both |
| BRAIN-D01 | both |
| BRAIN-D04 | both |
| BRAIN-D06 | both |
| BRAIN-D13 | both |
| BRAIN-D17 | both |
| BRAIN-D19 | both |

## Prerequisites

- [T032 — Verify trusted approval in Claude Desktop](../../13-assisted-movie-creation/tasks/T032-verify-trusted-approval-in-claude-desktop.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Host proof: model cannot invoke trusted approval or read its challenge, while the actual authorized human can approve the displayed exact revision.
- [T033 — Verify ChatGPT approval and complete both-client acceptance](../../13-assisted-movie-creation/tasks/T033-verify-chatgpt-approval-and-complete-both-client-acceptance.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions.
- [T035 — Initialize and replay seed and prepared accounts safely](../../15-reproducible-seed/tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles.
- [T036 — Record actionable alerts through one selected mode](../../16-operational-notifications/tasks/T036-record-actionable-alerts-through-one-selected-mode.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Default: exhausted eligible work yields one inspectable sanitized recording and zero sends; database mode needs no external provider credential.
- [T037 — Emit bounded private-safe JSON diagnostics](../../17-operational-evidence/tasks/T037-emit-bounded-private-safe-json-diagnostics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug.
- [T038 — Expose logical metrics, dashboards and alert episodes](../../17-operational-evidence/tasks/T038-expose-logical-metrics-dashboards-and-alert-episodes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard.
- [T039 — Implement the seven reproducible lifecycle commands](../../18-local-operations/tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps.
- [T040 — Rotate keys and aliases without losing access](../../19-protected-recovery/tasks/T040-rotate-keys-and-aliases-without-losing-access.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage.
- [T041 — Verify migrations, purge and coordinated restore](../../19-protected-recovery/tasks/T041-verify-migrations-purge-and-coordinated-restore.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change.
- [T042 — Reconcile every real API and RPC boundary row](T042-reconcile-every-real-api-and-rpc-boundary-row.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Matrix: all28 storage families/260 column rows plus36 GraphQL and45 RPC rows have their complete physical, owner, projection, retention, allowed/denied/presence/deadline/error/side-effect results. ResolveActor remains deny-only on selected network callers.
- [T043 — Verify policy, authorization and injection across complete flows](T043-verify-policy-authorization-and-injection-across-complete-flows.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Authorization: Visitor/Viewer/forged/cross-owner callers obtain no restricted data/effects through any real boundary.
- [T044 — Complete visual, interaction and accessibility evidence](T044-complete-visual-interaction-and-accessibility-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Coverage: all97 states have appropriate source/current evidence, with both themes and three widths; expected actual differences have explicit verdicts.
- [T045 — Measure capacity and prove operational recovery limits](T045-measure-capacity-and-prove-operational-recovery-limits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Benchmark: each run reports all offered/admitted/completed/throttled/error counts and independently computed p95 at the stated workload with active rate limits.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | README.md | T046 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | docs/release/ | T046 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [13.3 Verification locations and command selection](../../../04-spoolin-techdoc.md#133-verification-locations-and-command-selection): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [14. Technical dependencies and release criteria](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Appendix A.1 Baseline decisions and requirement changes](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [SC-01 · L100](../../../02-PRD.md#4-goals-and-success-criteria) | Meet the project baseline \| Every A01 through A17 obligation has a mapped requirement and planned evidence \| Software evidence unverified; Required |
| [SC-05 · L104](../../../02-PRD.md#4-goals-and-success-criteria) | Make review reproducible \| Documented setup, seed, execution, tests, and code explanation can be followed independently \| Unverified; Required/Committed |
| [SC-08 · L107](../../../02-PRD.md#4-goals-and-success-criteria) | Publish a reviewable release revision \| Publicly accessible reviewed repository revision and release evidence \| Release access unverified by this PRD |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-009 · L267](../../../02-PRD.md#62-quality-and-delivery-requirements) | Cohesive vertical slices and understandable, maintainable project code \| D04/D17 Committed \| AC-089, AC-090 |
| [NFR-010 · L268](../../../02-PRD.md#62-quality-and-delivery-requirements) | Reproducible local setup preserves normal data and isolates verification \| D13/D24 Committed \| AC-091 through AC-094 |
| [NFR-011 · L269](../../../02-PRD.md#62-quality-and-delivery-requirements) | Accessible repository, truthful README/AI disclosure, and code ownership \| A15 through A17 Required; D01 Committed \| AC-095 through AC-097 |
| [NFR-012 · L270](../../../02-PRD.md#62-quality-and-delivery-requirements) | Risk-informed security consideration with accurate evidence and limitations \| D06 Committed \| AC-098, AC-099 |
| [A12 · L291](../../../02-PRD.md#63-project-requirements-coverage) | Unit tests including complete relevant edge cases \| NFR-008 / AC-086, AC-088 |
| [A15 · L294](../../../02-PRD.md#63-project-requirements-coverage) | Public Git repository access \| NFR-011 / AC-095 |
| [A16 · L295](../../../02-PRD.md#63-project-requirements-coverage) | Setup/run/AI-use README \| NFR-010/NFR-011 / AC-091, AC-096 |
| [A17 · L296](../../../02-PRD.md#63-project-requirements-coverage) | Explain project code \| NFR-009/NFR-011 / AC-090, AC-097 |
| [D01 · L302](../../../02-PRD.md#64-project-decision-register) | Initial release first; future product work stays separate \| NFR-011 / AC-095, AC-096; section 5.4 |
| [D04 · L305](../../../02-PRD.md#64-project-decision-register) | Vertical slices \| NFR-009 / AC-089; techdoc |
| [D06 · L307](../../../02-PRD.md#64-project-decision-register) | ISO-informed consideration, no certification/conformity claim \| NFR-012 / AC-098, AC-099 |
| [D13 · L314](../../../02-PRD.md#64-project-decision-register) | Compose and Make wrappers \| NFR-010 / AC-091 through AC-094 |
| [D17 · L318](../../../02-PRD.md#64-project-decision-register) | SOLID/DRY/KISS/YAGNI, code quality, constructor injection \| NFR-009 / AC-089, AC-090; techdoc |
| [D19 · L320](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [AC-086 · L869](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Trace unit and edge cases to included requirements without inventing a coverage percentage \| NFR-008; technical design |
| [AC-088 · L871](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Report failures/blocked/unexecuted checks honestly; test commands fail correctly and isolate normal data \| NFR-008; technical design |
| [AC-089 · L872](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Review cohesive tested vertical slices, shared rules, constructor dependencies, and justified abstractions \| NFR-009; technical design |
| [AC-090 · L873](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify readability, ownership, and explanation of generated/handwritten code and design tradeoffs \| NFR-009; developer/release reviewer |
| [AC-099 · L875](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Distinguish intended controls from tested/deployed proof; do not imply ISO/regulatory certification, TLS enforcement, or successful restore without evidence \| NFR-012; technical design |
| [G-01 · L922](../../../02-PRD.md#122-release-gates-remain-planned) | Approved scope, business rules, flows, acceptance, and technical handoff are coherent \| Sections 3 through 10 and section 13 \| Complete for technical-design handoff; not software approval |
| [G-06 · L927](../../../02-PRD.md#122-release-gates-remain-planned) | Responsive/keyboard journeys, restart persistence, real integration, and independent setup work \| NFR-003/NFR-007 through NFR-010; US-015 through US-017 \| Planned |
| [G-07 · L928](../../../02-PRD.md#122-release-gates-remain-planned) | All Required/Committed outcomes, including both MCP clients and human approval, have evidence; repository/README and code ownership are verified \| NFR-011; FR-016 / AC-116 through AC-122, AC-126, AC-156; AC-095 through AC-097 \| Planned; release access requires verification |
| [J-07 · L947](../../../02-PRD.md#123-delivery-responsibilities-and-journeys) | Developer explains the solution \| Reviewed release available \| Explain project code, tradeoffs, failure behavior, checks, and AI contributions |
| [AC-093 · L1005](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | Verification commands \| Normal application data exist \| Unit/integration checks run, including a failing case \| Tests use isolated data, report failures honestly, and leave normal records/artwork intact. |
| [AC-093-C01 · L1005](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | Tests use isolated data, report failures honestly, and leave normal records/artwork intact. |
| [AC-095 · L1007](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | Repository release access \| The reviewed revision is ready \| Public release access is checked \| The release reviewer can publicly access the intended Git revision and required material; evidence identifies the reviewed revision. |
| [AC-095-C01 · L1007](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | The release reviewer can publicly access the intended Git revision and required material; evidence identifies the reviewed revision. |
| [AC-096 · L1008](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | README/AI disclosure \| Implementation and actual verification results exist \| README is compared with them \| Setup/run/seed/test/recovery instructions and AI use are accurate; limitations and unexecuted checks are explicit. MCP client use is documented without claiming unverified compatibility. |
| [AC-096-C01 · L1008](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | Setup/run/seed/test/recovery instructions and AI use are accurate; limitations and unexecuted checks are explicit. |
| [AC-096-C02 · L1008](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | MCP client use is documented without claiming unverified compatibility. |
| [AC-097 · L1009](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | Walkthrough \| Reviewed code and evidence are available \| The developer explains a feature and rehearses a small change \| They can explain generated/handwritten code, decisions, failure behavior, tests, and AI contribution. Release readiness requires verification evidence. |
| [AC-097-C01 · L1009](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | They can explain generated/handwritten code, decisions, failure behavior, tests, and AI contribution. |
| [AC-097-C02 · L1009](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | Release readiness requires verification evidence. |
| [US-018 · L485](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | ### US-018: Save edits without losing or duplicating work As a Maintainer, I want stale drafts preserved and repeated submissions resolved safely so I do not overwrite another change or create unintended duplicates. Requirement: NFR-014. Related security and interface obligations NFR-012/NFR-013, AC-098 through AC-103, and AC-106 remain in section 10. |
| [US-013 · L979](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | #### US-013: Bound recovery and protect setup As an operations owner, I want recovery to stop when it cannot safely succeed and setup to protect secrets. Requirements: NFR-004, NFR-005. Detailed AC-072/AC-074/AC-076/AC-077 checks remain engineering handoff items. |
| [US-014 · L989](../../../02-PRD.md#us-014-diagnose-user-visible-failure) | #### US-014: Diagnose user-visible failure As an operations owner, I want to connect a failed user action with a cause and recovery action. Requirement: NFR-006. AC-079 through AC-082 remain compact engineering handoff items for readiness, correlation, dashboards, and alerts; tool selection does not block product readiness. |
| [US-017 · L997](../../../02-PRD.md#us-017-reproduce-and-review-the-release) | #### US-017: Reproduce and review the release As a release reviewer, I want setup instructions and honest evidence so I can assess the intended revision. Requirements: NFR-010, NFR-011, FR-008. |
| [PRD-PERM-02 · L69](../../../02-PRD.md#31-permission-matrix) | Register or sign in \| Yes \| Already signed in; may sign out \| Already signed in; may sign out |
| [PRD-PERM-08 · L75](../../../02-PRD.md#31-permission-matrix) | Rate/review titles or manage TV-series entries \| No; deferred/excluded \| No; deferred/excluded \| No; deferred/excluded |
| [PRD-POLICY-12 · L138](../../../02-PRD.md#52-removal-and-editing-rules) | Stale saves are rejected, with the draft retained for review against current information. There is no force-overwrite option. If a target is removed, permissions expire, or an outcome is uncertain, explain what is known and retain permitted work for correction or review. Do not claim that a lost response means the operation failed or invite a blind duplicate submission. |
| [PRD-BOUND-06 · L182](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Each optional acting character name \| 1 character \| 200 characters \| Multiple character names remain supported; each supplied name is validated |
| [PRD-BOUND-10 · L186](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Source reference URL, where accepted \| Valid, nonempty HTTP/HTTPS URL \| 2,048 characters \| Does not require a new source-entry screen or authorize fetching arbitrary URLs |
| [PRD-BOUND-12 · L209](../../../02-PRD.md#artwork-policy) | Minimum file size \| 1 byte; reject empty files \| Recommended size default; rejecting empty files is Committed |
| [PRD-BOUND-13 · L210](../../../02-PRD.md#artwork-policy) | Maximum file size \| 5 MiB, exactly 5,242,880 bytes \| Recommended size default |
| [PRD-BOUND-14 · L211](../../../02-PRD.md#artwork-policy) | Other formats and animation \| Reject, including GIF, SVG, animated PNG, and animated WebP \| Committed release boundary |
| [PRD-FEEDBACK-01 · L795](../../../02-PRD.md#810-shared-feedback-rules) | Loading / processing \| Keep the current operation and stage visible; transfer progress reflects observed bytes, with separate processing feedback |
| [PRD-FEEDBACK-03 · L797](../../../02-PRD.md#810-shared-feedback-rules) | Success \| Confirm the completed business outcome; approved, transferred, and published are separate states |
| [PRD-FEEDBACK-05 · L799](../../../02-PRD.md#810-shared-feedback-rules) | Conflict \| Preserve the draft and let the Maintainer review current data before a new submission |
| [PRD-FEEDBACK-07 · L801](../../../02-PRD.md#810-shared-feedback-rules) | Cancel \| Leave unaffected data intact; cancellation does not imply that an already completed action was rolled back |
| [PRD-HANDOFF-03 · L836](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D04/D17 \| Committed \| Vertical slices, SOLID/DRY/KISS/YAGNI, constructor injection, and understandable code; no invented mandatory class/layer count. |
| [TD-C-063 · L6376](../../../04-spoolin-techdoc.md#13-verification-strategy) | V-READ..V-HANDOFF full required families; all SQL CHECK/UNIQUE/FK/delete branches, allowed/denied API fields, real streams/races/DB role denial,97 UI states, both clients, raw secret-safe proof, separate unit and isolated integrated commands. |
| [TD-C-064 · L6507](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria) | G-01..G-06 release evidence owners: locks, real DB/contracts, both host approvals, licensed dataset, operations, ownership/access/setup/disclosure. Unexecuted evidence and external prerequisites stay open independently of task definition readiness. |
| [TECH-G-06 · L6522](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria) | G-06 release ownership \| Assign technical/security/operator owners and verify required repository/setup/disclosure material and accessible release revision \| Release commitments in NFR-011 and AC-095–097; product owner coordinates |
| [ADR-10 · L6557](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-10 \| One techdoc includes schema/contracts/assurance/traceability, no extra implementation artifacts \| Shared schema/API/assurance references remain co-located; executable artifacts belong to implementation |
| [V-HANDOFF · L6402](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-HANDOFF \| Review source→design→ATDD→ticket links later, ownership/style/generation, README/AI disclosure and publicly accessible reviewed revision; retain requirement links and executed release-verification results |
| [TD-ROW-L3556 · L3556](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | operation \| operation_receipt: all existing request columns; receipt_type distinguishes internal initialization \| Scoped idempotency, safe seven-day result and lifetime no-repeat tombstone |
| [TD-ROW-L3567 · L3567](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | key_version / rate_bucket \| crypto_key_version / request_rate_window \| Nonce allocation/rotation and atomic configurable quotas |
| [TD-ROW-L3588 · L3588](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `operational_event` \| Each service: `operations/events` \| Restricted append-only activity/attempt/recovery evidence with bounded retention |
| [TD-ROW-L5186 · L5186](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Worker claim/complete \| Claim and attempt evidence commit before effect; completion uses lease epoch CAS \| READ COMMITTED, SKIP LOCKED; reconcile uncertain remote effects |
| [TD-ROW-L5188 · L5188](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Rotation/purge \| Bounded batches, row lock/CAS, original key retained until completion and restore horizon \| READ COMMITTED; resumable through row key IDs, no long global transaction |
| [TD-BLOCK-L4035 · L4035](../../../04-spoolin-techdoc.md#media-receipt-restriction) | Media receipt restriction — complete sql contract, all members, constraints and field numbers |
| [BRAIN-A12 · L57](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A12 \| Unit tests with complete cases, including edges \| Requirement-linked business and boundary cases; no unsupported coverage guarantee |
| [BRAIN-A15 · L60](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A15 \| Public Git repository access \| Accessible reviewable repository |
| [BRAIN-A16 · L61](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A16 \| README explaining setup, execution, and AI usage \| Reproducible instructions and accurate disclosure |
| [BRAIN-A17 · L62](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A17 \| Explain project code in a possible walkthrough \| Developer understands generated and handwritten code and design choices |
| [BRAIN-D01 · L70](../../../01-brainstorming-result.md#4-decision-register) | D01 \| Committed \| Deliver the initial release first; future product expansion stays separate |
| [BRAIN-D04 · L73](../../../01-brainstorming-result.md#4-decision-register) | D04 \| Committed \| Organize implementation around vertical slices |
| [BRAIN-D06 · L75](../../../01-brainstorming-result.md#4-decision-register) | D06 \| Committed \| Consider ISO/IEC 27001/27002-informed security by design; do not claim certification or organizational conformity |
| [BRAIN-D13 · L82](../../../01-brainstorming-result.md#4-decision-register) | D13 \| Committed \| Docker Compose with Make wrappers for reproducible startup, shutdown, restart, inspection, and testing |
| [BRAIN-D17 · L86](../../../01-brainstorming-result.md#4-decision-register) | D17 \| Committed \| Apply code quality, SOLID, DRY, KISS, YAGNI, and constructor dependency injection within vertical slices |
| [BRAIN-D19 · L88](../../../01-brainstorming-result.md#4-decision-register) | D19 \| Committed \| Unit and integration tests, including real gRPC communication and persistence against the selected database engine |

## Implementation

1. Assign named component/security/operator reviewers and backup custody with product owner Muhammad Okfriansyah; task IDs establish logical proof ownership but do not invent assigned people.
2. Assemble a sanitized release index for every included obligation/case/row and G-01–G-06, with current source/build/environment/reviewer evidence and explicit unresolved/conditional results. Preserve historical reports and mark changed evidence Stale.
3. Write accurate setup/prerequisites/one-command run/seed/test/recovery/connector instructions and AI-use disclosure, distinguishing generated/handwritten code and component simulations from real proof. Reproduce instructions from a fresh checkout.
4. Establish the intended Git revision and publicly accessible repository under the release delivery work. Verify actual public access to that revision and required material; retain the revision and access-verification timestamp as evidence.
5. Rehearse a feature trace and small change with its tests/failure explanation. Verify all group integration/release obligations, source-gap resolution and fresh independent reviews before declaring delivery complete; no release success or certification promise.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T046-A1:** Reproduction: a fresh reader follows README to the reviewed usable100/100 installation, correct role access and truthful test/blocked results without private authoring context.
- **T046-A2:** Delivery: accessible intended Git revision,actual public-access verification timestamp,accurate AI disclosure and owner walkthrough evidence exist; prepared documents alone do not satisfy repository release access.
- **T046-A3:** Release: every required task/group/gate has fresh passing evidence or release remains incomplete; future evidence and unsupported client/provider checks never become implicit passes.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

This task authors and reconciles delivery evidence rather than executable product behavior; use source/access/reproduction/reader checks and do not invent unit tests. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-076](../../../05-atdd.md#case-atdd-076-repository-release-access) / System | Intended reviewed Git commit and release reviewer access; verify the repository revision and required evidence are publicly accessible. | Then The release reviewer can publicly access the intended Git revision and required material; evidence identifies the reviewed revision. Observation: Inspect the intended reviewed Git revision and actual public repository access; an inaccessible local directory does not establish release availability. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-077](../../../05-atdd.md#case-atdd-077-readmeai-disclosure) / System | ComparefinalREADME command/prerequisite/seed/recovery/AI-use disclosure with actual run/build/client/provider evidence; allskips/limits visible. | Then Setup/run/seed/test/recovery instructions and AI use are accurate; limitations and unexecuted checks are explicit. MCP client use is documented without claiming unverified compatibility. Observation: Read actual README setup, usage, architecture, verification and AI disclosure against the reviewed revision; directions must reproduce required paths without real secrets. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-078](../../../05-atdd.md#case-atdd-078-walkthrough) / System | Developer traces create/edit/upload throughGraphQLgRPCownertransaction andtests; rehearses small change; explains AI-assisted code without claim placement. | Then They can explain generated/handwritten code, decisions, failure behavior, tests, and AI contribution. Release readiness requires verification evidence. Observation: Inspect the source-required walkthrough/demonstration and attributable contributor code ownership/evidence; documentation generation alone does not demonstrate application behavior. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-213](../../../05-atdd.md#case-atdd-213-keep-later-execution-claims-traceable-and-fresh) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §13/14 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then each actual result identifies build, expectationversion, environment, date, boundary and limitations And source changes mark affected evidenceStaleuntilimpactreview/fresh execution And requiredrealDB/gRPC/clients/provider checks cannot bepassed by fixtures orskips; remaining ticket/codework stays inits separate phase Observation: Review each actual result for application revision, expectation hash, environment, date and named boundary; compare changed sources/oracles to stale evidence. Required live checks cannot become Passed from screenshots/mocks/skips; later ticket/code work is not performed here. |
| [ATDD-275](../../../05-atdd.md#case-atdd-275-keep-fixture-evidence-within-its-actual-scope) / Document review | All twelve current design flow pages/97 state references, techdoc §3.1.1/3.1.2, existing discovery preview, source register and supplied design reports. Compare fixed copy and technical rules with replaceable illustrative records/counts/dates and simulated outcomes. | Then static prototype checks are recorded as design evidence withdate/scope And production authentication, transactions, persistence, provider delivery andclientapprovalremainNotRun until actual boundary proof And source changes require impact review before prior evidence applies Observation: Review all twelve UI-reference rows against techdoc §3.1.1/3.1.2: production context, state/component links, feature owner, existing API dependencies and case group must agree. Future frontend plans must carry these references and applicable recovery/access states; fixture HTML selectors/scripts are not implementation requirements. Distinguish fixed branding and accepted interaction from replaceable titles/counts/dates and illustrative outcomes. Confirm the existing discovery preview caption/alt text states its two-theme illustrative scope, with gallery links. This is document review, with no new frontend, executable tests or runtime evidence. Manual documentary review; retain original reported 582 state combinations on 10 October and 40 brand combinations on 11 October only as supplied reports, with unavailable build revision stated; no production case becomes Passed. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| delivery-proof | Follow README from fresh checkout; inspect actual public Git access and reviewed revision and rehearse named feature/change with owner | `/Users/mekari/Developer/personal-project/movie-app` | Reproducible behavior, accessible reviewed revision, truthful disclosure and independently usable evidence | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| delivery-proof | Not run | — | — | — | — |
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
