# Reusable task implementation prompt

Use the `running-prompt` skill from the repository root and supply only the inputs you want to override. It reads this document and fills omitted fields from the defaults below for new runs. On resume, explicit overrides take precedence over retained authorized run inputs; defaults fill only values missing from both. You can also copy the full prompt. Saving, editing or explaining this document does not start application work; invoking its implementation workflow authorizes the selected run.

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

```yaml
Target: next
Plan: docs/06-PLAN.md
Mode: implement
Dependencies: selected-only
Delivery: local
Additional context: none
```

## Execution

Read root AGENTS.md, .ai/config.json and the pinned bundle docs/DELIVERY.md. Honor the six inputs above; for resume explicit inputs override retained authorized inputs, which override defaults. Accept task IDs/lists/paths and group IDs/paths. `next` selects one eligible incomplete task; `all-ready` selects the incomplete plan tasks and recomputes development eligibility. `selected-only` excludes unselected prerequisites; `include-prerequisites` includes their transitive closure. Record the resolved selection once without silent expansion.

Read selected outcomes, relevant contracts, task tests and existing code. Run doctor once and distinguish installation health from live host trust/tool/service availability. Keep product IDs and explicit source boundaries. Plan mode permits inspection/planning only; setup or workflow installation does not start application tasks.

Start or resume one shared selection record using the pinned coordination contract. Record outcome, task/dependency IDs, branch, PR head, required checks and next observable behavior. Default to direct serial implementation in one suitable checkout. Add isolated claimed worktrees only for concurrent writers or protection of existing work; serialize shared resources. Tested predecessors in this selection may support development before final acceptance. Preserve all stopped work and reuse valid results.

Implement the entire requested selection and run targeted checks while coding. Preserve existing quality/security thresholds:80% changed coverage, 50 function lines, complexity 10, nesting 3 and required behavior/boundary checks. Use real database/transport/browser/reader observations where needed; mocks prove only their stated boundary. Missing tools, empty selections, failures and stale results do not pass. Follow the selected test strategy and actual component commands. Do not weaken assertions or gate configuration to obtain success.

Commit source at natural milestones. At least every 45 minutes record actual changed behavior, passing relevant checks, local head, published PR head, remaining tasks and next action. Support-tool activity is not product progress. Optional evidence-tool repair shares two attempts or 20 minutes across the entire selection, including resumes and renamed phases. Real product/security/accessibility defects remain blocking implementation work. Preserve the configured required-correction limit; continue independent selected work or report the precise blocker.

Use existing runners and one findings ledger. Browser assertions/direct inspection suffice for ordinary behavior; retain screenshots where needed. Record media only when the required observation needs it or the user requests it. Necessary speech observations need an actual reader. Explicitly assign exhaustive matrices/media provenance to release owners without deleting product outcomes or marking deferred proof passed. Avoid custom recorders/proof-only screens when the actual feature suffices.

Compose tested contributions in dependency order before one independent review of the entire selection. Two tasks receive one combined review; two groups receive one combined review. No separate task/intermediate-group/evidence-method review. The reviewer inspects requirements, coherent connections, actual checks, standards, security and relevant accessibility. Keep the same branch/ledger for corrections; rerun affected checks and verify fixes. Only the coordinator marks task acceptance after final current checks and review pass.

For local delivery retain reviewed source locally. For draft-pr delivery publish the authorized selection branch after review and create/update its draft PR if the configured remote/access exist. Missing remote blocks publication only. Honor existing authorization for other actions within scope. Preserve unrelated work and run the wording checker before handoff.

Report selected tasks, actual behavior/checks, local head, PR head, review and remaining blockers literally. State whether a PR update changed source commits, metadata only, or nothing. Do not describe unimplemented tasks as nearly delivered. “Finish and wrap up” retains the completion goal unless the user explicitly stops execution. Stop at the authorized selection.
