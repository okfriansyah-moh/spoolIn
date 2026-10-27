# Create the SpoolIn technical document

Use the adopted **SpoolIn** identity and the naming/copy guidance in `docs/design-system/brand-history.md`.

Use `$techdoc-creation` to create **exactly one project deliverable: `docs/04-spoolin-techdoc.md`**. The installed orchestrator is `/Users/mekari/.codex/skills/techdoc-creation/SKILL.md`; read it and its required references. Complete the technical design and focused reviews within the authorized document scope. This prompt is an invocation artifact; it is not the techdoc itself.

The delivery sequence is **PRD + design system + techdoc → ATDD → small, dependency-ordered implementation tickets → implementation in chunks**. This run creates the techdoc only. Do not generate formal ATDD artifacts, test code, tickets, application code, deployment files, a Makefile, or a documentation site. Describe their required behavior inside the document. Keep diagrams, schemas, API/RPC contracts, ADRs, risk/control mappings, configuration tables and verification evidence embedded; create no sidecar documentation or coverage files.

## 1. Read the governing context

Read the complete current versions of:

- `docs/02-PRD.md`, especially source/status authority, roles and permissions, lifecycle policies, input/artwork rules, FR/NFR/AC coverage, engineering handoff and the open-decision register.
- `docs/01-brainstorming-result.md`, preserving committed versus recommended and working-design distinctions.
- `docs/03-design-system.md` and `docs/design-system/index.md`, `components.md`, `coverage.md`, `coverage.json`, and `tokens.json` within the design-system directory.
- The linked `docs/design-system/mockup/` gallery, component examples and flow/state artifacts. Inspect their defined behavior and supporting scripts/styles where necessary; do not infer working backend behavior from fixtures. Use available rendering tools for consequential interaction inspection and state any unavailable checks.

Inspect applicable repository instructions and any implementation/configuration that exists. The project may be documentation-only. Record source versions/content hashes and use actual source IDs rather than hardcoded requirement counts. The design system owns adopted presentation and interaction contracts; the PRD owns product behavior. Older brainstorming recommendations and sample UI values do not silently override either.

The explicit decisions in section 2 below are later user-approved additions and override older open choices about database and startup/seed invocation. Record those overrides with affected PRD/decision IDs in the techdoc. Preserve the source documents. Resolve lower-authority examples/proposals using the established hierarchy without reopening approved requirements; ask the user only when material authority or intent remains unresolved, while continuing independent design work.

The user supplied private RFC/techdoc examples as contextual inspiration for depth: overview, architecture, contracts, data, diagrams, security, performance, testing, rollout/rollback, migrations, runbooks and known limitations. Do not include their links or copy their private content, technology choices, metrics or implementation claims. The intended structure is described here; access to those examples is not required.

## 2. Carry forward the approved engineering constraints

- Frontend: **Svelte with TypeScript**. Backend: **Kotlin with Spring Boot**. Frontend application operations use **GraphQL**; meaningful backend service communication uses **gRPC**. Preserve the project's at-least-two-service requirement and the App/Catalog/Media working design unless a justified resolution is recorded. Decide compatible versions and detailed boundaries using primary documentation.
- Database: **PostgreSQL is selected**. Ordinary persistence uses **Spring Data JPA/Hibernate**. Complex native SQL is parameterized and retains equivalent integrity/concurrency safeguards. Select database/version/migration details with rationale. Earlier “HPA” wording was clarified as JPA/Hibernate; it does not select Kubernetes autoscaling.
- Organize cohesive vertical slices with SOLID/DRY/KISS/YAGNI and constructor injection where appropriate. Define practical code style, error handling, resource/async lifetimes, formatting/linting and generated-code conventions. Avoid artificial classes/layers or speculative abstractions.
- Preserve successful mutation/audit atomicity and outbox/equivalent durable follow-up for required cross-service work. Specify concurrent claiming, idempotent effects and crash/restart recovery. Add saga/compensation only where the actual lifecycle needs it.
- Define per-operation duplicate-request handling separately from duplicate-data policies, stale-write protection, race prevention, limits/rate limiting and uncertain-outcome reconciliation. Names and titles do not define globally unique identities.
- Use bounded exponential backoff with jitter only for transient failures that are safe to retry. Specify total deadlines, cancellation, one retry owner, retry budgets, persisted durable attempts, exhaustion inspection and one configured notification channel. Older numerical defaults remain proposals until selected explicitly.

