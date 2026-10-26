
## Reuse and repository cleanup

Follow the [shared reuse and cleanup policy](../.ai/bundles/1.4.0/docs/DELIVERY.md#reuse-and-evidence-based-cleanup) for implementation and review. Keep reusable project prompts in `docs/prompts/`. The root lint entry point checks prompt placement and local prompt links; use the existing unused-variable and dependency checks for affected code.

<!-- agent-workflow:begin -->
## Portable implementation workflow

Use `.ai/config.json` to resolve the repository bundle before global fallback. Read `.ai/bundles/1.4.0/docs/DELIVERY.md`; it defines one workflow for Codex, Cursor AI, Claude Code and GitHub Copilot. Run doctor once before substantial execution and load only relevant guidance.

The current chat implements the approved outcome directly in one suitable checkout, runs agreed affected checks, obtains one independent combined review, corrects findings and delivers within existing authorization. Optional scoped workers use isolated resources for concurrent writing; no nested coordinator or per-task review ceremony. Host adapters change syntax, not completion rules.

Apply the selected delivery policy's reuse and cleanup rules before adding or removing code, files, dependencies or docs. Implementation stays within the approved scope; reuse checks belong in the existing implementation and combined review.

Match actual approved visual references and complete user journeys, including completion, return destination and retained context. Protect workflow files, bundled skills, agent definitions, gate policy and unplanned Python files during product work. Harness maintenance requires its own requested task. Preserve IDs, unrelated work, configured quality/security rules and actual proof boundaries.

Explicit no-test instructions prohibit test execution through hooks and hidden browser trials as well as added tests. Report pending proof honestly; source review does not establish runtime acceptance. Independent review requires a separate reviewer.

Keep one Git-shared selection and findings ledger across hosts/resumes. Preserve ownership, constraints, valid results and cumulative limits. Record actual product progress every 45 minutes; optional evidence-tool repair shares two attempts or 20 minutes. Report implementation, checks, review and local/published revisions separately. Installation does not resume application work. Run the bundled wording checker before delivery. Read coordination/gate contracts only when using their helpers.

Read `.ai/bundles/1.4.0/docs/GATES.md` before planning, implementation or acceptance. Its permanent quality contract governs every maintained component; existing failures block feature completion.

Before planning, scaffolding, implementation or acceptance, read the selected bundle docs/GATES.md permanent quality contract. Every maintained runtime component must prove the full minimum; stronger project requirements remain stronger. An older project pin that cannot enforce this contract blocks feature acceptance until explicitly upgraded. Documentation-only work selects document checks without launching application suites.

Use shared execution-device resource admission before substantial checks; retain stop/recovery state across hosts and leave unsafe checks pending.
<!-- agent-workflow:end -->
