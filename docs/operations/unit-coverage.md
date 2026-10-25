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

Selection: `UNIT-COVERAGE-80`, owner `codex-unit-coverage`, branch
`codex/spoolin-cleanup`, initial revision `83a9ec42b0f68dd6dde145b8b3666e3a52b98816`.
Scope includes the reporting workflow, unit coverage configurations, coverage behavior
tests, and application tests needed to reach the threshold. Preserve unrelated source,
workflow rules, and integration proof. No product API change is intended.

Verification and findings are recorded below; pending results are not acceptance.
The user approved running coverage-related checks despite high system load on
25 October 2026. Workloads remain sequential and container resources stay bounded.

| Check / finding | Status |
|---|---|
| Script syntax and Compose configuration | Passed before runtime checks |
| Fresh six-component unit coverage | Stable-input rerun in progress; preceding run passed all 3,662 tests |
| Report false-pass regression checks | Four checks passed, including missing LCOV, empty/failed suites, invalid reports, and rounding |
| Sonar processing and HTML reader observation | Frontend HTML observed; local Sonar startup and native scanner verified; imports pending |
| Independent combined review | No outstanding source findings after corrections; runtime acceptance remains pending |
| Threshold acceptance | Pending; requires every component at least 80% |

At most two combined-review correction rounds are allowed. No source has been
published for this selection. Next action: obtain fresh measurements, repair uncovered
behavior with meaningful unit tests, verify report imports, and complete independent review.

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
