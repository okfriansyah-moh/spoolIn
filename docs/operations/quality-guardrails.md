# Workflow quality guardrails: adoption record

Recorded 26 October 2026. This is workflow enforcement adoption, separate from the [historical product measurements](sonar-quality.md). The root README retains those measurements and their original revision/date.

## Installation

The global defaults and movie-app now select agent-workflow **1.4.0**, built from workflow source revision `eae807a`. The installer upgraded managed instructions, native roles, skills, prompts and adapters. Both doctor receipts report healthy with no conflicts. Personal instruction content was preserved in five inspected files, including the existing Cursor rules. Movie-app retains its custom resource limits and all 80 existing checks, with one focused adapter check added. Other repository pins were not upgraded.

The owning [permanent quality contract](../../.ai/bundles/1.4.0/docs/GATES.md#permanent-quality-contract) requires independent component coverage, exact zero issue/duplication counters and all required checks. New projects remain blocked until their component/report/command/toolchain configuration is complete. No passing debt baseline was added.

Global Copilot CLI guidance is managed in `~/.copilot/copilot-instructions.md`; supported IDE guidance is in `~/.copilot/instructions/agent-workflow.instructions.md`. Global Cursor guidance is in `~/.cursor/rules/agent-workflow.mdc`. Codex and Claude managed instructions and all generated roles reference the same policy. Installation files alone do not establish native loading.

## Verification

- Workflow runtime/schema/installer suite: **375 tests passed** in temporary repositories/homes. **12 focused acceptance-boundary tests also passed** after capture, artifact-retention and nested-lock checks. Temporary installation cases cover repeat installation, conflict handling, rollback and preserved personal content.
- Movie report contracts: **7 tests passed** inside the pinned Docker toolchain under the shared execution lease.
- Movie proof adapter: **6 tests passed**, including current configuration invalidation, failed/unfinished execution, missing producer receipts and modified raw reports.
- Shell syntax, Node syntax, Compose configuration and authored wording checks passed.
- One independent combined review inspected source, corrections, installer receipts and native limitations.

Raw local receipts are retained under `.ai/state/quality-guardrails/`; they are not committed because native records include machine configuration and local paths. Global backup: `.local/share/agent-workflow/install-backups/20261026-184641-7eb6d933`. Movie-app backup: `.ai/install-backups/20261026-184641-7bb8c7d1`. Use the installer rollback command with the matching scope and backup if recovery is needed.

## Native host observations

Fresh global/project disposable CLI probes were attempted for Codex, Claude Code and Copilot. They did **not** establish complete native loading/trust. Claude's project probe observed its native role catalog and completed delegation, but skill source paths and hook correlation were not proven. Codex and Copilot probes lacked completed role/skill/hook evidence or timed out. Resumed-session inheritance of the new guidance remains unverified.

Cursor and VS Code were inspected separately through their native interfaces. Cursor's visible user-rule list did not demonstrate the newly installed file; the Copilot IDE inspection did not produce an instruction-loading receipt. A subsequent Cursor native probe was prevented by resource admission. These remain **unverified/blocked**, rather than being inferred from doctor results. No permissions, models or organization settings were changed to obtain a pass.

## Product completion blockers

The new proof verifier correctly returns failure for the historical reports because they lack measurement-time producer receipts. Source/configuration changes made during workflow adoption also invalidate the old measurement fingerprint. No old report was relabeled with current input hashes. Future full measurements must retain pre-execution inputs, raw report hashes and a completed analyzer result linked to the unit producer.

The exhaustive inventory check also found that the historical Catalog Kover report does not list `SeedLimits.kt`, a constants-only source file. The contract keeps it in maintained production scope and conservatively blocks incomplete instrumentation; no source exclusion was introduced to make this pass.

Broader Core/Catalog static-check failures, MCP lint failures and the previously disclosed local secret-scan findings remain blockers as recorded in [the original quality record](sonar-quality.md#separate-baseline-failures). They are outside this enforcement-only change. Existing integration/database/browser acceptance requirements remain required.

The fourteen approved network-range classifications are preserved as exact issue/source/evidence records. Publication queries actual analyzer dispositions and requires matching independent review; newly generated issue IDs and future findings receive no automatic classification. A fresh CI Sonar database bootstraps its own short-lived local credentials and may report these findings until they receive their own review. Credentials from another database are never assumed to work.

Remote CI and a fresh complete product-quality pass have not been established by the workflow installation tests. Missing tools, credentials, safe capacity, reports or required check results remain blocked. Testing and analysis are separate Docker phases; direct commands acquire the shared machine lease, enforce memory/worker bounds and retain failure reports.

The reviewed workflow source is published in [agentic-workflow PR #2](https://github.com/okfriansyah-moh/agentic-workflow/pull/2). The installation was refreshed after the final acceptance correction; native loading remains unverified and no release tag was published.

The full new-bundle whitespace check flags inherited trailing spaces and blank lines in copied skill/reference/license files. The installer preserved the reviewed source bytes and managed hashes; these formatting findings were not waived or silently rewritten. The authored changes outside the copied bundle pass the whitespace check. The wording scan inspected the complete copied bundle; its only matches are the personal policy’s expressly permitted definitions, negative test inputs and immutable upstream evidence.

The first remote adoption run stopped before testing with unavailable process measurements. The Linux parser had rejected valid kernel-process rows with process-group zero; source `eae807a` corrects that case while preserving rejection of invalid/unknown measurements. Nineteen focused resource-adapter tests and the final 375-test suite passed. Global/project installations were refreshed and both doctors remain healthy. Remote CI must rerun this correction; it is not claimed green.
