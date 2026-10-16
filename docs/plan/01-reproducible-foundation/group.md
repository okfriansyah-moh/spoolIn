---
kind: group
id: G01
feature: reproducible-foundation
tasks:
- T001
- T002
- T003
integration_owner: T003
---

# 01-reproducible-foundation

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted. Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears. Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.

This stage belongs to **reproducible-foundation**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [2.1 Version and dependency baseline](../../04-spoolin-techdoc.md#21-version-and-dependency-baseline).
- [2.4.1 Repository root](../../04-spoolin-techdoc.md#241-repository-root).
- [2.4.7 Tests, build order and dependency rules](../../04-spoolin-techdoc.md#247-tests-build-order-and-dependency-rules).
- [13.3 Verification locations and command selection](../../04-spoolin-techdoc.md#133-verification-locations-and-command-selection).
- [14. Technical dependencies and release criteria](../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria).
- [2.4.4 Shared contracts and generated types](../../04-spoolin-techdoc.md#244-shared-contracts-and-generated-types).
- [5.1 GraphQL SDL](../../04-spoolin-techdoc.md#51-graphql-sdl).
- [5.2 Internal protobuf contracts](../../04-spoolin-techdoc.md#52-internal-protobuf-contracts).
- [5.4 Projection and presence crosswalk](../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk).
- [5.10 MCP output schema registry](../../04-spoolin-techdoc.md#510-mcp-output-schema-registry).
- [5.11 JSON projections and draft schemas](../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas).
- [9.1 MCP tool input schemas](../../04-spoolin-techdoc.md#91-mcp-tool-input-schemas).
- [2.2 Deployment and communication inventory](../../04-spoolin-techdoc.md#22-deployment-and-communication-inventory).
- [2.4.3 Backend applications and feature slices](../../04-spoolin-techdoc.md#243-backend-applications-and-feature-slices).
- [6.1 Ownership, conventions and relationships](../../04-spoolin-techdoc.md#61-ownership-conventions-and-relationships).
- [10.2 Flyway and schema evolution](../../04-spoolin-techdoc.md#102-flyway-and-schema-evolution).
- [12.1 Database access and resource budgets](../../04-spoolin-techdoc.md#121-database-access-and-resource-budgets).

## Task list

| Task | Result | Detail |
|---|---|---|
| T001 | Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted. | [T001 — Lock the build and isolated verification harness](tasks/T001-lock-the-build-and-isolated-verification-harness.md) |
| T002 | Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears. | [T002 — Generate and validate the shared wire contracts](tasks/T002-generate-and-validate-the-shared-wire-contracts.md) |
| T003 | Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners. | [T003 — Establish private service and database ownership](tasks/T003-establish-private-service-and-database-ownership.md) |

## Dependencies

No planned task prerequisite; inspect the declared source and environment inputs.

- T001 starts after source inspection.
- T002 starts after T001.
- T003 starts after T001, T002.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T003 — Establish private service and database ownership](tasks/T003-establish-private-service-and-database-ownership.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T001: Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted.
- T001: Isolation: a fixture with the normal installation name is rejected before connecting; a failing synthetic test returns 1 and a missing required prerequisite returns 2, leaving normal sentinel data untouched.
- T001: Scope: no catalog feature or real credential is added by scaffolding; absent runtime features remain explicitly pending.
- T002: Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- T002: Presence: omitted optional edit preserves and explicit null/trimmed blank clears according to PRD/techdoc; ordered arrays retain complete names. T006 owns the ATDD-278 rule proof; T042 owns complete GraphQL/protobuf presence proof.
- T002: Projection: an undeclared output field and out-of-range scalar are rejected without narrowing/truncation; source contracts remain unmodified.
- T003: Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- T003: Transport: real Core→Catalog gRPC succeeds under allowed certificates and a wrong service identity fails before handler execution; no public DB/exporter listener.
- T003: Navigation boundary: a representative static fixture is served for a frontend navigation while a missing API route remains an API error; startup reports truthful migration/readiness failure. Production SPA behavior remains with T037/T044.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
