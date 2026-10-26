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

## Verification and delivery record

Task `SHA256-IDENTITIES`, base `bbf91bdc`, branch `codex/unit-coverage-80`, existing
[draft PR17](https://github.com/okfriansyah-moh/spoolIn/pull/17). The user explicitly
selected full SHA-256 migration with existing databases and pending work preserved.

Implementation, real PostgreSQL upgrade/replay checks, six-component unit coverage,
Sonar SHA-1 finding closure and one independent combined review are pending. The
previous coverage evidence remains a baseline and does not verify this migration.
