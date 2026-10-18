"""Run a declared native suite and retain fresh actual case evidence at its stated boundary."""
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import uuid
import xml.etree.ElementTree as ET


def parse_cases(paths, format_name):
    cases = []
    for path in paths:
        if format_name == "junit":
            cases.extend({"class": case.get("classname"), "method": case.get("name"),
                          "executed": case.find("skipped") is None,
                          "failed": case.find("failure") is not None or case.find("error") is not None}
                         for case in ET.parse(path).iter("testcase"))
        elif format_name == "vitest":
            document = json.loads(path.read_text())
            cases.extend({"class": suite["name"], "method": case["fullName"],
                          "executed": case["status"] not in {"pending", "skipped", "todo"},
                          "failed": case["status"] not in {"passed", "pending", "skipped", "todo"}}
                         for suite in document["testResults"] for case in suite["assertionResults"])
        else:
            raise ValueError("Unsupported native report format")
    return cases


def execute(entry, root):
    for pattern in entry.get("required_paths", []):
        if not any(path.is_file() for path in root.glob(pattern)):
            return 2, {"selected": 1, "executed": 0, "failed": 0, "reason": "required-suite-source-unavailable"}
    report = root / entry["report"]
    report.unlink(missing_ok=True)
    retained = report.parent / (entry["id"] + "-" + uuid.uuid4().hex)
    retained.mkdir(parents=True)
    for pattern in entry["native_reports"]:
        for path in root.glob(pattern):
            path.unlink()
    started = time.time_ns()
    code, error, cases, artifacts, command_exit = 2, None, [], [], None
    try:
        with (retained / "command.log").open("wb") as log:
            completed = subprocess.run(entry["argv"], cwd=root, stdout=log, stderr=subprocess.STDOUT,
                                       timeout=entry["timeout"], check=False)
        command_exit = completed.returncode
        paths = sorted({path for pattern in entry["native_reports"] for path in root.glob(pattern)})
        if not paths or any(path.stat().st_mtime_ns < started for path in paths):
            raise ValueError("Fresh nonempty native reports are required")
        cases = parse_cases(paths, entry["native_format"])
        for index, path in enumerate(paths):
            target = retained / f"{index}-{path.name}"
            shutil.copyfile(path, target)
            artifacts.append({"path": str(target.relative_to(root)), "sha256": hashlib.sha256(target.read_bytes()).hexdigest()})
        code = 2 if completed.returncode == 2 else 1 if completed.returncode else 0
        if not cases or any(not case["executed"] for case in cases):
            code = 2
        elif any(case["failed"] for case in cases):
            code = 1
    except (OSError, ValueError, TypeError, AttributeError, KeyError, ET.ParseError, subprocess.TimeoutExpired) as failure:
        error = type(failure).__name__
    counts = {"selected": len(cases) or 1, "executed": sum(case["executed"] for case in cases),
              "failed": sum(case["failed"] for case in cases), "cases": cases, "artifacts": artifacts,
              "exit_code": code, "command_exit_code": command_exit, "setup_error": error, "proof_boundary": entry["proof_boundary"],
              "argv": entry["argv"], "sha": os.environ.get("TESTED_SHA", os.environ.get("GITHUB_SHA")),
              "run_id": os.environ.get("GITHUB_RUN_ID"), "finished_at_utc": datetime.now(timezone.utc).isoformat()}
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(counts, indent=2) + "\n")
    return code, counts
