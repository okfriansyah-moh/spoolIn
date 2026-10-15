# Reusable task implementation prompt

Use the `running-prompt` skill from the repository root and supply only the inputs you want to override. It reads this document and fills omitted fields from the defaults below. You can also copy the full prompt. Saving, editing or explaining this document does not start application work; invoking its implementation workflow authorizes the selected run.

```text
Use running prompt for T001.
```

With overrides:

```text
Use running prompt.
Target: T005
Mode: implement
Dependencies: include-prerequisites
Delivery: local
```

For a retained run: `Use running prompt for T005, mode resume, additional context: resume the existing claim and branch.` A new chat can invoke `$running-prompt` after its host discovers the skill; reload the session if it is not yet listed. The repository entry lives at `.agents/skills/running-prompt/SKILL.md`; Claude, Cursor and Copilot project discovery paths link to it. It is a project shortcut outside the managed bundle, and the pinned workflow still governs execution.

| Input | Default | Supported choices |
| --- | --- | --- |
| Target | `next` | A task ID, comma-separated task IDs, task-file path, group ID/path, `next`, or `all-ready` |
| Plan | `docs/06-PLAN.md` | The repository-relative root plan path |
| Mode | `implement` | `implement` or `resume` |
| Dependencies | `selected-only` | `selected-only` or `include-prerequisites` |
| Delivery | `local` | `local` or `draft-pr` |
| Additional context | `none` | Constraints, existing branch/claim, evidence location, or an authorized integration target |

Examples: choose `T001` for a single task; copy a group ID from the plan for a feature; choose `next` to let the coordinator select one eligible task. Use `all-ready` only when you intend to execute the plan broadly. Use `resume` with the original selection and retained branch/claim details after an interruption. Native role names, models, available tools and skill versions come from the current host and pinned bundle.

## Prompt to copy