## 3. Specify the one-command container startup

The application and supporting local services run through **Docker Compose and Make**. After documented prerequisites and required configuration, the reviewer runs only:

```sh
make start
```

Specify the complete dependency-aware sequence: validate prerequisites/configuration; build or obtain compatible images; start PostgreSQL and wait for readiness; apply database migrations with serialization/locking; safely initialize the versioned seed and prepared demo accounts; start all application services and project observability dependencies; check bounded readiness; print the browser URL and relevant local operational entry points. The browser must serve the usable application when the command reports success. There is no separate manual migrate, seed, frontend, backend or monitoring start step.

Document exact prerequisites, configuration/secret provisioning, network/port bindings, persistent volumes, migration ownership, initial versus repeat behavior, failure exit codes, readiness timeouts and redacted diagnostics. Required external credentials/account provisioning remain explicit prerequisites; do not invent real secrets or claim containers can provision third-party authorization automatically. Local test sinks may be useful but cannot count as successful external-provider/MCP evidence.

Initial catalog requirements remain exactly 100 distinct movies and 100 distinct TV series with source-permitted data/provenance. Reruns preserve user edits, archive states, permanent deletions, removed credits, account/key state and artwork. Define concurrent/repeated startup and partial migration/seed failure behavior without implicit destructive reset or duplicate effects.

Preserve these command contracts in the design:

| Command | Required outcome |
| --- | --- |
| `make start` | Entire required local stack initialized and ready; browser URL printed; failure returns nonzero |
| `make stop` | Stop without deleting persistent data, protected account/key state or artwork |
| `make restart` | Restart and verify readiness with the same preservation guarantees |
| `make status` | Report service state and health/readiness |
| `make logs` | Useful redacted logs without exposing credentials or sensitive payloads |
| `make test-unit` | Execute unit tests and fail the command when tests fail |
| `make test-integration` | Start required isolated dependencies, test real boundaries and persistence, return failure correctly, preserve normal app data |

This document specifies the future Makefile and containers; do not implement or run them during techdoc generation.

## 4. Apply the security baseline

Validate inputs at authoritative boundaries and normalize only as permitted by each field's policy. Preserve exact passwords, legitimate Unicode, code-point counting, omission-versus-clearing and PRD validation limits. Distinguish committed limits from recommended defaults and decide remaining bounds explicitly; never silently truncate. Use parameterized persistence, context-appropriate output encoding and constrained dynamic identifiers. Generic sanitization is not proof against all vulnerabilities.

Restrict upload formats/MIME, actual decoded content, byte size and processing resources independently of filename or declared MIME. Specify safe identifiers/storage paths, serving headers, authorization, image processing and lifecycle. Protect existing artwork during failed replacement and prevent attachment/cleanup races or deletion of referenced assets. Transfer completion is not confirmed attachment.

Exclude secrets from source/logs/errors/metrics. Specify narrow CORS, applicable CSRF/session protections, trusted origins/proxies, TLS boundaries and public error mapping without internal leaks. Include rate limits, resource exhaustion, injection, SSRF, path traversal, unsafe deserialization and relevant dependency/container threats with concrete controls and negative verification cases.

Authentication is **in scope**: Visitor, Viewer and Maintainer permissions, prepared accounts, immediate Viewer registration, sign-in/sign-out, session expiry and verified internal identity. Select a concrete mechanism; do not use “auth out of scope” boilerplate. Catalog people are distinct from private account PII. Keep recovery drafts account-isolated and exclude credentials.

Protect recoverable account PII with **AES-256-GCM**, and passwords with one-way password hashing. Define key provisioning, nonce uniqueness under concurrency/restarts, authenticated metadata/envelope, key versions, authorized reads during rotation, new writes, resumable re-encryption, retirement, backup/restore and safe failures. No plaintext fallback or loss of existing protected data.

