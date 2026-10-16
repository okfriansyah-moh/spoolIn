---
kind: group
id: G24
feature: assisted-movie-creation
tasks:
- T038
- T039
integration_owner: T039
---

# 24-assisted-movie-creation-client-acceptance

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Host proof: model cannot invoke trusted approval or read its challenge, while the actual authorized human can approve the displayed exact revision. ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions.

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
| T038 | Host proof: model cannot invoke trusted approval or read its challenge, while the actual authorized human can approve the displayed exact revision. | [T038 — Verify trusted approval in Claude Desktop](tasks/T038-verify-trusted-approval-in-claude-desktop.md) |
| T039 | ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions. | [T039 — Verify ChatGPT approval and complete both-client acceptance](tasks/T039-verify-chatgpt-approval-and-complete-both-client-acceptance.md) |

## Dependencies

External inputs: [T028 — Implement isolated MCP transport and connector identity](../18-assisted-movie-creation-transport-and-review/tasks/T028-implement-isolated-mcp-transport-and-connector-identity.md), [T029 — Commit immutable proposals and exact approval once](../18-assisted-movie-creation-transport-and-review/tasks/T029-commit-immutable-proposals-and-exact-approval-once.md), [T030 — Build the trusted in-chat review resource](../18-assisted-movie-creation-transport-and-review/tasks/T030-build-the-trusted-in-chat-review-resource.md), [T037 — Implement the seven reproducible lifecycle commands](../23-local-operations/tasks/T037-implement-the-seven-reproducible-lifecycle-commands.md).

- T038 starts after T028, T029, T030, T037.
- T039 starts after T028, T029, T030, T038, T037.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T039 — Verify ChatGPT approval and complete both-client acceptance](tasks/T039-verify-chatgpt-approval-and-complete-both-client-acceptance.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T038: Host proof: model cannot invoke trusted approval or read its challenge, while the actual authorized human can approve the displayed exact revision.
- T038: Cases: every ordinary/adverse row records its own outcome; correction requires fresh approval and repeat/uncertain commit creates at most one movie.
- T038: Gate: unsupported isolation leaves CLAUDE publication disabled and this task unfinished; ChatGPT proof is independently owned by T039.
- T039: ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions.
- T039: Two clients: both have separate ordinary/adverse per-row results, with all exercised approval bypasses denied and invalid required metadata unpublished.
- T039: Truth: missing tunnel/host/account evidence keeps G-03 pending and never falls back to model-generated approval.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
