#!/usr/bin/env python3
"""Offline export of historical UUIDv5 bindings into a SHA-256 lookup index.

SHA-1 exists ONLY in legacy_uuid() to reproduce historical IDs during this
one-time, offline preservation step. Application runtime uses no weak fallback.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import selectors
import stat
import struct
import subprocess
import sys
import tempfile
import time
import uuid

MAGIC = b"SPIDIDX1"
MAX_INPUT_BYTES = 16 * 1024 * 1024
MAX_ENTRIES = 500_000
MAX_MANIFESTS = 16
MAX_ERROR_BYTES = 65536
PSQL_SECONDS = 60
OWNERS = ("core", "catalog", "media")
SEED_NAMESPACE = uuid.UUID("9da61864-80f5-5c70-bc79-3914da94079e")
FIXED_STEPS = ("asset", "media-upload", "upload-process", "attach", "confirm", "media-reserve", "media-confirm", "media-release")
TABLES = {
    "core": ("operation_receipt", "durable_work"),
    "catalog": ("operation_receipt", "durable_work", "artwork_attachment_request"),
    "media": ("operation_receipt", "durable_work", "asset", "asset_reference", "hold_operation_binding"),
}


def identity(value):
    parsed = uuid.UUID(str(value))
    if str(parsed) != value:
        raise ValueError("ID must be a lowercase, full UUID")
    return parsed


def legacy_uuid(namespace, name):
    """Historical compatibility calculation, never an application ID generator."""
    digest = bytearray(hashlib.sha1(namespace.bytes + name).digest()[:16])
    digest[6] = (digest[6] & 15) | 80
    digest[8] = (digest[8] & 63) | 128
    return uuid.UUID(bytes=bytes(digest))


class Registry:
    def __init__(self):
        self.records = {}

    def add(self, namespace, name, retained=None):
        name = name.encode("utf-8") if isinstance(name, str) else name
        key = hashlib.sha256(namespace.bytes + name).digest()
        old = legacy_uuid(namespace, name)
        if retained is not None and old != identity(str(retained)):
            raise ValueError("Persisted identity disagrees with its historical parent/name binding")
        if key not in self.records and len(self.records) >= MAX_ENTRIES:
            raise ValueError("Identity index exceeds the bounded export capacity")
        prior = self.records.setdefault(key, old.bytes)
        if prior != old.bytes:
            raise ValueError("Conflicting identity-index entry")
        return old

    def binary(self):
        return MAGIC + struct.pack(">Q", len(self.records)) + b"".join(key + self.records[key] for key in sorted(self.records))


def source_name(source, kind, external):
    parts = [part.encode("utf-8") for part in (source, kind, external)]
    return b"".join(struct.pack(">I", len(part)) + part for part in parts)


def add_manifest(registry, manifest):
    namespace = identity(manifest["namespaceUUID"])
    version = manifest["manifestVersion"]
    registry.add(namespace, f"initialization/catalog/CATALOG_INITIALIZATION/{version}")
    for group in ("records", "people", "roles"):
        for record in manifest[group]:
            kind = record.get("kind") if group == "records" else "PERSON" if group == "people" else "ROLE"
            registry.add(namespace, source_name(record["source"], kind, record["externalId"]), record["stableUUID"])
            for genre in record.get("genres", []):
                registry.add(namespace, source_name(genre["source"], "GENRE", genre["externalId"]), genre["stableUUID"])
            for credit in record.get("credits", []):
                registry.add(namespace, source_name(credit["source"], "CREDIT", credit["externalId"]), credit["stableUUID"])
            artwork = record.get("artwork")
            if artwork:
                name = f"{record['stableUUID']}:{artwork['sha256']}"
                registry.add(namespace, f"SEED_ARTWORK:{name}", artwork["assetId"])
                registry.add(namespace, f"SEED_ARTWORK_REFERENCE:{name}", artwork["holdId"])


def validate_exports(exports):
    if not exports:
        return {}
    by_owner = {}
    for export in exports:
        bounded_json(export)
        owner = export["owner"]
        if owner not in OWNERS or owner in by_owner or export.get("schema_version") != 1:
            raise ValueError("Expected exactly one version-1 export for each owner")
        if export.get("active_runtime_sessions") != 0 or export.get("active_leases") != 0:
            raise ValueError("All owners must be stopped and no unexpired work lease may remain")
        if set(export) != {"schema_version", "owner", "active_runtime_sessions", "active_leases", *TABLES[owner]}:
            raise ValueError("Owner export includes unknown or private fields")
        allowed_rows = {
            "operation_receipt": {"id", "receipt_type", "setup_version"} if owner == "core" else {"id", "receipt_type", "seed_version", "seed_snapshot_hash"} if owner == "catalog" else {"id", "receipt_type"},
            "durable_work": {"id", "operation_id", "kind", "generation", "notification_key", "payload"},
            "artwork_attachment_request": {"id"}, "asset": {"id", "upload_operation_id"},
            "asset_reference": {"id"}, "hold_operation_binding": {"operation_id", "hold_id", "verb"},
        }
        for table in TABLES[owner]:
            if not isinstance(export.get(table), list):
                raise ValueError(f"Missing complete {owner} export table: {table}")
            for row in export[table]:
                if not isinstance(row, dict) or set(row) - allowed_rows[table]:
                    raise ValueError("Export row contains unknown or private fields")
                if table == "durable_work" and (not isinstance(row.get("payload"), dict) or set(row["payload"]) - {"subjectId", "intentId"}):
                    raise ValueError("Work export payload contains unknown or private fields")
        by_owner[owner] = export
    if set(by_owner) != set(OWNERS):
        raise ValueError("Exports from all three owners are required")
    return by_owner


def build(exports, manifests):
    """Return immutable sorted binary bytes from complete exports and frozen manifests."""
    by_owner = validate_exports(exports)
    registry = Registry()
    manifest_namespaces = {}
    hash_namespaces = {}
    if len(manifests) > MAX_MANIFESTS:
        raise ValueError("Too many historical manifests for bounded migration")
    for manifest in manifests:
        bounded_json(manifest)
        version = manifest["manifestVersion"]
        namespace = identity(manifest["namespaceUUID"])
        if version in manifest_namespaces and manifest_namespaces[version] != namespace:
            raise ValueError("Manifest version has conflicting namespaces")
        manifest_namespaces[version] = namespace
        snapshot = manifest.get("snapshotSHA256")
        if snapshot:
            hash_namespaces.setdefault(snapshot, set()).add(namespace)
        add_manifest(registry, manifest)
    roots = set()
    for owner, export in by_owner.items():
        for table in TABLES[owner]:
            for row in export[table]:
                for column in ("id", "operation_id", "upload_operation_id", "hold_id"):
                    if row.get(column):
                        roots.add(identity(row[column]))
                if table == "durable_work":
                    payload = row.get("payload") or {}
                    if not isinstance(payload, dict):
                        raise ValueError("Work payload must be a JSON object")
                    for field in ("subjectId", "intentId"):
                        if payload.get(field):
                            roots.add(identity(payload[field]))
                if table == "operation_receipt":
                    receipt_type = row.get("receipt_type")
                    if owner == "core" and receipt_type == "ACCOUNT_INITIALIZATION":
                        version = row.get("setup_version")
                        if not isinstance(version, str) or not version:
                            raise ValueError("Core initialization receipt requires its setup version")
                        registry.add(SEED_NAMESPACE, f"initialization/core/ACCOUNT_INITIALIZATION/{version}", row["id"])
                    if owner == "catalog" and receipt_type == "CATALOG_INITIALIZATION":
                        version = row.get("seed_version")
                        candidates = hash_namespaces.get(row.get("seed_snapshot_hash"), set())
                        if not candidates and version in manifest_namespaces:
                            candidates = {manifest_namespaces[version]}
                        if not candidates and len(set(manifest_namespaces.values())) == 1:
                            candidates = set(manifest_namespaces.values())
                        if not isinstance(version, str) or not version or len(candidates) != 1:
                            raise ValueError("Catalog initializer namespace requires an unambiguous historical manifest")
                        registry.add(next(iter(candidates)), f"initialization/catalog/CATALOG_INITIALIZATION/{version}", row["id"])
        if owner == "catalog":
            for row in export["artwork_attachment_request"]:
                hold = identity(row["id"])
                registry.add(hold, f"release-{hold}")
    for root in roots:
        for step in FIXED_STEPS:
            registry.add(root, step)
    for owner, export in by_owner.items():
        for row in export["durable_work"]:
            kind = row["kind"]
            parent = identity(row["operation_id"]) if row.get("operation_id") else None
            payload = row.get("payload") or {}
            if kind in ("ARTWORK_ATTACH", "ARTWORK_CONFIRM", "MEDIA_PROCESS"):
                if parent is None:
                    raise ValueError("Known work kind is missing its operation parent")
                step = {"ARTWORK_ATTACH": "attach", "ARTWORK_CONFIRM": "confirm", "MEDIA_PROCESS": "upload-process"}[kind]
                registry.add(parent, step, row["id"])
            elif kind == "ARTWORK_RELEASE":
                if parent is None or not payload.get("subjectId"):
                    raise ValueError("Artwork release work lacks its persisted parent/hold")
                hold = identity(payload["subjectId"])
                registry.add(parent, f"release-{hold}", row["id"])
            generation = row.get("generation")
            if not isinstance(generation, int) or isinstance(generation, bool) or generation < 0:
                raise ValueError("Work generation must be a nonnegative integer")
            registry.add(identity(row["id"]), f"notification-{generation}")
            key = row.get("notification_key")
            if key and kind == "NOTIFICATION":
                match = re.fullmatch(r"(?:work:)?([a-f0-9-]{36})(?::generation:|:)([0-9]+)", key)
                if match:
                    registry.add(identity(match[1]), f"notification-{int(match[2])}", row["id"])
                elif not key.startswith("alert:"):
                    raise ValueError("Unknown retained notification-key shape")
        if owner == "media":
            for row in export["hold_operation_binding"]:
                verb = row["verb"].lower()
                if verb not in ("reserve", "confirm", "release"):
                    raise ValueError("Unknown hold operation verb")
                registry.add(identity(row["hold_id"]), f"media-{verb}", row["operation_id"])
    return registry.binary()


def verify_existing(path, content):
    """Bound existing-file reads and fsync identical bytes before any marker commit."""
    descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
    try:
        metadata = os.fstat(descriptor)
        if not stat.S_ISREG(metadata.st_mode) or metadata.st_mode & 0o022 or metadata.st_size != len(content):
            raise ValueError("Existing artifact must be a protected regular file with identical bytes")
        with os.fdopen(descriptor, "rb", closefd=False) as source:
            if source.read(len(content) + 1) != content:
                raise ValueError("Refusing to replace an index with different bytes; choose a new immutable path")
        os.fsync(descriptor)
    finally:
        os.close(descriptor)
    directory = os.open(Path(path).parent, os.O_RDONLY)
    try:
        os.fsync(directory)
    finally:
        os.close(directory)


def immutable_write(path, content, mode=0o444):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.is_symlink():
        raise ValueError("Immutable artifact must not be a symbolic link")
    if path.exists():
        verify_existing(path, content)
        return
    descriptor, temporary = tempfile.mkstemp(prefix=".identity-", dir=path.parent)
    try:
        with os.fdopen(descriptor, "wb") as output:
            output.write(content)
            output.flush()
            os.fsync(output.fileno())
        os.chmod(temporary, mode)
        os.link(temporary, path)
        directory = os.open(path.parent, os.O_RDONLY)
        try:
            os.fsync(directory)
        finally:
            os.close(directory)
    finally:
        os.unlink(temporary)


def inventory_sql(owner):
    if owner not in OWNERS:
        raise ValueError("Unknown database owner")
    fields = ["'schema_version',1", f"'owner','{owner}'",
              "'active_runtime_sessions',(SELECT count(*) FROM pg_stat_activity WHERE usename IN "
              "('spoolin_core_runtime','spoolin_catalog_runtime','spoolin_media_runtime',"
              "'spoolin_core_maintenance','spoolin_catalog_maintenance','spoolin_media_maintenance',"
              "'spoolin_core_migrator','spoolin_catalog_migrator','spoolin_media_migrator') AND pid<>pg_backend_pid())",
              "'active_leases',(SELECT count(*) FROM appdata.durable_work WHERE state='LEASED' AND lease_until>clock_timestamp())"]
    columns = {
        "operation_receipt": "id,receipt_type" + (",setup_version" if owner == "core" else ",seed_version,encode(seed_snapshot_hash,'hex') AS seed_snapshot_hash" if owner == "catalog" else ""),
        "durable_work": "id,operation_id,kind,generation,notification_key,jsonb_strip_nulls(jsonb_build_object('subjectId',payload->'subjectId','intentId',payload->'intentId')) AS payload",
        "artwork_attachment_request": "id",
        "asset": "id,upload_operation_id",
        "asset_reference": "id",
        "hold_operation_binding": "operation_id,hold_id,verb",
    }
    for table in TABLES[owner]:
        order = "operation_id" if table == "hold_operation_binding" else "id"
        fields.append(f"'{table}',(SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY t.{order}),'[]'::jsonb) FROM (SELECT {columns[table]} FROM appdata.{table}) t)")
    return "SELECT jsonb_build_object(" + ",".join(fields) + ")"


def psql(connection, sql, executable="psql", extra=()):
    command = [executable, *extra, "--no-psqlrc", "--quiet", "--no-align", "--tuples-only", "--set=ON_ERROR_STOP=1", "--dbname", connection]
    with tempfile.TemporaryFile() as commands:
        commands.write(sql.encode("utf-8"))
        commands.seek(0)
        process = subprocess.Popen(command, stdin=commands, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            env={**os.environ, "PGCONNECT_TIMEOUT": "10", "PGOPTIONS": os.environ.get("PGOPTIONS", "") + " -c statement_timeout=30000 -c lock_timeout=5000"})
        poller = selectors.DefaultSelector()
        result = bytearray()
        error_bytes = 0
        deadline = time.monotonic() + PSQL_SECONDS
        try:
            poller.register(process.stdout, selectors.EVENT_READ, "output")
            poller.register(process.stderr, selectors.EVENT_READ, "error")
            while poller.get_map():
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise ValueError("Database command exceeded the bounded wall-clock deadline")
                for event, _ in poller.select(timeout=remaining):
                    chunk = os.read(event.fd, 65536)
                    if not chunk:
                        poller.unregister(event.fileobj)
                    elif event.data == "output":
                        result.extend(chunk)
                        if len(result) > MAX_INPUT_BYTES:
                            raise ValueError("Database export exceeds 16 MiB; bounded batch migration is required")
                    else:
                        error_bytes += len(chunk)
                        if error_bytes > MAX_ERROR_BYTES:
                            raise ValueError("Database diagnostics exceeded the bounded representation")
            try:
                status = process.wait(timeout=max(0.001, deadline - time.monotonic()))
            except subprocess.TimeoutExpired:
                raise ValueError("Database command exceeded the bounded wall-clock deadline") from None
            if status:
                raise ValueError("Database command failed; inspect locally without exposing connection secrets")
            return result.decode("utf-8").strip()
        finally:
            poller.close()
            if process.poll() is None:
                process.kill()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    pass
            process.stdout.close()
            process.stderr.close()


def collect(connections, executable="psql", extra=()):
    if set(connections) != set(OWNERS):
        raise ValueError("Connections for all three owners are required")
    exports = [json.loads(psql(connections[owner], inventory_sql(owner), executable, extra)) for owner in OWNERS]
    validate_exports(exports)
    return exports


def sql_string(value):
    return "'" + value.replace("'", "''") + "'"


def marker_sql(owner, export, digest):
    if not re.fullmatch(r"[a-f0-9]{64}", digest):
        raise ValueError("Invalid registry digest")
    snapshot = sql_string(json.dumps(export, separators=(",", ":"), sort_keys=True)) + "::jsonb"
    tables = ",".join("appdata." + table for table in TABLES[owner])
    current = "(" + inventory_sql(owner) + ")"
    return f"""BEGIN;
SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='30s';
LOCK TABLE {tables} IN ACCESS EXCLUSIVE MODE;
LOCK TABLE appdata.identity_hash_migration IN EXCLUSIVE MODE;
DO $identity_cutover$
BEGIN
  IF {current} IS DISTINCT FROM {snapshot} THEN
    RAISE EXCEPTION 'Identity export changed or owner is no longer quiescent';
  END IF;
  IF (SELECT count(*) FROM appdata.identity_hash_migration WHERE singleton=true) <> 1 THEN
    RAISE EXCEPTION 'Identity marker singleton is missing';
  END IF;
  IF EXISTS (SELECT 1 FROM appdata.identity_hash_migration WHERE registry_sha256 IS NOT NULL AND registry_sha256 <> '{digest}') THEN
    RAISE EXCEPTION 'Identity marker belongs to another immutable index';
  END IF;
  UPDATE appdata.identity_hash_migration SET registry_sha256='{digest}' WHERE singleton=true;
END $identity_cutover$;
COMMIT;
"""


def apply_markers(connections, exports, output, content, executable="psql", extra=()):
    """Commit only marker hashes; the final ready sidecar gates coordinated restart."""
    expected = validate_exports(exports)
    if set(expected) != set(OWNERS):
        raise ValueError("Applying requires all owner exports")
    fresh = collect(connections, executable, extra)
    if validate_exports(fresh) != expected:
        raise ValueError("Sources changed since export")
    digest = hashlib.sha256(content).hexdigest()
    immutable_write(output, content)
    for owner in OWNERS:
        psql(connections[owner], marker_sql(owner, expected[owner], digest), executable, extra)
    for owner in OWNERS:
        marker = json.loads(psql(connections[owner],
            "SELECT coalesce(jsonb_agg(registry_sha256),'[]'::jsonb) FROM appdata.identity_hash_migration WHERE singleton=true", executable, extra))
        if marker != [digest]:
            raise ValueError("Owner marker verification failed; keep all owners stopped")
    if validate_exports(collect(connections, executable, extra)) != expected:
        raise ValueError("Sources changed during cutover; keep all owners stopped")
    verify_existing(output, content)
    immutable_write(str(output) + ".ready", (digest + "\n").encode("ascii"), mode=0o644)


def private_export(path, export):
    content = (json.dumps(export, sort_keys=True, separators=(",", ":")) + "\n").encode()
    immutable_write(path, content, mode=0o600)


def bounded_json(value):
    total = 0
    for chunk in json.JSONEncoder().iterencode(value):
        total += len(chunk.encode("utf-8"))
        if total > MAX_INPUT_BYTES:
            raise ValueError("JSON source exceeds 16 MiB")


def read_json(path):
    with Path(path).open("rb") as source:
        content = source.read(MAX_INPUT_BYTES + 1)
    if len(content) > MAX_INPUT_BYTES:
        raise ValueError("JSON source exceeds 16 MiB")
    return json.loads(content)


def load_manifests(paths):
    """Include referenced frozen predecessors without allowing references to escape their directory."""
    loaded = {}
    def visit(path):
        path = Path(path).resolve(strict=True)
        if path in loaded:
            return
        if len(loaded) >= MAX_MANIFESTS:
            raise ValueError("Too many historical manifests for bounded migration")
        manifest = read_json(path)
        loaded[path] = manifest
        baseline = manifest.get("baselineManifest")
        if baseline is not None:
            relative = Path(baseline)
            if relative.is_absolute() or ".." in relative.parts:
                raise ValueError("Baseline manifest must be a safe relative path")
            target = (path.parent / relative).resolve(strict=True)
            if not target.is_relative_to(path.parent):
                raise ValueError("Baseline manifest escapes its containing directory")
            visit(target)
    for path in paths:
        visit(path)
    return list(loaded.values())


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", action="append", type=Path, help="Frozen historical manifest; repeat to include older versions")
    parser.add_argument("--exports", action="append", type=Path, default=[], help="Complete owner export JSON; all three required")
    parser.add_argument("--frozen-only", action="store_true", help="Generate only frozen seed compatibility mappings for empty databases")
    parser.add_argument("--output", type=Path, help="Write immutable binary index; no database marker updates without --apply")
    parser.add_argument("--connection", action="append", default=[], metavar="OWNER=DSN", help="psql service/connection string; keep passwords in pgpass/environment")
    parser.add_argument("--psql", default="psql", help="psql executable or controlled wrapper")
    parser.add_argument("--psql-arg", action="append", default=[], help="Fixed wrapper argument; repeat as needed")
    parser.add_argument("--export-dir", type=Path, help="Save complete private owner exports before inspect/apply")
    parser.add_argument("--apply", action="store_true", help="Explicit coordinated marker cutover; all owners must stay stopped")
    args = parser.parse_args(argv)
    manifests = args.manifest or [Path(__file__).resolve().parents[2] / "data/seed/manifest.json"]
    if len(args.exports) > len(OWNERS):
        parser.error("At most three owner export files may be loaded")
    exports = [read_json(path) for path in args.exports]
    connections = {}
    for binding in args.connection:
        owner, separator, value = binding.partition("=")
        if not separator or owner not in OWNERS or owner in connections or not value:
            parser.error("--connection requires one OWNER=DSN for each owner")
        connections[owner] = value
    if args.frozen_only and (connections or args.apply or args.export_dir):
        parser.error("--frozen-only cannot export databases or apply markers")
    if connections and not exports:
        exports = collect(connections, args.psql, args.psql_arg)
    if args.apply and (not args.output or set(connections) != set(OWNERS)):
        parser.error("--apply requires --output and all three live owner connections")
    if args.export_dir:
        validate_exports(exports)
        if not exports:
            parser.error("--export-dir requires complete database exports")
        for export in exports:
            private_export(args.export_dir / (export["owner"] + ".json"), export)
    if args.frozen_only and exports:
        parser.error("--frozen-only cannot be combined with --exports")
    if not args.frozen_only and not exports:
        parser.error("Supply all three --exports, or explicitly choose --frozen-only")
    content = build(exports, load_manifests(manifests))
    if args.output:
        immutable_write(args.output, content)
    if args.apply:
        apply_markers(connections, exports, args.output, content, args.psql, args.psql_arg)
    print(json.dumps({"records": (len(content) - 16) // 48, "sha256": hashlib.sha256(content).hexdigest(), "markers_updated": args.apply}))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, TypeError) as error:
        print(f"Identity migration refused: {type(error).__name__}", file=sys.stderr)
        sys.exit(1)
