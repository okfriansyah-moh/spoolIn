#!/usr/bin/env python3
"""Bounded local operator readiness probes with allowlisted diagnostic output only."""
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timezone

_status_spec = importlib.util.spec_from_file_location("spoolin_operational_status", Path(__file__).with_name("operational_status.py"))
operational_status = importlib.util.module_from_spec(_status_spec)
_status_spec.loader.exec_module(operational_status)

SERVICES = ("postgres", "core", "catalog", "media", "mcp", "prometheus", "alertmanager", "grafana")
HEALTHCHECKED = {"postgres", "mcp", "prometheus", "alertmanager"}
SAFE_STATUSES = {"UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN", "ok", "running", "created", "restarting",
                 "exited", "dead", "paused", "removing", "healthy", "unhealthy", "starting"}


def safe_status(value):
    return value if isinstance(value, str) and value in SAFE_STATUSES else "unavailable"


def compose_rows(text):
    try:
        parsed = json.loads(text)
        rows = parsed if isinstance(parsed, list) else [parsed]
    except json.JSONDecodeError:
        rows = [json.loads(line) for line in text.splitlines() if line.strip()]
    if not all(isinstance(row, dict) for row in rows):
        raise ValueError("Invalid Compose rows")
    return rows


def container_observations(rows):
    results = []
    for service in SERVICES:
        selected = [row for row in rows if row.get("Service") == service]
        if len(selected) != 1:
            results.append({"service": service, "probe": "container", "result": "FAIL", "reason": "missing-or-duplicate"})
            continue
        row = selected[0]
        state, health = safe_status(row.get("State")), safe_status(row.get("Health"))
        ready = state == "running" and (service not in HEALTHCHECKED or health == "healthy")
        results.append({"service": service, "probe": "container", "state": state, "health": health,
                        "result": "PASS" if ready else "FAIL"})
    return results


def health_observation(service, body, expected):
    result = {"service": service, "probe": "https-readiness", "result": "FAIL"}
    try:
        value = json.loads(body)
        result["observed"] = safe_status(value.get(expected[0]))
        components = value.get("components", {})
        if isinstance(components, dict):
            result["components"] = {name: safe_status(item.get("status")) for name, item in components.items()
                                    if name in {"readinessState", "schema", "rateReadiness", "db"} and isinstance(item, dict)}
        if value.get(expected[0]) == expected[1]:
            result["result"] = "PASS"
    except (ValueError, AttributeError):
        result["reason"] = "invalid-health-json"
    return result


def https_probe(service, port, expected, directory):
    if service != "grafana":
        raise ValueError("Host HTTPS health probe is restricted to Grafana")
    required = ["SERVICE_CA_FILE"]
    missing = [name for name in required if not os.environ.get(name)]
    if missing:
        return {"service": service, "probe": "https-readiness", "result": "FAIL", "missingConfiguration": missing}
    if not re.fullmatch(r"[0-9]{1,5}", port) or not 1 <= int(port) <= 65535:
        return {"service": service, "probe": "https-readiness", "result": "FAIL", "reason": "invalid-port"}
    path = directory / service
    command = ["curl", "--silent", "--show-error", "--fail", "--connect-timeout", "2", "--max-time", "5",
               "--max-filesize", "65536", "--proto", "=https", "--cacert", os.environ["SERVICE_CA_FILE"]]
    route = "/api/health"
    command += ["--output", str(path), "--write-out", "%{http_code}", f"https://localhost:{port}{route}"]
    completed = subprocess.run(command, capture_output=True, timeout=8, check=False)
    code = completed.stdout.decode("ascii", errors="replace")
    if completed.returncode or code != "200":
        return {"service": service, "probe": "https-readiness", "result": "FAIL", "curlExit": completed.returncode,
                "httpStatus": code if re.fullmatch(r"[0-9]{3}", code) else "unavailable"}
    result = health_observation(service, path.read_bytes(), expected)
    result["httpStatus"] = code
    return result



def observe_https(service, port, expected, directory):
    try:
        return https_probe(service, port, expected, directory)
    except (OSError, ValueError, subprocess.TimeoutExpired):
        return {"service": service, "probe": "https-readiness", "result": "FAIL", "reason": "probe-unavailable"}


