---
kind: task
id: T039
group: G18
depends_on:
- T001
- T003
- T007
- T011
- T013
- T018
- T020
- T022
- T024
- T027
- T028
- T029
- T031
- T035
- T036
- T038
readiness: Ready
status: Not started
risk: high
test_strategy: alongside
visual_review: not-applicable
---

# T039 — Implement the seven reproducible lifecycle commands

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-065 | both |
| ATDD-068 | both |
| ATDD-072 | both |
| ATDD-073 | both |
| ATDD-074 | both |
| ATDD-075 | both |
| ATDD-134 | verifies |
| ATDD-135 | verifies |
| ATDD-202 | both |
| ATDD-203 | both |
| ATDD-204 | both |
| ATDD-205 | both |
| ATDD-214 | both |
| ATDD-215 | both |
| ATDD-216 | both |
| ATDD-217 | both |
| ATDD-218 | both |
| ATDD-219 | both |
| ATDD-220 | both |
| ATDD-327 | verifies |
| ATDD-342 | verifies |
| SC-01 | both |
| SC-03 | both |
| SC-05 | both |
| FR-004 | both |
| FR-010 | both |
| FR-012 | both |
| FR-017 | both |
| NFR-001 | both |
| NFR-002 | both |
| NFR-003 | both |
| NFR-004 | both |
| NFR-005 | both |
| NFR-006 | both |
| NFR-008 | both |
| NFR-009 | both |
| NFR-010 | both |
| NFR-016 | both |
| A01 | both |
| A02 | both |
| A03 | both |
| A04 | both |
| A12 | both |
| A16 | both |
| D02 | both |
| D03 | both |
| D04 | both |
| D05 | both |
| D07 | both |
| D10 | both |
| D12 | both |
| D13 | both |
| D17 | both |
| D19 | both |
| D21 | both |
| D22 | both |
| D23 | both |
| AC-050 | both |
| AC-050-C01 | both |
| AC-071 | both |
| AC-071-C01 | both |
| AC-153 | both |
| AC-153-C01 | both |
| AC-153-C02 | both |
| AC-153-C03 | both |
| AC-153-C04 | both |
| AC-153-C05 | both |
| AC-154 | both |
| AC-154-C01 | both |
| AC-154-C02 | both |
| AC-154-C03 | both |
| AC-154-C04 | both |
| AC-155 | both |
| AC-155-C01 | both |
| AC-155-C02 | both |
| AC-155-C03 | both |
| AC-157 | both |
| AC-157-C01 | both |
| AC-157-C02 | both |
| AC-064 | both |
| AC-065 | both |
| AC-067 | both |
| AC-074 | both |
| AC-076 | both |
| AC-077 | both |
| AC-079 | both |
| AC-087 | both |
| AC-088 | both |
| AC-089 | both |
| G-02 | both |
| G-03 | both |
| G-04 | both |
| G-05 | both |
| G-06 | both |
| J-05 | both |
| AC-078 | both |
| AC-078-C01 | both |
| AC-091 | both |
| AC-091-C01 | both |
| AC-092 | both |
| AC-092-C01 | both |
| AC-092-C02 | both |
| AC-093 | both |
| AC-093-C01 | both |
| AC-094 | both |
| AC-094-C01 | both |
| AC-094-C02 | both |
| OD-002 | both |
| OD-015 | both |
| OD-020 | both |
| US-012 | both |
| TD-C-058 | both |
| TECH-G-01 | both |
| TD-U-002 | both |
| V-CONFIG | both |
| TD-ROW-L6075 | both |
| TD-ROW-L6076 | both |
| TD-ROW-L6077 | both |
| TD-ROW-L6078 | both |
| TD-ROW-L6079 | both |
| TD-ROW-L6080 | both |
| TD-ROW-L6081 | both |
| TD-BLOCK-L3642 | both |
| ATDD-G-01 | both |
| BRAIN-A01 | both |
| BRAIN-A02 | both |
| BRAIN-A03 | both |
| BRAIN-A04 | both |
| BRAIN-A12 | both |
| BRAIN-A16 | both |
| BRAIN-D02 | both |
| BRAIN-D03 | both |
| BRAIN-D04 | both |
| BRAIN-D05 | both |
| BRAIN-D07 | both |
| BRAIN-D10 | both |
| BRAIN-D12 | both |
| BRAIN-D13 | both |
| BRAIN-D17 | both |
| BRAIN-D19 | both |
| BRAIN-D21 | both |
| BRAIN-D22 | both |
| BRAIN-D23 | both |

## Prerequisites

