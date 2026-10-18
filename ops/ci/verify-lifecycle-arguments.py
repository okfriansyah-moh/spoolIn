#!/usr/bin/env python3
"""Remote command-argument regressions and actual catalog lifecycle persistence assertions."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def invalid_arguments():
    # Docker tripwire proves invalid input is refused before an effect; this is not runtime stack proof.
    cases = [("start", ["unsupported"]), ("stop", ["unsupported"]), ("restart", ["unsupported"]), ("migrate", ["core", "extra"]),
             ("logs", ["unknown"]), ("logs", ["core", "catalog"]), ("logs", ["-f", "--follow"]),
             ("logs", ["--tail=0"]), ("logs", ["core; touch rejected-shell-input"]),
             ("test-unit", ["--unsupported"]), ("test-integration", ["--unsupported"])]
    with tempfile.TemporaryDirectory(dir=os.environ["RUNNER_TEMP"], prefix="lifecycle-arguments-") as temporary:
        directory = Path(temporary)
        marker = directory / "docker-invoked"
        docker = directory / "docker"
        docker.write_text('#!/bin/sh\nprintf invoked > "$SPOOLIN_DOCKER_TRIPWIRE"\nexit 99\n')
        docker.chmod(0o700)
        environment = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ["PATH"],
                           SPOOLIN_DOCKER_TRIPWIRE=str(marker))
        for script, arguments in cases:
            result = subprocess.run(["bash", str(ROOT / "ops/bin" / (script + ".sh")), *arguments],
                                    env=environment, capture_output=True, timeout=15, check=False)
            require(result.returncode == 2, f"{script} invalid arguments must exit2")
            require(not marker.exists(), "Invalid arguments attempted Docker effects")
        for value in ("core catalog", "core; touch rejected-shell-input", "--tail=0"):
            result = subprocess.run(["make", "logs", "SERVICE=" + value], cwd=ROOT, env=environment,
                                    capture_output=True, timeout=15, check=False)
            require(result.returncode == 2, "Make logs must reject the exact SERVICE value without shell evaluation")
            require(not (ROOT / "rejected-shell-input").exists() and not marker.exists(), "SERVICE was evaluated as shell code")
    print(json.dumps({"boundary": "argument validation with explicit Docker tripwire", "cases": len(cases) + 3,
                      "result": "PASS", "runtimeAcceptance": "UNVERIFIED"}))


def catalog_snapshot(path, compare):
    spec = importlib.util.spec_from_file_location("seed_runtime", ROOT / "ops/ci/verify-seed-runtime.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    serialized = json.dumps(module.snapshot(), sort_keys=True, separators=(",", ":")).encode()
    digest = hashlib.sha256(serialized).hexdigest()
    if compare:
        require(json.loads(path.read_text())["catalogSHA256"] == digest,
                "Actual catalog/people/credits/genre/provenance/seed receipt changed across lifecycle")
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({"catalogSHA256": digest, "sha": os.environ["TESTED_SHA"],
                                    "runId": os.environ["GITHUB_RUN_ID"]}) + "\n")
    print(json.dumps({"result": "PASS", "boundary": "actual read-only Catalog backup-role snapshot",
                      "catalogSHA256": digest, "comparison": compare, "mediaAndKeyPersistence": "separate-required-proof"}))


def application_logs(path):
    lines = path.read_text().splitlines()
    require(bool(lines), "Core log selection is empty")
    for line in lines:
        require(len(line.encode()) <= 16384, "Core log physical line exceeds the approved bound")
        record = json.loads(line)
        require(isinstance(record, dict) and record.get("resource", {}).get("service.name") == "core",
                "Actual Core viewing path must preserve complete unprefixed JSON records")
    print(json.dumps({"result": "PASS", "boundary": "actual make logs SERVICE=core viewing path", "records": len(lines)}))


def main():
    require(os.environ.get("GITHUB_ACTIONS") == "true" and os.environ.get("RUNNER_ENVIRONMENT") == "github-hosted",
            "Requires a verified GitHub-hosted runner; local execution refused")
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog-snapshot", type=Path)
    parser.add_argument("--compare", action="store_true")
    parser.add_argument("--application-logs", type=Path)
    arguments = parser.parse_args()
    if arguments.catalog_snapshot:
        catalog_snapshot(arguments.catalog_snapshot, arguments.compare)
    elif arguments.application_logs:
        application_logs(arguments.application_logs)
    else:
        require(not arguments.compare, "Compare requires catalog snapshot")
        invalid_arguments()


if __name__ == "__main__":
    main()
