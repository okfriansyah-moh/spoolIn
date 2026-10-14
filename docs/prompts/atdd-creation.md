# Create the SpoolIn acceptance specification

Use [$atdd-creation](/Users/mekari/.codex/skills/atdd-creation/SKILL.md) to create or update **exactly one project deliverable: `docs/05-atdd.md`**. Read the installed orchestrator and its required references. This file is the invocation prompt; it is not the acceptance specification.

The delivery sequence is **PRD + brainstorming context + design system + techdoc → ATDD → dependency-ordered implementation tickets → implementation in chunks**. This run authors the ATDD only. Keep executable tests, application changes, tickets and deployment work for their separately requested phases. Supporting skills operate within this document scope. Preserve all governing sources and this prompt; keep temporary inventories, source snapshots, previous-document copies and checker reports outside project deliverables.

## 1. Read and reconcile the governing sources

Read the complete current versions, including referenced tables and appendices:

- [PRD](../02-PRD.md): source/status authority, actors, permissions, lifecycle, input/artwork policies, every FR/NFR/AC clause, engineering handoff and open decisions.
- [Brainstorming context](../01-brainstorming-result.md): distinguish commitments and accepted decisions from working designs and recommendations.
- The complete [design-system handoff](../design-system/index.md): [specification](../03-design-system.md), [components](../design-system/components.md), [coverage](../design-system/coverage.md), [machine-readable coverage](../design-system/coverage.json), [tokens](../design-system/tokens.json), [contrast evidence](../design-system/contrast.json), [verification](../design-system/verification.md) and [brand history](../design-system/brand-history.md).
- The linked [mockup gallery](../design-system/mockup/index.html), all relevant flow/state examples, scripts, styles and asset/provenance records. Inventory the complete state handoff, including backend and security responsibilities. Inspect rendered interactions when needed and tools permit. Mockup fixtures establish presentation and interaction examples, not functioning persistence, authorization or provider evidence.
- The current [technical document](../04-spoolin-techdoc.md): architecture and frontend/backend responsibilities; all GraphQL, HTTP, gRPC and MCP contracts; database constraints; transactions; durable work; security; seed/migration behavior; configuration; commands; operations; verification families; decisions; traceability; source fingerprints and release dependencies.
- The [techdoc invocation prompt](techdoc-creation.md), including its later approved engineering constraints and source overrides. Treat its instruction to stop before ATDD as the boundary of that earlier phase, not a prohibition on this now-requested ATDD phase. Do not execute that prompt or regenerate the techdoc.

Inspect applicable repository instructions and any implementation/configuration that exists. The project may still contain only documentation. Use the adopted **SpoolIn** identity and terminology. Derive the language, frameworks, protocols, deployment model and exact behavior from the governing sources; do not import defaults from skill examples. Do not run startup commands, generate code or treat existing implementation as authority over approved acceptance.

Current explicit user decisions take precedence. The PRD owns product behavior, the adopted design system owns presentation/interaction contracts, and the technical document owns the selected engineering contracts within those commitments. Carry forward later approved overrides recorded in the techdoc and its invocation prompt without reopening them. Superseded recommendations and unapproved mockup proposals do not override accepted requirements. Resolve known precedence and document it; expose genuine contradictions or missing consequential outcomes with affected source IDs, a precise decision needed and dependent cases. Continue authoring independent cases.

## 2. Inventory obligations independently before drafting

Record current source versions and exact content hashes. Extract an independent inventory directly from all governing inputs before writing cases. Preserve source IDs and split compound clauses into stable references where one mapping would conceal an untested outcome. Include all relevant PRD criteria, design states, operation/permission matrices, database invariants, technical verification families, configuration policies and release obligations. Derive counts from the current documents; do not hardcode earlier totals.

Classify obligations as required, explicitly excluded or deferred with a source-grounded reason. Missing expected results remain visible gaps. Inferred risk scenarios must identify their source context and test an established invariant; do not invent new product guarantees. Record precise missing thresholds, workloads, deadline semantics or provider/client prerequisites instead of selecting arbitrary numbers.

Before final validation, re-extract obligations from the sources in a fresh pass and reconcile omissions. Compare the document against this source inventory, not just against identifiers already present in generated cases. Every identified in-scope obligation needs adequate scenario assertions or an explicit unresolved gap. Shared cases may cover several obligations only when their assertions demonstrate each one.

## 3. Apply comprehensive engineering coverage

Treat these as required coverage families for this application, with individual checks activated by the authoritative contracts. A justified source-based exclusion is permitted; lack of time, skill availability or implementation is not a scope exclusion.

