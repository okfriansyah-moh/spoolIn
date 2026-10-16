---
kind: task
id: T037
group: G17
depends_on:
- T002
- T003
- T008
- T011
- T029
- T026
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T037 — Emit bounded private-safe JSON diagnostics

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-339 | both |
| ATDD-339-T1-R01 | both |
| ATDD-339-T1-R02 | both |
| ATDD-339-T1-R03 | both |
| ATDD-339-T1-R04 | both |
| ATDD-339-T1-R05 | both |
| ATDD-339-T1-R06 | both |
| ATDD-339-T1-R07 | both |
| ATDD-339-T1-R08 | both |
| ATDD-339-T1-R09 | both |
| ATDD-339-T1-R10 | both |
| ATDD-339-T1-R11 | both |
| ATDD-339-T1-R12 | both |
| ATDD-340 | both |
| ATDD-340-T1-R01 | both |
| ATDD-340-T1-R02 | both |
| ATDD-340-T1-R03 | both |
| ATDD-340-T1-R04 | both |
| ATDD-340-T1-R05 | both |
| ATDD-340-T1-R06 | both |
| ATDD-340-T1-R07 | both |
| ATDD-340-T1-R08 | both |
| ATDD-340-T1-R09 | both |
| ATDD-340-T1-R10 | both |
| ATDD-340-T1-R11 | both |
| ATDD-340-T1-R12 | both |
| ATDD-341 | both |
| ATDD-342 | implements |
| D23 | both |
| TD-C-060 | both |
| TD-LOG-JSON | both |
| TD-LOG-LEVELS | both |
| TD-LOG-CONFIG | both |
| TD-LOG-CONTEXT | both |
| TD-LOG-PRIVACY | both |
| TD-LOG-SINK | both |
| TD-U-009 | both |
| TD-ROW-L6033 | both |
| TD-ROW-L6150 | both |
| TD-ROW-L6151 | both |
| TD-ROW-L6152 | both |
| TD-ROW-L6153 | both |
| TD-ROW-L6154 | both |
| TD-ROW-L6155 | both |
| TD-ROW-L6156 | both |
| TD-ROW-L6157 | both |
| TD-ROW-L6158 | both |
| TD-ROW-L6172 | both |
| TD-ROW-L6173 | both |
| TD-ROW-L6174 | both |
| TD-ROW-L6175 | both |
| TD-ROW-L6176 | both |
| F-LOG | both |
| BRAIN-D23 | both |

## Prerequisites

