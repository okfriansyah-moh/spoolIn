---
kind: group
id: G14
tasks:
- T029
- T030
- T031
- T032
- T033
integration_owner: T033
---

# 13-assisted-movie-creation

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability. Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing. Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action. Host proof: model cannot invoke trusted approval or read its challenge, while the actual authorized human can approve the displayed exact revision. ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

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
| T029 | Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability. | [T029 — Implement isolated MCP transport and connector identity](tasks/T029-implement-isolated-mcp-transport-and-connector-identity.md) |
| T030 | Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing. | [T030 — Commit immutable proposals and exact approval once](tasks/T030-commit-immutable-proposals-and-exact-approval-once.md) |
| T031 | Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action. | [T031 — Build the trusted in-chat review resource](tasks/T031-build-the-trusted-in-chat-review-resource.md) |
| T032 | Host proof: model cannot invoke trusted approval or read its challenge, while the actual authorized human can approve the displayed exact revision. | [T032 — Verify trusted approval in Claude Desktop](tasks/T032-verify-trusted-approval-in-claude-desktop.md) |
| T033 | ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions. | [T033 — Verify ChatGPT approval and complete both-client acceptance](tasks/T033-verify-chatgpt-approval-and-complete-both-client-acceptance.md) |

## Dependencies

External inputs: [T001 — Lock the build and isolated verification harness](../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md), [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T006 — Implement registration and authoritative sessions](../03-account-access/tasks/T006-implement-registration-and-authoritative-sessions.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md), [T012 — Serve precise public catalog and people reads](../07-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md), [T017 — Implement authoritative movie creation and edits](../09-movie-management/tasks/T017-implement-authoritative-movie-creation-and-edits.md), [T039 — Implement the seven reproducible lifecycle commands](../18-local-operations/tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md). Consume their concrete outputs as stated in member prerequisites.

- T029 starts after T001, T002, T006, T008, T009, T010; its task definition owns the specific files/contracts and acceptance boundary.
- T030 starts after T008, T009, T010, T012, T017, T029; its task definition owns the specific files/contracts and acceptance boundary.
- T031 starts after T004, T029, T030; its task definition owns the specific files/contracts and acceptance boundary.
- T032 starts after T029, T030, T031, T039; its task definition owns the specific files/contracts and acceptance boundary.
- T033 starts after T029, T030, T031, T032, T039; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T033 — Verify ChatGPT approval and complete both-client acceptance](tasks/T033-verify-chatgpt-approval-and-complete-both-client-acceptance.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T029: Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.
- T029: Transport: stdio carries protocol frames only, reconnect preserves original operation identity, and private exporter cannot be discovered as a tool.
- T029: Projection: exact schema rejects undeclared private fields/challenges in model output; adapter has no DB privileges or secret-bearing resource bundle.
- T030: Approval: only exact current human/client-bound content publishes one movie once; altered content, expired challenge, wrong actor/client and model consent claim publish nothing.
- T030: Revision: the owner returns every complete revised value/source/unknown and invalidates prior approval; rejected proposal stays nonpublic. T031 renders these values and T033 owns complete ATDD-188 correction/redraw proof.
- T030: Durability: lost response returns original movie result, late reject never deletes it, and bounded provenance/result rejection causes no partial publication.
- T031: Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action.
- T031: Isolation: synthetic markup/instructions render as data, challenge is absent from model output and resource bundle, and component tests cannot manufacture trusted host proof.
- T031: Resource: packaged self-contained artifact loads under selected CSP with accessible actions in both themes and supported host size contexts.
- T032: Host proof: model cannot invoke trusted approval or read its challenge, while the actual authorized human can approve the displayed exact revision.
- T032: Cases: every ordinary/adverse row records its own outcome; correction requires fresh approval and repeat/uncertain commit creates at most one movie.
- T032: Gate: unsupported isolation leaves CLAUDE publication disabled and this task unfinished; ChatGPT proof is independently owned by T033.
- T033: ChatGPT: actual human action is bound to the exact displayed content and cannot be forged through tool parameters or retrieved instructions.
- T033: Two clients: both have separate ordinary/adverse per-row results, with all exercised approval bypasses denied and invalid required metadata unpublished.
- T033: Truth: missing tunnel/host/account evidence keeps G-03 pending and never falls back to model-generated approval.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
