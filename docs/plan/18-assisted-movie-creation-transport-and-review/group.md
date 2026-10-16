---
kind: group
id: G18
feature: assisted-movie-creation
tasks:
- T028
- T029
- T030
integration_owner: T030
---

# 18-assisted-movie-creation-transport-and-review

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability. Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing. Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action.

This stage belongs to **assisted-movie-creation**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [2.4.5 MCP adapter and review UI](../../04-spoolin-techdoc.md#245-mcp-adapter-and-review-ui).
- [4.5 MCP adapter](../../04-spoolin-techdoc.md#45-mcp-adapter).
- [5.8 MCP tools, resources and gateway mapping](../../04-spoolin-techdoc.md#58-mcp-tools-resources-and-gateway-mapping).
- [5.10 MCP output schema registry](../../04-spoolin-techdoc.md#510-mcp-output-schema-registry).
- [9.1 MCP tool input schemas](../../04-spoolin-techdoc.md#91-mcp-tool-input-schemas).
- [5.11 JSON projections and draft schemas](../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas).
- [6.2.5 Catalog tables](../../04-spoolin-techdoc.md#625-catalog-tables).
- [9.2 Proposal, approval and uncertain publication](../../04-spoolin-techdoc.md#92-proposal-approval-and-uncertain-publication).
- [3.1.1 UI reference and implementation map](../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map).
- [14. Technical dependencies and release criteria](../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T028 | Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability. | [T028 — Implement isolated MCP transport and connector identity](tasks/T028-implement-isolated-mcp-transport-and-connector-identity.md) |
| T029 | Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing. | [T029 — Commit immutable proposals and exact approval once](tasks/T029-commit-immutable-proposals-and-exact-approval-once.md) |
| T030 | Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action. | [T030 — Build the trusted in-chat review resource](tasks/T030-build-the-trusted-in-chat-review-resource.md) |

## Dependencies

External inputs: [T001 — Lock the build and isolated verification harness](../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md), [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T006 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md), [T007 — Implement owner receipts and atomic mutation evidence](../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md), [T008 — Implement registration and authoritative sessions](../06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md), [T009 — Enforce scoped service identity and shared admission](../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md), [T012 — Serve precise public catalog and people reads](../10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md), [T016 — Implement authoritative movie creation and edits](../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md).

- T028 starts after T001, T002, T008, T009, T006, T007.
- T029 starts after T009, T006, T007, T012, T016, T028.
- T030 starts after T004, T028, T029.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T030 — Build the trusted in-chat review resource](tasks/T030-build-the-trusted-in-chat-review-resource.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T028: Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.
- T028: Transport: stdio carries protocol frames only, reconnect preserves original operation identity, and private exporter cannot be discovered as a tool.
- T028: Projection: exact schema rejects undeclared private fields/challenges in model output; adapter has no DB privileges or secret-bearing resource bundle.
- T029: Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing.
- T029: Revision: the owner returns every complete revised value/source/unknown and invalidates prior approval; rejected proposal stays nonpublic. T030 renders these values and T039 owns complete ATDD-188 correction/redraw proof.
- T029: Durability: lost response returns original movie result, late reject never deletes it, and bounded provenance/result rejection causes no partial publication.
- T030: Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action.
- T030: Isolation: synthetic markup/instructions render as data, challenge is absent from model output and resource bundle, and component tests cannot manufacture trusted host proof.
- T030: Resource: packaged self-contained artifact loads under selected CSP with accessible actions in both themes and supported host size contexts.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