- [T002 — Generate and validate the shared wire contracts](../../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T008 — Enforce scoped service identity and shared admission](../../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T011 — Run fenced recovery and bounded retries](../../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.
- [T029 — Implement isolated MCP transport and connector identity](../../13-assisted-movie-creation/tasks/T029-implement-isolated-mcp-transport-and-connector-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.
- [T026 — Coordinate protective holds, attachment and cleanup](../../11-artwork-management/tasks/T026-coordinate-protective-holds-attachment-and-cleanup.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | libraries/backend-support/src/main/kotlin/com/spoolin/support/logging/ | T037 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/logback-spring.xml | T037 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/resources/logback-spring.xml | T037 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/resources/logback-spring.xml | T037 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/src/operations/logging.ts | T037 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | mcp/test/operations/logging.test.ts | T037 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/operations/logging/ | T037 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | libraries/backend-support/src/test/kotlin/com/spoolin/support/logging/ | T037 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [12.2.1 JSON log contract](../../../04-spoolin-techdoc.md#1221-json-log-contract): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [11.1 Configuration contract](../../../04-spoolin-techdoc.md#111-configuration-contract): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [13.2 Additional boundary verification](../../../04-spoolin-techdoc.md#132-additional-boundary-verification): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [D23 · L324](../../../02-PRD.md#64-project-decision-register) | Health/readiness, logs, metrics, dashboards, actionable alerts \| NFR-006 / AC-079 through AC-082 |
| [TD-C-060 · L6124](../../../04-spoolin-techdoc.md#122-metrics-alerts-and-privacy) | All application-owned physical log lines follow bounded redacted JSON/level/resource/context contract, fixed safe fields, no raw framework dumps,1,024 queue/16KiB record, bounded failure fallback/drop metrics; diagnostics never replace durable evidence. |
| [TD-LOG-JSON · L6160](../../../04-spoolin-techdoc.md#1221-json-log-contract) | **TD-LOG-JSON:** every application-owned record, including framework startup, request failures and worker diagnostics, follows this shape. Disable plain Boot banners, ANSI decoration and raw console/exception printing on those streams. Configure JVM SLF4J/Logback through a redacting JSON encoder; the MCP helper uses structured objects and the standard JSON serializer. Framework/ORM adapters pass through the same redaction and level mapping. Never enable SQL parameter, HTTP body/header or RPC-message dumps, including in debug mode. Unknown fields are dropped before serialization; their values cannot be recovered through an auxiliary dropped-values field. |
| [TD-LOG-LEVELS · L6178](../../../04-spoolin-techdoc.md#12211-level-selection-and-configuration) | **TD-LOG-LEVELS:** expected validation/conflict outcomes are not system errors. A GraphQL HTTP 200 can still have a failed domain/system outcome; completion events include that classification. Approval, publication, database recording, provider acceptance and confirmed delivery keep their existing distinct meanings. Normalization maps framework TRACE to debug, INFO to info, WARN to warning and ERROR to error. An explicitly identified unrecoverable startup failure uses fatal; the component still returns its documented failure exit/status. The unused Mekari levels notice, critical and alert are not emitted by SpoolIn's selected application profile. |
| [TD-LOG-CONFIG · L6186](../../../04-spoolin-techdoc.md#12211-level-selection-and-configuration) | **TD-LOG-CONFIG:** `LOG_LEVEL` uses §11.1 precedence, defaults to `info`, accepts only `info` or `debug`, and takes effect through a validated restart. Omitted means info; explicit empty/unknown values block readiness with a redacted correction. Both modes always emit warning/error/fatal and required lifecycle/security outcomes; neither can disable audit persistence or expose secrets. Temporary debug use ends with an operator restart back to info. Existing 10 MiB × 5 rotated-log budget per service remains; it bounds retained diagnostic files rather than promising a time span. |
| [TD-LOG-CONTEXT · L6195](../../../04-spoolin-techdoc.md#12212-correlation-ownership-and-safe-fields) | **TD-LOG-CONTEXT:** a trace identifies one execution flow; spans identify individual calls within it. Use W3C trace context through OpenTelemetry-compatible context APIs, with the resolved instrumentation dependency recorded under G-01. Core creates a trace at browser ingress; MCP creates one at connector ingress. Untrusted browser/model headers cannot choose trusted trace/actor/operation context. Trusted mTLS calls propagate validated traceparent metadata, keep the trace and create child spans; malformed context is replaced safely, never used for authorization. Do not forward arbitrary tracestate/baggage or user content. |
| [TD-LOG-PRIVACY · L6211](../../../04-spoolin-techdoc.md#12212-correlation-ownership-and-safe-fields) | **TD-LOG-PRIVACY:** construct safe fields before serialization. Exclude raw Cookie/Set-Cookie, Authorization, CSRF values, password/verifier, email/name/account/credential identifiers, lookup digests, encryption keys/nonces/ciphertext, approval challenges, catalog/draft field values, query variables, request/response bodies, original filenames, source evidence and provider URLs. No automatic entity, Throwable or protocol-message serialization. Log fixed error codes/classes; raw exception messages, causes and stack dumps are excluded. The optional error_class is a fixed allowlisted classification, not arbitrary Throwable text. Operational UUID correlation is searchable only by the operator and never becomes a metric label, credential or authority to read a receipt. |
| [TD-LOG-SINK · L6228](../../../04-spoolin-techdoc.md#12213-sinks-evidence-and-failure-behavior) | **TD-LOG-SINK:** service containers emit JSON to their managed stdout/stderr log sink; stdio MCP bridge diagnostics use stderr exclusively because stdout carries protocol messages. Human command entry points/status output are distinct from diagnostic streams. `make logs` preserves complete JSON lines with service identity inside resource, no display prefixes; parsed filters keep record boundaries and bounded tails. The operator can pretty-print a copy for reading. |
| [TD-U-009 · L6541](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes) | TD-U-009 \| JSON application logs with explicit levels, safe correlation and redaction \| §12.2.1 adopts the inspected Mekari field convention with explicit SpoolIn scope/choices; no employer SDK or infrastructure dependency, API or storage schema change |
| [TD-ROW-L6033 · L6033](../../../04-spoolin-techdoc.md#111-configuration-contract) | Logging \| LOG_LEVEL defaults info, accepts only debug/info; validated restart, same redaction at both; JSON fields/sinks in §12.2.1 \| Each service and MCP; no secret-bearing debug mode |
| [TD-ROW-L6150 · L6150](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `level` \| Lowercase enum: debug, info, warning, error or fatal; see the level table |
| [TD-ROW-L6151 · L6151](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `timestamp` \| UTC RFC3339 string ending in Z, with the available 0..9 fractional digits; represents event time, not collector time |
| [TD-ROW-L6152 · L6152](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `trace_id` \| 32 lowercase hexadecimal characters. Nonzero for active traced work; all zeros only when no trace exists, such as pre-context boot diagnostics |
| [TD-ROW-L6153 · L6153](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `span_id` \| 16 lowercase hexadecimal characters. Nonzero for an active span; all zeros when untraced. A nonzero span requires a nonzero trace |
| [TD-ROW-L6154 · L6154](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `message` \| Required fixed event summary from application code, &lt;=256 UTF-8 bytes; no interpolated user/error text |
| [TD-ROW-L6155 · L6155](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `resource` \| Required object with `service.name` (core/catalog/media/mcp), `service.version` (resolved build version), `service.namespace` (spoolin) and `service.env` (local/test); build version is a fixed &lt;=128-byte value |
| [TD-ROW-L6156 · L6156](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `attributes` \| Optional allowlisted object: event, operation, boundary, outcome, code, error_class, peer_service, route, method, duration_ms, count and existing correlation_id as applicable. Enum/string bounds &lt;=128 UTF-8 bytes except lowercase, hyphenated UUID; count/duration finite nonnegative numbers; &lt;=16 fields |
| [TD-ROW-L6157 · L6157](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `caller` \| Optional project-relative code location without workstation/user paths; omitted by default |
| [TD-ROW-L6158 · L6158](../../../04-spoolin-techdoc.md#1221-json-log-contract) | `dropped_attributes_count` \| Optional nonnegative integer when safe attributes were omitted; duplicate keys are rejected before encoding. Omitted values are never copied into `dropped_attributes` |
| [TD-ROW-L6172 · L6172](../../../04-spoolin-techdoc.md#12211-level-selection-and-configuration) | debug \| Optional bounded diagnostic detail, disabled by default \| Selected execution branch or sanitized timing; never query values or private payloads |
| [TD-ROW-L6173 · L6173](../../../04-spoolin-techdoc.md#12211-level-selection-and-configuration) | info \| Normal lifecycle and request outcome \| Service ready, request started/completed, validation rejection, stale-version conflict, successful replay |
| [TD-ROW-L6174 · L6174](../../../04-spoolin-techdoc.md#12211-level-selection-and-configuration) | warning \| Handled security denial, throttling or degraded/retryable behavior requiring investigation \| CSRF denial, quota rejection, retry scheduled, uncertain external effect |
| [TD-ROW-L6175 · L6175](../../../04-spoolin-techdoc.md#12211-level-selection-and-configuration) | error \| Failed request/task or integrity/security failure \| Dependency unavailable, exhausted recovery, encryption authentication failure; outcome/code remain explicit |
| [TD-ROW-L6176 · L6176](../../../04-spoolin-techdoc.md#12211-level-selection-and-configuration) | fatal \| This component cannot start or must stop safely \| Invalid required key/config/schema prerequisites; logging this event alone does not terminate a process |
| [BRAIN-D23 · L92](../../../01-brainstorming-result.md#4-decision-register) | D23 \| Committed \| Health/readiness checks, correlated logs, metrics, dashboards, and actionable alerts; numerical SLOs and any SLA remain open |

## Implementation

1. Implement allowlisted UTF-8 one-object-per-physical-line JSON with required UTC timestamp/lowercase severity/fixed message/resource identity and exact optional context schema. Disable Boot banners, ANSI decoration and raw framework stack/payload dumps on application diagnostic streams.
2. Validate LOG_LEVEL info/debug by configuration presence/restart and source aliases; both modes preserve security/error records and identical privacy/bounds. Logical outcomes drive severity independently of HTTP200.
3. Start and close trusted trace/span context at boundaries and fresh worker attempts while preserving separate UUID correlation/operation IDs. Test ingress distrust, pooled thread/task reuse, cancellation and no-context boot.
4. Encode safe fields before serialization, cap records16KiB/queue1,024, and handle CR/LF/quotes/nested/dropped values without leaking secrets. Keep bridge diagnostics on stderr and protocol stdout clean.
5. Implement bounded sinks/rotation/drop signals/failure fallback and actual JVM/MCP capture. Missing log completion never overrides receipt/audit/work truth; native infrastructure and human command streams retain explicit scope.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T037-A1:** Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug.
- **T037-A2:** Context: concurrent/cancelled requests cannot inherit each other's identity, and restarted work has fresh trace with retained safe operation link.
- **T037-A3:** Failure: full/unavailable sink remains bounded, exposes truthful drop/failure signals and does not fabricate rollback or corrupt MCP protocol output.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-339](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) / Unit | F-LOG controlled event time 2026-10-13T12:00:00.123456789Z, trace a1ac0334fcb72ad1513859d416b5ff26/span b819e2324b94eacd, Core/test/example-build resource and correlation UUID 8a30868c-2fbe-4093-aaab-e054c4a1a7c9. Each invalid field/byte/optional-attribute row starts from a complete valid record. Use synthetic private canaries and duplicate-key input pairs before map construction; malformed JSON strings are inputs, never emitted record templates. | Then valid output is one complete LF-terminated UTF-8 JSON object with independently expected types and values And invalid required data yields only a small fixed logger-contract error or count And rejected optional fields and all private values cannot return through messages or dropped-value data And encoding never produces a forged second physical line or truncates serialized JSON Observation: Independent rule \| Input \| Expected observable output \| \| --- \| --- \| --- \| \| Valid baseline \| info; fixed Request completed summary; the named clock/context/resource \| One compact object plus LF; decoded timestamp exactly 2026-10-13T12:00:00.123456789Z; trace/span/correlation preserved in separate fields; required resource has core/example-build/spoolin/test \| \| UTC precision \| Controlled UTC instants with0,3,6,9 available fractional digits; timezone-free or non-UTC timestamp is a separate invalid input \| Valid UTC representation ends Z, preserving available precision/event time; invalid required timestamp produces safe contract error/count \| \| Untraced boot \| No active context \| trace_id=32 zero characters and span_id=16 zero characters; no fabricated active span \| \| Trace shape \| Independently31/32/33 hex and15/16/17 hex, uppercase/nonhex, nonzero span with zero trace \| Valid exact lowercase 32/16 only; invalid required tuple yields safe contract error/count. Boundary regeneration is tested separately in341 \| \| Message byte bound \| Fixed code summaries at255/256/257 UTF-8 bytes, including multibyte text \| 255/256 retained unchanged;257 yields safe contract error/count, never truncated or echoed. User-supplied message text never replaces fixed summary \| \| Resource \| Omit required name/version/namespace/env; wrong type or unknown service/env; build version127/128/129 bytes \| Complete selected resource only; valid 128 byte version preserved; invalid required resource yields safe contract error/count \| \| Optional map \| Allowlisted typed fields; string127/128/129 bytes; finite count/duration 0 and fractional duration; negative/NaN/infinity or wrong types \| Valid bounded values preserved. Invalid optional fields omitted and safely counted; no array coercion, nonfinite JSON or private fallback. Lowercase, hyphenated UUID is validated independently \| \| Optional keys/cap \| Unknown key, repeated key/value pairs before map construction, &gt;16 supplied fields \| Unknown/excess fields omitted/countable; duplicate keys rejected before encoding rather than last-write-wins. Actual emitted attributes &lt;=16; rejected values absent from all auxiliary fields \| \| Private/exception input \| Each F-PRIVACY category in nested attrs, raw exception message/cause/stack/entity/protocol object and user-shaped keys \| Fixed safe summary/code/allowlisted error_class only; no auto serialization, private key/value, dropped_attributes list, SQL/body/header dump or raw exception text \| \| Caller \| Default absent; optional valid project-relative location; absolute workstation/user path \| Default omitted; valid code-only location permitted; unsafe optional location omitted/countable without echoing path \| \| Escaping \| Fixed trusted code-summary fixture with CR/LF/quote/backslash/control characters; malicious input separately rejected \| JSON serializer escapes characters; precisely one physical object/LF. Independent parser recovers the trusted fixture exactly; rejected hostile values never become a line/key \| \| Encoded bound \| Valid schema-sized records and synthetic oversized required/optional inputs at 16383/16384/16385 encoded bytes where representable \| Every emitted record &lt;= 16,384 bytes and parses completely; optional omission precedes encoding, invalid/oversized event becomes small safe contract error/count. No fabricated valid record is required if narrower field bounds make a target size unrepresentable \| Compare decoded keys/types/values and independently measured UTF-8 bytes against this hand-defined table, using a strict duplicate-aware parser independent of the encoder and no key-order requirement. Probe all synthetic private categories, not only a plaintext substring in final output: capture projection/serialization calls transiently and assert forbidden objects are never serialized. Retain only booleans/counts, never raw canaries. This proves isolated construction and failure classification; real framework bypasses, streaming, context lifetime and queue/storage durability require 341/342. Safe drop/counting is not a lossless diagnostic promise. |
| [ATDD-340](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) / Unit | Controlled event kinds and framework TRACE/INFO/WARN/ERROR; LOG_LEVEL absent, info, debug, empty, whitespace, DEBUG, warning, error, false and unknown. Environment/file/default conflicts use explicit sources, with no live restart. Treat domain result/code as independent of HTTP200; provider delivery states come from selected owner truth. | Then expected validation and conflict are info while handled denial or degradation is warning And failed work is error and unrecoverable startup is fatal without claiming the logger terminates the process And omitted configuration is info, valid debug enables bounded detail, and invalid configuration blocks readiness And both modes retain required outcomes and the same private-data exclusions Observation: Independent row \| Controlled input \| Expected rule result \| \| --- \| --- \| --- \| \| Normal lifecycle/domain \| ready/start/completed, validation rejection, stale conflict, successful replay \| info; correct fixed outcome/code; rejection cannot masquerade as success \| \| Handled/degraded \| CSRF denial, quota rejection, retry scheduled, uncertain external effect \| warning; distinct denial/retry/uncertain outcome/code \| \| System/security/integrity \| dependency unavailable, exhausted work, encryption authentication failure, even HTTP200 \| error; source-selected failed outcome/code, never info because HTTP status is200 \| \| Unrecoverable prerequisite \| invalid required key/config/schema \| fatal classification; no simulated process termination claim; later command case proves failed exit/readiness \| \| Optional detail \| permitted execution/timing diagnostic \| debug; no private payload in either mode \| \| Framework mapping \| TRACE, INFO, WARN, ERROR \| debug, info, warning, error exactly; unrecoverable startup explicitly fatal; no emitted warn/TRACE/notice/critical/alert alias \| \| Default \| LOG_LEVEL absent in env/file \| info; debug detail suppressed; required info/warning/error/fatal outcomes retained \| \| Selected debug \| Exact debug \| permitted debug plus all required info/warning/error/fatal; unchanged redaction \| \| Source precedence \| env info/file debug; env debug/file info; env absent/file debug \| info; debug; debug respectively. Default used only when both absent \| \| Invalid effective input \| empty/whitespace/DEBUG/warning/error/false/unknown; invalid env with valid file \| Validation fails readiness with safe setting name/correction, no value echo/fallback. Invalid selected source is not treated as absent \| \| Application timing \| Validated info→debug→info revisions \| Rule yields correct effective configuration per restart revision; isolated rule cannot prove live restart/hot-reload enforcement \| \| Evidence meaning \| APPROVED vs published; RECORDED vs provider accepted vs confirmed delivery \| Preserve distinct fixed classifications/outcome values; logging never upgrades authority or delivery truth \| Use independently enumerated expected pairs, a memory emission spy and explicit event/result inputs; do not calculate expectations by calling the production classifier. Configuration validation and encoder success do not prove failed startup exits, cross-service readiness/restart, actual SLF4J/Node aliases, queue delivery or persisted audit.342 and218 supply those boundaries. No input mode may enable bodies/credentials/rawThrowable dumps, disable audit or claim same-origin/Cookie protection from logging alone. |
| [ATDD-341](../../../05-atdd.md#case-atdd-341-keep-trace-context-scoped-to-real-requests-and-recovery) / Integration | F-LOG; separately reset browser→Core→Catalog/Media and connector→MCP→Core flows; valid trusted00-a1ac0334fcb72ad1513859d416b5ff26-b819e2324b94eacd-01, missing/malformed/zero/uppercase/oversized traceparent and user-controlled tracestate/baggage. Concurrent A/B flows synchronized at dispatch/response/cancel/thread-reuse; durable child work original correlation UUID retained over actual worker crash/restart. Context IDs observed only transiently and recorded without private canaries. | Then Core and MCP create trusted roots and internal mTLS child calls keep the trace with distinct child spans And malformed or hostile context cannot choose actor, operation, receipt access or inherited private state And reused threads and async tasks carry only their current request context And each recovery attempt has a fresh trace while the original durable correlation UUID remains searchable Observation: Capture actual boundary metadata and JSON records from Core, Catalog, Media and MCP, using independent traceparent/record parsing. For a trusted flow compare the same nonzero 32 hex trace across parent/child boundaries, distinct valid 16 hex child spans and unchanged existing correlation UUID; no UUID is cast into a trace ID or added to database/protobuf fields. At untrusted browser/model ingress, send a syntactically valid attacker-chosen trace ID as well as malformed rows: the emitted trusted root must not equal the attacker choice, and forbidden tracestate/baggage/private values must not forward. Trusted malformed context is safely regenerated while original identity/authorization checks still decide the operation; a valid trace grants no receipt authority. Coordinate A/B overlap, cancellation/timeout/exception and immediate reuse of the actual pooled thread/async continuation. Inspect every start/completion/finally path: B must not inherit A's trace/span, correlation or account projection, and an untraced boot event alone may use zero IDs. Do not use zero traces for active work or insist unrelated flows share a root. On actual worker crash/restart, compare retained work/receipt/attempt evidence and original correlation UUID from fresh database readers; the resumed attempt emits a new trace and properly scoped spans without resetting generation/budget/effect identity. Use existing destination receipt/reconciliation cases for whether a send repeats; log counts alone cannot prove exactly-once effects. User-facing error stays existing safe correlationId; no added public trace field or authentication source is inferred. Trace/UUID must not become metric labels or expose private receipt data. Retain each record's emitting service resource; outbound peer is separate. Test all info/debug modes and safe early/failed paths with F-PRIVACY absence, strict source codes and actual framework/context adapters. No external tracing collector or paid service is required; resolved instrumentation/version and actual context observations are future execution evidence. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-339-T1-R01 · L13989](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Valid baseline; info; fixed Request completed summary; the named clock/context/resource; One compact object plus LF; decoded timestamp exactly 2026-10-13T12:00:00.123456789Z; trace/span/correlation preserved in separate fields; required resource has core/example-build/spoolin/test |
| [ATDD-339-T1-R02 · L13990](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | UTC precision; Controlled UTC instants with0,3,6,9 available fractional digits; timezone-free or non-UTC timestamp is a separate invalid input; Valid UTC representation ends Z, preserving available precision/event time; invalid required timestamp produces safe contract error/count |
| [ATDD-339-T1-R03 · L13991](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Untraced boot; No active context; trace_id=32 zero characters and span_id=16 zero characters; no fabricated active span |
| [ATDD-339-T1-R04 · L13992](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Trace shape; Independently31/32/33 hex and15/16/17 hex, uppercase/nonhex, nonzero span with zero trace; Valid exact lowercase 32/16 only; invalid required tuple yields safe contract error/count. Boundary regeneration is tested separately in341 |
| [ATDD-339-T1-R05 · L13993](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Message byte bound; Fixed code summaries at255/256/257 UTF-8 bytes, including multibyte text; 255/256 retained unchanged;257 yields safe contract error/count, never truncated or echoed. User-supplied message text never replaces fixed summary |
| [ATDD-339-T1-R06 · L13994](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Resource; Omit required name/version/namespace/env; wrong type or unknown service/env; build version127/128/129 bytes; Complete selected resource only; valid 128 byte version preserved; invalid required resource yields safe contract error/count |
| [ATDD-339-T1-R07 · L13995](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Optional map; Allowlisted typed fields; string127/128/129 bytes; finite count/duration 0 and fractional duration; negative/NaN/infinity or wrong types; Valid bounded values preserved. Invalid optional fields omitted and safely counted; no array coercion, nonfinite JSON or private fallback. Lowercase, hyphenated UUID is validated independently |
| [ATDD-339-T1-R08 · L13996](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Optional keys/cap; Unknown key, repeated key/value pairs before map construction, &gt;16 supplied fields; Unknown/excess fields omitted/countable; duplicate keys rejected before encoding rather than last-write-wins. Actual emitted attributes &lt;=16; rejected values absent from all auxiliary fields |
| [ATDD-339-T1-R09 · L13997](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Private/exception input; Each F-PRIVACY category in nested attrs, raw exception message/cause/stack/entity/protocol object and user-shaped keys; Fixed safe summary/code/allowlisted error_class only; no auto serialization, private key/value, dropped_attributes list, SQL/body/header dump or raw exception text |
| [ATDD-339-T1-R10 · L13998](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Caller; Default absent; optional valid project-relative location; absolute workstation/user path; Default omitted; valid code-only location permitted; unsafe optional location omitted/countable without echoing path |
| [ATDD-339-T1-R11 · L13999](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Escaping; Fixed trusted code-summary fixture with CR/LF/quote/backslash/control characters; malicious input separately rejected; JSON serializer escapes characters; precisely one physical object/LF. Independent parser recovers the trusted fixture exactly; rejected hostile values never become a line/key |
| [ATDD-339-T1-R12 · L14000](../../../05-atdd.md#case-atdd-339-encode-bounded-private-safe-json-log-records) | Encoded bound; Valid schema-sized records and synthetic oversized required/optional inputs at 16383/16384/16385 encoded bytes where representable; Every emitted record &lt;= 16,384 bytes and parses completely; optional omission precedes encoding, invalid/oversized event becomes small safe contract error/count. No fabricated valid record is required if narrower field bounds make a target size unrepresentable |
| [ATDD-340-T1-R01 · L14045](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Normal lifecycle/domain; ready/start/completed, validation rejection, stale conflict, successful replay; info; correct fixed outcome/code; rejection cannot masquerade as success |
| [ATDD-340-T1-R02 · L14046](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Handled/degraded; CSRF denial, quota rejection, retry scheduled, uncertain external effect; warning; distinct denial/retry/uncertain outcome/code |
| [ATDD-340-T1-R03 · L14047](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | System/security/integrity; dependency unavailable, exhausted work, encryption authentication failure, even HTTP200; error; source-selected failed outcome/code, never info because HTTP status is200 |
| [ATDD-340-T1-R04 · L14048](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Unrecoverable prerequisite; invalid required key/config/schema; fatal classification; no simulated process termination claim; later command case proves failed exit/readiness |
| [ATDD-340-T1-R05 · L14049](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Optional detail; permitted execution/timing diagnostic; debug; no private payload in either mode |
| [ATDD-340-T1-R06 · L14050](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Framework mapping; TRACE, INFO, WARN, ERROR; debug, info, warning, error exactly; unrecoverable startup explicitly fatal; no emitted warn/TRACE/notice/critical/alert alias |
| [ATDD-340-T1-R07 · L14051](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Default; LOG_LEVEL absent in env/file; info; debug detail suppressed; required info/warning/error/fatal outcomes retained |
| [ATDD-340-T1-R08 · L14052](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Selected debug; Exact debug; permitted debug plus all required info/warning/error/fatal; unchanged redaction |
| [ATDD-340-T1-R09 · L14053](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Source precedence; env info/file debug; env debug/file info; env absent/file debug; info; debug; debug respectively. Default used only when both absent |
| [ATDD-340-T1-R10 · L14054](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Invalid effective input; empty/whitespace/DEBUG/warning/error/false/unknown; invalid env with valid file; Validation fails readiness with safe setting name/correction, no value echo/fallback. Invalid selected source is not treated as absent |
| [ATDD-340-T1-R11 · L14055](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Application timing; Validated info→debug→info revisions; Rule yields correct effective configuration per restart revision; isolated rule cannot prove live restart/hot-reload enforcement |
| [ATDD-340-T1-R12 · L14056](../../../05-atdd.md#case-atdd-340-select-log-severity-and-validate-logging-configuration) | Evidence meaning; APPROVED vs published; RECORDED vs provider accepted vs confirmed delivery; Preserve distinct fixed classifications/outcome values; logging never upgrades authority or delivery truth |
| [F-LOG · L117](../../../05-atdd.md#f-log-selected-diagnostic-evidence) | Use the techdoc 1.11 log contract and Mekari standard v26 with independent strict JSON/UTF-8-byte observations. Selected application emitters are Core, Catalog, Media and MCP; native infrastructure/protocol and human command streams retain their declared separate scopes. Required fields, info/debug configuration, five severity values,16 KiB record / 1,024 queue bounds, safe optional omissions and 10 MiB × 5 rotation are acceptance, not measured runtime evidence. Correlation UUID and trace/span IDs have separate formats/purposes. Root context originates at trusted ingress, child context over trusted mTLS and recovery gets a fresh trace joined by original work UUID. No collector/paid SDK/backend is invented. Use fixed trusted summaries/enumcodes and synthetic F-PRIVACY canaries; the decoder/projection spy may inspect raw inputs only transiently and artifacts retain only redacted classifications/counts. Field allowlisting precedes JSON encoding, so escaped private text is still forbidden. Simulate unit clock/context/config sources only for 339/340; 341/342 require actual logger/framework, transport, worker, private exporter and owner persistence. Log drops/absence/start/completion counts cannot stand in for transactional operation/audit truth. Resolve actual dependency versions and resource/sink evidence under G-01/G-05 before execution. |

Partial implementation contributions: [ATDD-342](../../../05-atdd.md#case-atdd-342-enforce-json-diagnostics-and-bounded-sink-failure-behavior). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T037` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T037` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| boundary-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T038 — Expose logical metrics, dashboards and alert episodes](T038-expose-logical-metrics-dashboards-and-alert-episodes.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
