---
kind: group
id: G08
feature: account-access
tasks:
- T010
integration_owner: T010
---

# 08-account-access-browser-journeys

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.

This stage belongs to **account-access**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [6.2.4 Core tables](../../04-spoolin-techdoc.md#624-core-tables).
- [8.1 Authenticated encryption and key lifecycle](../../04-spoolin-techdoc.md#81-authenticated-encryption-and-key-lifecycle).
- [8.3 API security enforcement](../../04-spoolin-techdoc.md#83-api-security-enforcement).
- [12.3 Retention, purge and backup runbooks](../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks).
- [4.2 SpoolIn Core service](../../04-spoolin-techdoc.md#42-spoolin-core-service).
- [5.5 Browser HTTP and operational endpoints](../../04-spoolin-techdoc.md#55-browser-http-and-operational-endpoints).
- [8.5 Browser storage and session-cookie controls](../../04-spoolin-techdoc.md#85-browser-storage-and-session-cookie-controls).
- [3.1 Routes, screens and API dependencies](../../04-spoolin-techdoc.md#31-routes-screens-and-api-dependencies).
- [3.4.1 Browser storage and restored-page state](../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state).
- [8.5.4 Session and CSRF responsibility map](../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T010 | Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role. | [T010 — Connect account and session browser journeys](tasks/T010-connect-account-and-session-browser-journeys.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T008 — Implement registration and authoritative sessions](../06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md), [T009 — Enforce scoped service identity and shared admission](../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md).

- T010 starts after T004, T008, T009.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T010 — Connect account and session browser journeys](tasks/T010-connect-account-and-session-browser-journeys.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T010: Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- T010: Feedback: invalid/repeated registration retains permitted non-secret fields, never password; safe errors reveal no other account details.
- T010: History: restored private pages and delayed responses remain concealed until current same-account verification; uncertain logout cannot claim completion.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
