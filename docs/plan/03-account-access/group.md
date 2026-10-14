---
kind: group
id: G03
tasks:
- T005
- T006
- T007
integration_owner: T007
---

# 03-account-access

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged. Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit. Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

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
| T005 | Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged. | [T005 — Protect account identities and credential storage](tasks/T005-protect-account-identities-and-credential-storage.md) |
| T006 | Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit. | [T006 — Implement registration and authoritative sessions](tasks/T006-implement-registration-and-authoritative-sessions.md) |
| T007 | Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role. | [T007 — Connect account and session browser journeys](tasks/T007-connect-account-and-session-browser-journeys.md) |

## Dependencies

External inputs: [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md). Consume their concrete outputs as stated in member prerequisites.

- T005 starts after T002, T003; its task definition owns the specific files/contracts and acceptance boundary.
- T006 starts after T005, T009, T010; its task definition owns the specific files/contracts and acceptance boundary.
- T007 starts after T004, T006, T008; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T007 — Connect account and session browser journeys](tasks/T007-connect-account-and-session-browser-journeys.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T005: Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged.
- T005: Nonce: parallel allocations and rollback consume distinct counters with no reuse; counter exhaustion blocks encryption.
- T005: Identity: normalized-email duplicates cannot create two accounts; legitimate exact Unicode/whitespace password bytes verify without normalization or truncation.
- T006: Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.
- T006: Session: expired/revoked credentials fail even if restored by the browser; missing/foreign CSRF/Origin denies mutations while bootstrap can obtain anti-forgery state.
- T006: Privacy: raw credential appears only in Set-Cookie, no private data enters logs/receipts/browser storage, and confirmed logout revokes before clearing its cookie.
- T007: Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- T007: Feedback: invalid/repeated registration retains permitted non-secret fields, never password; safe errors reveal no other account details.
- T007: History: restored private pages and delayed responses remain concealed until current same-account verification; uncertain logout cannot claim completion.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
