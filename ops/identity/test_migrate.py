"""Small offline preservation checks; no database, Docker, or external dependency required."""
import hashlib
import importlib.util
import json
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from unittest.mock import patch
import uuid

SPEC = importlib.util.spec_from_file_location("identity_migration", Path(__file__).with_name("migrate.py"))
migration = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(migration)
ROOT = uuid.UUID("00000000-0000-4000-8000-000000000001")
HOLD = uuid.UUID("00000000-0000-4000-8000-000000000002")
SUBJECT = uuid.UUID("00000000-0000-4000-8000-000000000003")


def exports():
    return [{"schema_version": 1, "owner": owner, "active_runtime_sessions": 0, "active_leases": 0,
             **{table: [] for table in migration.TABLES[owner]}} for owner in migration.OWNERS]


def work(kind, parent=ROOT, subject=SUBJECT, generation=0):
    step = {"MEDIA_PROCESS": "upload-process", "ARTWORK_ATTACH": "attach", "ARTWORK_CONFIRM": "confirm", "ARTWORK_RELEASE": f"release-{subject}"}[kind]
    return {"id": str(uuid.uuid5(parent, step)), "operation_id": str(parent), "kind": kind,
            "payload": {"subjectId": str(subject)}, "generation": generation, "notification_key": None}


def lookup(content, namespace, name):
    key = hashlib.sha256(namespace.bytes + name).digest()
    count = struct.unpack(">Q", content[8:16])[0]
    for index in range(count):
        record = content[16 + 48 * index:64 + 48 * index]
        if record[:32] == key:
            return uuid.UUID(bytes=record[32:])
    raise AssertionError("Binding missing from index")


