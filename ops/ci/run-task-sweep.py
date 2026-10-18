"""Run the G04–G16 selected suites serially, or retain an explicit unavailable result."""

from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
TASKS = tuple(f"T{number:03}" for number in range(6, 27))
REPORT = ROOT / ".ai/evidence/numbering-v1/g04-g16-task-sweep.json"


def hosted(environment):
    return (environment.get("GITHUB_ACTIONS") == "true"
            and environment.get("RUNNER_ENVIRONMENT") == "github-hosted"
            and environment.get("RUNNER_OS") == "Linux")


def selections(mapping):
    selected, unavailable = [], []
    for task in TASKS:
        record = mapping["tasks"].get(task, {})
        for level in ("unit", "integration"):
            entries = record.get(level)
            if not entries:
                unavailable.append({"task": task, "level": level,
                                    "reason": record.get("pending_reason") or "Required suite mapping is absent"})
            else:
                selected.extend({"task": task, "level": level, "entry": entry} for entry in entries)
    return selected, unavailable


def selector():
    path = ROOT / "build-tools/verification-tests/task_selector.py"
    spec = importlib.util.spec_from_file_location("task_selector", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def write_report(selected, unavailable, results, code):
    revision = subprocess.run(["git", "rev-parse", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=True)
    mapping = ROOT / "build-tools/verification-map.json"
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "tasks": TASKS, "selected_suites": len(selected), "completed_suites": len(results),
        "selected": sum(item["selected"] for item in results),
        "executed": sum(item["executed"] for item in results),
        "failed": sum(item["failed"] for item in results),
        "unavailable": unavailable, "results": results, "exit_code": code,
        "assessed_sha": revision.stdout.strip(), "run_id": os.environ.get("GITHUB_RUN_ID"),
        "run_attempt": os.environ.get("GITHUB_RUN_ATTEMPT"), "runner_os": os.environ.get("RUNNER_OS"),
        "runner_environment": os.environ.get("RUNNER_ENVIRONMENT"),
        "image_version": os.environ.get("ImageVersion"),
        "mapping_sha256": hashlib.sha256(mapping.read_bytes()).hexdigest() if mapping.is_file() else None,
        "selections": [{"task": item["task"], "level": item["level"],
                        "id": item["entry"]["id"], "report": item["entry"]["report"]} for item in selected],
        "finished_at_utc": datetime.now(timezone.utc).isoformat(),
    }
    content = json.dumps(payload, indent=2) + "\n"
    REPORT.write_text(content)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    history = REPORT.with_name(f"g04-g16-task-sweep-{stamp}-{revision.stdout.strip()[:12]}.json")
    history.write_text(content)


def report_hash(reference):
    path = ROOT / reference
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None


def main(arguments):
    try:
        selected, unavailable = selections(json.loads((ROOT / "build-tools/verification-map.json").read_text()))
    except (OSError, ValueError, KeyError, TypeError):
        selected, unavailable = [], [{"reason": "Required suite mapping is unavailable or invalid"}]
    if arguments:
        unavailable.append({"reason": "This fixed G04–G16 selection takes no arguments"})
    if not hosted(os.environ):
        unavailable.append({"reason": "GitHub-hosted Linux runner is required; no local heavy checks executed"})
    results, code = [], 2 if unavailable or not selected else 0
    if code == 0:
        runner = selector()
        for item in selected:
            result_code, counts = runner.execute(item["entry"])
            results.append({"task": item["task"], "level": item["level"], "id": item["entry"]["id"],
                            "exit_code": result_code, "report": item["entry"]["report"],
                            "report_sha256": report_hash(item["entry"]["report"]), **counts})
            if result_code:
                code = result_code
                break
    write_report(selected, unavailable, results, code)
    if code:
        print("G04–G16 sweep incomplete; inspect the requirement and execution report", file=sys.stderr)
    return code


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
