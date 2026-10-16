T001 verifies build fixtures only. Production schemas belong to T002, the runtime harness to
the service/operations tasks and application routes to T004. The fixture paths remain separate
from those owner sources; the root Gradle script configures the declared module directories.

Use Eclipse Temurin 21.0.12.1+1, Node 24.21.0 and npm 11.19.0 on PATH, with JAVA_HOME pointing
to that JDK. `bash ops/bin/test-unit.sh --task T001` runs the isolated harness checks and two
clean locked builds. Network access is needed for uncached dependencies. `make test-unit TASK=T001`
selects the same check. Generated protobuf/gRPC fixture Java, the static Kit 3 fallback, compiled
MCP SDK fixture types and the JAR must have identical bytes across both cycles. Root report:
`.ai/evidence/numbering-v1/t001-tests.json`; actual harness outcomes are also exported to
`.ai/evidence/numbering-v1/t001-harness.xml` as JUnit cases. Command logs, inputs and output hashes:
`.ai/evidence/numbering-v1/t001-build/`. No normal database or client connector is selected.

The task map records each planned task’s actual source file and proposed test paths. Unknown,
empty and unavailable required selections return 2. A failed runnable test returns 1. A full
`make test-unit` requires every task in that map; it returns 2 until their suites exist.
`make test-integration` and the selected T001 integration command return 2 because the real
PostgreSQL/gRPC/browser fixture is pending. A unit pass never asserts downstream real-service
acceptance. Browser/client/provider selectors remain unavailable until their owners implement them.

`fixture.py` supplies UUIDv4 namespaces, reserved loopback ports, private synthetic tokens,
test-only database/role names, fixed observation time and an ownership-checked reset interface.
It refuses ordinary installation names before creating resources. Named `actors()` (F-ACTORS)
and `catalog()` (F-CATALOG) interfaces raise setup errors until their real owner fixtures exist;
they never manufacture initialized accounts or catalog rows. Tokens are ephemeral,
generated independently and omitted from reports. `start_services()` raises a setup error until
the actual service fixture exists. Cleanup touches only the matching run marker; its failure
cannot replace an earlier test failure. Runtime notification, actor and catalog initialization
remain pending and cannot be inferred from these fixture interfaces.

The seven public command contracts are defined in technical design §11.3. Only test-unit and
test-integration are wired here. T037 owns start, stop, restart, status and logs.

Lock maintenance is an explicit build-owner action: regenerate npm’s single lock with the selected
tools and strict peers; run `python3 build-tools/verification-tests/lock_dependencies.py` for project/settings/plugin
locks; review the inventory, license and advisory changes; then rerun the selected verification.
Normal verification never writes dependency locks. Wrapper distribution bytes are checked against
the pinned publisher checksum. Image architecture digests are recorded under `gradle/` and are
inputs for later container tasks, not proof that a service image has been built.

The lock-maintenance helper relocates Gradle’s empty `incomingCatalogForLibs0` settings inventory
to `gradle/locks/settings/gradle.lockfile` after checking that it has no external coordinates.
That file is informational; the local TOML catalog is the exact source input. Plugin and project
locks under the other `gradle/locks/` directories are consumed by Gradle strict locking. A normal
verification run must leave the repository root free of `settings-gradle.lockfile`.

GraphQL generators use the direct Codegen core API, exact TypeScript/operations/document plugins
and explicit file reads. The CLI loader chain is excluded because its brace-pattern parser has
an advisory without an available patched release. T002 may use `generatorClasspath` and
`writeGeneratorClasspath` for the Boot-managed graphql-java parser, `generateGraphql` for its
`build-tools/generate-graphql.mjs` entry point and `generateContracts` for ordered GraphQL/protobuf
generation. The ts-proto/grpc-js/Buf runtime graph supports streaming and compiles a separate
bidirectional streaming fixture. Runtime service schemas remain T002’s responsibility.

The approved security patch uses Kotlin2.4.20 compiler/JVM/Spring/JPA plugins and application
stdlib/reflect, Tomcat embed11.0.26 and Jackson3.1.7 BOM. The remaining selected Boot4.1.1
managed versions stay unchanged. The build checks every service runtime family against the
approved pins and exercises Jackson/Kotlin immutable-value serialization with the real libraries.
Tool engines retain their own publisher-selected support libraries.

T002 supplies `build-tools/contracts.gradle.kts`, applied by the root when present, and
`build-tools/generate-contracts.mjs`, called before frontend/MCP check/build when production
contract sources exist. That entry point owns ordered generation; missing required generation
returns2. `npm run generate` requires the production entry point. T002’s selected unit runner is
`python3 tests/contracts/run.py`; it writes fresh workflow-test counts to
`.ai/evidence/numbering-v1/t002-contract-tests.json`. The selector writes `.ai/evidence/numbering-v1/t002-tests.json`.

T003’s integration selector invokes all three actual owner `integrationTest` tasks through
`run_owner_integration.py`. Every declared bootstrap source glob, Docker and a fresh nonempty
JUnit report per owner are required. The runner passes `SPOOLIN_TEST_NAMESPACE` and a protected
`SPOOLIN_TEST_CONFIG` file; owner suites must use that disposable fixture and independently
verify their real PostgreSQL/gRPC boundaries. Testcontainers reuse is disabled. Skipped cases
remain unexecuted and block the result. Test counts come from actual JUnit testcase elements;
missing, stale or empty reports cannot pass. Final report: `.ai/evidence/numbering-v1/t003-tests.json`.
The `architectureTest` service task uses compiled integration-test sources and selects
`*ArchitectureTest`; T003 configures its owned bootstrap classes and unit/coverage selections.
ArchUnit1.5.1 is pinned and exercised with the selected JUnit engine in the build fixture.

Explicit lock maintenance also records Gradle SHA-256 artifact verification metadata, including
Linux arm64/amd64 protobuf/gRPC generator binaries. Normal builds enforce that metadata without
rewriting it. Historical evidence from the original dependency baseline is archived separately
under `.ai/evidence/numbering-v1/archive/t001-original-baseline/`; it cannot prove the patched implementation.

Explicit lock maintenance archives the previous integrity manifest under
`.ai/evidence/numbering-v1/lock-maintenance/` before regenerating metadata for the selected graph. A failed
resolution restores the previous manifest. Plugin classpath Jackson uses the same approved
3.1.7 BOM; older BOM descriptors can still be read as upstream metadata, while selected
Jackson core/databind artifacts and resolver locks use 3.1.7.
