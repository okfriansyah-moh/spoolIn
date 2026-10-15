T002 assembles the six normative SDL groups into `contracts/graphql/schema.graphql`, the ten
protobuf groups into `contracts/grpc/src/main/proto/spoolin/v1/spoolin.proto`, and the MCP schema
fragments into their JSON registries. The examples in §5.1.7 are not SDL definitions. Policy and
draft JSON schemas translate the exact §5.11 tables. `draft.schema.json` has target-specific
definitions; it adds no payload version and preserves incomplete correction values. The source
map records the original technical-design fingerprint and assembled definition groups.

`node build-tools/generate-contracts.mjs` orders generation from these inputs. Its cache checks
both input hashes and every retained generated output hash; deleting or editing an output forces
regeneration. `--force` reruns generation. The direct GraphQL Codegen core API and locked
GraphQL17 parser generate all36 root-operation documents plus any authored feature documents.
Codegen6 operation types import the separately generated schema types, avoiding duplicate enum
and input declarations. Generated imports use explicit `.js` suffixes for NodeNext. Protobuf timestamps retain raw
seconds/nanos with string seconds; a JavaScript Date would lose required microsecond precision.

The JVM DTO generator is the authored `generate-graphql.mjs` revision, using that same locked
parser. It emits immutable Java records usable from Kotlin, including separate absent/present
input values. Supplied null stays distinct from omission. Record collection values are copied;
the DTOs are neither persistence entities nor authorization. Policy/draft JSON fields use their
generated projection types. Audit before/after values retain the declared scalar/array/object
shape; owner code must still construct the public-field allowlist.

`build-tools/contracts.gradle.kts` connects generated Core Java sources and generated GraphQL
resources to Java/Kotlin compilation and packaging. A direct clean Core Kotlin compile must
generate its Java DTO sources first, even before Java compilation is selected. `contracts/grpc/build.gradle.kts` owns Java/streaming TypeScript
protobuf generation and descriptors. This module consumes its strict lock at
`contracts/grpc/gradle.lockfile`; it replaces the earlier empty module placeholder lock location.
The root-owned other locks and artifact-integrity manifest remain unchanged. Dependency inventory
and reproducibility checks include the owned module lock. No dependency version is substituted.

All outputs below are ignored derived files:

- `frontend/src/lib/generated/`: schema/operation/JSON types, documents and token CSS.
- `mcp/src/generated/`: grpc-js clients, JSON/schema adapters, token CSS and resolved theme values.
- `services/core/build/generated/sources/graphql/`: Java DTOs and presence values.
- `services/core/build/generated/resources/contracts/graphql/`: generated SDL resource source.
- `services/core/build/resources/main/graphql/`: packaged resource destination.
- `contracts/grpc/build/`: Java stubs, descriptor set and compilation products.

Native MCP output adapters resolve every local reference before registration. The generated
model input map contains the three model tools; approval/rejection schemas are in a separate
app-only map. This map expresses the source contract; host UI provenance and permission checks
remain with the adapter/owner implementation. Shared token adapters resolve both themes from
`docs/design-system/tokens.json`; no independently authored token palette exists.

`build-tools/contracts/wire.mjs` and `validation.mjs` are executable verification rules for this
contract boundary, not a running Core transport. The schema validator implements the keywords
used by these source schemas, including active UUID/UTC/int64 format checks. It is not a general
JSON Schema implementation. Runtime GraphQL scalar coercion, GraphQL argument-presence adapters,
service validation/authorization and output construction remain handwritten owner work. Draft
checks preserve safe correction values and reject forbidden keys; they do not apply final-write
normalization or current business-policy bounds. The separate patch fixtures prove the declared
omission, optional clearing, whole-date replacement and ordered-name rules locally. Allowed
patch keys come from the SDL patch types, so draft-only credit endpoint fields cannot enter
a patch.

Ordinary patch text trims Unicode White_Space, including U+0085 and excluding U+FEFF,
and rejects NUL. Correction drafts retain exact untrimmed text and may retain a null release
year; final patch/write validation still requires that year. Timestamp seconds must fit the
imported protobuf range from year0001 through year9999 before nanos truncate to microseconds.

`bash ops/bin/test-unit.sh --task T002` runs the actual `tests/contracts/run.py` suites. JSON/JUnit
reports contain real unittest outcomes, with generation/compile subprocess logs and hashes under
`.ai/evidence/t002-generation/`. Java and TypeScript consumers exercise exact int64 values,
presence, unknown protobuf fields, field numbers and streaming signatures. The Core plain JAR
must contain the source SDL byte for byte. Repeated generation compares every generated output
hash and repairs a removed output. These are isolated contract/compile checks. They do not prove
TCP behavior, database storage sizing, client approval isolation or all live operations; T042 and
the named runtime case owners retain those boundaries.