class MigrationTests(unittest.TestCase):
    def test_binary_is_sorted_unique_and_preserves_independent_uuid5(self):
        inputs = exports()
        inputs[0]["operation_receipt"] = [{"id": str(ROOT), "receipt_type": "REQUEST"}]
        content = migration.build(inputs, [])
        self.assertEqual(b"SPIDIDX1", content[:8])
        self.assertEqual(8, struct.unpack(">Q", content[8:16])[0])
        keys = [content[start:start + 32] for start in range(16, len(content), 48)]
        self.assertEqual(sorted(set(keys)), keys)
        self.assertEqual(uuid.uuid5(ROOT, "asset"), lookup(content, ROOT, b"asset"))
        self.assertEqual(content, migration.build(list(reversed(inputs)), []))

    def test_all_owner_roots_children_and_retained_work_bindings_are_included(self):
        inputs = exports()
        inputs[1]["durable_work"] = [work("ARTWORK_ATTACH"), work("ARTWORK_CONFIRM"), work("ARTWORK_RELEASE", subject=HOLD)]
        inputs[1]["artwork_attachment_request"] = [{"id": str(HOLD)}]
        inputs[2]["durable_work"] = [work("MEDIA_PROCESS")]
        inputs[2]["asset_reference"] = [{"id": str(HOLD)}]
        inputs[2]["hold_operation_binding"] = [{"operation_id": str(uuid.uuid5(HOLD, "media-confirm")), "hold_id": str(HOLD), "verb": "CONFIRM"}]
        content = migration.build(inputs, [])
        for parent, step in [(ROOT, "attach"), (ROOT, "upload-process"), (ROOT, f"release-{HOLD}"), (HOLD, f"release-{HOLD}"), (HOLD, "media-confirm")]:
            self.assertEqual(uuid.uuid5(parent, step), lookup(content, parent, step.encode()))

    def test_notification_current_and_retained_generations_survive_without_source_receipt(self):
        inputs = exports()
        source = work("ARTWORK_ATTACH", generation=3)
        notification_id = uuid.uuid5(uuid.UUID(source["id"]), "notification-1")
        inputs[1]["durable_work"] = [source, {"id": str(notification_id), "operation_id": str(ROOT), "kind": "NOTIFICATION", "payload": {}, "generation": 0,
                                               "notification_key": f"work:{source['id']}:generation:1"}]
        content = migration.build(inputs, [])
        for generation in (1, 3):
            step = f"notification-{generation}"
            self.assertEqual(uuid.uuid5(uuid.UUID(source["id"]), step), lookup(content, uuid.UUID(source["id"]), step.encode()))

    def test_known_work_id_omissions_private_fields_and_live_owners_fail_closed(self):
        for change in ("missing-owner", "private-field", "live-runtime", "leased", "wrong-id", "missing-parent"):
            inputs = exports()
            if change == "missing-owner": inputs.pop()
            elif change == "private-field": inputs[0]["operation_receipt"] = [{"id": str(ROOT), "receipt_type": "REQUEST", "result": "private"}]
            elif change == "live-runtime": inputs[0]["active_runtime_sessions"] = 1
            elif change == "leased": inputs[2]["active_leases"] = 1
            else:
                row = work("MEDIA_PROCESS")
                row["id" if change == "wrong-id" else "operation_id"] = str(HOLD) if change == "wrong-id" else None
                inputs[2]["durable_work"] = [row]
            with self.subTest(change=change), self.assertRaises(ValueError): migration.build(inputs, [])

    def test_manifest_raw_length_prefix_and_custom_catalog_initialization_namespace(self):
        namespace = uuid.UUID("12345678-1234-5678-9234-567812345678")
        name = b"\x00\x00\x00\x08wikidata\x00\x00\x00\x06PERSON\x00\x00\x00\x03Q42"
        retained = uuid.uuid5(namespace, name.decode())
        manifest = {"namespaceUUID": str(namespace), "manifestVersion": "frozen", "snapshotSHA256": "a" * 64,
                    "records": [], "roles": [], "people": [{"source": "wikidata", "externalId": "Q42", "stableUUID": str(retained)}]}
        inputs = exports()
        initialization = uuid.uuid5(namespace, "initialization/catalog/CATALOG_INITIALIZATION/installed-version")
        inputs[1]["operation_receipt"] = [{"id": str(initialization), "receipt_type": "CATALOG_INITIALIZATION", "seed_version": "installed-version", "seed_snapshot_hash": "a" * 64}]
        content = migration.build(inputs, [manifest])
        self.assertEqual(retained, lookup(content, namespace, name))
        self.assertEqual(initialization, lookup(content, namespace, b"initialization/catalog/CATALOG_INITIALIZATION/installed-version"))
        with self.assertRaises(ValueError): migration.build(inputs, [])

    def test_recursive_frozen_inputs_are_bounded_to_relative_baseline_paths(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "baseline.json").write_text('{"manifestVersion":"old"}')
            (root / "main.json").write_text('{"manifestVersion":"new","baselineManifest":"baseline.json"}')
            self.assertEqual(["new", "old"], [item["manifestVersion"] for item in migration.load_manifests([root / "main.json"])])
            (root / "main.json").write_text('{"baselineManifest":"../outside.json"}')
            with self.assertRaises(ValueError): migration.load_manifests([root / "main.json"])

    def test_entry_and_input_caps_fail_before_unbounded_accumulation(self):
        inputs = exports()
        inputs[0]["operation_receipt"] = [{"id": str(ROOT), "receipt_type": "REQUEST"}]
        with patch.object(migration, "MAX_ENTRIES", 2), self.assertRaises(ValueError): migration.build(inputs, [])
        with patch.object(migration, "MAX_INPUT_BYTES", 10), self.assertRaises(ValueError): migration.build(inputs, [])

    def test_artifact_is_immutable_protected_and_never_follows_symlinks(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "index.bin"
            migration.immutable_write(output, b"index")
            self.assertEqual(0o444, output.stat().st_mode & 0o777)
            migration.immutable_write(output, b"index")
            with self.assertRaises(ValueError): migration.immutable_write(output, b"different")
            link = Path(directory) / "link.bin"
            link.symlink_to(output)
            with self.assertRaises(ValueError): migration.immutable_write(link, b"index")

    def test_interrupted_apply_has_no_ready_file_and_same_index_can_resume(self):
        inputs = exports()
        content = migration.build(inputs, [])
        digest = hashlib.sha256(content).hexdigest()
        markers = {}
        attempts = []
        interrupted = False
        def database(connection, sql, *unused):
            nonlocal interrupted
            if sql.startswith("BEGIN"):
                attempts.append(connection)
                if connection == "catalog" and not interrupted:
                    interrupted = True
                    raise ValueError("Interrupted fixture")
                markers[connection] = digest
                return ""
            return json.dumps([markers[connection]])
        connections = {owner: owner for owner in migration.OWNERS}
        with tempfile.TemporaryDirectory() as directory, patch.object(migration, "collect", return_value=inputs), patch.object(migration, "psql", side_effect=database):
            output = Path(directory) / "index.bin"
            with self.assertRaises(ValueError): migration.apply_markers(connections, inputs, output, content)
            self.assertFalse(Path(str(output) + ".ready").exists())
            migration.apply_markers(connections, inputs, output, content)
            self.assertEqual(digest + "\n", Path(str(output) + ".ready").read_text())
            self.assertEqual({owner: digest for owner in migration.OWNERS}, markers)

    def test_source_drift_prevents_marker_writes(self):
        inputs = exports()
        changed = exports()
        changed[0]["operation_receipt"] = [{"id": str(ROOT), "receipt_type": "REQUEST"}]
        with tempfile.TemporaryDirectory() as directory, patch.object(migration, "collect", return_value=changed), patch.object(migration, "psql") as database:
            with self.assertRaises(ValueError): migration.apply_markers({owner: owner for owner in migration.OWNERS}, inputs, Path(directory) / "index", migration.build(inputs, []))
            database.assert_not_called()

    def test_export_sql_excludes_receipt_results_and_private_work_fields(self):
        for owner in migration.OWNERS:
            sql = migration.inventory_sql(owner)
            self.assertNotIn("SELECT *", sql)
            self.assertNotIn("notification_summary", sql)
            self.assertNotIn("request_hash", sql)
            self.assertIn("jsonb_strip_nulls", sql)
            for component in migration.OWNERS:
                for role in ("runtime", "maintenance", "migrator"):
                    self.assertIn(f"spoolin_{component}_{role}", sql)

    def test_database_wrapper_has_a_deadline_even_when_output_never_closes(self):
        with patch.object(migration, "PSQL_SECONDS", 0.1), self.assertRaisesRegex(ValueError, "deadline"):
            migration.psql("unused", "SELECT 1", sys.executable, ("-c", "import time; time.sleep(30)"))

    def test_database_output_and_discarded_diagnostics_are_bounded(self):
        for stream, limit in (("stdout", "MAX_INPUT_BYTES"), ("stderr", "MAX_ERROR_BYTES")):
            with self.subTest(stream=stream), patch.object(migration, limit, 10), self.assertRaises(ValueError):
                migration.psql("unused", "SELECT 1", sys.executable,
                               ("-c", f"import sys; sys.{stream}.write('x' * 100); sys.{stream}.flush()"))

    def test_database_command_receives_sql_without_a_writable_input_pipe(self):
        result = migration.psql("unused", "SELECT 1", sys.executable,
                                ("-c", "import sys; print(sys.stdin.read())"))
        self.assertEqual("SELECT 1", result)

    def test_export_validation_precedes_any_export_file_writes(self):
        inputs = exports()
        inputs[0]["owner"] = "../escape"
        with patch.object(migration, "read_json", side_effect=inputs), patch.object(migration, "private_export") as write:
            with self.assertRaises(ValueError):
                migration.main(["--exports", "a", "--exports", "b", "--exports", "c", "--export-dir", "private"])
            write.assert_not_called()

    def test_excess_export_arguments_are_rejected_before_reading(self):
        with patch.object(migration, "read_json") as read, patch.object(sys, "stderr"):
            with self.assertRaises(SystemExit):
                migration.main(["--exports", "a", "--exports", "b", "--exports", "c", "--exports", "d"])
            read.assert_not_called()

    def test_identical_existing_artifact_is_flushed_before_reuse(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "index.bin"
            migration.immutable_write(output, b"index")
            with patch.object(migration.os, "fsync", wraps=migration.os.fsync) as flush:
                migration.immutable_write(output, b"index")
                self.assertEqual(2, flush.call_count)


if __name__ == "__main__":
    unittest.main()