def private_https(compose, service):
    if service not in {"core", "catalog", "media"}:
        raise ValueError("Private health owner is unsupported")
    failed = {"service": service, "probe": "https-readiness", "boundary": "private-network-mtls", "result": "FAIL"}
    try:
        completed = subprocess.run([*compose, "exec", "-T", "mcp", "node", "/app/private-readiness.mjs", service],
                                   capture_output=True, timeout=6, check=False)
        if completed.returncode not in {0, 1} or len(completed.stdout) > 4096:
            return dict(failed, reason="private-probe-process-failed")
        value = json.loads(completed.stdout)
        if value.get("service") != service or value.get("boundary") != "private-network-mtls":
            return dict(failed, reason="private-probe-response-invalid")
        result = dict(failed, observed=safe_status(value.get("observed")))
        if value.get("connectionCode") in {"ECONNREFUSED", "ENOTFOUND", "CERT_HAS_EXPIRED", "ERR_TLS_CERT_ALTNAME_INVALID",
                                            "DEPTH_ZERO_SELF_SIGNED_CERT", "UNABLE_TO_VERIFY_LEAF_SIGNATURE", "SELF_SIGNED_CERT_IN_CHAIN",
                                            "ECONNRESET", "ETIMEDOUT", "ERR_SSL_TLSV13_ALERT_CERTIFICATE_REQUIRED", "TLS_CONNECTION_FAILED"}:
            result["connectionCode"] = value["connectionCode"]
        components = value.get("components")
        if isinstance(components, dict):
            result["components"] = {name: safe_status(status) for name, status in components.items()
                                    if name in {"readinessState", "schema", "rateReadiness", "db"}}
        if completed.returncode == 0 and value.get("result") == "PASS" and result["observed"] == "UP":
            result["result"] = "PASS"
        return result
    except (OSError, ValueError, AttributeError, subprocess.TimeoutExpired):
        return dict(failed, reason="private-probe-unavailable")


def observations(root):
    installation = os.environ.get("SPOOLIN_INSTALLATION", "")
    if not re.fullmatch(r"[a-z][a-z0-9-]{2,63}", installation) or os.environ.get("SPOOLIN_SKIP_COMPOSE") == "1":
        return [{"probe": "configuration", "result": "FAIL", "reason": "owned-compose-installation-required"}]
    compose = ["docker", "compose", "--project-name", installation, "-f", str(root / "ops/compose.yaml")]
    command = [*compose, "ps", "--all", "--format", "json", *SERVICES]
    try:
        completed = subprocess.run(command, capture_output=True, timeout=15, check=False)
        if completed.returncode:
            results = [{"probe": "compose", "result": "FAIL", "exitCode": completed.returncode}]
        else:
            results = container_observations(compose_rows(completed.stdout.decode("utf-8")))
    except (OSError, ValueError, subprocess.TimeoutExpired):
        results = [{"probe": "compose", "result": "FAIL", "reason": "unavailable-or-malformed"}]
    with tempfile.TemporaryDirectory(prefix="spoolin-readiness-") as temporary:
        directory = Path(temporary)
        for service in ("core", "catalog", "media"):
            results.append(private_https(compose, service))
        results.append(observe_https("grafana", os.environ.get("GRAFANA_HTTPS_PORT", "3000"), ("database", "ok"), directory))
    return results


def main():
    os.umask(0o077)
    report = {"observedAt": datetime.now(timezone.utc).isoformat(), "externalClients": "UNVERIFIED", "probes": []}
    try:
        root = Path(__file__).resolve().parents[2]
        report["probes"] = observations(root) + operational_status.observations(root)
    except (OSError, ValueError, subprocess.TimeoutExpired):
        report["probes"].append({"probe": "execution", "result": "FAIL", "reason": "probe-unavailable-or-malformed"})
    report["tunnel"] = operational_status.tunnel_observation()
    report["scope"] = "local service and database readiness; tunnel/client acceptance remains separate"
    if os.environ.get("MCP_PUBLICATION_ENABLED", "false").lower() == "true":
        report["probes"].append({"probe": "required-tunnel-runner", "result": "FAIL", "reason": "registered-runner-unavailable"})
    report["result"] = "PASS" if report["probes"] and all(row["result"] == "PASS" for row in report["probes"]) else "FAIL"
    rendered = json.dumps(report, sort_keys=True, indent=2) + "\n"
    output = os.environ.get("SPOOLIN_READINESS_REPORT")
    if output:
        target = Path(output)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(rendered)
    print(rendered, end="")
    return 0 if report["result"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