- [T001 — Lock the build and isolated verification harness](../../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted.
- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T007 — Connect account and session browser journeys](../../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T011 — Run fenced recovery and bounded retries](../../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.
- [T013 — Connect discovery, details and public people views](../../07-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria.
- [T018 — Connect movie editors and confirmed outcomes](../../09-movie-management/tasks/T018-connect-movie-editors-and-confirmed-outcomes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Editor: valid create/update displays committed values under the same ID; optional omissions stay unknown and retained invalid fields remain fully correctable.
- [T020 — Connect people management and retained edits](../../10-people-and-credits/tasks/T020-connect-people-management-and-retained-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied.
- [T022 — Connect credit editing and verify the people outcome](../../10-people-and-credits/tasks/T022-connect-credit-editing-and-verify-the-people-outcome.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning.
- [T024 — Connect private archives and irreversible confirmations](../../12-archive-management/tasks/T024-connect-private-archives-and-irreversible-confirmations.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content.
- [T027 — Connect measured progress and recover attachment outcomes](../../11-artwork-management/tasks/T027-connect-measured-progress-and-recover-attachment-outcomes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree.
- [T028 — Expose complete read-only audit outcomes](../../14-accountable-history/tasks/T028-expose-complete-read-only-audit-outcomes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Attribution: history contains actor/action/affected record/time/outcome/relevant changes for each included event; no absent fields are inferred from scalar compatibility.
- [T029 — Implement isolated MCP transport and connector identity](../../13-assisted-movie-creation/tasks/T029-implement-isolated-mcp-transport-and-connector-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Scope: Viewer/revoked/wrong-client connector cannot gain publication or excluded edit/delete/account/artwork/archive/audit capability.
- [T031 — Build the trusted in-chat review resource](../../13-assisted-movie-creation/tasks/T031-build-the-trusted-in-chat-review-resource.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Review: complete exact values and source context are readable before approval, correction changes revision and requires a fresh intentional action.
- [T035 — Initialize and replay seed and prepared accounts safely](../../15-reproducible-seed/tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles.
- [T036 — Record actionable alerts through one selected mode](../../16-operational-notifications/tasks/T036-record-actionable-alerts-through-one-selected-mode.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Default: exhausted eligible work yields one inspectable sanitized recording and zero sends; database mode needs no external provider credential.
- [T038 — Expose logical metrics, dashboards and alert episodes](../../17-operational-evidence/tasks/T038-expose-logical-metrics-dashboards-and-alert-episodes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | ops/bin/start.sh | T039 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/stop.sh | T039 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/restart.sh | T039 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/status.sh | T039 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/logs.sh | T039 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/operations/commands/ | T039 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | modify | Makefile | Extend the artifact created by T001; serialize this shared write |
| app | modify | ops/compose.yaml | Extend the artifact created by T003; serialize this shared write |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [11.1 Configuration contract](../../../04-spoolin-techdoc.md#111-configuration-contract): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [11.2 Startup DAG and readiness](../../../04-spoolin-techdoc.md#112-startup-dag-and-readiness): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [11.3 All seven Make command contracts](../../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.1 Database access and resource budgets](../../../04-spoolin-techdoc.md#121-database-access-and-resource-budgets): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [SC-01 · L102](../../../02-PRD.md#4-goals-and-success-criteria) | Meet the external assessment \| Every A01 through A17 obligation has a mapped requirement and planned evidence \| Software evidence unverified; Required |
| [SC-03 · L104](../../../02-PRD.md#4-goals-and-success-criteria) | Preserve user decisions \| Edits, archive state, and permanent deletions survive ordinary seed reruns; saved data/artwork survive restarts \| Unverified; Committed |
| [SC-05 · L106](../../../02-PRD.md#4-goals-and-success-criteria) | Make review reproducible \| Documented setup, seed, execution, tests, and code explanation can be followed independently \| Unverified; Required/Committed |
| [FR-004 · L240](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [FR-010 · L246](../../../02-PRD.md#61-functional-requirements) | Recover failed/uncertain artwork changes while protecting prior and still-used artwork \| D20/D21 Committed; product policy Committed (section 13.1); D11 mechanics Open \| AC-043 through AC-046, AC-152, AC-155, AC-157 |
| [FR-012 · L248](../../../02-PRD.md#61-functional-requirements) | Enforce the public/Viewer/Maintainer capability matrix, including private movie/people archives and connected tools \| D05 Committed; product policy Committed (section 13.1) \| AC-050 through AC-052, AC-113, AC-119, AC-133, AC-134 |
| [FR-017 · L253](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-001 · L261](../../../02-PRD.md#62-quality-and-delivery-requirements) | Required assessment stack and real frontend/backend protocols \| A01 through A05, D03 Required \| AC-064 through AC-066; section 10 |
| [NFR-002 · L262](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preferred App/Catalog/Media ownership with unrelated catalog work usable during artwork outage \| D02/D10 Working design \| AC-067, AC-068; section 10 |
| [NFR-003 · L263](../../../02-PRD.md#62-quality-and-delivery-requirements) | Saved data/artwork persist through restarts; committed persistence safeguards remain \| D18/D19 Committed \| AC-069 through AC-071 |
| [NFR-004 · L264](../../../02-PRD.md#62-quality-and-delivery-requirements) | Safe recovery is bounded and respects cancellation; uncertain outcomes cannot multiply effects \| D21 Committed; D22 defaults Recommended \| AC-072 through AC-075 |
| [NFR-005 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Setup rejects unusable configuration and protects secrets \| D13/D21; configuration and secret protection Committed \| AC-076 through AC-078, AC-153 through AC-155, AC-157 |
| [NFR-006 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Operations owners can assess readiness, diagnose user failures, and act on alerts \| D23 Committed \| AC-079 through AC-082 |
| [NFR-008 · L268](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-009 · L269](../../../02-PRD.md#62-quality-and-delivery-requirements) | Cohesive vertical slices and understandable, maintainable submitted code \| D04/D17 Committed \| AC-089, AC-090 |
| [NFR-010 · L270](../../../02-PRD.md#62-quality-and-delivery-requirements) | Reproducible local setup preserves normal data and isolates verification \| D13/D24 Committed \| AC-091 through AC-094 |
| [NFR-016 · L276](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [A01 · L282](../../../02-PRD.md#63-assessment-coverage) | Svelte frontend in TypeScript \| NFR-001 / AC-064 |
| [A02 · L283](../../../02-PRD.md#63-assessment-coverage) | Spring Boot in Kotlin \| NFR-001 / AC-064 |
| [A03 · L284](../../../02-PRD.md#63-assessment-coverage) | At least two meaningful Spring Boot services \| NFR-001 / AC-065 |
| [A04 · L285](../../../02-PRD.md#63-assessment-coverage) | Real service-to-service gRPC \| NFR-001/NFR-008 / AC-065, AC-087 |
| [A12 · L293](../../../02-PRD.md#63-assessment-coverage) | Unit tests including complete relevant edge cases \| NFR-008 / AC-086, AC-088 |
| [A16 · L297](../../../02-PRD.md#63-assessment-coverage) | Setup/run/AI-use README \| NFR-010/NFR-011 / AC-091, AC-096 |
| [D02 · L305](../../../02-PRD.md#64-project-decision-register) | Three App/Catalog/Media applications; two remains the assessment minimum \| NFR-002 / AC-067; techdoc |
| [D03 · L306](../../../02-PRD.md#64-project-decision-register) | GraphQL frontend and gRPC backend boundaries \| NFR-001/NFR-013 / AC-065, AC-066, AC-100 |
| [D04 · L307](../../../02-PRD.md#64-project-decision-register) | Vertical slices \| NFR-009 / AC-089; techdoc |
| [D05 · L308](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D07 · L310](../../../02-PRD.md#64-project-decision-register) | Viewer/Maintainer and prepared accounts \| Viewer/Maintainer roles and prepared accounts are adopted as Committed policy in section 13.1; FR-011/FR-012 / AC-049, AC-050, AC-111 |
| [D10 · L313](../../../02-PRD.md#64-project-decision-register) | Catalog owns titles/people/credits, Media owns assets \| NFR-002 / AC-067, AC-068 |
| [D12 · L315](../../../02-PRD.md#64-project-decision-register) | Authentication mechanism and verified internal identity \| Product access settled; mechanics Open in techdoc. FR-011/FR-012 / AC-047 through AC-052 |
| [D13 · L316](../../../02-PRD.md#64-project-decision-register) | Compose and Make wrappers \| NFR-010 / AC-091 through AC-094 |
| [D17 · L320](../../../02-PRD.md#64-project-decision-register) | SOLID/DRY/KISS/YAGNI, code quality, constructor injection \| NFR-009 / AC-089, AC-090; techdoc |
| [D19 · L322](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [D21 · L324](../../../02-PRD.md#64-project-decision-register) | Bounded safe retries and one configured exhaustion channel \| NFR-004/NFR-005, FR-015 / AC-061 through AC-063, AC-072 through AC-078 |
| [D22 · L325](../../../02-PRD.md#64-project-decision-register) | Initial retry settings: 3 retries after the initial attempt (at most 4 total application attempts), initial interval 1,000 ms, multiplier 2, maximum interval 30,000 ms \| These are proposed starting values, not finalized per-operation policies. NFR-004 / AC-072, AC-074; section 10.1 |
| [D23 · L326](../../../02-PRD.md#64-project-decision-register) | Health/readiness, logs, metrics, dashboards, actionable alerts \| NFR-006 / AC-079 through AC-082 |
| [AC-050 · L440](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Role matrix \| Visitors, Viewers, and Maintainers try each capability \| An action is requested \| Results match section 3.1, including direct attempts to access hidden actions; all Maintainers have the same management permissions. |
| [AC-050-C01 · L440](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Results match section 3.1, including direct attempts to access hidden actions; all Maintainers have the same management permissions. |
| [AC-071 · L475](../../../02-PRD.md#us-012-preserve-saved-work) | Restart persistence \| Saved records, archives, artwork, audit, and pending/failed work exist \| The stack stops/starts or restarts \| Those outcomes remain available; recovery continues within its retained budget and no normal command silently resets data. |
| [AC-071-C01 · L475](../../../02-PRD.md#us-012-preserve-saved-work) | Those outcomes remain available; recovery continues within its retained budget and no normal command silently resets data. |
| [AC-153 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Valid policy override \| A documented deployment configuration changes an applicable text/file minimum or maximum, or enables a nonempty subset of the static formats \| The configuration becomes effective using the techdoc's documented application procedure \| Forms and authoritative validation use the same effective policy. Show changed guidance and enforce inclusive new boundaries through direct requests and MCP as well. Required fields stay required; empty search still clears the text restriction even if the nonempty-query minimum increases. A disabled format is rejected for new uploads. No Maintainer configuration screen is implied. |
| [AC-153-C01 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Forms and authoritative validation use the same effective policy. |
| [AC-153-C02 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show changed guidance and enforce inclusive new boundaries through direct requests and MCP as well. |
| [AC-153-C03 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Required fields stay required; empty search still clears the text restriction even if the nonempty-query minimum increases. |
| [AC-153-C04 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | A disabled format is rejected for new uploads. |
| [AC-153-C05 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | No Maintainer configuration screen is implied. |
| [AC-154 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid policy configuration \| A text bound is negative/nonintegral/nonfinite, a required minimum permits emptiness, file size is nonpositive, minimum exceeds maximum, a setting is explicitly invalid/empty, or the format set is empty/unsupported \| The policy is loaded or applied \| Affected functionality does not become ready under the invalid policy. Restricted diagnostics identify the setting and correction without secrets; no silent unlimited limit, supported-format expansion, or unsafe fallback occurs. An unset value follows only its documented default. Application/reload behavior remains OD-020. |
| [AC-154-C01 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Affected functionality does not become ready under the invalid policy. |
| [AC-154-C02 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Restricted diagnostics identify the setting and correction without secrets; no silent unlimited limit, supported-format expansion, or unsafe fallback occurs. |
| [AC-154-C03 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | An unset value follows only its documented default. |
| [AC-154-C04 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Application/reload behavior remains OD-020. |
| [AC-155 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing data after tightening \| Existing text/artwork were accepted under older limits, or an account's password was valid before new-password bounds changed \| Limits tighten, the existing record is read, an unchanged stored field is retained during another edit, or the user signs in \| Do not truncate/delete stored text or artwork or invalidate the existing credential solely for the new limits. Keep authorized reads available and permit unchanged fields to remain while validating newly changed input. Invalid new replacement/upload still fails; ordinary reruns do not overwrite preserved data to force conformity. |
| [AC-155-C01 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not truncate/delete stored text or artwork or invalidate the existing credential solely for the new limits. |
| [AC-155-C02 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Keep authorized reads available and permit unchanged fields to remain while validating newly changed input. |
| [AC-155-C03 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid new replacement/upload still fails; ordinary reruns do not overwrite preserved data to force conformity. |
| [AC-157 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Policy changes during a draft/upload \| Input or artwork met earlier limits, and the policy changes before submission/attachment \| The operation is evaluated against the effective policy \| If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-157-C01 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. |
| [AC-157-C02 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-064 · L857](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Build and inspect the mandated frontend/backend technologies and selected compatible toolchain \| NFR-001; technical design owner unassigned |
| [AC-065 · L858](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise meaningful real gRPC features across at least two running Spring Boot applications \| NFR-001; technical design |
| [AC-067 · L860](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Validate the preferred App/Catalog/Media responsibilities and absence of cross-service database access \| NFR-002; technical design |
| [AC-074 · L864](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Validate retry overrides/units/ranges; zero disables retries; D22 numerical defaults remain Recommended \| NFR-004/NFR-005; technical design |
| [AC-076 · L865](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify required configuration and redacted startup/not-ready failure without unsafe fallback \| NFR-005; technical design |
| [AC-077 · L866](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify documented precedence and explicit missing/empty/false/zero semantics \| NFR-005; technical design |
| [AC-079 · L867](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Distinguish process health, readiness, and actual operation success \| NFR-006; technical design |
| [AC-087 · L872](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration \| NFR-008; technical design |
| [AC-088 · L873](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Report failures/blocked/unexecuted checks honestly; test commands fail correctly and isolate normal data \| NFR-008; technical design |
| [AC-089 · L874](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Review cohesive tested vertical slices, shared rules, constructor dependencies, and justified abstractions \| NFR-009; technical design |
| [G-02 · L925](../../../02-PRD.md#122-release-gates-remain-planned) | Mandated stack/protocols and chosen integration approach work \| NFR-001/NFR-002/NFR-013; AC-064 through AC-067, AC-100 through AC-103 \| Planned in techdoc |
| [G-03 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-04 · L927](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
| [G-05 · L928](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [G-06 · L929](../../../02-PRD.md#122-release-gates-remain-planned) | Responsive/keyboard journeys, restart persistence, real integration, and independent setup work \| NFR-003/NFR-007 through NFR-010; US-015 through US-017 \| Planned |
| [J-05 · L947](../../../02-PRD.md#123-delivery-responsibilities-and-journeys) | Assessment reviewer runs the assessment \| Repository access and documented prerequisites \| Obtain the initial catalog, exercise product behavior, run documented checks, and verify persistence across restarts |
| [AC-078 · L989](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Secrets \| Setup uses account/provider/key material \| Documentation and diagnostics are inspected \| Real secrets are absent from repository examples, user messages, logs, audit, and notifications. |
| [AC-078-C01 · L989](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Real secrets are absent from repository examples, user messages, logs, audit, and notifications. |
| [AC-091 · L1005](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | Fresh setup \| A clean checkout and documented prerequisites are available \| The assessment reviewer follows setup and seed instructions \| The runnable stack and correct initial catalog become available, with clear access details or actionable startup failure. |
| [AC-091-C01 · L1005](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | The runnable stack and correct initial catalog become available, with clear access details or actionable startup failure. |
| [AC-092 · L1006](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | Normal operations \| Edits, artwork, and archived movies/people exist \| Documented stop/start/restart/status/log actions run \| Preserve data and both archive states, verify readiness where appropriate, and expose diagnostic state without secrets. No implicit destructive reset occurs. |
| [AC-092-C01 · L1006](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | Preserve data and both archive states, verify readiness where appropriate, and expose diagnostic state without secrets. |
| [AC-092-C02 · L1006](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | No implicit destructive reset occurs. |
| [AC-093 · L1007](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | Verification commands \| Normal application data exist \| Unit/integration checks run, including a failing case \| Tests use isolated data, report failures honestly, and leave normal records/artwork intact. |
| [AC-093-C01 · L1007](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | Tests use isolated data, report failures honestly, and leave normal records/artwork intact. |
| [AC-094 · L1008](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | Repeatability \| A second environment follows the same documented setup/snapshot \| Setup completes \| Equivalent supported behavior and initial records result; platform and external-client/provider prerequisites are explicit. Offline operation is not promised. |
| [AC-094-C01 · L1008](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | Equivalent supported behavior and initial records result; platform and external-client/provider prerequisites are explicit. |
| [AC-094-C02 · L1008](../../../02-PRD.md#us-017-reproduce-and-submit-the-assessment) | Offline operation is not promised. |
| [OD-002 · L1021](../../../02-PRD.md#131-resolved-product-decisions) | Public discovery; Viewer and Maintainer; same permissions for all Maintainers; no role-management UI \| FR-011/FR-012; AC-049/AC-050/AC-113 |
| [OD-015 · L1054](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Mandated technologies and working ownership retained; compatible versions, database/build/test/migration choices, and detailed boundaries remain for techdoc \| NFR-001/NFR-002/NFR-003/NFR-008 \| Technical design; before build/contracts |
| [OD-020 · L1058](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Startup/secret protection and input-policy configuration/compatibility outcomes fixed; config keys/inventory/sources/precedence, distribution, startup/application/reload mechanics, and secure key/provider provisioning remain for techdoc \| NFR-005/NFR-015/NFR-016, FR-017 \| Technical design; before runnable setup |
| [US-012 · L469](../../../02-PRD.md#us-012-preserve-saved-work) | ### US-012: Preserve saved work As a Maintainer, I want my saved changes to remain available after the application restarts. Requirements: NFR-003. Persistence constraints and AC-069/AC-070 are defined in section 10. |
| [TD-C-058 · L6042](../../../04-spoolin-techdoc.md#112-startup-dag-and-readiness) | Startup DAG validates host/trust/config/images/DB/migrations/seed/apps/MCP/monitoring with bounded stages; exactly seven Make contracts and honest exits, normal persistence retained, integration isolated and blocked required checks exit2. |
| [TECH-G-01 · L6517](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria) | G-01 dependency/image lock \| Resolve pinned JVM/npm/generated-tool peer graph, architecture-specific immutable images and tunnel runner/flags; verify licenses/advisories and reproducible build \| Build/runtime setup; build owner unassigned |
| [TD-U-002 · L6535](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes) | TD-U-002 \| `make start` includes migrations, safe seed, demo accounts, apps, MCP and operational monitoring \| D13, OD-006/020, AC-091–094; separate manual seed/migration startup is superseded |
| [V-CONFIG · L6396](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-CONFIG \| Missing/empty/false/zero/out-of-range/malformed settings, every RATE_LIMIT_* profile plus key file/revision divergence; assert readiness fail with redacted correction, documented precedence, policy revision consistency and unchanged stored values after tightening |
| [TD-ROW-L6075 · L6075](../../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts) | `make start` \| Validate→images→DB→migrations→seed/accounts→apps/MCP/monitoring/tunnel→bounded checks; idempotent rerun, volumes retained \|0 local selected services ready; nonzero actionable stage failure; external interaction unverified explicitly |
| [TD-ROW-L6076 · L6076](../../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts) | `make stop` \| Stop accepting work, drain, close tunnel/apps/monitoring/DB; preserve named volumes/config/secrets/artwork \|0 stopped; nonzero drain/stop failure; never `down -v` |
| [TD-ROW-L6077 · L6077](../../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts) | `make restart` \| Graceful stop then normal start with same persistent installation \|0 ready; failure retains diagnostic state/data |
| [TD-ROW-L6078 · L6078](../../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts) | `make status` \| Read-only process/readiness/seed/work/notification/tunnel state, redacted \|0 healthy selected local stack,1 degraded/not-ready; external integration evidence separate |
| [TD-ROW-L6079 · L6079](../../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts) | `make logs` \| Redacted bounded tail (default 200lines/service), follow only explicit flag; correlation filter allowed; preserve complete JSON records without color/prefix text (§12.2.1) \|0 readable logs,1 failure; no unbounded dump or secret environment |
| [TD-ROW-L6080 · L6080](../../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts) | `make test-unit` \| Deterministic unit/contract checks using isolated temporary artifacts, no normal DB credentials \|0 all selected checks passed,1 test failures,2 setup/config failure |
| [TD-ROW-L6081 · L6081](../../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts) | `make test-integration` \| Disposable Compose/Testcontainers namespace and independent DBs/artwork/secrets/ports; PostgreSQL 18.6+real gRPC; cleanup only test namespace \|0 all required runnable checks passed,1 test failure,2 prerequisites/blocked required checks; report skipped client/provider checks separately, no false pass |
| [TD-BLOCK-L3642 · L3642](../../../04-spoolin-techdoc.md#partial-date-validation-function) | Partial-date validation function — complete sql contract, all members, constraints and field numbers |
| [BRAIN-A01 · L48](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A01 \| Svelte frontend in TypeScript \| Frontend source and working build |
| [BRAIN-A02 · L49](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A02 \| Spring Boot services in Kotlin \| Backend source and runnable applications |
| [BRAIN-A03 · L50](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A03 \| At least two Spring Boot services \| Distinct running services with meaningful responsibilities |
| [BRAIN-A04 · L51](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A04 \| Service-to-service communication via gRPC \| Real RPC calls supporting application features; protobuf contracts and integration checks |
| [BRAIN-A12 · L59](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A12 \| Unit tests with complete cases, including edges \| Requirement-linked business and boundary cases; no unsupported coverage guarantee |
| [BRAIN-A16 · L63](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A16 \| README explaining setup, execution, and AI usage \| Reproducible instructions and accurate disclosure |
| [BRAIN-D02 · L73](../../../01-brainstorming-result.md#4-decision-register) | D02 \| Working design \| Three Kotlin/Spring Boot applications: `app-service`, `catalog-service`, `media-service` |
| [BRAIN-D03 · L74](../../../01-brainstorming-result.md#4-decision-register) | D03 \| Required \| GraphQL for frontend application operations; gRPC between backend services |
| [BRAIN-D04 · L75](../../../01-brainstorming-result.md#4-decision-register) | D04 \| Committed \| Organize implementation around vertical slices |
| [BRAIN-D05 · L76](../../../01-brainstorming-result.md#4-decision-register) | D05 \| Committed \| Design for authentication, authorization, RBAC, and audit logging from the start; implement core functionality first, then complete security |
| [BRAIN-D07 · L78](../../../01-brainstorming-result.md#4-decision-register) | D07 \| Recommended \| Viewer/Maintainer roles and predefined initial accounts; exact permission policy and account provisioning remain open |
| [BRAIN-D10 · L81](../../../01-brainstorming-result.md#4-decision-register) | D10 \| Working design \| Keep movies, people, and credits within Catalog; keep asset lifecycle in Media |
| [BRAIN-D12 · L83](../../../01-brainstorming-result.md#4-decision-register) | D12 \| Open \| Select authentication/session mechanism and verified identity propagation through gRPC |
| [BRAIN-D13 · L84](../../../01-brainstorming-result.md#4-decision-register) | D13 \| Committed \| Docker Compose with Make wrappers for reproducible startup, shutdown, restart, inspection, and testing |
| [BRAIN-D17 · L88](../../../01-brainstorming-result.md#4-decision-register) | D17 \| Committed \| Apply code quality, SOLID, DRY, KISS, YAGNI, and constructor dependency injection within vertical slices |
| [BRAIN-D19 · L90](../../../01-brainstorming-result.md#4-decision-register) | D19 \| Committed \| Unit and integration tests, including real gRPC communication and persistence against the selected database engine |
| [BRAIN-D21 · L92](../../../01-brainstorming-result.md#4-decision-register) | D21 \| Committed \| Bounded exponential backoff with jitter for safe transient failures; environment-configurable retry policy and exhaustion notification through one configured channel |
| [BRAIN-D22 · L93](../../../01-brainstorming-result.md#4-decision-register) | D22 \| Recommended \| Initial retry defaults in section 9.2; exact operation policies must respect deadlines and idempotency |
| [BRAIN-D23 · L94](../../../01-brainstorming-result.md#4-decision-register) | D23 \| Committed \| Health/readiness checks, correlated logs, metrics, dashboards, and actionable alerts; numerical SLOs and any SLA remain open |

## Implementation

1. Complete the Makefile targets and Compose runtime from their prerequisite owners. Validate host/tool/trust/protected-file configuration, build/image locks and selected runner flags before attempting startup.
2. Implement ordered bounded DB→owner migrations/grants→seed/accounts→apps/MCP/monitoring/tunnel readiness. Only print successful browser URL when the usable local application and selected dependencies are ready; external client interaction remains separately unverified.
3. Implement graceful bounded drain/stop/restart retaining named volumes, keys/configuration/artwork and durable budgets; never implicitly reset or use volume deletion. Preserve interrupted/failed diagnostics and original cause.
4. Implement read-only status with healthy/degraded result and seed/work/notification/tunnel state. Logs default200 complete JSON lines/service, explicit follow/filter, no color/prefix or secret dump.
5. Exercise exact §11.3 exit codes:0success, common2prerequisite,3build,4migration/seed,5readiness,130interruption; test commands1failure/2setup. Confirm normal sentinel data survive tests and commands.
6. For ATDD-220, exercise runnable, failing and required-unavailable task selections to prove command exit/cleanup/isolation contracts. A selected available owner/browser suite may demonstrate exit0; a missing live-client prerequisite must demonstrate exit2. Full aggregate success after every real client/provider obligation is reconciled belongs to T046 release closure.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T039-A1:** One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps.
- **T039-A2:** Persistence: stop/start/restart/forced termination preserve edited records, archives, keys, artwork, receipts and retained retry budget.
- **T039-A3:** Truthful exits: failure at each stage returns its specified nonzero result; health/status never asserts actual client/provider approval or delivery.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Use checks alongside implementation because build/visual/runtime composition must be exercised as its interfaces appear; acceptance still governs before completion. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-065](../../../05-atdd.md#case-atdd-065-restart-persistence) / System | Modifiedtitle/person/archives/OLD, audit, pending work with attempts and failed notification; stop/start and restart; compare exact persistent state. | Then Those outcomes remain available; recovery continues within its retained budget and no normal command silently resets data. Observation: Snapshot persisted domain/accounts/drafts/audit/work/notification state before stop/start/restart and compare fresh owner reads after recovery. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-068](../../../05-atdd.md#case-atdd-068-secrets) / System | Unique synthetic canaries in secret files; scan repository/examples/process diagnostics/errors/audit/logs/metrics/traces/notifications; report no values. | Then Real secrets are absent from repository examples, user messages, logs, audit, and notifications. Observation: Search repository examples and actual diagnostics/errors/logs/audit/metrics/traces/notifications for synthetic secret canaries; show only protected file references. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-072](../../../05-atdd.md#case-atdd-072-fresh-setup) / System | CleancheckoutwithdocumentedDockerComposev2/Make/ports/localTLS/config/secrets/client registration as applicable; onlymake start. | Then The runnable stack and correct initial catalog become available, with clear access details or actionable startup failure. Observation: Use clean isolated checkout prerequisites and only make start; inspect migrated/seeded/accounts/ready browser URL and separately reported client/provider prerequisites. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-073](../../../05-atdd.md#case-atdd-073-normal-operations) / System | Modifiednormalinstallation; eachstop/start/restart/status/logs contract in independent row; preserve keys/ciphertext/accounts/archives/assets/ledgers. | Then Preserve data and both archive states, verify readiness where appropriate, and expose diagnostic state without secrets. No implicit destructive reset occurs. Observation: Capture actual stop/start/restart/status/log behavior and persistent-state hashes; each command follows the documented exits and protects keys/ciphertext/assets. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-074](../../../05-atdd.md#case-atdd-074-verification-commands) / System | Normaldatahashbaseline; unit test fail/integration fail/prerequisite blocked; isolated namespace cleanup and normal data unchanged. | Then Tests use isolated data, report failures honestly, and leave normal records/artwork intact. Observation: Invoke isolated unit/integration failed and prerequisite-blocked selections; inspect exits1/2, proper task boundaries and normal-data hashes/cleanup scope. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-075](../../../05-atdd.md#case-atdd-075-repeatability) / System | Two supported macOS/Linux arm64/amd64 environments same locked manifest; compare semantic records/behavior, explicit on line/client prerequisites. | Then Equivalent supported behavior and initial records result; platform and external-client/provider prerequisites are explicit. Offline operation is not promised. Observation: Compare locked semantic seed/record/behavior across declared macOS/Linux architectures; record actual external prerequisites rather than claiming offline client/provider proof. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-134](../../../05-atdd.md#case-atdd-134-build-the-selected-stack-from-a-clean-checkout) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §2.1/2.4 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then resolved versions, licenses and image digests match §2.1 And generated GraphQL/protobuf/token outputs rebuild without committed ignored files And the actual JAR/image loads SDL, SPA fallback, migrations, mounted seed and bundled review UI Observation: Capture locked graph/image digests per architecture, clean generator outputs and a packaged JAR/resource listing. Start only the isolated built image and verify SDL, SPA fallback, migrations, seed mount and review resource are loadable; a source-file presence check is insufficient. |
| [ATDD-135](../../../05-atdd.md#case-atdd-135-enforce-service-and-network-ownership) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §2.2/4/12.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then each service can use only its owned database and permitted typed service methods And database, gRPC, exporters and administration remain private And loopback browser/dashboard and MCP tunnel access expose only their documented entry points Observation: Run the credential-by-database/listener matrix and retain exact denied operations plus private listener/loopback bindings. Inspect dependency ownership paths; no successful cross-owner SQL or public internal listener is permitted. |
| [ATDD-202](../../../05-atdd.md#case-atdd-202-apply-validated-configuration-by-presence-and-revision) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §11.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then presence gives environment precedence overfile then documented default; explicit invalid is never treated asunset And secrets require mounted files and badranges/units/provider dependencies/physical ceilings block affected readiness with redacted correction And rolling restart publishes onepolicy hash; divergent protected writes fail without hotreload or rewriting legacy data Observation: Resolve and inspect actual effective configuration for every F-CONFIG group and known key; independently vary environment/file/default, empty/false/zero and each bound. Capture redacted readiness correction and shared policy revision before/after restart; stored legacy values remain intact. |
| [ATDD-203](../../../05-atdd.md#case-atdd-203-make-one-command-establish-a-usable-local-installation) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §11.2/11.3 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then dependency order applies migrations/seed/accounts before bounded final readiness and prints the usablebrowserURL And stage failures use documented nonzero exits and preserve data Observation: Invoke isolated startup with each single-stage fault and retain actual exit/stage/timing plus existing data hashes. Check final browser URL and truthful component/client/provider status; image and readiness budgets are observed separately and no external registration is fabricated. |
| [ATDD-204](../../../05-atdd.md#case-atdd-204-drain-bounded-work-and-survive-a-forced-stop) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §12.3 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then readiness drops, newclaims/writes stop and requests/workers drain at most30 s each And stream/process/channel/pool resources close before database shutdown And committed receipts/leases recover after forced kill without relying on shutdown callbacks or deleting volumes Observation: Observe real readiness and claim/write refusal before drain, with active request/stream/processor barriers. Capture bounded resource closure and forced-kill restart receipts/leases; correctness must survive when shutdown callbacks never execute. |
| [ATDD-205](../../../05-atdd.md#case-atdd-205-reject-overload-before-exhausting-protected-resources) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §12.1/7.6 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then bounded pools8perowner/maxDB50, workers2perowner, uploads2peractor/processors2 prevent unbounded buffering And excess work gets truthful capacity feedback rather than silent queue growth or partial attachment And critical disk refusal protects databaseWAL and referenced artwork while read/failure state remains useful Observation: Saturate each actual bounded pool/worker/upload/processor and critical disk condition independently. Measure active/buffered work and capacity response while rereading protected DB/artwork; reject excess rather than merely counting fake executor submissions. |
| [ATDD-214](../../../05-atdd.md#case-atdd-214-initialize-the-complete-stack-with-one-command) / System | F-STACK prerequisites, frozen F-SEED, prepared secret-file bootstrap; image stage≤15 min and final readiness≤300 s; external actual-client interaction stays separately unverified | Then success returns0 only after selected local services are ready and the printed browser URL serves the usable application; no manual seed/migrate/service step remains And failure returns2 prerequisites/config, 3 image/build, 4 migration/seed, 5 readiness or130 interruption with its original redacted stage cause And concurrent/repeated starts preserve existing accounts, keys, artwork and seed decisions Observation: Run only in the later isolated command acceptance environment. Record shell exit, bounded redacted stdout/stderr, readiness and before/after normal/test-state checks. Match each row to §11.3 exactly; test doubles do not establish persistence, network or external integration. These command specifications are not executed during ATDD authoring. |
| [ATDD-215](../../../05-atdd.md#case-atdd-215-stop-without-deleting-persistent-work) / System | F-CATALOG/F-ART/F-CRYPTO/F-WORK; snapshot state before graceful drain and forced interruption; no down -v | Then success returns0 with the selected stack stopped and named volumes/config/protected keys intact And drain/stop failure returns nonzero without implicit destructive reset And a later start recovers the saved committed receipts and original pending budgets Observation: Run only in the later isolated command acceptance environment. Record shell exit, bounded redacted stdout/stderr, readiness and before/after normal/test-state checks. Match each row to §11.3 exactly; test doubles do not establish persistence, network or external integration. These command specifications are not executed during ATDD authoring. |
| [ATDD-216](../../../05-atdd.md#case-atdd-216-restart-and-verify-the-same-installation) / System | Edited F-STACK namespace with independently measured before/after rows, assets and key IDs; barrier during stop and start | Then success returns0 only after graceful stop then normal bounded start restores readiness And restart preserves database data, account/key state, artwork, archive and recovery evidence And failure retains diagnostic state and original stage exit rather than claiming ready Observation: Run only in the later isolated command acceptance environment. Record shell exit, bounded redacted stdout/stderr, readiness and before/after normal/test-state checks. Match each row to §11.3 exactly; test doubles do not establish persistence, network or external integration. These command specifications are not executed during ATDD authoring. |
| [ATDD-217](../../../05-atdd.md#case-atdd-217-report-health-separately-from-effects-and-integration-evidence) / System | One independently reset row per status condition; include RECORDED/ACCEPTED/UNKNOWN/FAILED notifications and pending/terminal work; compare no state mutation | Then healthy selected local stack returns0 and degraded/not-ready returns1 And process/readiness/seed/work/notification/tunnel status is read-only and redacted And running or connected never substitutes for actual business success, provider delivery or client approval evidence Observation: Run only in the later isolated command acceptance environment. Record shell exit, bounded redacted stdout/stderr, readiness and before/after normal/test-state checks. Match each row to §11.3 exactly; test doubles do not establish persistence, network or external integration. These command specifications are not executed during ATDD authoring. |
| [ATDD-218](../../../05-atdd.md#case-atdd-218-bound-useful-logs-without-exposing-secret-payloads) / System | F-PRIVACY canaries from TD-PRIVACY, more than200safe lines per service, valid/malformed correlation IDs; explicitly terminate follow without altering application state | Then default returns a bounded tail of200lines per service and0 when readable; retrieval failure returns1 And following requires the explicit flag and correlation filtering stays useful And no credential, sensitive body, SQL parameter, provider URL or secret environment is printed Observation: Run only in the later isolated command acceptance environment. Record shell exit, bounded redacted stdout/stderr, readiness and before/after normal/test-state checks. Match each row to §11.3 exactly; test doubles do not establish persistence, network or external integration. These command specifications are not executed during ATDD authoring. For selected application diagnostics, independently parse each retained physical line as one physical JSON object with required fields and resource.service.name, without Compose/service-name prefixes, ANSI text or split records. Parsed UUID correlation filtering retains complete objects, never matching arbitrary body text; default remains the last 200 lines per service and explicit follow/exit behavior is unchanged. Classify native PostgreSQL/monitoring/tunnel output separately under its own restricted format and human command/status notices separately; those streams are not asserted to implement application NDJSON. Use the actual managed log rotation within 10 MiB×5/service, without claiming a fixed retention duration. Complement ATDD-342 for real encoder/framework/queue/stdio and sink-failure enforcement; successful retrieval cannot establish audit durability or lossless diagnostics. |
| [ATDD-219](../../../05-atdd.md#case-atdd-219-fail-unit-verification-truthfully-in-isolation) / System | Disposable build artifacts and deterministic fixtures; normal installation canary hashes before/after; generated-contract/tool lock prerequisites explicit | Then exit0 requires all selected unit/contract checks passing; test failure returns1 and setup/config failure2 And generated build prerequisites run but owner/cross-service integration, browser and live-client/provider suites are excluded from this command And no normal database credentials or data are used or reset Observation: Invoke the real isolated make test-unit wrapper with a deliberately failing selected unit check and with a missing prerequisite. Inspect exit1/2, selected frontend/MCP/root contracts/JVM unit tasks, generation/compilation prerequisites and unchanged normal-data hashes. No owner integrationTest, cross-service/browser/live-provider/client task may be silently included. This System case proves command selection; the explicit Unit cases prove isolated rules. |
| [ATDD-220](../../../05-atdd.md#case-atdd-220-verify-real-boundaries-without-touching-normal-data) / System | Normal app catalog/art/key canary hashes; disposable F-STACK; real configured provider/client evidence for every selected claim and explicit unavailable prerequisites | Then real isolated PostgreSQL 18.6, gRPC and required runnable browser/owner checks run with independent keys, ports and volumes And exit0 means all selected required checks pass;1 means a test failed;2 means setup or a blocked required prerequisite, never a passed skip And normal installation names are refused, cleanup touches only this test namespace and preserves original failure when cleanup also fails Observation: Invoke actual isolated make test-integration with runnable, failed and required-blocked rows. Inspect disposable namespace/credentials/volumes, migrations/initialization, owner integrationTest/cross-service/browser selection, exits0/1/2 and cleanup preserving the original failure. Real client/provider evidence remains separately attributable and normal application data stays unchanged. |
| [ATDD-327](../../../05-atdd.md#case-atdd-327-apply-rate-configuration-through-validated-restart-with-persistent-windows) / System | Every10key precedence/default/bound case; twoCore replicas, unchangedprofile restart with partiallyconsumed window and changedprofile deployment; enabledadapter resource budgets. Real protected Core key-file missing/wronglength/wrongpermissions; different replica key canary or profile revision; explicit MAX change versus WINDOW change. | Then invalid/nonpositive/out-of-bound config blocks affected readiness with redacted correction And unchanged restart preserves shared consumed windows rather than granting fresh quota And changed settings and policy agreement follow selected deployment semantics; adapter bounds remain independently enforced Observation: Capture actual resolved config/revision and fresh bucket state before/after restart on both replicas; distinguish changed-window policy from same-policy persistence. Defaults stay unchanged and isolated measurement profiles are never presented as production defaults. Check actual32-byte owner-only key provisioning, no MCP key mount/sharing, private nonlogged canary equality and readiness refusal on divergent key/config. MAX-only changes reuse same-duration counters; WINDOW changes create the selected aligned duration buckets and retain old rows until expiry, without a cross-revision quota claim. |
| [ATDD-342](../../../05-atdd.md#case-atdd-342-enforce-json-diagnostics-and-bounded-sink-failure-behavior) / Integration | F-LOG independent boot/framework/ORM/ingress/worker records: ready, validation/conflict, CSRF/quota denial, dependency outage, retry/uncertain effect, recovery exhaustion and crypto authentication failure, including HTTP200 domain/system failures. Use actual info/debug restarts, invalid LOG_LEVEL, record boundaries16384bytes, queue occupancy1023/1024/1025,10MiB×5rotation; slow consumer, stopped reader, disk-full/permission and broken-pipe failures. Inject synthetic private values only in transient isolated capture and retain redacted results. | Then application-owned records remain bounded private-safe JSON with the selected field shapes and levels And stdio MCP stdout remains protocol-only while diagnostics use stderr And slow or failed sinks retain bounded queues and observable dropped counts without recursive or raw fallback And committed receipts, work attempts and audit retain their source-defined truth independently of diagnostic delivery Observation: Independently parse each physical application diagnostic line from actual JVM SLF4J/Logback, ORM/framework adapters and Node helpers, including boot before context and early failures. Assert one LF-terminated complete UTF-8 object, required fields/resources and exact info/warning/error/fatal mapping for the named event rows. Suppress plain Boot banners/ANSI/raw console/stacktrace/body/header/SQL parameter/RPC dumps in both modes. Unknown/private fields cannot escape through messages, causes, caller or dropped-value lists. Simulated encoder output cannot establish actual framework configuration. Change only selected LOG_LEVEL through source precedence and validated restarts: default info suppresses optionaldebug; exact debug enables permitted detail and returning info suppresses it; required lifecycle/security warning/error/fatal events remain in both. Invalid selected values fail actual readiness/start with the documented command exit and safe correction, never silently falling back. Fatal logging alone is not the failure-exit oracle. Verify finite bounded attributes, fixed summaries/error_class, code/outcome and timestamp event time; userinput/Throwable canaries must be absent at intermediate/final captures and alternate streams. View decoded strings through the actual operator viewing path as escaped text, without executing markup; no production UI page is invented. Drive an actual queue fill with a held sink at 1023/1024 and attempted 1025 records, inspect maximum occupancy and drop counter, then resume/close the consumer. Use existing request/worker total deadlines to observe terminal outcomes rather than sleep-only oracles; diagnostics cannot indefinitely block them. Oversized/invalid required records produce only safe contract error/count, optionalinvalid fields omit/count, all emitted lines &lt;= 16,384 bytes with no partial serialized truncation. When disk/pipe fails, inspect the alternate diagnostic stream for the same JSON/redaction, no recursion/raw fallback, and the private exporter for increased dropped-record count despite absent logs. Do not require a lost record to reappear or exact record counts under failure. Run stdio bridge initialization/calls and its actual logger-failure paths: stdout contains valid negotiated protocol messages exclusively, stderr safe diagnostics; service container application records have resource identity and no prefixes. Keep native PostgreSQL/monitoring/tunnel output and human command/status output separately scoped; do not rewrite their protocols to satisfy JSON. Observe real10MiB×5/service rotation and bounded make logs with218; framework log content remains operator-only. For the same independently reset successful mutation/work claim/completion and rollback/crash points, disable/block/fail log delivery and query actual receipt/durable_work/operational_event/catalog_audit_event from fresh readers. Existing required commits/rollback/attempt phases, authorized audit projection and retention are unchanged: log start is not successful mutation, no log does not prove rollback, threshold/drop cannot suppress durable evidence. Confirmation/approval/publication and RECORDED/ACCEPTED/DELIVERY_CONFIRMED remain distinct. Disk/pipe losses are truthful best-effort diagnostic evidence; real database/worker cases retain their own independent proof. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| boundary-tests | `bash ops/bin/test-integration.sh --task T039` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
