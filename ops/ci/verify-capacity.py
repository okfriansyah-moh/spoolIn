#!/usr/bin/env python3
"""Run one independent remote F-BENCHMARK repetition; missing evidence never becomes a passing budget."""
import argparse
from collections import Counter
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import subprocess
import tempfile

from capacity_http import ApplicationClient
from capacity_plans import plans
from capacity_runtime import CapacityStack, ROOT, checked, host_record, require_hosted, seed_baseline
from capacity_workload import MetadataWriter, SampleSink, assert_scope, nearest_rank, read_cases, request_contract, run_readers

OPERATIONS = {"reader": {"titles", "title"}, "writer": {"updateMovie", "updatePerson", "updateCredit"}}
OUTCOMES = {"SUCCESS", "DOMAIN_REJECTED", "SYSTEM_FAILED", "UNCERTAIN"}
CODES = {"OK", "FIELD_INVALID", "INVALID_ARGUMENT", "UNAUTHENTICATED", "PERMISSION_DENIED", "NOT_FOUND", "CONFLICT",
         "RATE_LIMITED", "RESOURCE_EXHAUSTED", "UNAVAILABLE", "DEADLINE_EXCEEDED", "CANCELLED", "INTERNAL", "UNKNOWN",
         "PENDING", "RESULT_EXPIRED", "FAILED_PRECONDITION", "DATA_LOSS", "UNIMPLEMENTED"}


def instant(value):
    return datetime.fromisoformat(value.replace("Z", "+00:00")).astimezone(timezone.utc)


def core_samples(stack, phase, window):
    checked([*stack.compose, "stop", "--timeout", "65", "core"], stack.environment, timeout=90)
    container = checked([*stack.compose, "ps", "--all", "--quiet", "core"], stack.environment).strip()
    if not container or "\n" in container:
        raise RuntimeError("Exact stopped Core container required for timing evidence")
    with tempfile.TemporaryDirectory(prefix="capacity-jfr-", dir=os.environ["RUNNER_TEMP"]) as private:
        binary, parsed = Path(private) / "core.jfr", Path(private) / "events.json"
        checked(["docker", "cp", container + ":/var/spoolin/timing/core-timing.jfr", str(binary)], timeout=90)
        if binary.stat().st_size > 536870912:
            raise RuntimeError("Core JFR exceeded bounded evidence capacity")
        tool = str(Path(os.environ["JAVA_HOME"]) / "bin/jfr")
        with parsed.open("x") as output:
            result = subprocess.run([tool, "print", "--json", "--events", "spoolin.CoreRequestTiming", str(binary)],
                                    stdout=output, stderr=subprocess.DEVNULL, timeout=120, check=False)
        if result.returncode or parsed.stat().st_size > 536870912:
            raise RuntimeError("Pinned JDK could not decode bounded custom Core events")
        events = json.loads(parsed.read_text()).get("recording", {}).get("events", [])
    samples = select_events(events, phase, window)
    with (stack.output / "core-samples.jsonl").open("x") as output:
        for value in samples:
            output.write(json.dumps(value, separators=(",", ":")) + "\n")
    return samples


def select_events(events, phase, window):
    selected = []
    for event in events:
        if event.get("type") != "spoolin.CoreRequestTiming":
            raise RuntimeError("Unexpected event in private custom-only timing projection")
        value = event.get("values") or {}
        started = instant(value["startTime"])
        if not instant(window["started"]) <= started <= instant(window["drained"]):
            continue
        duration = value.get("durationNanos")
        if (value.get("operation") not in OPERATIONS[phase] or value.get("outcome") not in OUTCOMES
                or value.get("code") not in CODES or not isinstance(duration, int) or isinstance(duration, bool) or duration < 0):
            raise RuntimeError("Core measurement fields violate the fixed safe event contract")
        selected.append({name: value[name] for name in ("operation", "outcome", "code", "durationNanos", "startTime")})
    return selected


def budget(samples, counts, phase, window):
    if not samples:
        raise RuntimeError("No actual Core-boundary timing samples selected")
    values = [sample["durationNanos"] for sample in samples]
    successful = [sample["durationNanos"] for sample in samples if sample["outcome"] == "SUCCESS"]
    errors = sum(counts.get(key, 0) for key in ("transportErrors", "throttled", "domainErrors", "httpErrors", "assertionErrors"))
    complete = len(samples) == counts.get("completed", 0) and counts.get("offered", 0) > 0
    limit = (300 if phase == "reader" else 500) * 1000000
    p95 = nearest_rank(values)
    per_class = [count for name, count in counts.items() if name.startswith("class:")]
    balanced = len(per_class) == (5 if phase == "reader" else 3) and max(per_class) - min(per_class) <= (10 if phase == "reader" else 1)
    passed = complete and not errors and balanced and not window.get("aborted") and p95 < limit
    return {"result": "PASS" if passed else "FAIL", "boundary": "actual Core GraphQL completion/System.nanoTime",
            "counts": counts, "coreSamples": len(samples), "coreOutcomeCounts": dict(Counter(row["outcome"] for row in samples)),
            "coreCodeCounts": dict(Counter(row["code"] for row in samples)), "allCompletedP95Nanos": p95,
            "successfulP95Nanos": nearest_rank(successful) if successful else None,
            "comparison": "strictly less than", "budgetNanos": limit, "completeSampleAccounting": complete,
            "balancedClosedLoopClasses": balanced, "window": window, "pooled": False,
            "admittedHttp200": counts.get("http200", 0),
            "admissionDefinition": "HTTP200 completed through normal limiter; throttled and transport failures counted separately"}


