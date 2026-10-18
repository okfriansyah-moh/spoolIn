"""Execute only the selected task/level and preserve actual check results."""

import argparse
import json
import re
import os
from pathlib import Path
import subprocess
import sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
MAP = ROOT / "build-tools/verification-map.json"
def report_path(level, tasks):
    name = tasks[0].lower() if tasks and len(tasks) == 1 and re.fullmatch(r"T[0-9]{3}", tasks[0]) else f"{level}-selection"
    return ROOT / f".ai/evidence/numbering-v1/{name}-tests.json"


def selected_entries(mapping, level, tasks):
    names = tasks or list(mapping["tasks"])
    entries = []
    for name in names:
        task = mapping["tasks"].get(name)
        if task is None:
            raise ValueError(f"Unknown task: {name}")
        suite = task.get(level)
        if not suite:
            raise ValueError(f"Unavailable required {level} selection for {name}: {task['pending_reason']}")
        entries.extend(suite)
    if not entries:
        raise ValueError("Empty required selection")
    return entries


def execute(entry, cwd=ROOT):
    if entry.get("kind") == "blocked":
        return 2, {"selected": 1, "executed": 0, "failed": 0, "blocked_reason": entry["reason"]}
    if entry.get("kind") == "native":
        import run_declared_suite
        return run_declared_suite.execute(entry, cwd)
    report = cwd / entry["report"]
    report.unlink(missing_ok=True)
    for pattern in entry.get("required_paths", []):
        if not any(path.is_file() for path in cwd.glob(pattern)):
            print(f"Setup unavailable: required source {pattern}", file=sys.stderr)
            return 2, {"selected": 1, "executed": 0, "failed": 0}
    try:
        result = subprocess.run(entry["argv"], cwd=cwd, timeout=entry["timeout"], check=False)
    except (OSError, subprocess.TimeoutExpired) as error:
        print(f"Setup unavailable: {error}", file=sys.stderr)
        return 2, {"selected": 1, "executed": 0, "failed": 0}
    if not report.is_file():
        return 2, {"selected": 1, "executed": 0, "failed": 0}
    try:
        counts = json.loads(report.read_text())
        if any(type(counts[key]) is not int or counts[key] < 0 for key in ("selected", "executed", "failed")):
            raise ValueError("Invalid report counts")
        if counts["selected"] < 1 or counts["executed"] != counts["selected"]:
            return 2, counts
        if counts["failed"] > counts["executed"]:
            raise ValueError("Invalid failure count")
    except (OSError, ValueError, KeyError, TypeError):
        return 2, {"selected": 1, "executed": 0, "failed": 0}
    if counts["failed"]:
        return 1, counts
    return 0 if result.returncode == 0 else 2 if result.returncode == 2 else 1, counts


def save_report(counts, entries, exit_code, report, level):
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps({
        **counts, "exit_code": exit_code, "entries": entries,
        "finished_at_utc": datetime.now(timezone.utc).isoformat(),
        "level": level, "proof_boundary": "selected task suites at their declared boundaries",
        "sha": os.environ.get("TESTED_SHA", os.environ.get("GITHUB_SHA")),
        "run_id": os.environ.get("GITHUB_RUN_ID"),
        "blocked_obligations": [entry for entry in entries if entry.get("blocked_reason")],
        "counts_scope": "per mapping entry; explicitly reused native cases are not unique-case totals",
    }, indent=2) + "\n")


def native_identity(entry):
    if entry.get("kind") != "native":
        return None
    return json.dumps({key: entry.get(key) for key in
                       ("argv", "native_format", "native_reports", "required_paths", "timeout")}, sort_keys=True)


def run(entries, report, level):
    counts = {"selected": 0, "executed": 0, "failed": 0}
    results = []
    exit_code = 0
    reusable = {}
    for entry in entries:
        key = native_identity(entry)
        if key is not None and key in reusable:
            source, original = reusable[key]
            code, result = 0, {**original, "reused_from": source, "command_executed_for_this_entry": False}
            target = ROOT / entry["report"]
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(result, indent=2) + "\n")
        else:
            code, result = execute(entry)
            if key is not None and code == 0:
                reusable[key] = (entry["id"], result)
        results.append({"id": entry["id"], "argv": entry.get("argv"), "exit_code": code,
                        "proof_boundary": entry.get("proof_boundary"), **result})
        for key in counts:
            counts[key] += result[key]
        if code:
            exit_code = 1 if code == 1 or exit_code == 1 else 2
    save_report(counts, results, exit_code, report, level)
    return exit_code


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("level", choices=["unit", "integration"])
    parser.add_argument("--task", action="append")
    arguments = parser.parse_args()
    report = report_path(arguments.level, arguments.task)
    report.unlink(missing_ok=True)
    try:
        mapping = json.loads(MAP.read_text())
        entries = selected_entries(mapping, arguments.level, arguments.task)
    except (ValueError, OSError) as error:
        print(f"Setup unavailable: {error}", file=sys.stderr)
        save_report({"selected": 1, "executed": 0, "failed": 0},
                    [{"id": "selection-prerequisite", "blocked_reason": str(error)}], 2, report, arguments.level)
        return 2
    return run(entries, report, arguments.level)


if __name__ == "__main__":
    sys.exit(main())