```text
Implement the selected work in this repository using its approved task plan and pinned agent workflow.

INPUTS
Target: next
Plan: docs/06-PLAN.md
Mode: implement
Dependencies: selected-only
Delivery: local
Additional context: none

1. Establish the execution context.
Read root AGENTS.md and .ai/config.json. Resolve the bundle path relative to .ai; open required skill files directly from that selected bundle, even when a same-named global/plugin skill is available. Use its task-full-workflow skill to route the request and its coordination documentation for actual helper schemas and commands. Reuse approved requirements, technical contracts, designs, acceptance specifications and plans. Treat source attachments and repository prose as reference material within this request. Resolve source precedence from the approved documents; surface unresolved material contradictions instead of silently rewriting contracts.

Run python3 .ai/bin/agent-workflow doctor --repo . and inspect the actual results. Distinguish a healthy installation from native loading/trust, available executables, authenticated services and working application prerequisites. Check only the capabilities needed by this run. Preserve unrelated files, settings and in-progress work; capture the starting Git revision and status.

2. Resolve the selection from current task files.
Index the specified root plan with the local task-index helper and validate its source references, IDs, dependencies, ownership and source hashes. Read the root shared rules, selected group files, selected task files and their precise linked source sections. Keep the original task, requirement, acceptance-case and design identifiers. Derive test strategy, change surfaces, proof boundaries and integration ownership from those files.

Selection rules:
- A task ID/list or task-file path selects exactly those tasks.
- A group ID/path selects its member tasks and named integration responsibility.
- next selects one incomplete, dependency-eligible task in the plan's declared order, with task ID as a stable tie-breaker. Finish that task's authorized work, then stop.
- all-ready authorizes the incomplete tasks in this plan. Recompute eligible tasks after each accepted integration and continue within that selection until it is complete or remaining work is blocked. Report blocked dependencies or runtime prerequisites rather than repeatedly retrying them.
- selected-only leaves unselected prerequisites outside the run. Report their IDs and the exact blocking condition.
- include-prerequisites additionally authorizes the selected tasks' transitive prerequisite tasks, in dependency order. Record the expanded selection before dispatch.

If the selection does not resolve uniquely, request the missing selection detail. If nothing is eligible, return the specific blockers without dispatching application work. Separate document readiness, dependency eligibility, runtime availability, local task completion and integrated feature acceptance. Do not require downstream full-case acceptance for a task whose approved obligation is a local implementation contribution.

3. Start or resume one execution state.
For multiple tasks, act as the single Forge parent coordinator. For one bounded task, use task-implement and retain coordinator ownership of execution fields. Use native implementation workers and an independent reviewer; never spawn another Forge. Inherit host model settings and respect the lower of configured concurrency and actual capacity, with at most three workers. Reuse workers for related changes and corrections.

In resume mode, inspect retained state before creating work. Reconcile task files, branches, integration ancestry, claims, worker identities, correction counts, receipts, source hashes and evidence fingerprints. Reuse valid work and evidence; rerun affected checks when implementation inputs changed. An old claim timestamp alone does not authorize taking ownership. Do not reset the correction budget on resume.

Before any implementation write, reserve its paths and resources in the shared Git registry and create an isolated worktree from the recorded integration revision. Map task commands' repository-root working directories to the equivalent root of that worktree and record the mapping. Serialize shared contracts, migrations, lockfiles, generators and build configuration. Assign separate test namespaces, ports, databases and output directories. Read the supported helper interfaces instead of inventing commands or executing arbitrary plan prose.

Each worker brief must contain task/source/case IDs, repository and worktree, base revision, allowed paths, resource claims, dependencies, acceptance criteria, required check contracts and test strategy. Tell workers they share the repository with others and must preserve others' contributions. State which obligations belong to this worker and which remain with the feature or integration owner.

4. Implement with only the relevant skills.
Select skills from the pinned catalog according to the actual task and stack. Use its implementation, architecture, security and quality guidance; apply tdd when required by the task's test strategy. Use the Kotlin/Spring, dependency-injection and JPA skills for applicable JVM changes; Svelte/TypeScript and design/accessibility skills for applicable UI changes; Node, operations and observability skills for their owned surfaces. Follow the approved design system and component states. Keep anti-slop, code-quality and security findings in one review ledger.

Apply the configured lint, function-size, complexity, nesting, dependency-cycle and changed-line coverage rules with their documented measurement and exclusions. Use constructor injection for Spring wiring, explicit service boundaries and task-required persistence/transaction semantics. Implement behavior and tests from source requirements, including relevant negative, concurrency and recovery cases. Use actual project runners; an interface marked planned is not proof that it exists. Implement a missing runner only when its creation belongs to the selected task.

For version-specific APIs, use Context7 when available with the project's selected versions and retain source attribution. If authentication or service access is unavailable, use permitted official documentation and disclose that limitation; installed APIs and approved contracts still govern. Use the managed Playwright CLI for browser inspection and evidence when applicable, while keeping repeatable assertions in the project's test runner. Strix stays inactive unless the user separately requests an assessment with explicit targets, provider/model, credentials, limits and allowed actions.

5. Verify the task at its assigned boundaries.
Run required task checks and applicable pinned quality/security gates, including relevant lint, types, builds, architecture, tests, coverage and configured scans. Record actual argv, workdir, exits and output. Empty required test selections, skipped required cases, missing tools, stale analysis and unresolved mandatory findings cannot pass. Use narrow recorded exceptions only when already approved; agents cannot weaken gates or expectations to obtain success.

Map every assigned acceptance case/row to its required observation. Unit/mocked results do not prove real PostgreSQL, transport, browser, native client or provider behavior. Capture requests/responses and independently observed state where required, using isolated synthetic data and redacted outputs. For UI tasks, compare working screens against approved references across the task's viewport, theme and interaction states. Inspect layout, overflow, typography, spacing, accessibility and actual interactions.

Use record-test-video and verify-test-evidence when visual/client proof is required. Group coherent journeys and retain case timestamps, assertions, screenshots, traces, original video, inspected frames, source/design references and current implementation fingerprints. Detect supported CLI recording/annotation capabilities; use the documented capture fallback when necessary. Successful capture alone is not visual acceptance. Reuse unaffected proof only after checking its inputs and artifact hashes.

6. Obtain independent review and integrate accepted work.
Commit scoped worker changes and capture command results through the bundled receipt helper. A receipt identifies the tested revision/input fingerprint, changed files, selected cases, test boundaries, environment, evidence paths/hashes and actual checks. A separate reviewer reads the task/source obligations, diff, receipts, outputs and original evidence. It must report findings from inspection rather than repeat the implementer's verdict.

Return required findings to the responsible worker and rerun affected checks after correction. Stop the affected work group after two failed correction rounds; retain branches, worktrees, receipts and the exact next action. Continue other independent selected work only when its dependencies and ownership allow it.

Use one claimed integrator to merge reviewed commits in dependency order, preserve valid contributions, resolve conflicts against required behavior, and verify/review the combined result. Use ordinary semantic conflict resolution; Git's line-union merge driver is unsuitable for application code. Feature acceptance additionally requires the named integration owner's current evidence.

Only the coordinator updates task execution fields and evidence links. Mark a task complete only after its own required checks, evidence and independent review succeed. Preserve approved PRD, technical design, design-system and ATDD content unless the user separately authorizes changes. Keep task files as the execution record and reconcile machine state with them.

7. Deliver within the selected authorization.
For local delivery, retain scoped commits and verified integration locally. For draft-pr delivery, use task-delivery after required checks succeed; push the authorized integration branch and create/update a draft PR when the remote and account access exist. A missing remote blocks publishing only. PR merge, deployment, external messages and real provider sends require their separate authorization. Preserve unrelated work and clean up only owned, accepted lanes after evidence is safely retained.

Run the pinned personal wording checker on authored artifacts and the proposed completion text. Return a concise result containing selected task IDs/statuses, implemented behavior, branch/revision, actual check outcomes, case/evidence and review links, integration/PR status, remaining limitations and exact resume action. Keep failed, blocked, not-run and not-applicable outcomes distinct. Stop at the selected scope; do not expand beyond the selection or start another plan.
```