For MCP movie creation in ChatGPT and Claude, define least-privilege tools and trustworthy human approval of exact content. Model statements cannot establish approval. Changed content requires fresh approval; replays/uncertain commits must resolve the original operation. Cover prompt injection from retrieved metadata or model output, provenance, denied actions and private-data exposure. Record client-specific feasibility and verification blockers honestly.

Use verified current OWASP ASVS/control guidance and threat-to-control-to-test mappings. For ISO/IEC 27001/27002 record relevant risks, controls, owners, evidence and limitations. Do not claim certification, organization-wide compliance or complete security from document coverage.

## 5. Apply the monitoring and operations baseline

Specify structured logs with validated request/correlation IDs across GraphQL, gRPC and durable work, with no sensitive payloads. Distinguish health/liveness, readiness and successful business outcomes. Define request/error/latency metrics, GraphQL failures under HTTP 200, gRPC status/deadlines, upload transfer/processing/attachment outcomes, retry exhaustion, pending-work age and storage/database health. Separate expected validation/access/conflict results from system failures.

Choose project monitoring tools and dashboards compatible with the one-command stack. Define actionable alert proposals with signal/query, threshold, window, severity, owner or explicitly unassigned responsibility, routing/deduplication, recovery condition and linked runbook. Account for notification-provider failure and controlled recovery. Preserve the commitment to one configured channel; do not silently replace delivery evidence with mock success.

Label numeric thresholds, workloads and SLOs as proposals with rationale and a measurement plan until tested. Do not invent a contractual SLA, measured latency or availability guarantee. Document release obligations, actual implementation evidence and production follow-up separately; pending committed monitoring is not automatically future scope.

Include deployment/configuration, capacity/resource budgets, graceful shutdown, backup/restore including encryption keys, rollout/rollback, migrations, schema compatibility, safe recovery and troubleshooting. Hosting/provider/budget remain unresolved unless explicitly selected. Kubernetes/HPA is conditional on a later selected deployment, not required by local Compose.

## 6. Make the result ready for ATDD

Use stable section anchors, embedded decision IDs and technical verification IDs. Preserve existing PRD IDs. Map every requirement and acceptance criterion to its design/contract anchor, observable verification obligation and evidence status, including design-system backend/security responsibilities. Define success, denied, invalid, stale, concurrent, uncertain, exhausted and recovered outcomes where applicable.

Include concrete schema/contract snippets, indexes and transaction rules, per-operation configuration/retry/limit policies, important sequence/state diagrams, and embedded ADRs with alternatives and consequences. Keep shared contracts within this document. Do not replace technical decisions with a list of skills or vague best practices.

Use future ATDD needs to identify preconditions, fixtures, observable assertions, test boundaries and failure cases, without generating the formal ATDD suite or ticket breakdown now. Distinguish proposed design, accepted design, existing implementation, executed checks and unexecuted tests.

Resolve ordinary engineering choices within approved requirements. Ask only for material missing product/deployment/cost/security decisions. Record blockers with affected requirements and dependent work; a document with unresolved essential contracts remains a draft and must not be called ready for dependent ATDD.

## 7. Review and finish

Run the orchestrator's architecture, security, operations and reader reviews. Verify source coverage, internal links/anchors, terminology, diagram/contract consistency, authority and evidence claims. Use temporary locations for checks and report unavailable rendering/runtime validation honestly. No production test passes may be claimed from this documentation exercise.

Preserve the PRD, brainstorming result, design system and this prompt. The sole project write is `docs/04-spoolin-techdoc.md`. Finish with its link, readiness status, actual checks and any material blockers. Stop before ATDD and ticket creation.

## Output wording

Use concrete wording for each concept: seed data, source schemas, shared contracts, normalized identities, and deterministic serialization where those meanings apply. Remove redundant adjectives. Apply the personal output wording policy and check the complete deliverable before handoff, including headings, snippets, diagram labels, names, and completion text. Preserve technical requirements and externally defined contracts.

For planning, implementation and acceptance, apply the [permanent quality contract](../../.ai/bundles/1.4.0/docs/GATES.md#permanent-quality-contract). Register every maintained runtime component and keep required failing checks blocking.
