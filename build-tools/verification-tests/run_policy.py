"""Run T006 isolated policy rules and retain fresh machine-readable reports."""

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / ".ai/evidence/numbering-v1/t006-policy/unit" / ("run-" + uuid.uuid4().hex)


def artifact(path):
    return {"path": str(path.relative_to(ROOT)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}


def command(argv, name):
    started = time.time_ns()
    log = OUTPUT / f"{name}.log"
    with log.open("wb") as stream:
        result = subprocess.run(argv, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, timeout=1400)
    observed = {"argv": argv, "exit_code": result.returncode, "started_ns": started, "log": artifact(log)}
    (OUTPUT / f"{name}-command.json").write_text(json.dumps(observed, indent=2) + "\n")
    return started, observed


def junit(directory, started, destination, pattern="TEST-*.xml"):
    paths = sorted(directory.glob(pattern))
    if not paths or any(path.stat().st_mtime_ns < started for path in paths):
        raise ValueError("Fresh nonempty policy JUnit reports are required")
    destination.mkdir(parents=True, exist_ok=True)
    cases, reports = [], []
    for path in paths:
        target = destination / path.name
        shutil.copyfile(path, target)
        reports.append(artifact(target))
        cases.extend({"class": case.attrib["classname"], "method": case.attrib["name"],
                      "executed": case.find("skipped") is None,
                      "failed": case.find("failure") is not None or case.find("error") is not None}
                     for case in ET.parse(target).iter("testcase"))
    if not cases:
        raise ValueError("Selected policy suite is empty")
    return cases, reports


def backend(module, patterns, name, task_name="test"):
    directory = ROOT / module / "build/test-results" / task_name
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    task = ":" + module.replace("/", ":") + ":" + task_name
    argv = ["./gradlew", task, "--console", "plain"]
    for pattern in patterns:
        argv.extend(["--tests", pattern])
    started, result = command(argv, name)
    cases, reports = junit(directory, started, OUTPUT / "junit" / name)
    return result, cases, reports


def frontend(patterns=None):
    path = OUTPUT / "vitest.json"
    path.unlink(missing_ok=True)
    _, setup = command(["npm", "run", "pretest:coverage", "--workspace", "frontend"], "frontend-setup")
    if setup["exit_code"]:
        raise ValueError("Required policy frontend setup failed")
    argv = ["npm", "exec", "--workspace", "frontend", "--", "vitest", "run", "--config", "vitest.config.ts",
            *(patterns or ["src/lib/policy/policy.test.ts"]), "--reporter=json", f"--outputFile=../{path.relative_to(ROOT)}"]
    started, result = command(argv, "frontend")
    if not path.is_file() or path.stat().st_mtime_ns < started:
        raise ValueError("Fresh policy Vitest report is required")
    document = json.loads(path.read_text())
    cases = [{"class": suite["name"], "method": case["fullName"], "executed": case["status"] != "pending",
              "failed": case["status"] != "passed"} for suite in document["testResults"] for case in suite["assertionResults"]]
    result["setup"] = setup
    return result, cases, [artifact(path)]


def browser():
    started, result = command(["node", "tests/browser/run-account.mjs"], "browser")
    cases, reports = junit(ROOT / "test-results/account", started, OUTPUT / "junit/browser", "junit.xml")
    return result, cases, reports


def main():
    global OUTPUT
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T006", "T010"], required=True)
    parser.add_argument("--level", choices=["unit", "integration"], required=True)
    arguments = parser.parse_args()
    if arguments.task == "T006" and arguments.level != "unit":
        parser.error("T006 integration is assigned to its existing boundary runner")
    OUTPUT = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-checks/{arguments.level}" / ("run-" + uuid.uuid4().hex)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    report = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    report.unlink(missing_ok=True)
    for old in OUTPUT.glob("*-command.json"):
        old.unlink()
    commands, cases, reports, error = [], [], [], None
    code = 2
    try:
        suites = [lambda: backend("libraries/backend-support", ["com.spoolin.support.*"], "shared"),
                  lambda: backend("services/core", ["com.spoolin.core.features.policy.*", "com.spoolin.core.boundary.graphql.*"], "core"),
                  frontend]
        if arguments.task == "T010":
            suites = [lambda: backend("services/core", ["*FrontendPolicyRulesTest"], "core"),
                      lambda: frontend(["src/lib/features/session", "src/lib/transport"])]
            if arguments.level == "integration":
                suites = [lambda: backend("services/core", [], "core", "accountIntegrationTest"), browser]
        for run in suites:
            actual, selected, originals = run()
            commands.append(actual)
            cases.extend(selected)
            reports.extend(originals)
            if actual["exit_code"]:
                break
        code = 1 if any(item["exit_code"] for item in commands) or any(case["failed"] for case in cases) else 0
        if len(commands) != len(suites) or not cases or not all(case["executed"] for case in cases):
            code = 2 if not code else code
    except (OSError, ValueError, ET.ParseError, subprocess.TimeoutExpired) as problem:
        commands = [json.loads(path.read_text()) for path in sorted(OUTPUT.glob("*-command.json"))]
        error = str(problem)
        print(f"Policy check unavailable: {error}", file=sys.stderr)
    report.write_text(json.dumps({"selected": len(cases), "executed": sum(case["executed"] for case in cases),
                                  "failed": sum(case["failed"] for case in cases), "exit_code": code,
                                  "commands": commands, "reports": reports, "cases": cases, "setup_error": error,
                                  "proof_boundary": ("actual Core PostgreSQL18.6, trusted HTTPS and built-UI Firefox" if arguments.level == "integration" else "isolated rules and rendered component units; no live transport verdict"),
                                  "finished_at_utc": datetime.now(timezone.utc).isoformat()}, indent=2) + "\n")
    shutil.copyfile(report, OUTPUT / "results.json")
    return code


if __name__ == "__main__":
    sys.exit(main())
