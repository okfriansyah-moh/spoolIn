---
kind: group
id: G26
feature: release-assurance
tasks:
- T042
- T043
- T044
- T045
- T046
integration_owner: T046
---

# 26-release-assurance

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Matrix: all28 storage families/260 column rows plus36 GraphQL and45 RPC rows have their complete physical, owner, projection, retention, allowed/denied/presence/deadline/error/side-effect results. ResolveActor remains deny-only on selected network callers. Authorization: Visitor/Viewer/forged/cross-owner callers obtain no restricted data/effects through any real boundary. Coverage: all97 states have appropriate source/current evidence, with both themes and three widths; expected actual differences have explicit verdicts. Benchmark: each run reports all offered/admitted/completed/throttled/error counts and independently computed p95 at the stated workload with active rate limits. Reproduction: a fresh reader follows README to the reviewed usable100/100 installation, correct role access and truthful test/blocked results without private authoring context.

This stage belongs to **release-assurance**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [5.3 Operation, permission and deadline inventory](../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory).
- [5.4 Projection and presence crosswalk](../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk).
- [5.7.1 Internal method authorization map](../../04-spoolin-techdoc.md#571-internal-method-authorization-map).
- [5.9 Browser operation inventory](../../04-spoolin-techdoc.md#59-browser-operation-inventory).
- [13.1 Verification families and database-specific obligations](../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations).
- [14. Technical dependencies and release criteria](../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria).
- [8.2 Threat model and assurance records](../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records).
- [8.3 API security enforcement](../../04-spoolin-techdoc.md#83-api-security-enforcement).
- [8.4 Configurable rate admission](../../04-spoolin-techdoc.md#84-configurable-rate-admission).
- [8.5 Browser storage and session-cookie controls](../../04-spoolin-techdoc.md#85-browser-storage-and-session-cookie-controls).
- [13.2 Additional boundary verification](../../04-spoolin-techdoc.md#132-additional-boundary-verification).
- [3.1.1 UI reference and implementation map](../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map).
- [3.6 Components, accessibility and responsive behavior](../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior).
- [13.3 Verification locations and command selection](../../04-spoolin-techdoc.md#133-verification-locations-and-command-selection).
- [7.4 Index and query-plan obligations](../../04-spoolin-techdoc.md#74-index-and-query-plan-obligations).
- [12.1 Database access and resource budgets](../../04-spoolin-techdoc.md#121-database-access-and-resource-budgets).
- [12.3 Retention, purge and backup runbooks](../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks).
- [12.4 Metric contracts and alert expressions](../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions).
- [Appendix A.1 Baseline decisions and requirement changes](../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T042 | Matrix: all28 storage families/260 column rows plus36 GraphQL and45 RPC rows have their complete physical, owner, projection, retention, allowed/denied/presence/deadline/error/side-effect results. ResolveActor remains deny-only on selected network callers. | [T042 — Reconcile every real API and RPC boundary row](tasks/T042-reconcile-every-real-api-and-rpc-boundary-row.md) |
| T043 | Authorization: Visitor/Viewer/forged/cross-owner callers obtain no restricted data/effects through any real boundary. | [T043 — Verify policy, authorization and injection across complete flows](tasks/T043-verify-policy-authorization-and-injection-across-complete-flows.md) |
| T044 | Coverage: all97 states have appropriate source/current evidence, with both themes and three widths; expected actual differences have explicit verdicts. | [T044 — Complete visual, interaction and accessibility evidence](tasks/T044-complete-visual-interaction-and-accessibility-evidence.md) |
| T045 | Benchmark: each run reports all offered/admitted/completed/throttled/error counts and independently computed p95 at the stated workload with active rate limits. | [T045 — Measure capacity and prove operational recovery limits](tasks/T045-measure-capacity-and-prove-operational-recovery-limits.md) |
| T046 | Reproduction: a fresh reader follows README to the reviewed usable100/100 installation, correct role access and truthful test/blocked results without private authoring context. | [T046 — Prepare the reviewed repository and release handoff](tasks/T046-prepare-the-reviewed-repository-and-release-handoff.md) |

## Dependencies

External inputs: [T009 — Enforce scoped service identity and shared admission](../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md), [T010 — Connect account and session browser journeys](../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md), [T013 — Connect discovery, details and public people views](../10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T017 — Connect movie editors and confirmed outcomes](../12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md), [T019 — Connect people management and retained edits](../13-people-and-credits/tasks/T019-connect-people-management-and-retained-edits.md), [T021 — Connect credit editing and verify the people outcome](../13-people-and-credits/tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md), [T025 — Connect private archives and irreversible confirmations](../15-archive-management/tasks/T025-connect-private-archives-and-irreversible-confirmations.md), [T026 — Connect measured progress and recover attachment outcomes](../16-artwork-management-browser-journeys/tasks/T026-connect-measured-progress-and-recover-attachment-outcomes.md), [T027 — Prove recovery through real movie, person and credit editors](../17-draft-recovery-editor-integration/tasks/T027-prove-recovery-through-real-movie-person-and-credit-editors.md), [T030 — Build the trusted in-chat review resource](../18-assisted-movie-creation-transport-and-review/tasks/T030-build-the-trusted-in-chat-review-resource.md), [T031 — Expose complete read-only audit outcomes](../19-accountable-history/tasks/T031-expose-complete-read-only-audit-outcomes.md), [T033 — Initialize and replay seed and prepared accounts safely](../20-reproducible-seed/tasks/T033-initialize-and-replay-seed-and-prepared-accounts-safely.md), [T034 — Record actionable alerts through one selected mode](../21-operational-notifications/tasks/T034-record-actionable-alerts-through-one-selected-mode.md), [T035 — Emit bounded private-safe JSON diagnostics](../22-operational-evidence/tasks/T035-emit-bounded-private-safe-json-diagnostics.md), [T036 — Expose logical metrics, dashboards and alert episodes](../22-operational-evidence/tasks/T036-expose-logical-metrics-dashboards-and-alert-episodes.md), [T037 — Implement the seven reproducible lifecycle commands](../23-local-operations/tasks/T037-implement-the-seven-reproducible-lifecycle-commands.md), [T038 — Verify trusted approval in Claude Desktop](../24-assisted-movie-creation-client-acceptance/tasks/T038-verify-trusted-approval-in-claude-desktop.md), [T039 — Verify ChatGPT approval and complete both-client acceptance](../24-assisted-movie-creation-client-acceptance/tasks/T039-verify-chatgpt-approval-and-complete-both-client-acceptance.md), [T040 — Rotate keys and aliases without losing access](../25-protected-recovery/tasks/T040-rotate-keys-and-aliases-without-losing-access.md), [T041 — Verify migrations, purge and coordinated restore](../25-protected-recovery/tasks/T041-verify-migrations-purge-and-coordinated-restore.md).

- T042 starts after T010, T009, T013, T027, T017, T019, T021, T025, T026, T031, T039, T033, T034, T037, T040, T041.
- T043 starts after T042.
- T044 starts after T013, T027, T017, T019, T021, T025, T026, T031, T030, T039, T043.
- T045 starts after T036, T037, T041, T042, T043.
- T046 starts after T038, T039, T033, T034, T035, T036, T037, T040, T041, T042, T043, T044, T045.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T046 — Prepare the reviewed repository and release handoff](tasks/T046-prepare-the-reviewed-repository-and-release-handoff.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T042: Matrix: all28 storage families/260 column rows plus36 GraphQL and45 RPC rows have their complete physical, owner, projection, retention, allowed/denied/presence/deadline/error/side-effect results. ResolveActor remains deny-only on selected network callers.
- T042: Compatibility: exact declared values and safe failures survive real boundaries without private entity leakage or loss of scalar precision.
- T042: Evidence: a missing required row/runtime produces incomplete/setup status, never Passed; source hashes and executing revision match.
- T043: Authorization: Visitor/Viewer/forged/cross-owner callers obtain no restricted data/effects through any real boundary.
- T043: Validation: identical effective policy applies everywhere, changed invalid values commit nothing, unchanged data/credentials remain usable and passwords are never transformed or retained.
- T043: Injection: user input cannot alter query structure, run browser/script/process code, fetch arbitrary destinations or disclose protected data; permitted text still displays intact.
- T044: Coverage: all97 states have appropriate source/current evidence, with both themes and three widths; expected actual differences have explicit verdicts.
- T044: Usability: every included journey remains readable/reachable at narrow size/zoom with keyboard and non-color feedback, including failure/stale/uncertain recovery.
- T044: Identity: approved SpoolIn copy/mark and single shared component system survive theme switch without resetting work; no screenshot-only accessibility pass.
- T045: Benchmark: each run reports all offered/admitted/completed/throttled/error counts and independently computed p95 at the stated workload with active rate limits.
- T045: Resources: overload is rejected before unbounded allocation, normal unrelated metadata remains usable during Media faults, and persistence/recovery survives forced stop.
- T045: Claims: targets without reproducible measurement remain unverified; selected external provider claims require actual evidence, database recording is identified accurately.
- T046: Reproduction: a fresh reader follows README to the reviewed usable100/100 installation, correct role access and truthful test/blocked results without private authoring context.
- T046: Delivery: accessible intended Git revision,actual public-access verification timestamp,accurate AI disclosure and owner walkthrough evidence exist; prepared documents alone do not satisfy repository release access.
- T046: Release: every required task/group/gate has fresh passing evidence or release remains incomplete; future evidence and unsupported client/provider checks never become implicit passes.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
