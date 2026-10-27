# SHA-256 identity migration

New deterministic identities use SHA-256 and UUIDv8. Existing UUIDv5 values remain
unchanged through an immutable compatibility registry. Existing records, receipts,
work IDs, retry budgets, destination bindings, account initialization receipts and
frozen seed identities must survive the upgrade.

## Implementation contract

The shared identity function hashes the network-order namespace UUID and exact name
bytes with SHA-256. It looks up that 32-byte key in a trusted legacy index before
forming a UUIDv8 from the first 16 digest bytes. A recorded compatibility value wins;
an unknown name receives UUIDv8. Every participating owner uses the same registry.
There is no acceptance of two alternative IDs for an operation and no runtime SHA-1
fallback. The one-time export computes historical UUIDv5 values only to preserve
existing identities; it never changes stored IDs or security fingerprints.

The index format is `SPIDIDX1`, a big-endian eight-byte count, and sorted fixed-width
records containing a 32-byte lookup key and 16-byte UUID. File-backed binary search
keeps lookup memory bounded. Startup validates its SHA-256 digest, size, key ordering
and uniqueness. A packaged bounded index retains the reviewed frozen seed inputs.

New owner migrations record whether legacy data exists. An existing database cannot
start the upgraded service until the offline migration records the installed registry
hash. Runtime credentials can read this marker but cannot change it. The export covers
all three owners while application and maintenance writers are stopped. The immutable
registry and its digest must be installed consistently before restarting services.
Partial preparation leaves affected owners unavailable; it cannot silently select new
IDs for legacy work. Old and new service binaries must not run concurrently during
cutover. After new UUIDv8 operations are admitted, recover forward; reverting to the
old binaries requires restoring a coordinated pre-cutover backup.

## Existing installation cutover

Back up all three owner databases together. Stop owner services, scheduled workers,
MCP callers and administrative writers. Keep them stopped until preparation completes.
Build the upgraded images, including the PostgreSQL image that reconciles role grants,
and run the existing owner migration commands for Core, Catalog and Media. The new
schema versions are Core 8, Catalog 26 and Media 4. Do not start owner applications yet.

For the existing Compose installation, the transport wrapper uses the PostgreSQL
container's protected credential and socket. Set `SPOOLIN_INSTALLATION` to the existing
Compose project name and `SPOOLIN_RUNTIME_CONFIG_DIR` to its protected host configuration
directory, then run from the repository root:

```sh
python3 ops/identity/migrate.py \
  --psql ops/identity/compose-psql.sh \
  --connection 'core=dbname=spoolin_core user=postgres' \
  --connection 'catalog=dbname=spoolin_catalog user=postgres' \
  --connection 'media=dbname=spoolin_media user=postgres' \
  --output "$SPOOLIN_RUNTIME_CONFIG_DIR/legacy-identities.bin" --apply
```

Supply every historical seed manifest with repeated `--manifest` options when it differs
from the repository's frozen inputs. Unknown or inconsistent legacy bindings fail the
export. See [the exporter instructions](../../ops/identity/README.md) for inspect-only
exports, protected connection alternatives, input limits and interruption recovery.

After a successful apply, the same registry digest is recorded in all three databases
and `legacy-identities.bin.ready` exists alongside the immutable index. Export
`SPOOLIN_IDENTITY_REGISTRY=/run/spoolin-config/legacy-identities.bin` before restarting
all upgraded owners. Their existing read-only configuration mounts expose both files.
Keep the index and ready file with database backups; never clear the database guard or
replace the index with a different export to bypass an error. A missing, modified or
unprepared index blocks startup. Resume an interrupted apply using identical inputs
and the same output path while writers remain stopped.

A fresh empty installation needs no external index: packaged frozen seed IDs retain
their values and new identities use UUIDv8. Once an installation has recorded a registry
digest, that registry remains required even after receipts expire.

## Verification and delivery record

Task `SHA256-IDENTITIES`, base `bbf91bdc`, branch `codex/unit-coverage-80`, existing
[draft PR17](https://github.com/okfriansyah-moh/spoolIn/pull/17). The user explicitly
selected full SHA-256 migration with existing databases and pending work preserved.

Verified source `52a18565983342cc16358fe811094c3e74554e78` passes a real PostgreSQL
18.6 upgrade through both the dedicated and regular integration entry points. Checks
cover prior schemas, retained rows and retry budgets, interrupted cross-owner apply,
resumed preparation, runtime read-only privileges, receipt replay and the environment-
configured resolver. The offline exporter has 17 passing tests; six startup cases and
shared identity rules pass within the full 4,240-test unit run.

All six native line thresholds, all six Sonar combined/line thresholds and all affected
changed-line thresholds exceed 80%. Sonar automatically closed all four SHA-1 findings
as fixed, with none unresolved. See [the coverage evidence](unit-coverage.md#sha-256-identity-migration-verification--26-october)
for percentages, counters, revisions and analysis time. This implementation was tested
against disposable upgrade databases; existing installation cutover follows the steps
above and has not been applied to a production database during this work.

Independent combined source and evidence review accepted measured revision `52a18565`,
including Sonar finding closure and report observation, with no outstanding findings.
