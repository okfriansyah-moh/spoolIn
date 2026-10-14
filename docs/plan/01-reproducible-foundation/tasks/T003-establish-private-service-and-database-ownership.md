---
kind: task
id: T003
group: G01
depends_on:
- T001
- T002
readiness: Ready
status: Not started
risk: high
test_strategy: alongside
visual_review: not-applicable
---

# T003 — Establish private service and database ownership

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-135 | implements |
| ATDD-136 | implements |
| TD-C-002 | both |
| TD-C-017 | implements |
| TD-C-024 | both |
| TD-C-025 | implements |
| TD-C-059 | implements |
| TD-U-001 | both |
| ADR-01 | both |

## Prerequisites

- [T001 — Lock the build and isolated verification harness](T001-lock-the-build-and-isolated-verification-harness.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted.
- [T002 — Generate and validate the shared wire contracts](T002-generate-and-validate-the-shared-wire-contracts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/core/build.gradle.kts | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/build.gradle.kts | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/build.gradle.kts | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/Application.kt | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/Application.kt | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/Application.kt | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/application.yaml | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/resources/application.yaml | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/resources/application.yaml | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/postgres/bootstrap.sql | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/postgres/grants.sql | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/migrate.sh | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/containers/ | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/compose.yaml | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/integrationTest/kotlin/com/spoolin/core/boundary/bootstrap/ | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/integrationTest/kotlin/com/spoolin/catalog/boundary/bootstrap/ | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/integrationTest/kotlin/com/spoolin/media/boundary/bootstrap/ | T003 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [2.2 Deployment and communication inventory](../../../04-spoolin-techdoc.md#22-deployment-and-communication-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [2.4.3 Backend applications and feature slices](../../../04-spoolin-techdoc.md#243-backend-applications-and-feature-slices): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.1 Ownership, conventions and relationships](../../../04-spoolin-techdoc.md#61-ownership-conventions-and-relationships): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [10.2 Flyway and schema evolution](../../../04-spoolin-techdoc.md#102-flyway-and-schema-evolution): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.1 Database access and resource budgets](../../../04-spoolin-techdoc.md#121-database-access-and-resource-budgets): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [TD-C-002 · L167](../../../04-spoolin-techdoc.md#22-deployment-and-communication-inventory) | Preserve browser→Core GraphQL, typed mTLS gRPC service graph and separate MCP process. Bind host interfaces loopback; DB/gRPC/exporters private; honor deadlines, retry ownership and no domain transaction or connection over remote calls. |
| [TD-C-017 · L2344](../../../04-spoolin-techdoc.md#55-browser-http-and-operational-endpoints) | Implement exact HTTP endpoint/host/origin/content-type/CSRF/cache/stream/error contracts, standard authenticated gRPC health, bounded atomic Alertmanager v4 event ingestion and safe operation reconciliation semantics. |
| [TD-C-024 · L3260](../../../04-spoolin-techdoc.md#61-ownership-conventions-and-relationships) | Three databases/appdata schemas, UUIDv4/v5 source identities, checked strings not PG enums, microsecond timestamps, bounded typed JSON/arrays, public/private/restricted projection rules. Selected 29 tables replace historical 38 without aliases; no deployed conversion assumed. |
| [TD-C-025 · L3573](../../../04-spoolin-techdoc.md#62-table-dictionary-and-embedded-ddl) | Service-owned entities/migrations and explicit typed embedded notification/approval/processing/initialization values; operations tables first, owner extensions ordered before references. Maintenance separated; no cross-owner repository library. |
| [TD-C-059 · L6085](../../../04-spoolin-techdoc.md#12-operations-observability-and-recovery) | Own-DB least privilege roles/TLS/SCRAM, append-only audit/runtime restrictions and measured pool/memory/disk/processor budgets; performance budgets are targets pending measured host evidence. |
| [TD-U-001 · L6534](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes) | TD-U-001 \| PostgreSQL; JPA/Hibernate; parameterized native SQL where justified \| D18, OD-015; older database choice Open is resolved. Kubernetes remains outside the deployment baseline |
| [ADR-01 · L6548](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-01 \| Three service DBs on one PostgreSQL instance. Reject shared tables/cross-DB FK; retain local atomicity with logical references and durable work \| TD-U-001, NFR-002/003; revisit only with demonstrated operational need |

## Implementation

1. Create three Boot applications and service-owned PostgreSQL databases/appdata schemas, private TCP gRPC/management listeners and loopback browser exposure. Configure authenticated TLS DB connections, mTLS service certificates and exact health boundaries.
2. Prepare non-login owner plus migrator/runtime/maintenance/backup roles per database. Bootstrap Catalog pg_trgm only with privileged preparation; apply grants after owner migrations. No service runtime performs bootstrap, another owner's SQL or schema repair.
3. Implement serialized Flyway runner and source-set wiring, with migration history/checksum/schema-range readiness and the 120-second lock bound. Feature tasks add their own immutable migration files and JPA mappings.
4. Wire Core packaging and source SDL plus the static-resource/SPA fallback boundary using a clearly labeled representative static test fixture. Test fallback only for frontend navigation, excluding GraphQL, artwork, health, private and missing API paths. T004 creates the production shell; T039 packages it and T044 proves actual routes/focus/session redirects. The fixture is no production UI evidence.
5. Exercise real network/credential denial and a disposable schema to prove the migration runner, without inventing deployed historical schema conversion.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T003-A1:** Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- **T003-A2:** Transport: real Core→Catalog gRPC succeeds under allowed certificates and a wrong service identity fails before handler execution; no public DB/exporter listener.
- **T003-A3:** Navigation boundary: a representative static fixture is served for a frontend navigation while a missing API route remains an API error; startup reports truthful migration/readiness failure. Production SPA behavior remains with T039/T044.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Use checks alongside implementation because build/visual/runtime composition must be exercised as its interfaces appear; acceptance still governs before completion. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| Local contract contribution | Owned source clauses and selected section fixtures | Verify generated/owner behavior locally; full assigned case proof remains with the downstream owner identified in the root register |

Partial implementation contributions: [ATDD-135](../../../05-atdd.md#case-atdd-135-enforce-service-and-network-ownership), [ATDD-136](../../../05-atdd.md#case-atdd-136-keep-browser-navigation-and-api-failures-distinct). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| boundary-tests | `bash ops/bin/test-integration.sh --task T003` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

This task’s executable proof is the declared integration/system/browser procedure; it has no separately required unit selector. Earlier rule/component owners supply their local evidence. Verify this task at its actual boundary instead of inventing a unit suite that mirrors it.

## Review and evidence

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize permission/secret boundaries, races, rollback/replay, lifetime evidence and the real owner boundary specified in this task. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| boundary-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including self-review, fresh independent review and applicable visual/interaction checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
