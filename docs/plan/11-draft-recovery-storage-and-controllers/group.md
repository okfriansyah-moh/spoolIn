---
kind: group
id: G11
feature: draft-recovery
tasks:
- T014
- T015
integration_owner: T015
---

# 11-draft-recovery-storage-and-controllers

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation. Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request.

This stage belongs to **draft-recovery**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [3.4 Forms, drafts and conflict recovery](../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery).
- [5.6.5 Code example: save and reconcile a draft](../../04-spoolin-techdoc.md#565-code-example-save-and-reconcile-a-draft).
- [5.11 JSON projections and draft schemas](../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas).
- [6.2.4 Core tables](../../04-spoolin-techdoc.md#624-core-tables).
- [7.3.1 Durable draft receipts](../../04-spoolin-techdoc.md#731-durable-draft-receipts).
- [3.4.1 Browser storage and restored-page state](../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state).
- [8.5.4 Session and CSRF responsibility map](../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T014 | Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation. | [T014 — Persist encrypted drafts with durable save and discard receipts](tasks/T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts.md) |
| T015 | Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request. | [T015 — Build account-isolated browser draft controllers](tasks/T015-build-account-isolated-browser-draft-controllers.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T005 — Protect account identities and credential storage](../03-account-access-credential-storage/tasks/T005-protect-account-identities-and-credential-storage.md), [T006 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md), [T007 — Implement owner receipts and atomic mutation evidence](../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md), [T008 — Implement registration and authoritative sessions](../06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md), [T009 — Enforce scoped service identity and shared admission](../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md), [T010 — Connect account and session browser journeys](../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md).

- T014 starts after T005, T008, T009, T006, T007.
- T015 starts after T004, T010, T014.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T015 — Build account-isolated browser draft controllers](tasks/T015-build-account-isolated-browser-draft-controllers.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T014: Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation.
- T014: Discard replay: deleted draft content is never reread or rebuilt; later state remains absent even when the old request repeats.
- T014: Isolation: B cannot read/replay A's recovery state, quota/policy changes cannot break identical retained replay, and distinct stale edits require review.
- T015: Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request.
- T015: Isolation: delayed A response after switch to B never renders A content; returning A may resume only after current permission/target/operation checks.
- T015: Feedback: autosave failure preserves permitted in-memory input with an unbacked warning; clear/discard does not claim success before confirmed outcome.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
