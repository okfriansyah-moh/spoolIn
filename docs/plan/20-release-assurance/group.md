---
kind: group
id: G20
tasks:
- T042
- T043
- T044
- T045
- T046
integration_owner: T046
---

# 20-release-assurance

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Matrix: all28 storage families/260 column rows plus36 GraphQL and45 RPC rows have their complete physical, owner, projection, retention, allowed/denied/presence/deadline/error/side-effect results. ResolveActor remains deny-only on selected network callers. Authorization: Visitor/Viewer/forged/cross-owner callers obtain no restricted data/effects through any real boundary. Coverage: all97 states have appropriate source/current evidence, with both themes and three widths; expected actual differences have explicit verdicts. Benchmark: each run reports all offered/admitted/completed/throttled/error counts and independently computed p95 at the stated workload with active rate limits. Reproduction: a fresh reader follows README to the reviewed usable100/100 installation, correct role access and truthful test/blocked results without private authoring context. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

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
| T046 | Reproduction: a fresh reader follows README to the reviewed usable100/100 installation, correct role access and truthful test/blocked results without private authoring context. | [T046 — Prepare the reviewed repository and assessment handoff](tasks/T046-prepare-the-reviewed-repository-and-assessment-handoff.md) |

## Dependencies

External inputs: [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T013 — Connect discovery, details and public people views](../07-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T016 — Prove recovery through real movie, person and credit editors](../08-draft-recovery/tasks/T016-prove-recovery-through-real-movie-person-and-credit-editors.md), [T018 — Connect movie editors and confirmed outcomes](../09-movie-management/tasks/T018-connect-movie-editors-and-confirmed-outcomes.md), [T020 — Connect people management and retained edits](../10-people-and-credits/tasks/T020-connect-people-management-and-retained-edits.md), [T022 — Connect credit editing and verify the people outcome](../10-people-and-credits/tasks/T022-connect-credit-editing-and-verify-the-people-outcome.md), [T024 — Connect private archives and irreversible confirmations](../12-archive-management/tasks/T024-connect-private-archives-and-irreversible-confirmations.md), [T027 — Connect measured progress and recover attachment outcomes](../11-artwork-management/tasks/T027-connect-measured-progress-and-recover-attachment-outcomes.md), [T028 — Expose complete read-only audit outcomes](../14-accountable-history/tasks/T028-expose-complete-read-only-audit-outcomes.md), [T031 — Build the trusted in-chat review resource](../13-assisted-movie-creation/tasks/T031-build-the-trusted-in-chat-review-resource.md), [T032 — Verify trusted approval in Claude Desktop](../13-assisted-movie-creation/tasks/T032-verify-trusted-approval-in-claude-desktop.md), [T033 — Verify ChatGPT approval and complete both-client acceptance](../13-assisted-movie-creation/tasks/T033-verify-chatgpt-approval-and-complete-both-client-acceptance.md), [T035 — Initialize and replay seed and prepared accounts safely](../15-reproducible-seed/tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md), [T036 — Record actionable alerts through one selected mode](../16-operational-notifications/tasks/T036-record-actionable-alerts-through-one-selected-mode.md), [T037 — Emit bounded private-safe JSON diagnostics](../17-operational-evidence/tasks/T037-emit-bounded-private-safe-json-diagnostics.md), [T038 — Expose logical metrics, dashboards and alert episodes](../17-operational-evidence/tasks/T038-expose-logical-metrics-dashboards-and-alert-episodes.md), [T039 — Implement the seven reproducible lifecycle commands](../18-local-operations/tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md), [T040 — Rotate keys and aliases without losing access](../19-protected-recovery/tasks/T040-rotate-keys-and-aliases-without-losing-access.md), [T041 — Verify migrations, purge and coordinated restore](../19-protected-recovery/tasks/T041-verify-migrations-purge-and-coordinated-restore.md). Consume their concrete outputs as stated in member prerequisites.

- T042 starts after T007, T008, T013, T016, T018, T020, T022, T024, T027, T028, T033, T035, T036, T039, T040, T041; its task definition owns the specific files/contracts and acceptance boundary.
- T043 starts after T042; its task definition owns the specific files/contracts and acceptance boundary.
- T044 starts after T013, T016, T018, T020, T022, T024, T027, T028, T031, T033, T043; its task definition owns the specific files/contracts and acceptance boundary.
- T045 starts after T038, T039, T041, T042, T043; its task definition owns the specific files/contracts and acceptance boundary.
- T046 starts after T032, T033, T035, T036, T037, T038, T039, T040, T041, T042, T043, T044, T045; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T046 — Prepare the reviewed repository and assessment handoff](tasks/T046-prepare-the-reviewed-repository-and-assessment-handoff.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

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
- T046: Delivery: accessible intended Git revision,actual submission/access timestamp,accurate AI disclosure and owner walkthrough evidence exist; prepared documents alone do not satisfy repository submission.
- T046: Release: every required task/group/gate has fresh passing evidence or release remains incomplete; future evidence and unsupported client/provider checks never become implicit passes.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
