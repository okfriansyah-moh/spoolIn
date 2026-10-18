"""Actual packaged operator commands against an explicitly owned, paused remote CI installation."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import tempfile

from operator_fixtures import recovery_fixture, retention_fixture

OWNERS = ("core", "catalog", "media")


def require_owned_runner():
    expected = "ci-" + os.environ["GITHUB_RUN_ID"] + "-" + os.environ["GITHUB_RUN_ATTEMPT"]
    if (os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted"
            or os.environ.get("RUNNER_OS") != "Linux" or os.environ.get("SPOOLIN_INSTALLATION") != expected
            or os.environ.get("SPOOLIN_SKIP_COMPOSE") == "1"):
        raise RuntimeError("Owned GitHub-hosted Linux installation required")
    return ["docker", "compose", "--project-name", expected, "-f", str(Path("ops/compose.yaml").resolve())]


class OperatorRuntime:
    def __init__(self, invoke, record):
        self.compose = require_owned_runner()
        self.invoke = invoke
        self.record = record
        self.current_step = "owned-stack-setup"

    def database(self, owner, sql, role="runtime", expected=0):
        if owner not in OWNERS or role not in {"runtime", "maintenance", "backup", "postgres"}:
            raise ValueError("Unsupported owner or fixed database role")
        user = "postgres" if role == "postgres" else "spoolin_" + owner + "_" + role
        secret = "/run/spoolin/postgres-password" if role == "postgres" else "/run/secrets/" + owner + "-" + role + "-password"
        shell = 'export PGPASSWORD="$(cat ' + secret + ')"; exec psql -X -qAt -v ON_ERROR_STOP=1 '
        shell += '--set=VERBOSITY=sqlstate -h /var/run/postgresql -U ' + user + ' -d spoolin_' + owner
        result = subprocess.run([*self.compose, "exec", "-T", "--user", "0:0", "postgres", "sh", "-ec", shell],
                                input="SET search_path=appdata; SET statement_timeout='5s'; " + sql,
                                text=True, capture_output=True, timeout=15, check=False)
        if result.returncode != expected or len(result.stdout) > 262144:
            raise RuntimeError("Controlled owner database probe failed; raw output withheld")
        return result.stdout.strip(), result.stderr

    def value(self, owner, sql, role="runtime"):
        output, _ = self.database(owner, "SELECT row_to_json(v) FROM (" + sql + ") v;", role)
        return json.loads(output)

    def run(self):
        stopped = subprocess.run([*self.compose, "stop", "mcp", *OWNERS], capture_output=True, timeout=90, check=False)
        if stopped.returncode:
            raise RuntimeError("Could not pause all application writers")
        self.record("writers", {"result": "PASS", "state": "STOPPED", "reason": "serialized controlled operational faults"})
        for name, action in (("migration", self.migrations), ("recovery", self.recovery), ("maintenance", self.maintenance)):
            try:
                self.current_step = name + "-starting"
                self.record(name, action())
            except Exception:
                self.record(name, {"result": "FAIL", "reason": "actual-command-or-state-assertion-failed", "step": self.current_step, "rawOutput": "withheld"})
        self.degraded_status()
        self.record("finalState", {"result": "OBSERVED", "writers": "STOPPED", "retainedFaults": True,
                                   "reason": "preserve unresolved evidence and prevent synthetic provider sends"})

    def degraded_status(self):
        try:
            output, _ = self.invoke("status.sh", expected=1, timeout=90)
            value = json.loads(output)
            owners = [row for row in value["probes"] if row.get("probe") == "owner-work-notification-state"]
            assert len(owners) == 3 and all(row["result"] == "FAIL" for row in owners)
            assert all(any(work["state"] == "EXHAUSTED" and work["count"] > 0 for work in row["work"]) for row in owners)
            self.invoke_make_degraded()
            self.record("degradedStatus", {"result": "PASS", "healthScriptExit": 1, "makeStatusExit": 2,
                                           "ownerState": "all owners retain observed EXHAUSTED work", "writers": "STOPPED"})
        except Exception:
            self.record("degradedStatus", {"result": "FAIL", "reason": "actual-degraded-state-assertion-failed"})

    def invoke_make_degraded(self):
        completed = subprocess.run(["make", "status"], text=True, capture_output=True, timeout=90, check=False)
        assert completed.returncode == 2

    def migrations(self):
        self.current_step = "migration-preconditions"
        self.invoke("migrate.sh", expected=2)
        self.invoke("migrate.sh", "other", expected=2)
        owners = {}
        for owner in OWNERS:
            self.current_step = "migration-idempotence-" + owner
            before = self.history(owner)
            self.invoke("migrate.sh", owner, timeout=160)
            self.invoke("migrate.sh", owner, timeout=160)
            assert self.history(owner) == before
            roles = self.value(owner, "SELECT pg_has_role('spoolin_" + owner + "_migrator','spoolin_" + owner + "_owner','MEMBER') AS member")
            assert roles["member"] is False
            owners[owner] = {"result": "PASS", "idempotent": True, "historyDigest": digest(before), "membershipRevoked": True}
        self.migration_drift()
        self.migration_lock()
        return {"result": "PASS", "owners": owners, "checksumDriftRefused": True, "installationLockRefused": True,
                "remaining": ["interrupted nontransactional index and previous-binary migration compatibility"]}

    def history(self, owner):
        return self.value(owner, "SELECT json_agg(json_build_array(installed_rank,version,checksum,success) ORDER BY installed_rank) AS rows FROM appdata.flyway_schema_history")

    def migration_drift(self):
        self.current_step = "migration-checksum-drift"
        original = self.value("catalog", "SELECT installed_rank,checksum FROM appdata.flyway_schema_history WHERE checksum IS NOT NULL ORDER BY installed_rank LIMIT 1")
        rank, checksum = int(original["installed_rank"]), int(original["checksum"])
        try:
            self.database("catalog", f"UPDATE appdata.flyway_schema_history SET checksum={checksum ^ 1} WHERE installed_rank={rank};", "postgres")
            self.invoke("migrate.sh", "catalog", expected=4, timeout=160)
            assert self.value("catalog", f"SELECT checksum FROM appdata.flyway_schema_history WHERE installed_rank={rank}")["checksum"] == checksum ^ 1
        finally:
            self.database("catalog", f"UPDATE appdata.flyway_schema_history SET checksum={checksum} WHERE installed_rank={rank};", "postgres")
        self.invoke("migrate.sh", "catalog", timeout=160)

    def migration_lock(self):
        self.current_step = "migration-installation-lock-timeout"
        lock = Path(os.environ["SPOOLIN_LOCK_DIR"]) / ("migration-" + os.environ["SPOOLIN_INSTALLATION"] + ".lock")
        lock.mkdir(mode=0o700)
        started = time.monotonic()
        try:
            self.invoke("migrate.sh", "core", expected=4, timeout=140)
            assert time.monotonic() - started >= 119
            assert lock.is_dir(), "A contending invocation must not remove another writer's lock"
        finally:
            lock.rmdir()

    def work(self, owner, identifier):
        return self.value(owner, "SELECT id,operation_id,state,generation,lease_epoch,attempt_count,notification_state,"
                          "deadline_at>clock_timestamp() AS live_deadline FROM appdata.durable_work WHERE id='" + identifier + "'")

    def events(self, owner, identifier):
        return self.value(owner, "SELECT json_agg(row_to_json(e) ORDER BY e.created_at,e.id) AS rows FROM "
                          "(SELECT id,event_type,generation,attempt_no,lease_epoch,attempt_outcome,error_code,created_at "
                          "FROM appdata.operational_event WHERE work_id='" + identifier + "') e")

    def recover(self, owner, identifier, generation="0", epoch="1", expected=0):
        return self.invoke("recover-work.sh", "--service", owner, "--work-id", identifier, "--execute",
                           "--expected-generation", generation, "--expected-epoch", epoch,
                           "--reason", "Controlled CI confirmed failed-before-acceptance recovery", expected=expected)

    def recovery(self):
        self.current_step = "recovery-synthetic-setup"
        operation, identifier, sql = recovery_fixture()
        self.database("core", sql)
        before = self.events("core", identifier)
        receipt_before = self.value("core", f"SELECT id,state,request_hash,result FROM appdata.operation_receipt WHERE id='{operation}'")
        arguments = ("--service", "core", "--work-id", identifier)
        self.operator_authority(arguments)
        self.current_step = "recovery-inspection-and-preconditions"
        inspected, _ = self.invoke("recover-work.sh", *arguments, "--inspect")
        value = json.loads(inspected)
        assert value["generation"] == 0 and value["leaseEpoch"] == 1 and value["state"] == "EXHAUSTED"
        assert len(value["phaseHistory"]) == 2
        self.invoke("recover-work.sh", *arguments, "--execute", "--expected-generation", "0", "--expected-epoch", "1", expected=2)
        self.recover("core", identifier, generation="1", expected=4)
        self.recover("core", identifier, epoch="2", expected=4)
        self.current_step = "recovery-current-fence-commit"
        self.recover("core", identifier)
        after = self.work("core", identifier)
        assert after["id"] == identifier and after["operation_id"] == operation and after["state"] == "READY"
        assert after["generation"] == 1 and after["lease_epoch"] == 1 and after["attempt_count"] == 0 and after["live_deadline"]
        assert after["notification_state"] == "PENDING"
        self.current_step = "recovery-history-original-receipt-and-replay"
        self.recover("core", identifier, expected=4)
        events = self.events("core", identifier)["rows"]
        assert [row for row in events if row["event_type"] != "RECOVERY"] == before["rows"]
        assert len([row for row in events if row["event_type"] == "RECOVERY"]) == 1
        assert self.value("core", f"SELECT id,state,request_hash,result FROM appdata.operation_receipt WHERE id='{operation}'") == receipt_before
        self.unknown_recovery()
        return {"result": "PASS", "case": "ATDD-158", "originalOperation": operation, "workId": identifier,
                "previousEvidenceDigest": digest(before), "generation": 1, "epoch": 1, "replayRefused": True,
                "providerDelivery": "NOT ATTEMPTED; workers paused", "unknownDeliveryRecovery": "REFUSED"}

    def operator_authority(self, arguments):
        self.invoke("recover-work.sh", "--unsupported", expected=2)
        previous = os.environ["SPOOLIN_LOCK_DIR"]
        with tempfile.TemporaryDirectory(prefix="unauthorized-operator-", dir=os.environ["RUNNER_TEMP"]) as path:
            Path(path).chmod(0o755)
            os.environ["SPOOLIN_LOCK_DIR"] = path
            try:
                self.invoke("recover-work.sh", *arguments, "--inspect", expected=2)
            finally:
                os.environ["SPOOLIN_LOCK_DIR"] = previous

    def unknown_recovery(self):
        self.current_step = "recovery-unknown-provider-refusal"
        operation, identifier, sql = recovery_fixture()
        sql = sql.replace("'FAILED'", "'UNKNOWN'", 1)
        self.database("core", sql)
        before = self.work("core", identifier), self.events("core", identifier)
        self.recover("core", identifier, expected=4)
        assert (self.work("core", identifier), self.events("core", identifier)) == before

    def maintenance(self):
        observations = {}
        self.invoke("maintenance.sh", "--service", "catalog", "--schedule", "daily", "--unknown", expected=2)
        for owner in OWNERS:
            observations[owner] = self.maintain_owner(owner)
        return {"result": "PASS", "case": "ATDD-208", "owners": observations,
                "remaining": ["draft-save/purge concurrency", "proposal redaction", "Media processing-block/output preservation"]}

    def maintain_owner(self, owner):
        self.current_step = "maintenance-setup-" + owner
        entries, expired, sql = retention_fixture()
        assert self.value(owner, "SELECT current_user AS role", "maintenance")["role"] == "spoolin_" + owner + "_maintenance"
        initializers = self.value(owner, "SELECT COALESCE(json_agg(row_to_json(r) ORDER BY id),'[]') AS rows FROM appdata.operation_receipt r WHERE receipt_type<>'REQUEST'")
        lifetime = self.lifetime_ledgers(owner)
        self.database(owner, sql)
        guarded = {label: (self.work(owner, value), self.events(owner, value))
                   for label, value in entries.items() if label != "eligible"}
        self.current_step = "maintenance-hourly-bounds-" + owner
        before = self.remaining_bodies(owner, expired)
        inspected, _ = self.invoke("maintenance.sh", "--service", owner, "--schedule", "hourly")
        assert json.loads(inspected)["executed"] is False and before == self.remaining_bodies(owner, expired)
        for remaining in (1, 0):
            output, _ = self.invoke("maintenance.sh", "--service", owner, "--schedule", "hourly", "--execute")
            result = json.loads(output)
            assert result["batchLimit"] == 100 and result["executed"] is True
            assert 0 <= result["counts"]["receiptBodies"] <= 100
            assert self.remaining_bodies(owner, expired) == remaining
        assert self.value(owner, "SELECT count(*) AS count FROM appdata.operation_receipt WHERE id IN (" +
                          ",".join("'" + item + "'" for item in expired) + ")")["count"] == 101
        self.current_step = "maintenance-daily-preservation-" + owner
        daily, _ = self.invoke("maintenance.sh", "--service", owner, "--schedule", "daily", "--execute")
        counts = json.loads(daily)["counts"]
        assert 0 <= counts["work"] <= 100 and 0 <= counts["payloads"] <= 100
        for label, identifier in entries.items():
            count = self.value(owner, f"SELECT count(*) AS count FROM appdata.durable_work WHERE id='{identifier}'")["count"]
            assert count == (0 if label == "eligible" else 1)
            if label != "eligible":
                assert (self.work(owner, identifier), self.events(owner, identifier)) == guarded[label]
        assert self.value(owner, "SELECT COALESCE(json_agg(row_to_json(r) ORDER BY id),'[]') AS rows FROM appdata.operation_receipt r WHERE receipt_type<>'REQUEST'") == initializers
        assert self.lifetime_ledgers(owner) == lifetime
        self.current_step = "maintenance-role-denials-" + owner
        _, error = self.database(owner, "CREATE TABLE appdata.ci_forbidden_ddl(id integer);", "maintenance", expected=3)
        assert "42501" in error
        _, error = self.database(owner, "DELETE FROM appdata.operation_receipt WHERE false;", "maintenance", expected=3)
        assert "42501" in error
        return {"result": "PASS", "boundedBodies": 101, "firstBatchRemaining": 1, "receiptTombstonesRetained": True,
                "initializersPreserved": True, "lifetimeLedgerDigests": lifetime, "eligiblePairedHistoryPurged": True,
                "preserved": ["EXHAUSTED", "UNCERTAIN", "unpaired attempt"], "maintenanceDDLAndReceiptDelete": "DENIED"}

    def lifetime_ledgers(self, owner):
        tables = {"core": ("crypto_key_version",),
                  "catalog": ("catalog_audit_event", "provenance_record", "seed_member", "movie_proposal"),
                  "media": ("asset", "asset_reference", "hold_operation_binding")}[owner]
        return {table: self.value(owner, "SELECT count(*) AS count, md5(COALESCE(string_agg(v.digest,'' ORDER BY v.digest),'')) AS digest "
                                 "FROM (SELECT md5(row_to_json(t)::text) AS digest FROM appdata." + table + " t) v")
                for table in tables}

    def remaining_bodies(self, owner, identifiers):
        ids = ",".join("'" + str(value) + "'" for value in identifiers)
        return self.value(owner, f"SELECT count(*) AS count FROM appdata.operation_receipt WHERE id IN ({ids}) AND result IS NOT NULL")["count"]


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
