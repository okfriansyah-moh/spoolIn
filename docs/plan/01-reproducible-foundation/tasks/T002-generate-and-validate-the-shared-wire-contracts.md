---
kind: task
id: T002
group: G01
depends_on:
- T001
readiness: Ready
status: In progress
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T002 — Generate and validate the shared wire contracts

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-302 | both |
| NFR-008 | both |
| TD-C-014 | both |
| TD-C-015 | both |
| TD-C-065 | both |
| TD-BLOCK-L1127 | both |
| TD-BLOCK-L1204 | both |
| TD-BLOCK-L1307 | both |
| TD-BLOCK-L1416 | both |
| TD-BLOCK-L1501 | both |
| TD-BLOCK-L1570 | both |
| TD-BLOCK-L1684 | both |
| TD-BLOCK-L1711 | both |
| TD-BLOCK-L1747 | both |
| TD-BLOCK-L1762 | both |
| TD-BLOCK-L1816 | both |
| TD-BLOCK-L2067 | both |
| TD-BLOCK-L2097 | both |
| TD-BLOCK-L2174 | both |
| TD-BLOCK-L2221 | both |
| TD-BLOCK-L2235 | both |
| TD-BLOCK-L2276 | both |
| TD-BLOCK-L2292 | both |
| F-NAMING | both |

## Prerequisites