def workload(stack, manifest, phase):
    cases = read_cases(manifest)
    stack.write("fixed-requests.json", request_contract(cases))
    client = ApplicationClient(stack.environment)
    try:
        cold = []
        for case in cases:
            response = client.request(case["query"], case["variables"])
            valid = assert_scope(response, case)
            cold.append({"class": case["name"], "scopeAssertion": valid, **response.safe_result()})
            if not valid:
                raise RuntimeError("Fixed read oracle differs from the approved seeded scope")
        stack.write("cold-requests.json", cold)
    finally:
        client.close()
    writer = MetadataWriter(stack.environment, manifest) if phase == "writer" else None
    runner = writer.run if writer else lambda seconds, sink: run_readers(stack.environment, cases, seconds, sink)
    warm = SampleSink(stack.output / "warmup-client-samples.jsonl")
    measured = SampleSink(stack.output / "measured-client-samples.jsonl")
    try:
        warm_window = runner(60, warm)
        stack.write("warmup.json", {"window": warm_window, "counts": dict(warm.counts), "excludedFromP95": True})
        warm_errors = sum(warm.counts.get(key, 0) for key in ("transportErrors", "throttled", "domainErrors", "httpErrors", "assertionErrors"))
        if warm_window.get("aborted") or warm_errors:
            raise RuntimeError("Warmup mutation was not confirmed; retain original intent and stop")
        stack.resources("before-measured")
        window = runner(300, measured)
        stack.resources("after-measured")
        counts = dict(measured.counts)
        if writer and not window.get("aborted"):
            writer.verify_persisted(stack)
        admission = json.loads(stack.database("core", "SELECT json_build_object('windows',count(*),'observedHits',COALESCE(sum(hits),0)) FROM request_rate_window;"))
        if admission["windows"] <= 0 or admission["observedHits"] <= 0:
            raise RuntimeError("Actual durable admission counters did not record workload")
        stack.write("durable-admission.json", admission)
    finally:
        warm.close()
        measured.close()
        if writer:
            writer.client.close()
    stack.write("window.json", window)
    stack.write("client-counts.json", counts)
    samples = core_samples(stack, phase, window)
    return budget(samples, counts, phase, window)


def arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("--phase", required=True, choices=("startup", "reader", "writer", "plans"))
    parser.add_argument("--repetition", required=True, type=int, choices=(1, 2, 3))
    value = parser.parse_args()
    if value.phase == "plans" and value.repetition != 1:
        parser.error("plans requires repetition1")
    return value


def measure_phase(stack, selected, report):
    startup = stack.start()
    manifest = seed_baseline(stack)
    stack.resources("ready")
    if selected.phase == "startup":
        warm = startup["imagesBefore"] == startup["imagesAfter"]
        report["measurement"] = {"warmImagesUnchanged": warm, "budgetNanos": 120000000000,
                                 "elapsedNanos": startup["elapsedNanos"], "readiness": "actual make start then make status exit0"}
        report["result"] = "PASS" if warm and startup["elapsedNanos"] < 120000000000 else "FAIL"
    elif selected.phase in OPERATIONS:
        report["measurement"] = workload(stack, manifest, selected.phase)
        report["result"] = report["measurement"]["result"]
    else:
        report["measurement"] = plans(stack, manifest)
        report["result"] = "PENDING"


def main():
    require_hosted()
    selected = arguments()
    output = ROOT / "artifacts/capacity" / f"{selected.phase}-{selected.repetition}"
    output.mkdir(parents=True, exist_ok=False)
    report = {"case": "ATDD-211", "task": "T045", "result": "FAIL", "host": host_record(),
              "phase": selected.phase, "repetition": selected.repetition, "independentReset": "fresh namespace and keys",
              "resultScope": "selected phase only; full ATDD211 and T045 acceptance remain unverified",
              "unverified": ["accepted static JPEG/PNG/WebP10s processing and near-resource-boundary decoder measurements",
                             "overload/isolation during Media fault, bounded shutdown, forced-crash persisted recovery",
                             "full protected backup/restore RPO/RTO and external provider evidence",
                             "both arm64/amd64 resource observations and all consequential SQL/N+1 proofs"]}
    stack = CapacityStack(selected.phase, selected.repetition, output)
    try:
        stack.prepare()
        measure_phase(stack, selected, report)
    except Exception as error:
        report["failureType"] = type(error).__name__
        report["failure"] = "Actual command, boundary, oracle or evidence failed; raw exception text withheld"
    finally:
        if report["result"] == "FAIL":
            try:
                stack.failure_evidence()
            except Exception:
                report["failureDiagnostics"] = "unavailable; no raw messages published"
        try:
            stack.cleanup()
            report["cleanup"] = ("exact owned capacity namespace and volumes removed" if stack.environment else "no capacity stack started")
        except Exception:
            report["cleanup"] = "FAIL: owned resources retained; operator inspection required"
            report["result"] = "FAIL"
        stack.write("report.json", report)
    print(json.dumps({"case": "ATDD-211", "phase": selected.phase, "repetition": selected.repetition,
                      "result": report["result"], "report": str(output / "report.json")}))
    return 0 if report["result"] == "PASS" else 78 if report["result"] == "PENDING" else 1


if __name__ == "__main__":
    raise SystemExit(main())
