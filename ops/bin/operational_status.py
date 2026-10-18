"""Read-only owner database aggregates; private records and external success are never projected."""
import json
import os
from pathlib import Path
import re
import subprocess

OWNERS = ("core", "catalog", "media")
STATES = {"READY", "LEASED", "DONE", "EXHAUSTED", "CANCELLED"}
MODES = {"database", "smtp", "slack", "google_chat"}
DELIVERY = {"RECORDED", "PENDING", "ACCEPTED", "DELIVERY_CONFIRMED", "UNKNOWN", "FAILED"}
COUNTS_SQL = """SELECT json_build_object(
 'work', COALESCE((SELECT json_agg(t) FROM
   (SELECT state,count(*) AS count FROM appdata.durable_work GROUP BY state ORDER BY state) t),'[]'),
 'notifications', COALESCE((SELECT json_agg(t) FROM
   (SELECT notification_mode AS mode,notification_state AS state,count(*) AS count
    FROM appdata.durable_work WHERE kind='NOTIFICATION'
    GROUP BY notification_mode,notification_state ORDER BY notification_mode,notification_state) t),'[]'))"""
SEED_SQL = """SELECT COALESCE(json_agg(t),'[]') FROM
 (SELECT state,seed_version AS version,encode(seed_snapshot_hash,'hex') AS hash,
 seed_movie_count AS movies,seed_tv_count AS series FROM appdata.operation_receipt
 WHERE receipt_type='CATALOG_INITIALIZATION') t"""


def query(compose, owner, sql):
    if owner not in OWNERS:
        raise ValueError("Unknown owner")
    shell = ('export PGPASSWORD="$(cat /run/secrets/' + owner + '-backup-password)"; '
             'exec psql -X -qAt -v ON_ERROR_STOP=1 -h /var/run/postgresql '
             '-U spoolin_' + owner + '_backup -d spoolin_' + owner)
    completed = subprocess.run([*compose, "exec", "-T", "postgres", "/bin/sh", "-ec", shell],
                               input="BEGIN READ ONLY; SET LOCAL statement_timeout='3s'; " + sql + "; COMMIT;",
                               text=True, capture_output=True, timeout=8, check=False)
    if completed.returncode:
        raise ValueError("Owner status query unavailable")
    return json.loads(completed.stdout)


def owner_observation(owner, value):
    work, notifications = value["work"], value["notifications"]
    if not isinstance(work, list) or not isinstance(notifications, list):
        raise ValueError("Invalid count lists")
    projected_work, projected_notifications = [], []
    for row in work:
        if row["state"] not in STATES or type(row["count"]) is not int or row["count"] < 0:
            raise ValueError("Invalid work aggregate")
        projected_work.append({"state": row["state"], "count": row["count"]})
    for row in notifications:
        if row["mode"] not in MODES or row["state"] not in DELIVERY or type(row["count"]) is not int or row["count"] < 0:
            raise ValueError("Invalid notification aggregate")
        projected_notifications.append({"mode": row["mode"], "state": row["state"], "count": row["count"]})
    degraded = any(row["state"] == "EXHAUSTED" and row["count"] for row in projected_work)
    degraded = degraded or any(row["state"] in {"UNKNOWN", "FAILED"} and row["count"] for row in projected_notifications)
    return {"service": owner, "probe": "owner-work-notification-state", "result": "FAIL" if degraded else "PASS",
            "work": projected_work, "notifications": projected_notifications,
            "deliveryMeaning": "RECORDED, ACCEPTED and DELIVERY_CONFIRMED are distinct"}


def seed_observation(rows, manifest):
    selected = [row for row in rows if isinstance(row, dict) and row.get("version") == manifest["manifestVersion"]] \
        if isinstance(rows, list) else []
    valid = (len(selected) == 1 and selected[0].get("state") == "SUCCEEDED"
             and selected[0].get("hash") == manifest["snapshotSHA256"]
             and selected[0].get("movies") == 100 and selected[0].get("series") == 100)
    return {"service": "catalog", "probe": "original-seed-initialization", "result": "PASS" if valid else "FAIL",
            "manifestVersion": manifest["manifestVersion"], "snapshotSHA256": manifest["snapshotSHA256"],
            "originalMovies": 100 if valid else None, "originalSeries": 100 if valid else None,
            "boundary": "original initialization receipt; complete current dataset/artwork requires separate acceptance"}


def observations(root):
    installation = os.environ.get("SPOOLIN_INSTALLATION", "")
    if not re.fullmatch(r"[a-z][a-z0-9-]{2,63}", installation) or os.environ.get("SPOOLIN_SKIP_COMPOSE") == "1":
        return [{"probe": "owner-state", "result": "FAIL", "reason": "owned-installation-required"}]
    compose = ["docker", "compose", "--project-name", installation, "-f", str(root / "ops/compose.yaml")]
    results = []
    for owner in OWNERS:
        try:
            results.append(owner_observation(owner, query(compose, owner, COUNTS_SQL)))
        except (OSError, ValueError, TypeError, KeyError, subprocess.TimeoutExpired):
            results.append({"service": owner, "probe": "owner-work-notification-state", "result": "FAIL", "reason": "unavailable"})
    try:
        manifest = json.loads((root / "data/seed/manifest.json").read_text())
        results.append(seed_observation(query(compose, "catalog", SEED_SQL), manifest))
    except (OSError, ValueError, TypeError, KeyError, subprocess.TimeoutExpired):
        results.append({"service": "catalog", "probe": "original-seed-initialization", "result": "FAIL", "reason": "unavailable"})
    return results


def tunnel_observation():
    # No production registered runner/pin exists in this source revision; no environment flag invents a connection.
    return {"client": "CHATGPT", "result": "UNVERIFIED", "enabled": None, "connected": None,
            "reason": "registered-pinned-runner-unavailable", "actualClientAcceptance": "PENDING"}