- [T001 — Lock the build and isolated verification harness](T001-lock-the-build-and-isolated-verification-harness.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | contracts/graphql/schema.graphql | T002 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | contracts/grpc/ | T002 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | contracts/json/ | T002 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | build-tools/ | T002 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/contracts/ | T002 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [2.4.4 Shared contracts and generated types](../../../04-spoolin-techdoc.md#244-shared-contracts-and-generated-types): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.1 GraphQL SDL](../../../04-spoolin-techdoc.md#51-graphql-sdl): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.2 Internal protobuf contracts](../../../04-spoolin-techdoc.md#52-internal-protobuf-contracts): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.4 Projection and presence crosswalk](../../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.10 MCP output schema registry](../../../04-spoolin-techdoc.md#510-mcp-output-schema-registry): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.11 JSON projections and draft schemas](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [9.1 MCP tool input schemas](../../../04-spoolin-techdoc.md#91-mcp-tool-input-schemas): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [TD-C-014 · L1105](../../../04-spoolin-techdoc.md#51-graphql-sdl) | Assemble complete GraphQL SDL once, preserving exact operations, scalars/enums/nullability and presence. Typed domain failures differ from transport errors; invalid mutations never partially apply; strict UUID/Long/DateTime/Base64 rules. |
| [TD-C-015 · L1722](../../../04-spoolin-techdoc.md#52-internal-protobuf-contracts) | Assemble one spoolin.v1 source proto with all messages, field numbers, four services, generated Java/TypeScript consumers, client/server streaming and FieldMask semantics; no generic SQL/GraphQL tunnel. |
| [TD-C-065 · L6526](../../../04-spoolin-techdoc.md#appendix-a-decision-history) | Honor accepted TD-U-001..009 and ADR01..10, target spoolin namespace/service/DB/cookie/owner renames, additive audit fields8/9 and durable draft new signatures; no historical aliases or rename of actual checkout. |
| [TD-BLOCK-L1127 · L1127](../../../04-spoolin-techdoc.md#511-scalars-and-enumerations) | 5.1.1 Scalars and enumerations — complete graphql contract, all members, constraints and field numbers |
| [TD-BLOCK-L1204 · L1204](../../../04-spoolin-techdoc.md#512-catalog-projections) | 5.1.2 Catalog projections — complete graphql contract, all members, constraints and field numbers |
| [TD-BLOCK-L1307 · L1307](../../../04-spoolin-techdoc.md#513-identity-policy-and-operation-projections) | 5.1.3 Identity, policy and operation projections — complete graphql contract, all members, constraints and field numbers |
| [TD-BLOCK-L1416 · L1416](../../../04-spoolin-techdoc.md#514-browser-input-objects) | 5.1.4 Browser input objects — complete graphql contract, all members, constraints and field numbers |
| [TD-BLOCK-L1501 · L1501](../../../04-spoolin-techdoc.md#515-browser-queries) | 5.1.5 Browser queries — complete graphql contract, all members, constraints and field numbers |
| [TD-BLOCK-L1570 · L1570](../../../04-spoolin-techdoc.md#516-browser-mutations) | 5.1.6 Browser mutations — complete graphql contract, all members, constraints and field numbers |
| [TD-BLOCK-L1684 · L1684](../../../04-spoolin-techdoc.md#517-code-example-clear-an-optional-movie-field) | 5.1.7 Code example: clear an optional movie field — complete graphql contract, all members, constraints and field numbers |
| [TD-BLOCK-L1711 · L1711](../../../04-spoolin-techdoc.md#517-code-example-clear-an-optional-movie-field-variables) | 5.1.7 Code example: clear an optional movie field variables — complete json contract, all members, constraints and field numbers |
| [TD-BLOCK-L1747 · L1747](../../../04-spoolin-techdoc.md#521-protobuf-namespace-and-imports) | 5.2.1 Protobuf namespace and imports — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L1762 · L1762](../../../04-spoolin-techdoc.md#522-shared-context-and-outcomes) | 5.2.2 Shared context and outcomes — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L1816 · L1816](../../../04-spoolin-techdoc.md#523-catalog-messages) | 5.2.3 Catalog messages — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L2067 · L2067](../../../04-spoolin-techdoc.md#524-identity-messages) | 5.2.4 Identity messages — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L2097 · L2097](../../../04-spoolin-techdoc.md#525-media-messages) | 5.2.5 Media messages — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L2174 · L2174](../../../04-spoolin-techdoc.md#526-mcp-gateway-and-policy-messages) | 5.2.6 MCP gateway and policy messages — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L2221 · L2221](../../../04-spoolin-techdoc.md#527-identityservice) | 5.2.7 IdentityService — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L2235 · L2235](../../../04-spoolin-techdoc.md#528-catalogservice) | 5.2.8 CatalogService — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L2276 · L2276](../../../04-spoolin-techdoc.md#529-mediaservice) | 5.2.9 MediaService — complete protobuf contract, all members, constraints and field numbers |
| [TD-BLOCK-L2292 · L2292](../../../04-spoolin-techdoc.md#5210-mcpgatewayservice) | 5.2.10 McpGatewayService — complete protobuf contract, all members, constraints and field numbers |

## Implementation

1. Assemble every embedded GraphQL definition, spoolin.v1 protobuf message/service and JSON registry exactly once; preserve field numbers, enums, operation names and additionalProperties rules. Use contracts/json/draft.schema.json, resolving the §2.4.8 form_draft filename discrepancy by the declared §2.4.4 tree.
2. Generate JVM protobuf stubs, Core GraphQL DTOs, frontend operation types, MCP clients and token CSS from their source artifacts; wire ordered dependencies and packaged resource locations. Do not edit derived files or maintain a second SDL.
3. Write independent scalar/presence fixtures for UUID, decimal-string Long, UTC precision, nullable optional fields, FieldMask and ordered character arrays, including literal slash content. Keep read inputs free of mutation-only parameters.
4. Compile producer/consumer compatibility and JSON local-reference closure; reject unsupported scalar/envelope values and private fields. Preserve additive audit outcome/subject and durable Core draft signatures from the current techdoc.
5. Document generated versus authored boundaries and generator limitations; compilation is contract evidence only, with live operation proof assigned to T042.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T002-A1:** Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- **T002-A2:** Presence: omitted optional edit preserves and explicit null/trimmed blank clears according to PRD/techdoc; ordered arrays retain complete names. T006 owns the ATDD-278 rule proof; T042 owns complete GraphQL/protobuf presence proof.
- **T002-A3:** Projection: an undeclared output field and out-of-range scalar are rejected without narrowing/truncation; source contracts remain unmodified.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-302](../../../05-atdd.md#case-atdd-302-reject-nonrepresentable-scalars-and-independently-bounded-envelopes) / Unit | Long9007199254740993 and signed max; GraphQL Int affected count 2147483647/2147483648; lowercase-hyphenated/malformed UUID, padded/unpaddedBase64; independent128 KiB request/result limits. | Then Long remains a decimal string with exact value And count overflow rejects safely rather than wrapping or truncating And invalid scalar encodings and independently oversized envelopes fail without partial projection Observation: Inspect literal encoded values and safe failure; PostgreSQL jsonb::text storage size is not inferred from wire JSON bytes and needs the real database case. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-NAMING · L234](../../../05-atdd.md#f-naming-coordinated-techdoc17-target) | Use SpoolIn Core/Catalog/Media for display, `spoolin_core`/`spoolin_catalog`/`spoolin_media` databases and `core`/`catalog`/`media`/`mcp` service SAN/network/metric identities. Core owns accounts, sessions, protected drafts, browser/API composition and operational records; its database is not another catalog. Core source/build is `services/core/`, `com.spoolin.core`, `:services:core` and `ops/containers/core.Dockerfile`. Each database has distinct `<database>_owner`/`_migrator`/`_runtime`/`_maintenance`/`_backup` roles preserving source-defined privileges. Generate one `contracts/grpc/src/main/proto/spoolin/v1/spoolin.proto` under package `spoolin.v1`; full methods are `/spoolin.v1.<Service>/<Method>`, and private health names are `spoolin.v1.IdentityService`/`McpGatewayService` on Core, `spoolin.v1.CatalogService` on Catalog and `spoolin.v1.MediaService` on Media. Core alone signs actors with issuer `core` and exact receiver audience/method; UI-origin MCP assertion audience is `core`, forwarded actor audience is `catalog`. GraphQL/MCP OperationOwner is `CORE`/`CATALOG`/`MEDIA`. Target root is `spoolin/` while the actual checkout path stays `movie-app`. Generic configuration keys, `appdata`, `frontend/src/app.html`, `grpc.health.v1`, MCP `app` visibility/app-only/UI-only, `mcp-app`/MCP Apps, MovieInput/createMovie/propose_movie and movie feature terms retain their spelling. Namespace/owner/issuer changes are deliberately breaking: update generated clients, allowlists/certificates, signing bindings and health expectations together. Obsolete negative fixtures are `movie.v1`, certificate SAN/issuer `app` and textual owner `APP`; none is an accepted alias. This is no live-database or checkout migration. Historical source names and qualified obligation IDs retain identity, including legacy boundary suffixes; source locators/current expectations use Core. The coordinated source is techdoc v1.10 with SHA-256 `e1c9762a64922e0e67ba70d469d18257c253958d9be20e7fc73400309172a3e5`. Previous-target compatibility concerns the supported range inside this new namespace, not obsolete aliases. No execution claim follows from document synchronization. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T002` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

Only isolated build/unit/component validation is required here. The source case/row owners named in Traceability retain complete real-service/client proof; no empty integration selection is required for local completion. Graphical captures below still inspect a rendered component/reference under the declared contexts.

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Passed: 22 selected/executed, zero failed; generated Java and strict TypeScript consumers compile | [Captured receipt](../../../../.ai/evidence/run-3682f6b9f7fc4c25b09faace61447a1d/receipt.json) | `b21d0f28a75f784f89df695ccd0125c426f833ce` | Claimed worker checkout; Temurin 21.0.12.1+1 and Node 24.21.0/npm 11.19.0 | `/root/foundation_review` |
| self-review | Passed; scalar, timestamp, generation-order and patch-field regressions corrected | [Correction review](../../../../.ai/evidence/t002-round1-self-review.json), [red](../../../../.ai/evidence/t002-round1-red.txt)/[green](../../../../.ai/evidence/t002-round1-green.txt) | `b21d0f28a75f784f89df695ccd0125c426f833ce` | Original worker evidence and generated consumers retained in its checkout | `/root/baseline_check` |
| independent-review | Passed: T002-A1/A2/A3; all four required findings resolved | [Local review](../../../../.ai/evidence/t002-independent-review.json), [integrated review](../../../../.ai/evidence/t002-integrated-review.json) | `90cfea0b86d021267be04c2d29a6a28feef69953`; fingerprint `89f6a88d0e051dab01fffd29618856be899fe0936b1232c43e21fc51e9f108c3` | Merged contracts, generators and reviewed scanner configuration | `/root/foundation_review` |
| integrated gates | Passed: all 14 applicable gates; 22 fresh contract cases; T001 regression 32/32 and 22 identical output pairs | [Gate result](../../../../.ai/reports/1792049116674865000-67882/result.json), [integration record](../../../../.ai/evidence/integration-b754013511ca4326b48991cf647eebd9/integration.json) | `90cfea0b86d021267be04c2d29a6a28feef69953` | Tracked combined checkout; source schemas and packaged SDL verified | `/root/foundation_review` |
| G01 recheck | Passed: all nine selected foundation cases; 38 required check results reconciled with zero open findings | [Feature acceptance](../../../../.ai/evidence/g01-acceptance.json), [integrated review](../../../../.ai/evidence/g01-integrated-review.json), [gate reconciliation](../../../../.ai/evidence/g01-gate-reconciliation.json) | `12e487f001b66c2f3a5802654c6928849aa47c49`; fingerprint `f1177a13efa4a4681131430fba540f92b1d9fc26e6278efbc3f6b58765d2a58f` | Tracked merged inputs; Temurin 21.0.12.1+1, Node 24.21.0/npm 11.19.0, PostgreSQL 18.6, Docker 29.8.2, macOS arm64 | `/root/foundation_review` |

The local contract contribution is complete. Live operation and complete presence proof remain with T042, the full ATDD-278 rule with T009, and PostgreSQL storage-size proof with its database case. G01’s nine foundation cases are accepted.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T003 — Establish private service and database ownership](T003-establish-private-service-and-database-ownership.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
