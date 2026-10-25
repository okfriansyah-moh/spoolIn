# Unit coverage in Docker

The minimum is **80% overall executable-line coverage in each component**: frontend, Core,
Catalog, Media, backend support, and MCP. A stronger component cannot offset a weaker one.
Branch and function/method percentages are displayed without additional thresholds.
Existing changed-line checks, security checks, and integration acceptance remain required.

## Run and read the results

Docker Engine and Compose are required; host Java and Node are unnecessary.
From the repository root:

```sh
make coverage
make coverage-report
```

Open [the HTML summary](http://localhost:8090). It links to a source-level HTML report for
each component. Files are also available in `.local/unit-coverage/`; `summary.json`
contains revision, working-change status, input fingerprint, UTC timestamps, tests,
covered/total lines, percentages, and failures. Logs are retained under `logs/`.
Use `COVERAGE_HTTP_PORT=8091 make coverage-report` to choose another report port.

`make coverage` runs suites sequentially, regenerates reports, and exits unsuccessfully
when any component falls below 80%, a test fails, no tests execute, or a required report
is missing. The exact fraction determines success; rounding a display to 80.00% does
not pass a result below 80%. Reports are retained on failure. Dependency/toolchain setup
failures also fail the command. Do not run two coverage jobs against one checkout.
Vitest's overall threshold is enabled for this dedicated complete run. Existing focused
test commands retain their own acceptance checks rather than being judged against the
whole component's denominator from a partial test selection.

The runner reuses `ops/local/toolchain.Dockerfile` with separate dependency cache volumes,
two CPUs, a 3 GiB container limit, one test worker, and bounded Java/Node heaps. It does not
start the application, PostgreSQL, or external API fixtures. Some existing MCP unit tests
exercise locally spawned transport processes; their normal build preparation is retained.

## Measurement boundaries

- Frontend Vitest includes maintained `.ts` and `.svelte` production source, including
  files not imported by tests. Test files, the generated API directory, and the two
  `*.fixture.ts` framework stand-ins are excluded. All DOM tests are discovered.
- MCP Vitest includes maintained `src/**/*.ts`; generated protobuf output is excluded.
- Kotlin uses `-PunitCoverage=true` to register a separate `unitTest` task and Kover
  `unit` report variant. It discovers the complete `src/test` suite without feature-name
  filters, uses only its normal runtime classpath, and instruments only that task.
  Only the maintained `main` source set is measured; generated contract modules and
  test-only modules are not part of the denominator.
- Existing default Kover reports and their integration binary inputs remain intact.
  The database-backed catalog filter-options test belongs to the integration source set
  and remains attached to Catalog's integration and combined XML report tasks.

You can run a single backend module's separate measurement when diagnosing a failure:

```sh
./gradlew -PunitCoverage=true :services:catalog:unitTest --rerun \
  :services:catalog:koverXmlReportUnit :services:catalog:koverHtmlReportUnit \
  :services:catalog:koverVerifyUnit :services:catalog:verifyUnitTests
```

Use the documented host toolchain for this optional host command. Docker remains the
shared full-run entry point. Unit XML/HTML stays in each module's
`build/reports/unit-coverage/`, separate from combined reports.
The dedicated unit task retains failed JUnit results so coverage reports can still be
written; `verifyUnitTests` and the full Docker command enforce test failures afterward.

## Local SonarQube

The standard Sonar project-card **Coverage** combines covered lines and conditions.
Publication now requires both this combined metric and overall line coverage to reach
80% independently for every project. The native full-source line threshold remains 80%;
branch and function percentages remain visible without separate thresholds.


The optional dashboard has a separate PostgreSQL database and persistent named volumes.
It is not the application's database. SonarQube needs additional memory beyond the unit
runner (its container limit is 3 GiB), and its Docker host must satisfy the
[SonarQube host prerequisites](https://docs.sonarsource.com/sonarqube-community-build/server-installation/pre-installation/linux).
Do not disable its host checks. Image versions and digests are pinned in the Compose file.

```sh
make coverage-sonar-up
```

Open [SonarQube](http://localhost:9000), log in with the initial `admin`/`admin`
credentials, change the password, and generate a local administration token. Then set
`SONAR_TOKEN` in your shell and run:

```sh
make coverage-sonar
```

The token needs project creation, quality-gate administration and analysis permissions.
It is passed through environment variables, not written to the generated scanner
properties or image layers. `SONAR_HTTP_PORT` changes the dashboard port.
`SONAR_DB_PASSWORD` optionally sets a different local database password; set it before
the database's first initialization and keep it consistent across starts.

Each TypeScript scan uses only its component’s `tsconfig.json`, avoiding filesystem-wide
configuration discovery and unnecessary compiler programs. See the
[Sonar TypeScript configuration guidance](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/languages/javascript-typescript-css).

The command creates six private `spoolin-unit-*` projects and associates each with an
overall `line_coverage < 80` failure condition. It imports fresh unit LCOV or
JaCoCo-compatible Kover XML, waits for analysis and gate processing, and records results
in `sonar-results.json`. It refuses stale source fingerprints and incomplete/failed test
runs. Below-threshold reports can still be published so the gaps remain visible.

**Svelte limitation:** Sonar does not provide full Svelte source analysis. Its frontend
project imports the TypeScript subset with paths resolved against the repository root.
The complete frontend percentage and enforced threshold are in the HTML/JSON report,
including Svelte. Do not present the Sonar frontend percentage as full frontend coverage.
The summary labels this distinction. Backend publication verifies every Kover source path in Sonar and requires imported hits
for covered files. Sonar and Kover identify some executable Kotlin lines differently, so
their percentages can differ slightly. Both measurements must independently reach 80%;
a missing source mapping or missing imported hits fails publication.

`make coverage-stop` stops this reporting stack and preserves its database, cache, and
dashboard volumes. It does not stop application containers. Report and dashboard ports
bind only to localhost. The report server does not expose test logs or scanner state.

## CI and delivery record

The `Unit coverage` workflow runs the same Docker entry point on pull requests, pushes
to `main`, and manual dispatch. It retains reports on failure and writes a coverage
table in the job summary. Local Sonar publication remains optional and uses local
credentials; CI does not attempt to access a developer's localhost dashboard.

Selection: `UNIT-COVERAGE-80`, owner `codex-unit-coverage`, implementation branch
`codex/spoolin-cleanup`, publication branch `codex/unit-coverage-80`, initial revision `83a9ec42b0f68dd6dde145b8b3666e3a52b98816`.
Scope includes the reporting workflow, unit coverage configurations, coverage behavior
tests, and application tests needed to reach the threshold. Preserve unrelated source,
workflow rules, and integration proof. No product API change is intended.

Verification and findings are recorded below; pending results are not acceptance.
The user approved running coverage-related checks despite high system load on
25 October 2026. Workloads remain sequential and container resources stay bounded.

### Accepted line-only baseline — 25 October

| Check / finding | Status |
|---|---|
| Script syntax and Compose configuration | Passed before runtime checks |
| Fresh six-component unit coverage | Passed: 4,141 tests, zero failures, every component above 80%; measured code `bc193871` |
| Report false-pass regression checks | Four checks passed, including missing LCOV, empty/failed suites, invalid reports, and rounding |
| Sonar processing and HTML reader observation | Passed: all six completed analyses/gates; 371 backend source mappings; fresh summary, frontend, and Core HTML opened |
| Independent combined review | Accepted: source, import correction, current fingerprints, final unit/Sonar reports; no outstanding findings |
| Threshold acceptance | Passed: all six native and Sonar overall line-coverage thresholds |

At most two combined-review correction rounds are allowed. Source is published in
[draft PR #17](https://github.com/okfriansyah-moh/spoolIn/pull/17). The line-only evidence was accepted. The expanded combined-coverage requirement below supersedes its acceptance scope.

Progress checkpoint, 25 October 2026: Docker reporting and CI configuration are implemented.
The first complete frontend run measured 54.89%; MCP measured 69.30% before the additional
poster tests. Neither passed the threshold. Unit-report variant registration and backend
failure-report retention required correction; strict variant dependency locks are being
generated. The compiler exhausted the initial small heap; the runner is now limited to
3 GiB with a 1 GiB Gradle heap. Corrections to stale copy assertions and test fixtures
preserve production behavior. Added tests cover proposal HTTP boundaries, catalog pages,
poster byte/URL validation, DNS restrictions, redirect limits, and response budgets.

Review findings ledger: unavailable backend variant tasks (corrected registration,
runtime confirmation pending); backend assertion failures prevented reports (reports
now precede `verifyUnitTests`); missing LCOV could pass (required with a regression case).


Progress checkpoint, 25 October 2026, 13:52 UTC: all six complete unit suites passed
(3,662 tests) without an out-of-memory failure. Measured line coverage was frontend
59.71%, Core 24.64%, Catalog 17.11%, Media 11.34%, backend support 50.61%, and MCP
82.73%. Only MCP met the threshold. That run was marked stale because reporting inputs
changed during execution; a stable-input run is underway. The independent reviewer
reported no outstanding blocking source findings. This is not threshold acceptance.


Runtime checkpoint, 25 October 2026, 14:03 UTC: the HTML service runs as an
unprivileged user with writable directories confined to bounded memory filesystems.
The frontend report opened in the browser, and requests for logs/scanner state returned
403. Local SonarQube completed startup with host checks enabled; its native ARM64
scanner reported its version successfully. First-use dashboard credentials are stored
outside the repository and test-container mounts. The dashboard is stopped during the
stable-input coverage rerun to keep memory use bounded.

A repeated Core unit run exposed a cold local gRPC channel missing the production
three-second RPC deadline. The test now awaits channel readiness with a bounded
30-second fixture-startup wait; application deadlines are unchanged. The corrected
suite ran without that failure. The failed run retained unit XML/HTML before
`verifyUnitTests` returned failure, confirming diagnostic retention at runtime.


Reporting corrections, 25 October 2026, 14:20 UTC: preserve the report output directory
when clearing prior files, so an existing Docker bind mount follows subsequent runs.
The report command also refreshes its service mount. The ARM scanner now includes the
pinned Node runtime required by Sonar's JavaScript/TypeScript analyzer. Core analysis
completed with all Kover source files resolved; native and Sonar executable-line counts
differ slightly, so verification checks mappings, imported hits, and both independent
thresholds instead of assuming identical analyzer counters. A final rerun is required
after these reporting corrections; threshold acceptance remains unmet.


Test expansion ownership, 25 October 2026: the reporting milestone is saved locally in
commit `9d9a01ca`; it is not published and does not satisfy the coverage target.
The user explicitly reiterated continued work until every component reaches 80%.
Scoped contributors write tests in isolated checkouts from that milestone; the parent
coordinates all substantial Docker checks sequentially, integrates tested changes, and
retains the same selection, reviewer, and findings ledger.

| Owner | Isolated checkout | Write scope | Execution resource |
|---|---|---|---|
| core_unit_tests | `~/.codex/worktrees/unit-core/movie-app` | Core unit tests | Parent-controlled Docker queue |
| catalog_unit_tests | `~/.codex/worktrees/unit-catalog/movie-app` | Catalog unit tests | Parent-controlled Docker queue |
| media_support_unit_tests | `~/.codex/worktrees/unit-media-support/movie-app` | Media and support unit tests | Parent-controlled Docker queue |
| codex-unit-coverage | Current checkout | Frontend unit tests, reporting and composition | One bounded workload at a time |


Progress checkpoint, 25 October 2026, 15:25 UTC: isolated contributors have authored
Core resolver/session batches, Catalog read/write/artwork batches, and Media asset/hold/
processor batches. Only Media's existing external codec process boundary has a small
approved injection seam; default production behavior is retained. Parent-controlled
Docker validation has begun. Core's first batch did not compile because three fixture
references differ from generated protobuf APIs; correction is underway. No new coverage
percentage is accepted yet. MCP's corrected native scanner completed and passed its
Sonar line gate; full six-component publication awaits fresh final inputs.

Progress checkpoint, 25 October 2026, 16:00 UTC: isolated complete unit suites now
pass for Catalog (5,024/6,032 lines, 83.29%), Media (1,826/2,179, 83.80%), and
backend support (2,864/3,422, 83.69%). Their tested contributions are integrated
locally. Core's combined suite passes at 5,216/6,685 (78.03%); maintenance-command
behavior tests are next. The expanded frontend run measured 76.39% but found two
new DOM fixtures missing native dialog support; that result is diagnostic, not
accepted. Corrected fixtures and additional credit/browse journeys are under
verification. MCP's last full suite remains 82.73%. Final stable-input six-component
coverage, Sonar import, affected checks, and the same independent combined review
remain pending. No threshold or production-source exclusion was relaxed.


Verified unit run, 25 October 2026, 16:28:29–16:35:43 UTC: `make coverage`
completed successfully against code revision `bc19387184e77bb0cfb64392512bb24b5be185a9`.
All 4,141 unit tests executed with zero failures or skips. The report's working-change
marker includes this delivery ledger; measured source inputs are bound by its fingerprint.

| Component | Executable lines covered / total | Line coverage | Unit tests |
|---|---:|---:|---:|
| Frontend | 5,101 / 6,129 | 83.23% | 587 |
| Core | 5,383 / 6,685 | 80.52% | 370 |
| Catalog | 5,024 / 6,032 | 83.29% | 301 |
| Media | 1,826 / 2,179 | 83.80% | 207 |
| Backend support | 2,864 / 3,422 | 83.69% | 2,521 |
| MCP | 1,121 / 1,355 | 82.73% | 155 |

The four report-contract regression checks passed in the same Docker run. The first
integrated run after import cleanup correctly failed closed on missing Core test fixture
imports and a missing Core report; the correction restored two explicit fixture imports
and removed an invalid assertion import. No assertion, production exclusion, or threshold
changed. The corrected Core test style check and the subsequent full run passed.

Affected frontend lint, type checking (zero errors, one existing RoleCreator warning),
and dependency checks passed. All four Kotlin unit-test source style checks and Gradle
script formatting checks passed. A broader production-source style scan still failed in
unchanged Core and Catalog files; these are existing findings outside this coverage
change. Unrelated Media formatter edits were reverted. Existing security checks and
integration acceptance remain separate; this unit run does not establish those results.
The same independent reviewer accepted the integrated source and confirmed the final
three import corrections. The reviewer subsequently accepted the final unit/Sonar evidence and matching source fingerprints with no outstanding findings.


Sonar publication completed at 17:04:11 UTC on 25 October 2026 for the same measured
revision and fingerprint. All six analyses completed and returned gate `OK`. Sonar's
Line coverage measurements were frontend TypeScript 84.1%, Core 80.6%, Catalog 83.3%,
Media 83.9%, backend support 83.8%, and MCP 82.9%. All 371 backend source mappings
were resolved and imported coverage hits verified (Core 160, Catalog 86, Media 48,
backend support 77). Native tools and Sonar count executable lines differently.

Sonar's general **Coverage** tile differs from **Line coverage**. The gate uses the
latter; inspect [Core's Line coverage view](http://localhost:9000/component_measures?id=spoolin-unit-core&metric=line_coverage&view=list)
or choose Line coverage within any component's Measures page. Use the native HTML/JSON
summary for complete frontend coverage, including Svelte.

The final independent combined review accepted source and evidence at `bc193871`,
including matching fingerprints, 4,141 successful unit tests, six native thresholds,
six completed Sonar gates, and resolved backend mappings. Reports and dashboard data
remain local; source was subsequently pushed to `codex/unit-coverage-80` for draft PR #17.
Separate existing formatting and integration/security
requirements are not claimed as passed by this coverage result.

This run's local dashboard credentials are stored outside the repository in
`~/.codex/unit-coverage/movie-app-sonar.json` with owner-only file permissions.
They are not committed or mounted into unit-test containers. The browser session is
signed in, and the HTML summary and Sonar Line Coverage view are left open.


Publication, 26 October 2026 (Asia/Jakarta): the user requested commit, push, and PR
creation. All implementation changes were already committed; branch
`codex/unit-coverage-80` was pushed and draft PR #17 targets `main`. This final
publication note changes documentation only; measured code remains `bc193871`.
Remote CI results are pending and are not substituted for the retained local results.

### Expanded Sonar coverage requirement — 26 October

The earlier accepted evidence above proves line coverage only. The standard Sonar
project cards showed frontend 79.6%, Core 73.6%, Catalog 77.4%, Media 77.7%, backend
support 77.3%, and MCP 81.4% combined coverage. Additional behavior tests and dual
Sonar conditions now pass. Fresh full Docker verification at clean source revision
`ce27e25daa2d7359003fd643a0a5e3d417657fcf` completed **4,224 tests, zero failures or skips**
from 2026-10-25T23:06:32.818Z to 23:10:21.423Z. All six native line percentages exceed
80%: frontend 83.67%, Core 87.31%, Catalog 86.51%, Media 86.09%, backend support
87.26%, and MCP 82.73%.

The completed Sonar publication at `2026-10-25T23:19:05.771Z` has matching fingerprint
`42754f705a0e88fc5e0fe4ae9f737751f1135a89651f13f9775bff163c215904` and these actual standard project-card results:

| Component | Sonar Coverage | Sonar line coverage | Gate |
|---|---:|---:|---|
| frontend | 80.9% | 85.0% | OK |
| core | 81.0% | 87.3% | OK |
| catalog | 81.1% | 86.5% | OK |
| media | 80.8% | 86.1% | OK |
| backend-support | 80.9% | 87.3% | OK |
| mcp | 81.4% | 82.9% | OK |

Both displayed and exact unrounded counter ratios pass. Every backend source mapping
and imported-hit check passed (160 Core, 86 Catalog, 48 Media, 77 support). The refreshed
Sonar Projects page shows six projects at >=80% Coverage and six passing gates. The HTML
report's published-analysis table was opened and observed with all six passing rows.
Evidence is retained in `.local/unit-coverage/summary.json`, `sonar-results.json`, and
`.local/unit-coverage-sonar80-final.png`.

Five report-contract tests, all four Kotlin test-source style checks, and affected
frontend lint/type/dependency checks passed; frontend type checking retains one existing
Svelte warning. No OOM occurred. Independent combined source and evidence review accepted
the expanded requirement with no outstanding findings. Documentation/PR metadata edits
after the measured revision do not alter coverage inputs. PR17 remains a draft; remote
CI results are separate from this verified local Docker outcome.
