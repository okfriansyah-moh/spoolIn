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

Read `AGENTS.md` and `.ai/config.json`, then follow the selected bundle's [delivery policy](../../.ai/bundles/1.3.0/docs/DELIVERY.md). That policy owns implementation, resource admission, checks, independent review, correction limits, progress, recovery, and delivery. Preserve the inputs and selection rules above; load supporting gate or coordination guidance only when needed.
