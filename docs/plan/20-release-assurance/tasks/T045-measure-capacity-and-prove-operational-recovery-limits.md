---
kind: task
id: T045
group: G20
depends_on:
- T038
- T039
- T041
- T042
- T043
readiness: Ready
status: Not started
risk: high
test_strategy: alongside
visual_review: not-applicable
---

# T045 — Measure capacity and prove operational recovery limits

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Benchmark: each run reports all offered/admitted/completed/throttled/error counts and independently computed p95 at the stated workload with active rate limits. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-211 | both |
| SC-07 | both |
| NFR-003 | both |
| D18 | both |
| AC-070 | both |
| TD-C-043 | both |
| TD-C-059 | verifies |
| TECH-G-05 | both |
| V-OPS | both |
| F-BENCHMARK | both |
| ATDD-G-05 | both |
| BRAIN-D18 | both |

## Prerequisites

- [T038 — Expose logical metrics, dashboards and alert episodes](../../17-operational-evidence/tasks/T038-expose-logical-metrics-dashboards-and-alert-episodes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard.
- [T039 — Implement the seven reproducible lifecycle commands](../../18-local-operations/tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps.
- [T041 — Verify migrations, purge and coordinated restore](../../19-protected-recovery/tasks/T041-verify-migrations-purge-and-coordinated-restore.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change.
- [T042 — Reconcile every real API and RPC boundary row](T042-reconcile-every-real-api-and-rpc-boundary-row.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Matrix: all28 storage families/260 column rows plus36 GraphQL and45 RPC rows have their complete physical, owner, projection, retention, allowed/denied/presence/deadline/error/side-effect results. ResolveActor remains deny-only on selected network callers.
- [T043 — Verify policy, authorization and injection across complete flows](T043-verify-policy-authorization-and-injection-across-complete-flows.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Authorization: Visitor/Viewer/forged/cross-owner callers obtain no restricted data/effects through any real boundary.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/performance/ | T045 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/operations/release/ | T045 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [7.4 Index and query-plan obligations](../../../04-spoolin-techdoc.md#74-index-and-query-plan-obligations): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.1 Database access and resource budgets](../../../04-spoolin-techdoc.md#121-database-access-and-resource-budgets): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.3 Retention, purge and backup runbooks](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.4 Metric contracts and alert expressions](../../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [14. Technical dependencies and release criteria](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [SC-07 · L108](../../../02-PRD.md#4-goals-and-success-criteria) | Establish justified performance expectations \| Techdoc defines workload, measurement, limits, and operational targets \| Baselines unknown; no invented SLO, coverage percentage, or SLA |
| [NFR-003 · L263](../../../02-PRD.md#62-quality-and-delivery-requirements) | Saved data/artwork persist through restarts; committed persistence safeguards remain \| D18/D19 Committed \| AC-069 through AC-071 |
| [D18 · L321](../../../02-PRD.md#64-project-decision-register) | JPA/Hibernate and justified parameterized native SQL \| NFR-003 / AC-069, AC-070; techdoc |
| [AC-070 · L862](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify applicable native SQL parameterization, joins/order/pagination, mapping, plans, and write safeguards against the chosen engine \| NFR-003; technical design |
| [TD-C-043 · L5328](../../../04-spoolin-techdoc.md#74-index-and-query-plan-obligations) | Exact indexes and native typed bindings; measure EXPLAIN ANALYZE BUFFERS on seed and100k titles, no N+1/unbounded responses, limiter-active benchmark quotas/reporting, actual constraints vs owner invariants; bounded purge/nonce/rate queries. |
| [TD-C-059 · L6085](../../../04-spoolin-techdoc.md#12-operations-observability-and-recovery) | Own-DB least privilege roles/TLS/SCRAM, append-only audit/runtime restrictions and measured pool/memory/disk/processor budgets; performance budgets are targets pending measured host evidence. |
| [TECH-G-05 · L6521](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria) | G-05 operational proof \| Verify seven command contracts, isolated test data, selected notification evidence, TLS/keys, graceful/forced restart, disk targets and coordinated restore \| Local operational acceptance; operator owner unassigned |
| [V-OPS · L6397](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-OPS \| JSON log parsing/level/config/redaction/context/sink checks in §12.2.1 plus isolated Make commands including deliberate failures, clean setup and rerun, metric/alert rules, HTTP 200 domain failures, logs/correlation redaction, graceful/forced restart, disk pressure, backup and full restore with keys/artwork; assert truthful exits and state |
| [BRAIN-D18 · L89](../../../01-brainstorming-result.md#4-decision-register) | D18 \| Committed \| Spring Data JPA/Hibernate for ordinary persistence; parameterized native SQL for complex joins when it improves clarity or query control |

## Implementation

1. Execute F-BENCHMARK on a recorded host/build/config:60s warmup,three independent five-minute runs,10 equal-class readers and separate metadata writer, with positive isolated raised quotas and enforcement enabled.
2. Record offered/admitted/completed/throttled/error counts, per-run nearest-rank p95 and source-selected payload/query mix. A fast rejected workload cannot satisfy admitted performance; keep100k-title query plans/stress separate from100/100 seed checks.
3. Measure independent120s warm-start and10s artwork processing targets, pools/queue/backpressure/disk/memory limits, bounded shutdown and forced-crash recovery under the specified workloads.
4. Capture EXPLAIN ANALYZE BUFFERS for consequential queries, constrained concurrency and noN+1/unbounded reads; explain deviations without changing targets silently.
5. Reconcile operational G-05 evidence including commands,TLS/keys,selected notification mode,receiver outage,protected restore and measured RPO/RTO. Record targets as measured pass/failure or unverified, never as an SLA.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T045-A1:** Benchmark: each run reports all offered/admitted/completed/throttled/error counts and independently computed p95 at the stated workload with active rate limits.
- **T045-A2:** Resources: overload is rejected before unbounded allocation, normal unrelated metadata remains usable during Media faults, and persistence/recovery survives forced stop.
- **T045-A3:** Claims: targets without reproducible measurement remain unverified; selected external provider claims require actual evidence, database recording is identified accurately.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Use checks alongside implementation because build/visual/runtime composition must be exercised as its interfaces appear; acceptance still governs before completion. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-211](../../../05-atdd.md#case-atdd-211-measure-proposed-budgets-under-an-explicit-workload) / System | F-BENCHMARK: approved 60 s warmup, three 5-minute runs, 10 readers/five equal read classes, separate one-writer/three equal update classes;200-title baseline and 100k-title stress fixture. | Then each run reports nearest-rank p95 against documented query300ms and metadata-write500ms budgets And warm startup120s and processing10s are measured at their own boundaries And raw samples, errors/timeouts, payloads, cold behavior and100k-title plans/stress are reported without a deployment SLO claim Observation: Use F-BENCHMARK exactly. Retain per-run timing samples, payload sizes/statuses/errors/timeouts and nearest-rank calculation independently from source histograms. Compare each declared-host 200-title budget without pooling runs; record 100k-title plans/stress separately and do not call these measured deployment SLOs. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-BENCHMARK · L228](../../../05-atdd.md#f-benchmark-approved-local-measurement-protocol) | Record declared host CPU/RAM/OS/architecture, locked build, source/seed hashes, payload sizes and normal service readiness. Baseline is exactly200titles(100movies/100TV), independent fixed requests and current source-selected resource limits. Before each isolated baseline run set validated READ and MUTATION quota profiles to1000000requests/60s, record source/credential/profile/revision and admit every request through the normal limiter; production defaults remain unchanged. A quota-dominated run is invalid service-budget evidence; retain offered/admitted/completed counts and every429/error. Run three independently reset repetitions with60 s warmup excluded and 5 min measured time. Ten readers use closed-loop requests with no think-time; cycle equal classes per reader: unfiltered title list(first20), rare literal substring(one known title), common literal substring(many known titles), type+two-genres+year/month filter, title detail. Hand-check each fixed result scope against the seeded fixture. Run a separate one-writer scenario cycling movie synopsis, person biography and credit character/role-compatible updates equally, with current versions/original intent UUIDs; no password/media/provider work contributes to metadata latency. Reset state before each repetition and wait on observable readiness, not arbitrary sleeps. Retain completed request durations measured by monotonic clock at the Core boundary, including typed domain outcomes; record transport errors/timeouts separately with elapsed/censoring info, never discard failed requests to improve p95. For each run independently sort n samples and take sample ceil(0.95*n), one-based; no pooled p95. Compare queryp95&lt;300 ms at 10readers and metadata-writep95&lt;500 ms to prospective local budgets and report failures plainly. Independently time three warm-image startup runs against 120 s (overall readiness 300 s remains a different operational limit), and independently verified accepted static JPEG/PNG/WebP processing against 10 s including near selected resource boundaries. Record cold runs separately. Synthetic100k-title fixture and source-required rare/common/no-filter/multi-genre/unknown/private/worker/purge EXPLAIN(ANALYZE, BUFFERS) support plan/stress reporting; no extra scale-out throughput or deployment SLO is selected. No measurements have been performed during authoring. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| boundary-tests | `bash ops/bin/test-integration.sh --task T045` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- Hand the owned outputs to [T046 — Prepare the reviewed repository and assessment handoff](T046-prepare-the-reviewed-repository-and-assessment-handoff.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
