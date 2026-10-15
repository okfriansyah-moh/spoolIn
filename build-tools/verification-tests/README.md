T001 verifies build fixtures only. Production schemas belong to T002, the runtime harness to
the service/operations tasks and application routes to T004. The fixture paths remain separate
from those owner sources; the root Gradle script configures the declared module directories.

Use Eclipse Temurin 21.0.12.1+1, Node 24.21.0 and npm 11.19.0 on PATH, with JAVA_HOME pointing
to that JDK. `bash ops/bin/test-unit.sh --task T001` runs the isolated harness checks and two
clean locked builds. Network access is needed for uncached dependencies. `make test-unit TASK=T001`
selects the same check. Generated protobuf/gRPC fixture Java, the static Kit 3 fallback, compiled
MCP SDK fixture types and the JAR must have identical bytes across both cycles. Root report:
`.ai/evidence/t001-tests.json`; command logs, inputs and output hashes:
`.ai/evidence/t001-build/`. No normal database or client connector is selected.

The task map records each planned task’s actual source file and proposed test paths. Unknown,
empty and unavailable required selections return 2. A failed runnable test returns 1. A full
`make test-unit` requires every task in that map; it returns 2 until their suites exist.
`make test-integration` and the selected T001 integration command return 2 because the real
PostgreSQL/gRPC/browser fixture is pending. A unit pass never asserts downstream real-service
acceptance. Browser/client/provider selectors remain unavailable until their owners implement them.

`fixture.py` supplies UUIDv4 namespaces, reserved loopback ports, private synthetic tokens,
test-only database/role names, fixed observation time and an ownership-checked reset interface.
It refuses ordinary installation names before creating resources. Tokens are ephemeral,
generated independently and omitted from reports. `start_services()` raises a setup error until
the actual service fixture exists. Cleanup touches only the matching run marker; its failure
cannot replace an earlier test failure. Runtime notification, actor and catalog initialization
remain pending and cannot be inferred from these fixture interfaces.

The seven public command contracts are defined in technical design §11.3. Only test-unit and
test-integration are wired here. T039 owns start, stop, restart, status and logs.

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
