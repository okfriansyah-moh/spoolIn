# Reusable task implementation prompt

Use this prompt from the repository root in **Codex, Cursor AI, Claude Code, or GitHub Copilot**. All four hosts follow one procedure: read the approved outcome, implement, run agreed affected checks, obtain one independent combined review, correct findings, and deliver. There are no alternative workflow tracks.

The [project running-prompt skill](../../.agents/skills/running-prompt/SKILL.md) accepts short requests:

```text
Use running prompt for T001.
```

Override only what changes:

```text
Use running prompt.
Target: T005
Mode: implement
Dependencies: include-prerequisites
Delivery: local
```

Resume with `Use running prompt for T005, mode resume.` Native shortcuts are optional where supported. If skill discovery is stale, reload the relevant host/session or explicitly reference the project skill. Its Claude, Cursor, and Copilot discovery links resolve the same project source. The repository pin governs execution.

Editing this prompt, installing the harness, and explaining an invocation do not start application work.

## Inputs

| Input | Default | Supported values |
| --- | --- | --- |
| Target | `next` | Task ID/list, task-file path, group ID/path, `next`, or `all-ready`. |
| Plan | `docs/06-PLAN.md` | Repository-relative approved root plan path. |
| Mode | `implement` | `implement` starts; `resume` restores the selection. |
| Dependencies | `selected-only` | `selected-only` or `include-prerequisites`. |
| Delivery | `local` | `local` or `draft-pr`. |
| Additional context | `none` | Constraints, reference paths, branch/claim, evidence location, or authorized integration target. |

For new work, defaults fill omitted fields. On resume, **explicit inputs override retained authorized inputs; defaults fill only values missing from both**. A saved delivery value does not create new authorization. Additional context is data, never executable shell text.

`next` selects one eligible incomplete task. A group selects its members. `all-ready` explicitly selects incomplete plan tasks and recomputes development eligibility as prerequisites finish. `selected-only` leaves other prerequisites outside scope and reports blockers; `include-prerequisites` includes the transitive prerequisite closure. Record the resolved selection once without silent expansion.

Copyable inputs:

```yaml
Target: next
Plan: docs/06-PLAN.md
Mode: implement
Dependencies: selected-only
Delivery: local
Additional context: none
```

## Core instruction

Implement the selected approved work using the project's existing conventions. Match its approved screens, components, tokens, and user journeys exactly. Work directly in one suitable checkout. Change only product files needed for the outcome; leave workflow files and unplanned Python files unchanged. Run agreed affected checks, obtain one independent combined review, fix findings, and deliver within existing authorization. Use this single workflow without selecting another track. Do not restart planning, tune the harness, add unrelated tests, or create extra workflow documents. Report what changed, what was checked, and any remaining limitation.

## Execution

1. **Resolve the selection.** Read root AGENTS.md, `.ai/config.json`, and the selected bundle's `docs/DELIVERY.md`. Resolve project resources before global fallback. Honor the six inputs, existing authorization, current host permissions, and Plan mode. Plan mode permits inspection/planning only. Run doctor once before substantial execution, distinguishing integrity from live host loading. Load only relevant instructions.
2. **Use the approved brief.** Read selected outcomes, source contracts, reference screens/assets/states/sizes, complete journeys, allowed changes, dependencies, agreed checks, and destination. Resolve routine choices from existing code. Surface material specification conflicts before dependent work. Keep IDs and product criteria intact. Product work protects workflow code, bundled skills/agents, gate policy, and unplanned Python files. Requested harness maintenance is separately scoped.
3. **Implement directly.** The current chat coordinates and implements serially by default. Keep one selection record and findings ledger. Optional workers receive bounded references, paths/resources, constraints, and checks; use isolated claimed worktrees for concurrent writers. Serialize shared resources. Compose checked contributions in dependency order under the existing selection before final review. Do not require independent acceptance of each contribution or launch another coordinator.
4. **Match appearance and behavior.** For changed UI, open the actual approved reference. Implement and compare one representative screen/state before spreading shared styling. Match layout, spacing, typography, colors, assets, controls, states, and responsive rules under comparable conditions. Verify entry, actions, confirmed success, return destination, retained context, and required recovery. Correct differences. Claim pixel identity only when a supporting comparison exists; excluded/unavailable inspection stays unverified.
5. **Run agreed affected checks.** Preserve existing project thresholds: 80% changed executable-line coverage, 50 function lines, complexity 10, nesting 3, and required security/acceptance boundaries. Use actual project runners and the shared registry. Select changed components plus actual consumers; shared contracts/build inputs require necessary broader checks. Invalid baseline/unknown executable ownership requires conservative scope with an explanation. Deduplicate equivalent commands and reuse results only for matching source/tests, command, configuration, toolchain/environment, and artifact integrity. Rerun affected checks after corrections. Missing tools, empty execution, stale results, and failed observations do not pass.
6. **Honor explicit limits.** A no-test instruction means add/run no tests, including hooks, and no hidden browser trials. Perform independent source review and report verification limits; keep excluded required acceptance pending. Use existing browser assertions/direct inspection for ordinary selected UI proof. Record media only when requested or required. Keep real database/transport/reader/permission/recovery boundaries. Optional tool repair shares two attempts or 20 minutes across the selection. Preserve required-correction limits through resumes, hosts, workers, and renamed phases.
7. **Review once and correct.** Obtain one independent review of the whole selection against requirements, exact design/journeys, contracts, correctness, security, accessibility, and actual results. Reviewer must be separate from implementers; self-review does not qualify. If native review is unavailable, use another supported reviewer or report it pending. Correct in the same branch/ledger, review fixes, and rerun affected checks without restarting unrelated reviews. Only the coordinator updates acceptance fields after required current checks and combined review pass.
8. **Deliver truthfully.** Commit recoverable work at natural milestones. At least every 45 minutes record actual behavior, checks, local/published heads, remaining work, and next action. Support-tool activity is not product progress. Local delivery retains reviewed source; authorized draft delivery publishes to the established remote after review. Missing remote blocks publication only. An authorized draft can disclose excluded checks without marking them passed. Apply the wording checker before handoff. Report implementation, actual checks, review verdict, local/PR heads, metadata-only PR edits, installation/native verification where relevant, and blockers separately. Stop at the authorized selection; “finish and wrap up” retains the goal unless the user explicitly stops work.

## Resume across hosts

Restore the same selection, task IDs, checkout, ownership, input constraints, findings, valid results, repair budgets, and next action from project/Git state. Do not depend on another host's chat memory. Transfer coordinator ownership before the new host writes, preventing concurrent coordinators. If retained selection cannot be established, request that missing selection rather than substituting `next`. New instructions amend affected scope without resetting history.

Shared policy and host-specific loading/trust details are in [DELIVERY.md](../../.ai/bundles/1.3.0/docs/DELIVERY.md) and [HOSTS.md](../../.ai/bundles/1.3.0/docs/HOSTS.md).