| Coverage area | Required depth |
| --- | --- |
| Product and UI | Catalog discovery, search/filter/order/pagination, Visitor/Viewer/Maintainer permissions, registration and sessions, forms and validation, lifecycle/archive/deletion, people and credits, artwork, empty/loading/error/conflict states, responsive behavior, accessibility and account-isolated recoverable drafts. |
| Contracts | Allowed and denied operations at every authoritative boundary; omitted versus present/null/cleared fields; validation and normalization; output projections; error codes and safe payloads; compatibility; total deadlines, cancellation and uncertain completion. Cover browser, GraphQL, HTTP, internal gRPC and MCP mappings where specified. |
| Data integrity | Constraints and cross-record invariants, ownership, transaction rollback, mutation/audit atomicity, ordering and pagination under change, concurrent writes and stale versions, migration compatibility/interruption, and versioned seed preservation across repeated and concurrent startup. |
| Reliability | Duplicate requests versus duplicate data, request-key/payload binding, lost replies and uncertain outcomes, durable work, retry ownership and bounded budgets, persisted attempts, competing workers and leases, crashes, partial completion, restart recovery and exhausted work. |
| Security and privacy | Session lifecycle, authoritative authorization, private account data, injection and output encoding, upload content/resource/path protections, cryptographic envelopes and nonce behavior, key rotation/re-encryption/retirement, safe failures, and redaction of secrets and sensitive payloads from errors, logs, metrics and traces. |
| Operations and performance | Every documented startup/stop/restart/status/log/test command contract, dependency readiness and failure exits, isolation of tests from app data, graceful shutdown, resource bounds/backpressure, source-defined workloads and performance targets, metrics and actionable alerts, provider delivery, backup and restore including protected data and keys. |
| MCP and external providers | Trustworthy human approval of exact content, identity/content binding, changed-content approval invalidation, replay prevention, uncertain publication reconciliation, hostile retrieved/model content, least privilege, provenance and private-data limits. Require separate real-client behavior/evidence for ChatGPT and Claude and real provider delivery where contractual; label simulation/local sinks explicitly. |
| Coverage integrity | Requirement → cases and case → source clauses; explicit gaps and justified exclusions; stable IDs and retired-case history; planned cases separated from actual execution evidence, with revisions, environments and freshness. |

Use boundary analysis, equivalence classes, decision tables, state transitions, role matrices, fault timelines and meaningful combinations. **Broad coverage means distinct rules, boundaries, transitions and failure modes—not inflated counts of near-identical cases.** Keep separate cases when the enforcing boundary, state transition, injected failure or expected result differs materially. A negative-only rule does not require an artificial positive case.

Challenge applicable scenarios through the orchestrator's specialist routes: contracts, threats/security requirements, migrations, idempotency/recovery, configuration, accessibility, determinism, testability and source consistency. Keep the parent output contract and single-document scope. The core already combines Mekari, BMAD, QA Test Planner, Saleem QA, Test Master and Superpowers principles; do not install separate upstream packages or replace existing TDD behavior.

## 4. Write one readable acceptance specification

Follow the skill's output contract and parser labels. Include purpose, actors, scope, source authority, readiness, test approach, coverage applicability, fixtures, a compact linked case index, detailed capability/engineering groups, gaps/dependencies, source register, bidirectional mappings and retired-case history. Use native Markdown navigation, narrow tables, explained terminology and optional named disclosures for lengthy engineering details. Keep business Given/When/Then in product vocabulary; place protocol, persistence, timing, resource and failure-injection checks beside the related behavior under Verification.

Each case needs a stable ID, source-clause references, priority, test level, platform, category, automation suitability, execution status, concrete preconditions/data, Given/When/Then, observable outcomes, boundary/evidence requirements and cleanup. Keep level, platform, category, automation suitability and status separate. Use shared fixtures and Scenario Outline rows when they preserve independent setup and distinct observable assertions. Specify the defect a case would catch when it is not obvious from the outcomes.

All new cases are **Not Run**. A described failure signal is not an observed TDD failure. Preserve supplied evidence with its build/revision, expectation version, environment and execution date; mark affected evidence stale when governing expectations change until a recorded impact review or fresh execution establishes applicability. Retain IDs for unchanged behavior, assign new IDs without recycling retired ones, and reconcile both mappings after source changes.

Require later real-boundary proof where database transactions, protocol semantics, provider delivery or client behavior matters. Test doubles may isolate failures but do not establish those real boundaries. The ATDD must distinguish the needed future evidence from actual supplied evidence. Do not claim a working integration, security compliance, measured performance or passing software from document generation.

## 5. Review, validate and hand off

Use the adapted ATDD review for format, relevance, redundancy, coverage gaps and missing edge cases, followed by doc-coauthoring's reader review and docs-sync's source reconciliation within this scope. Verify that a tester can understand setup, inputs, actions, outcomes, evidence needs and cleanup without the conversation.

Run the installed coverage helper with the independently extracted inventory. On updates pass a preserved previous-document snapshot. Resolve structural errors, inspect Markdown navigation/presentation, and review actual assertion adequacy separately. Explicit unresolved required outcomes keep the document **Draft with gaps** even when all currently known cases are structurally sound. A complete acceptance specification may be **Ready for implementation** while every case is still Not Run.

Finish with the `docs/05-atdd.md` link, readiness, coverage summary, material gaps/dependencies, actual validation and unavailable checks. Stop before tickets, executable tests or implementation.

## Output wording

Use concrete wording for each concept: seed data, source schemas, shared contracts, normalized identities, and deterministic serialization where those meanings apply. Remove redundant adjectives. Apply the personal output wording policy and check the complete deliverable before handoff, including headings, snippets, diagram labels, names, and completion text. Preserve technical requirements and externally defined contracts.
