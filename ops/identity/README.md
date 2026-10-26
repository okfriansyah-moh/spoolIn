# Offline identity preservation

`migrate.py` builds the shared compatibility index used during the SHA-256 identity upgrade. It preserves persisted IDs; it does not rewrite application data, receipt hashes, assets, or work. Historical UUIDv5 calculation exists only in this offline exporter. The upgraded application uses the shared SHA-256 lookup and new UUIDv8 derivation, without a weak-hash fallback.

Stop **all three owners and administrative writers** before exporting. Leave them stopped until the tool finishes, all three marker hashes match, and the index's `.ready` sibling exists. Unexpired work leases and runtime, maintenance, or migrator database sessions reject the export; only the current export connection is excluded. An interrupted apply can leave some database hashes committed; without the final ready file, legacy startup stays blocked.

## Build a frozen seed resource

```sh
python3 ops/identity/migrate.py --frozen-only --output /tmp/frozen-v5.bin
```

The default input is `data/seed/manifest.json`. Referenced baseline manifests are included recursively; references cannot escape the containing directory. Repeat `--manifest PATH` for additional historical inputs. Pure `build(exports, manifests)` accepts decoded JSON objects and returns binary bytes; callers supply all predecessor manifests explicitly.

The immutable binary format is `SPIDIDX1` (8 ASCII bytes), an unsigned big-endian 64-bit count, then sorted unique 48-byte records. Each record contains SHA-256(namespace UUID network bytes + original name bytes), followed by the retained UUID's 16 network bytes. Names preserve the historical UTF-8 or length-prefixed byte representation. Conflicting persisted parent/name bindings fail.

## Inspect and apply a coordinated export

Use `psql` service definitions, a protected password file, or environment credentials. Avoid passwords in connection arguments. A controlled wrapper can be selected with `--psql PATH` and repeated `--psql-arg=VALUE` arguments; no shell evaluates these values.

```sh
python3 ops/identity/migrate.py \
  --connection core=service=identity-core \
  --connection catalog=service=identity-catalog \
  --connection media=service=identity-media \
  --export-dir /protected/identity/export-1 \
  --output /protected/identity/registry-1.bin
```

This writes private immutable JSON exports and the index, but changes no database marker. Omit `--output` and `--export-dir` for an inspect-only run. Controlled backup inputs can instead use three repeated `--exports PATH` options. Applying backup inputs still requires live connections for source verification.

```sh
python3 ops/identity/migrate.py \
  --exports /protected/identity/export-1/core.json \
  --exports /protected/identity/export-1/catalog.json \
  --exports /protected/identity/export-1/media.json \
  --connection core=service=identity-core \
  --connection catalog=service=identity-catalog \
  --connection media=service=identity-media \
  --output /protected/identity/registry-1.bin --apply
```

Apply checks fresh inventories against the supplied exports, writes and fsyncs the immutable index, locks each owner's source tables while verifying again, and updates only `registry_sha256`. Existing different marker hashes or index bytes reject the operation. It verifies all three hashes and unchanged exports before writing and fsyncing `<index>.ready`, containing exactly the index's 64 lower-case hexadecimal SHA-256 characters and a newline. Restart only after installing this same index and ready sibling for every owner. Resume with identical inputs and output after an interruption; never clear `legacy_required` to bypass a failed upgrade.

Each owner export has `schema_version: 1`, `owner`, `active_runtime_sessions: 0`, `active_leases: 0`, and complete arrays:

- All owners: `operation_receipt` (ID, receipt type, applicable setup/seed version and catalog snapshot hash), and `durable_work` (ID, operation ID, kind, generation, notification key, payload containing only subject/intent IDs).
- Catalog: `artwork_attachment_request` IDs.
- Media: `asset` IDs/upload operation IDs, `asset_reference` IDs, and `hold_operation_binding` operation/hold/verb tuples.

Receipt results, request hashes, notification summaries, credentials, and arbitrary payload fields are excluded. The exporter includes fixed child names across the union of all owner namespaces, persisted release names, current and retained notification generations, seed source/artwork identities, and initialization versions. Catalog initializer namespaces come from matching supplied frozen manifests/snapshot hashes; a single shared manifest namespace also resolves historical installation-version aliases. Unknown or ambiguous namespaces and inconsistent known work IDs reject the export.

The marker table is `appdata.identity_hash_migration(singleton boolean primary key check(singleton), legacy_required boolean not null, registry_sha256 varchar(64))`, with exactly one `singleton=true` row. New empty databases have `legacy_required=false`. Existing receipts or durable work set it true. Runtime and maintenance roles receive SELECT only; the migration role receives SELECT and column-restricted UPDATE for the hash. Normal grant reconciliation must preserve those read permissions.

Each database command has a 60-second wall-clock deadline, including wrappers that never close their output. Diagnostics are discarded and capped at 64 KiB; SQL input uses a temporary file. Input JSON and `psql` output are capped at 16 MiB each, historical manifests at 16, and registry entries at 500,000. Larger installations require a separately reviewed bounded batch migration; this tool fails rather than accumulating unbounded memory. Generated exports use mode 0600, index files 0444, and ready files 0644. Existing symlinks, writable group/other artifacts, or different bytes are rejected. Identical existing files and their containing directories are fsynced before reuse. The readiness marker is not created for frozen-only resources.

Run the small standard-library checks through the coordinating verification queue:

```sh
python3 -m unittest discover -s ops/identity -p 'test_*.py'
```
