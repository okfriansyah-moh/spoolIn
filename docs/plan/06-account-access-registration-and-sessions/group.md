---
kind: group
id: G06
feature: account-access
tasks:
- T008
integration_owner: T008
---

# 06-account-access-registration-and-sessions

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.

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
| T008 | Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit. | [T008 — Implement registration and authoritative sessions](tasks/T008-implement-registration-and-authoritative-sessions.md) |

## Dependencies

External inputs: [T005 — Protect account identities and credential storage](../03-account-access-credential-storage/tasks/T005-protect-account-identities-and-credential-storage.md), [T006 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md), [T007 — Implement owner receipts and atomic mutation evidence](../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md).

- T008 starts after T005, T006, T007.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T008 — Implement registration and authoritative sessions](tasks/T008-implement-registration-and-authoritative-sessions.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T008: Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.
- T008: Session: expired/revoked credentials fail even if restored by the browser; missing/foreign CSRF/Origin denies mutations while bootstrap can obtain anti-forgery state.
- T008: Privacy: raw credential appears only in Set-Cookie, no private data enters logs/receipts/browser storage, and confirmed logout revokes before clearing its cookie.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
