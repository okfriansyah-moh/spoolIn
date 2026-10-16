# Plan numbering migration v1

Source revision: `ffcaae535b55d75b94256946c4c340e30c509048` (merged PR #6). This mapping changes planning identities and navigation; it establishes no new product acceptance.

## Task identities

IDs before this migration belong to the source revision above. Historical reports, commits and Evidence rows retain those IDs and their original artifact paths. Current IDs refer to the new plan.

| Previous ID | Current ID | Previous group | Current group | Status retained |
|---|---|---|---|---|
| T001 | [T001](plan/01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md) | G01 | G01 | Done |
| T002 | [T002](plan/01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md) | G01 | G01 | Done |
| T003 | [T003](plan/01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md) | G01 | G01 | Done |
| T004 | [T004](plan/02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md) | G02 | G02 | Done |
| T005 | [T005](plan/03-account-access-credential-storage/tasks/T005-protect-account-identities-and-credential-storage.md) | G03 | G03 | Done |
| T009 | [T006](plan/04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md) | G05 | G04 | In progress |
| T010 | [T007](plan/05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md) | G06 | G05 | In progress |
| T006 | [T008](plan/06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md) | G03 | G06 | In progress |
| T008 | [T009](plan/07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md) | G04 | G07 | In progress |
| T007 | [T010](plan/08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md) | G03 | G08 | In progress |
| T011 | [T011](plan/09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md) | G06 | G09 | Not started |
| T012 | [T012](plan/10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md) | G07 | G10 | Not started |
| T013 | [T013](plan/10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md) | G07 | G10 | Not started |
| T014 | [T014](plan/11-draft-recovery-storage-and-controllers/tasks/T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts.md) | G08 | G11 | Not started |
| T015 | [T015](plan/11-draft-recovery-storage-and-controllers/tasks/T015-build-account-isolated-browser-draft-controllers.md) | G08 | G11 | Not started |
| T017 | [T016](plan/12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md) | G09 | G12 | Not started |
| T018 | [T017](plan/12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md) | G09 | G12 | Not started |
| T019 | [T018](plan/13-people-and-credits/tasks/T018-implement-shared-person-writes-and-identity.md) | G10 | G13 | Not started |
| T020 | [T019](plan/13-people-and-credits/tasks/T019-connect-people-management-and-retained-edits.md) | G10 | G13 | Not started |
| T021 | [T020](plan/13-people-and-credits/tasks/T020-implement-ordered-credits-and-custom-contribution-roles.md) | G10 | G13 | Not started |
| T022 | [T021](plan/13-people-and-credits/tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md) | G10 | G13 | Not started |
| T025 | [T022](plan/14-artwork-management-processing-and-attachment/tasks/T022-accept-and-process-bounded-artwork-streams.md) | G12 | G14 | Not started |
| T026 | [T023](plan/14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md) | G12 | G14 | Not started |
| T023 | [T024](plan/15-archive-management/tasks/T024-implement-current-impact-archive-restore-and-deletion.md) | G11 | G15 | Not started |
| T024 | [T025](plan/15-archive-management/tasks/T025-connect-private-archives-and-irreversible-confirmations.md) | G11 | G15 | Not started |
| T027 | [T026](plan/16-artwork-management-browser-journeys/tasks/T026-connect-measured-progress-and-recover-attachment-outcomes.md) | G12 | G16 | Not started |
| T016 | [T027](plan/17-draft-recovery-editor-integration/tasks/T027-prove-recovery-through-real-movie-person-and-credit-editors.md) | G08 | G17 | Not started |
| T029 | [T028](plan/18-assisted-movie-creation-transport-and-review/tasks/T028-implement-isolated-mcp-transport-and-connector-identity.md) | G14 | G18 | Not started |
| T030 | [T029](plan/18-assisted-movie-creation-transport-and-review/tasks/T029-commit-immutable-proposals-and-exact-approval-once.md) | G14 | G18 | Not started |
| T031 | [T030](plan/18-assisted-movie-creation-transport-and-review/tasks/T030-build-the-trusted-in-chat-review-resource.md) | G14 | G18 | Not started |
| T028 | [T031](plan/19-accountable-history/tasks/T031-expose-complete-read-only-audit-outcomes.md) | G13 | G19 | Not started |
| T034 | [T032](plan/20-reproducible-seed/tasks/T032-assemble-the-permitted-frozen-100-movie-and-100-series-dataset.md) | G15 | G20 | Not started |
| T035 | [T033](plan/20-reproducible-seed/tasks/T033-initialize-and-replay-seed-and-prepared-accounts-safely.md) | G15 | G20 | Not started |
| T036 | [T034](plan/21-operational-notifications/tasks/T034-record-actionable-alerts-through-one-selected-mode.md) | G16 | G21 | Not started |
| T037 | [T035](plan/22-operational-evidence/tasks/T035-emit-bounded-private-safe-json-diagnostics.md) | G17 | G22 | Not started |
| T038 | [T036](plan/22-operational-evidence/tasks/T036-expose-logical-metrics-dashboards-and-alert-episodes.md) | G17 | G22 | Not started |
| T039 | [T037](plan/23-local-operations/tasks/T037-implement-the-seven-reproducible-lifecycle-commands.md) | G18 | G23 | Not started |
| T032 | [T038](plan/24-assisted-movie-creation-client-acceptance/tasks/T038-verify-trusted-approval-in-claude-desktop.md) | G14 | G24 | Not started |
| T033 | [T039](plan/24-assisted-movie-creation-client-acceptance/tasks/T039-verify-chatgpt-approval-and-complete-both-client-acceptance.md) | G14 | G24 | Not started |
| T040 | [T040](plan/25-protected-recovery/tasks/T040-rotate-keys-and-aliases-without-losing-access.md) | G19 | G25 | Not started |
| T041 | [T041](plan/25-protected-recovery/tasks/T041-verify-migrations-purge-and-coordinated-restore.md) | G19 | G25 | Not started |
| T042 | [T042](plan/26-release-assurance/tasks/T042-reconcile-every-real-api-and-rpc-boundary-row.md) | G20 | G26 | Not started |
| T043 | [T043](plan/26-release-assurance/tasks/T043-verify-policy-authorization-and-injection-across-complete-flows.md) | G20 | G26 | Not started |
| T044 | [T044](plan/26-release-assurance/tasks/T044-complete-visual-interaction-and-accessibility-evidence.md) | G20 | G26 | Not started |
| T045 | [T045](plan/26-release-assurance/tasks/T045-measure-capacity-and-prove-operational-recovery-limits.md) | G20 | G26 | Not started |
| T046 | [T046](plan/26-release-assurance/tasks/T046-prepare-the-reviewed-repository-and-release-handoff.md) | G20 | G26 | Not started |

## Feature lineage

| Previous group | Current stages |
|---|---|
| G03 | [G03](plan/03-account-access-credential-storage/group.md), [G06](plan/06-account-access-registration-and-sessions/group.md), [G08](plan/08-account-access-browser-journeys/group.md) |
| G06 | [G05](plan/05-reliable-work-mutation-receipts/group.md), [G09](plan/09-reliable-work-recovery-and-retries/group.md) |
| G04 | [G07](plan/07-guarded-access/group.md) |
| G05 | [G04](plan/04-effective-input-policy/group.md) |
| G01 | [G01](plan/01-reproducible-foundation/group.md) |
| G11 | [G15](plan/15-archive-management/group.md) |
| G20 | [G26](plan/26-release-assurance/group.md) |
| G08 | [G11](plan/11-draft-recovery-storage-and-controllers/group.md), [G17](plan/17-draft-recovery-editor-integration/group.md) |
| G02 | [G02](plan/02-application-shell/group.md) |
| G15 | [G20](plan/20-reproducible-seed/group.md) |
| G09 | [G12](plan/12-movie-management/group.md) |
| G18 | [G23](plan/23-local-operations/group.md) |
| G17 | [G22](plan/22-operational-evidence/group.md) |
| G10 | [G13](plan/13-people-and-credits/group.md) |
| G13 | [G19](plan/19-accountable-history/group.md) |
| G07 | [G10](plan/10-catalog-discovery/group.md) |
| G16 | [G21](plan/21-operational-notifications/group.md) |
| G14 | [G18](plan/18-assisted-movie-creation-transport-and-review/group.md), [G24](plan/24-assisted-movie-creation-client-acceptance/group.md) |
| G19 | [G25](plan/25-protected-recovery/group.md) |
| G12 | [G14](plan/14-artwork-management-processing-and-attachment/group.md), [G16](plan/16-artwork-management-browser-journeys/group.md) |

## Status and evidence

T001–T005 retain accepted Done records. Current T006–T010 retain In progress because merged source is not a passing check or independent acceptance. Current T011–T046 remain Not started. Historical task Evidence sections retain their exact bytes; the JSON map stores their SHA-256 digests. Required proof and known failures remain open.

The other chat acknowledged an idle handoff before migration: `selection-76f14edbf2224085`, owner `account-selection`, claim `lane-982add2caa054f5b`. Its original branch and last verified source remain historical facts. Live identifier translation uses the supported locked migration operation after integration; it does not reset repair budgets, correction history or receipts.

Use [the JSON mapping](plan-numbering-v1.json) for complete paths and before/after dependencies. Requirement and ATDD IDs, wire contracts and application behavior are unchanged.

## Verification boundary

`python3 build-tools/plan-numbering/verify.py` audits the migration snapshot against its recorded Git source: all 46 tasks, 26 stages, 2,542 obligation rows, dependencies, statuses and historical Evidence bytes. It is a migration audit, not a permanent restriction against later accepted task updates.

Plan validation uses the pinned plan-creation checker with `--historical-mapping docs/plan-numbering-v1.json` and the retained independent obligation inventory. That mode verifies original evidence against Git before treating it as archival; it does not turn missing or failed current checks into a pass.

The managed workflow release is 1.2.1, installed through the supported update process with prior bundles retained. Application runtime behavior, schema fields and requirement/case IDs are unchanged. Test labels, task selectors and synthetic fixture tags follow the new task IDs.

## New verification reports

Current verification writers and selectors use `.ai/evidence/numbering-v1/`. Reused task IDs therefore cannot overwrite earlier account, policy or receipt reports at the original evidence root. Browser observations, owner-fixture output, contract/build reports and the two matching gate-report globs use the same namespace. Historical task Evidence links remain unchanged. The JSON map lists every selected suite's previous and current report path.
