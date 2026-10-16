---
kind: task
id: T015
group: G11
depends_on:
- T004
- T010
- T014
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: required
---

# T015 — Build account-isolated browser draft controllers

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-048 | implements |
| ATDD-179 | implements |
| ATDD-259 | implements |
| ATDD-304 | both |
| ATDD-322 | both |
| ATDD-333 | implements |
| FR-011 | implements |
| NFR-008 | both |
| D05 | implements |
| D12 | implements |
| AC-048 | implements |
| AC-048-C01 | implements |
| AC-048-C02 | implements |
| TD-C-009 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T010 — Connect account and session browser journeys](../../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T014 — Persist encrypted drafts with durable save and discard receipts](T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/lib/features/draft/ | T015 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/features/draft/draft-controller.test.ts | T015 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/draft-component/ | T015 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.4 Forms, drafts and conflict recovery](../../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.4.1 Browser storage and restored-page state](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.6.5 Code example: save and reconcile a draft](../../../04-spoolin-techdoc.md#565-code-example-save-and-reconcile-a-draft): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [FR-011 · L245](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [D05 · L306](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D12 · L313](../../../02-PRD.md#64-project-decision-register) | Authentication mechanism and verified internal identity \| Product access settled; mechanics Open in techdoc. FR-011/FR-012 / AC-047 through AC-052 |
| [AC-048 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Expiry/sign-out \| A user signs out or their sign-in expires during work \| They attempt another restricted action \| Require sign-in again; public browsing remains available. Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-048-C01 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Require sign-in again; public browsing remains available. |
| [AC-048-C02 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [TD-C-009 · L975](../../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery) | Autosave permitted drafts after 750ms with distinct Core save operation and original Catalog operation reference; retain base/current/draft, same-account recovery and visible unbacked changes. Recheck target/policy/version/permission; no raw files/credentials/registration PII. |

## Implementation

1. Implement 750ms autosave with typed safe payloads and separate identities for current Core save/discard versus original Catalog write. Preserve base/current/draft and visible unbacked changes; never persist protected fields client-side.
2. Conceal private state during session uncertainty/expiry/account transition and accept only current-account/current-generation replies. Revoke object URLs and release controller resources on exit.
3. On same-account resume, reconcile a known original mutation receipt before reading current record/draft; only then evaluate current permission/target/version/policy and show deliberate recovery choices.
4. Expose controller integration hooks for movie/person/credit editors, with conflict/current-data comparison and no force-overwrite/blind retry. Local controlled-component checks may finish before T027's actual editor proof.
5. Test autosave/save/discard response races with controlled clock and deferred transports; explicitly label these as controller evidence.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T015-A1:** Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request.
- **T015-A2:** Isolation: delayed A response after switch to B never renders A content; returning A may resume only after current permission/target/operation checks.
- **T015-A3:** Feedback: autosave failure preserves permitted in-memory input with an unbacked warning; clear/discard does not claim success before confirmed outcome.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-304](../../../05-atdd.md#case-atdd-304-conceal-draft-state-across-account-switches) / Component | ControlledDOM/autosave: dirtyA at 749/750 ms, server save fails, session expires, sign inB, reauthenticateA; objectURL and pendingread handles. | Then autosave begins at750ms and failure visibly marks unbacked state And expiry conceals private state and releases handles; B cannot see A’s draft And A recovery waits for all current eligibility checks Observation: Inspect component autosave/draft privacy and pending Core operation controller. With a known save/discard UUID, controlled operation(owner:CORE) resolves before a fresh drafts query and before restoration; DraftInput.operationId continues to identify the separate Catalog submission. Mocked recovery does not prove durable receipts; no plaintext browser storage or restoration from old receipt body is permitted. |
| [ATDD-322](../../../05-atdd.md#case-atdd-322-recover-known-draft-intent-before-querying-current-values) / Component | Pending known Core D1 save/discard with lost response; currentdraftX/version8 or absent; CatalogC1 pending; switchA→B and originalCoreUUID unavailable row. | Then known D1 is reconciled through ownerCORE before current draft state is restored And old safe receipt version does not replace newer/absent current payload And B cannot see A recovery; lost identity does not authorize blind repeated mutation or invented historical result Observation: Record controlled request sequence, target owner and DOM/controller state. This proves client decision order only; real lost-response replay is specified separately. |

Partial implementation contributions: [ATDD-048](../../../05-atdd.md#case-atdd-048-expirysign-out), [ATDD-179](../../../05-atdd.md#case-atdd-179-conceal-drafts-until-the-same-account-can-resume), [ATDD-259](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback), [ATDD-333](../../../05-atdd.md#case-atdd-333-retain-safe-draft-replay-without-resurrecting-later-expired-or-discarded-state). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T015` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| visual-review | Inspect relevant changed states against the adopted references; retain exhaustive matrix coverage with T044 unless required by this feature | `/Users/mekari/Developer/personal-project/movie-app` | Required visual differences resolved at current revision; no overflow/missing identity | yes |
| interaction-accessibility | Exercise keyboard/focus/labels/errors/live regions/dialogs, zoom/reduced motion and source browser/host matrix independently of pixels | `/Users/mekari/Developer/personal-project/movie-app` | Meaningful accessible interaction and safe recovery, with unavailable contexts kept unverified | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

Only isolated build/unit/component validation is required here. The source case/row owners named in Traceability retain complete real-service/client proof; no empty integration selection is required for local completion. Graphical captures below still inspect a rendered component/reference under the declared contexts.

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Use the linked adopted references and actual feature for relevant changed states. Browser assertions and direct inspection establish ordinary behavior; retain screenshots when visual inspection needs them. Record actual fixtures, environment and outcomes. Exhaustive theme/width/browser matrices below remain release coverage references owned by T044 unless the selected behavior needs that exact observation. Do not delete their case IDs or claim deferred proof passed.

Check changed keyboard/focus/labels/errors/announcements and relevant zoom, motion, touch and browser behavior. Preserve known regressions and actual reader observations required to prove speech; exhaustive compatibility sweeps remain release-owned. For AI states include actual Claude/ChatGPT host context at T038/T039; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

| Flow/state reference | Actor and fixture condition | Trigger and expected comparison |
|---|---|---|
| [session / session-check](../../../design-system/mockup/session.html#session-check) · [ATDD-259-T1-R01 · L10775](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Authenticated candidate; hold `session` while opening a protected destination; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Announce Checking your session and conceal protected fields/draft/actions. Public catalog navigation stays usable; unresolved identity grants no protected submit. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / auth-unavailable](../../../design-system/mockup/session.html#auth-unavailable) · [ATDD-259-T1-R02 · L10776](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Reader; fail `session` while Core can still answer public catalog reads; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Sign-in is temporarily unavailable, no private account details or protected work, and Browse catalog. Failure is distinct from confirmed sign-out. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / expired](../../../design-system/mockup/session.html#expired) · [ATDD-259-T1-R03 · L10777](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Maintainer A; expire the session during editing; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show session-expired feedback, conceal protected draft and require reauthentication; public browsing remains. Do not describe committed changes as rolled back. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / denied](../../../design-system/mockup/session.html#denied) · [ATDD-259-T1-R04 · L10778](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Viewer/Visitor; navigate to a Maintainer destination; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show You do not have access and public navigation; render no protected record/draft fields or enabled management actions. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / recheck](../../../design-system/mockup/session.html#recheck) · [ATDD-259-T1-R05 · L10779](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Maintainer A; reauthenticate with an unconfirmed prior save; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Check before continuing; resolve the original owner operation and current eligibility before restoring/submitting draft values. Do not offer a blind new save UUID. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / other-account](../../../design-system/mockup/session.html#other-account) · [ATDD-259-T1-R06 · L10780](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Account B; sign in after A's editing session ends; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show draft-unavailable-to-this-account feedback with original-owner sign-in/public browse actions. A's draft values, identifiers and password recovery never appear. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / signout](../../../design-system/mockup/session.html#signout) · [ATDD-259-T1-R07 · L10781](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Signed-in account; choose sign-out from the session menu; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Explain that the session ends and protected work becomes unavailable to the next account; offer confirmation/cancel where shown. An uncertain save is not undone by sign-out. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / signed-out](../../../design-system/mockup/session.html#signed-out) · [ATDD-259-T1-R08 · L10782](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Signed-in account; receive confirmed successful `signOut`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Signed out, remove protected DOM/controller/cache state and offer public browsing. Do not restore protected work on browser Back. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / signout-failed](../../../design-system/mockup/session.html#signout-failed) · [ATDD-259-T1-R09 · L10783](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Signed-in account; receive confirmed failed `signOut` before revocation; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Sign-out did not complete and retry; retain the known active-session identity. Do not claim sign-out or transfer its protected work to another account. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / signout-uncertain](../../../design-system/mockup/session.html#signout-uncertain) · [ATDD-259-T1-R10 · L10784](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Signed-in account; lose the `signOut` response; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Sign-out is not confirmed, conceal protected content/submission and recheck the original result/session. Claim neither active authorization nor confirmed revocation until reconciled. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

These captures inspect this task’s controlled policy/draft/review component with source fixtures. They do not require later domain editors or real host integration to exist: T027/T043/T044 and T038/T039 respectively retain the full production/host state proof. Record that boundary explicitly with each capture.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| visual-review | Not run | — | — | — | — |
| interaction-accessibility | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T027 — Prove recovery through real movie, person and credit editors](../../17-draft-recovery-editor-integration/tasks/T027-prove-recovery-through-real-movie-person-and-credit-editors.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
