---
kind: task
id: T001
group: G01
depends_on: []
readiness: Ready
status: Done
risk: high
test_strategy: alongside
visual_review: not-applicable
---

# T001 — Lock the build and isolated verification harness

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-134 | implements |
| TD-C-001 | both |
| TD-C-003 | both |
| TD-C-013 | both |
| F-STACK | both |
| F-CLOCK | both |
| F-UNIT | both |

## Prerequisites

No planned task prerequisite. Start from the existing documentation-only checkout and inspect the toolchain prerequisites in the linked technical baseline.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | settings.gradle.kts | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | build.gradle.kts | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | gradle.properties | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | gradlew | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | gradlew.bat | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | gradle/ | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | package.json | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | package-lock.json | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | .editorconfig | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | .gitignore | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/package.json | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/vite.config.ts | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/tsconfig.json | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/package.json | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/tsconfig.json | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/build.gradle.kts | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/package.json | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/playwright.config.ts | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/fixtures/ | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/fixtures/ | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/test-unit.sh | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/test-integration.sh | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | Makefile | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | build-tools/verification-map.json | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | build-tools/verification-tests/ | T001 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [2.1 Version and dependency baseline](../../../04-spoolin-techdoc.md#21-version-and-dependency-baseline): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [2.4.1 Repository root](../../../04-spoolin-techdoc.md#241-repository-root): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [2.4.7 Tests, build order and dependency rules](../../../04-spoolin-techdoc.md#247-tests-build-order-and-dependency-rules): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [13.3 Verification locations and command selection](../../../04-spoolin-techdoc.md#133-verification-locations-and-command-selection): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [14. Technical dependencies and release criteria](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [TD-C-001 · L134](../../../04-spoolin-techdoc.md#21-version-and-dependency-baseline) | Resolve selected JVM/npm/image/generator/tunnel baseline, transitive peer graph, licenses/advisories, reproducible locks and linux/arm64 plus linux/amd64 immutable image digests. Source pins are selected targets; successful resolution remains G-01 evidence. |
| [TD-C-003 · L264](../../../04-spoolin-techdoc.md#24-detailed-project-structure) | Create proposed root Gradle/npm workspaces, owner feature slices and generation/package dependency order; support is stateless. Source schemas generate consumers once; frontend static SPA fallback excludes API/private paths; review resource is self-contained. |
| [TD-C-013 · L1029](../../../04-spoolin-techdoc.md#41-shared-implementation-conventions) | Constructor injection, immutable DTOs, explicit JPA entities, designed versions, no Open Session in View, exact PostgreSQL mappings and explicit native flush/clear. ktlint/detekt/strict TypeScript/ESLint/Prettier/svelte-check; bounded executors and graceful resource closure. |

## Implementation

1. Resolve the selected §2.1 version and peer graph, generator compatibility and licenses/advisories; record exact versions and immutable linux/arm64 and linux/amd64 image digests. If the selected graph cannot resolve, retain diagnostics under G-01 and request a source decision; never silently upgrade or weaken the baseline.
2. Create the declared Gradle modules and npm workspaces with one lockfile. Configure JDK 21, Kotlin Spring/JPA, strict TypeScript, ktlint/detekt, ESLint/Prettier/svelte-check; generated outputs remain ignored. Use the Kit 3 Vite/static-adapter contract, not a second frontend server.
3. Implement isolated test runners and F-STACK/F-ACTORS/F-CATALOG/C-RESET fixture interfaces. Provision per-run UUID namespaces, keys, ports and volumes through test-only configuration; refuse normal installation identifiers and preserve failed-run diagnostics.
4. Define documented --task Tnnn selection on ops/bin/test-unit.sh and ops/bin/test-integration.sh, with an explicit build-tools/verification-map.json mapping to each task's declared test paths and levels. Reject unknown, empty or unavailable required selection with exit2. The final aggregate make commands still require all included checks; task selection never marks an unselected suite passed.
5. Publish the exact planned commands in the root command map, including owner integrationTest tasks and browser flow selection. At this stage wire only test-unit/test-integration Make targets; remaining public targets belong to T039. A selected missing suite/prerequisite returns setup exit 2, not a successful empty run.
6. Check a clean locked generation/build cycle and deliberate failing/setup cases; separate unit selection from owner/cross-service/browser/client integration. Preserve original failure if cleanup also fails.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T001-A1:** Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted.
- **T001-A2:** Isolation: a fixture with the normal installation name is rejected before connecting; a failing synthetic test returns 1 and a missing required prerequisite returns 2, leaving normal sentinel data untouched.
- **T001-A3:** Scope: no catalog feature or real credential is added by scaffolding; absent runtime features remain explicitly pending.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Use checks alongside implementation because build/visual/runtime composition must be exercised as its interfaces appear; acceptance still governs before completion. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| Local contract contribution | Owned source clauses and selected section fixtures | Verify generated/owner behavior locally; full assigned case proof remains with the downstream owner identified in the root register |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-STACK · L75](../../../05-atdd.md#f-stack-isolated-future-runtime) | Create a uniquely named disposable installation `atdd-<run UUID>` with PostgreSQL 18.6 service-owned `spoolin_core`, `spoolin_catalog`, `spoolin_media` databases, separate runtime/migrator/maintenance test roles, real Core/Catalog/Media TCP gRPC and bounded private Media volume. Build the selected Kotlin/Spring and Svelte/TypeScript/Node stack from locked images/dependencies. Use independently provisioned synthetic keys/certificates and loopback test ports; no normal database, volume, credentials or client connector may be reused. Keep each row’s baseline snapshot and fault barrier separate. This fixture does not exist yet; its implementation belongs to the next phase. |
| [F-CLOCK · L101](../../../05-atdd.md#f-clock-f-work-and-fault-timeline) | Control the observation clock at 2026-10-12T03:00:00Z and move it explicitly relative to source deadlines/retention. Use deterministic jitter draws 0, middle and upper allowed bound in rule checks; record actual random sampling in integration. Production cryptographic randomness must remain secure and is never replaced by deterministic fixture tokens. Synchronize W1/W2 and concurrent sessions with barriers at before local commit, after commit/before reply, before remote send, after remote commit/before receipt, after lease loss and before cleanup unlink. Never use elapsed sleeps as the only completion proof. F-EXPIRY specifies exact equality; logical expiry is independent of physical purge. F-WORK starts from parent operation O and deterministic child UUIDv5(parent, stable step name), generation 0, explicit max attempts, nextAttemptAt and deadlineAt. Configure READY/LEASED/DONE/EXHAUSTED/CANCELLED independently. Reconciliation sees the destination’s real receipt, not a mock call count. Restart cannot change the saved budget. Recovery reasons and operator identities are synthetic and sanitized. |
| [F-UNIT · L170](../../../05-atdd.md#f-unit-isolated-deterministic-rules) | Each Unit case constructs fresh values and the named rule only. Use a fixed clock and listed draws for ordinary time/jitter; production cryptographic randomness is never replaced by these fixtures. Actual in-process cryptography with synthetic keys is allowed without claiming durable nonce allocation or real trust distribution. Independently list expected values and error classifications; do not calculate expectations by calling the implementation under test. Unit cleanup discards in-memory values and handles, not F-STACK volumes. |

Partial implementation contributions: [ATDD-134](../../../05-atdd.md#case-atdd-134-build-the-selected-stack-from-a-clean-checkout). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T001` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

Only isolated build/unit/component validation is required here. The source case/row owners named in Traceability retain complete real-service/client proof; no empty integration selection is required for local completion. Graphical captures below still inspect a rendered component/reference under the declared contexts.

## Review and evidence

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize permission/secret boundaries, races, rollback/replay, lifetime evidence and the real owner boundary specified in this task. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Passed: 32 selected/executed, zero failed; two clean builds produced 22 matching output pairs | [Captured receipt](../../../../.ai/evidence/run-fe11bfa4897343c9b6a99a5961433a95/receipt.json), [build assertions](../../../../.ai/evidence/t001-build.json) | `913242f19daa8227d6c8688268e88da6daeec386`; reconciled to integrated inputs | Temurin 21.0.12.1+1, Node 24.21.0/npm 11.19.0, macOS arm64; isolated fixtures | `/root/foundation_review` |
| self-review | Passed; selector regressions and plugin dependency findings corrected | [Self-review](../../../../.ai/evidence/t001-round1-self-review.json) | `913242f19daa8227d6c8688268e88da6daeec386` | Claimed worker checkout | `/root/baseline_check` |
| independent-review | Passed: T001-A1/A2/A3; zero unresolved required findings | [Local review](../../../../.ai/evidence/t001-independent-review.json), [integrated review](../../../../.ai/evidence/t001-integrated-review.json) | `132af27f552aa252e24c9052ae9f368e2efd66a9`; fingerprint `b8da560398341a8a00a0a96a13a26b3a36329ea0b02863caeed8c826132c2212` | Merged source, build and project scanner configuration | `/root/foundation_review` |
| integrated gates | Passed: all 11 applicable checks, including OSV, Semgrep, Gitleaks, 30 harness and 3 JVM cases; no skipped selected tests | [Gate result](../../../../.ai/reports/1792043125255867000-50639/result.json), [integration record](../../../../.ai/evidence/integration-b75ef3426ff046afac490073fc9f3e2b/integration.json) | `132af27f552aa252e24c9052ae9f368e2efd66a9` | Tracked combined checkout; locked Node installation | `/root/foundation_review` |

The local build/unit/source-review contribution is complete. G01 acceptance remains open for T002/T003. Real service, database, browser and complete ATDD-134 proof are not claimed here.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including self-review, fresh independent review and applicable visual/interaction checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T003 — Establish private service and database ownership](T003-establish-private-service-and-database-ownership.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
