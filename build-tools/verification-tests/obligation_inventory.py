#!/usr/bin/env python3
"""Inventory approved obligations without turning source or test declarations into acceptance."""

import argparse
import collections
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
PLAN = "docs/06-PLAN.md"
ATDD = "docs/05-atdd.md"
ASSESSMENT = "docs/release/remote-acceptance-assessment.json"
VERIFY_MAP = "build-tools/verification-map.json"
OUTPUT = "docs/release/remote-obligation-results.json"
DISPOSITIONS = {"Covered", "Deferred", "Excluded", "Gap"}
CASE_ID = re.compile(r"ATDD-\d{3}")
PENDING = "No exact obligation evidence inspected at this SHA; source and runner references do not prove acceptance."


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read_lines(path):
    return (ROOT / path).read_text().splitlines()


def cells(line):
    if not line.startswith("|"):
        return []
    return [value.strip() for value in re.split(r"(?<!\\)\|", line)[1:-1]]


def section(lines, heading):
    start = lines.index(heading) + 1
    end = next((i for i in range(start, len(lines)) if lines[i].startswith("## ")), len(lines))
    return list(enumerate(lines[start:end], start + 1))


def register(lines):
    rows = []
    for number, line in section(lines, "## Obligation coverage"):
        values = cells(line)
        if len(values) == 7 and values[3] in DISPOSITIONS:
            rows.append({"id": values[0], "source_type": values[1], "locator": values[2],
                         "disposition": values[3], "implementation_owners": re.findall(r"T\d{3}", values[4]),
                         "verification_owners": re.findall(r"T\d{3}", values[5]),
                         "scope_reason": values[6], "plan_line": number})
    return rows


def source_locator(row):
    match = re.fullmatch(r"\[.* · L(\d+)\]\(([^)#]+)(?:#[^)]*)?\)", row["locator"])
    if not match:
        return {"status": "Pending", "reason": "Unparsed approved locator", "locator": row["locator"]}
    path = "docs/" + match[2]
    number = int(match[1])
    lines = read_lines(path)
    if not 1 <= number <= len(lines):
        return {"status": "Pending", "reason": "Approved line locator is outside current source", "path": path}
    return {"path": path, "line": number, "line_sha256": digest(lines[number - 1].encode()),
            "link": f"{path}#L{number}", "status": "located"}


def field_ranges(lines, start, end):
    found = [(i, re.match(r"\*\*([^*]+):\*\*\s*(.*)", lines[i])) for i in range(start, end)]
    found = [(i, match) for i, match in found if match]
    result = {}
    for index, (number, match) in enumerate(found):
        last = found[index + 1][0] if index + 1 < len(found) else end
        text = "\n".join([match[2]] + lines[number + 1:last]).strip().removesuffix("---").strip()
        result[match[1]] = {"start_line": number + 1, "end_line": last,
                            "sha256": digest(text.encode()), "text": text}
    return result


def field_ref(fields, name):
    value = fields.get(name)
    if not value:
        return {"status": "Pending", "reason": f"Source has no {name} field"}
    return {"path": ATDD, "field": name, "start_line": value["start_line"],
            "end_line": value["end_line"], "sha256": value["sha256"],
            "link": f"{ATDD}#L{value['start_line']}"}


def atdd_cases(lines):
    starts = [(i, re.match(r"### Case (ATDD-\d{3}): (.*)", line)) for i, line in enumerate(lines)]
    starts = [(i, match) for i, match in starts if match]
    result = {}
    duplicates = []
    for index, (start, match) in enumerate(starts):
        end = starts[index + 1][0] if index + 1 < len(starts) else len(lines)
        next_section = next((i for i in range(start + 1, end) if lines[i].startswith("## ")), end)
        fields = field_ranges(lines, start + 1, next_section)
        record = {"id": match[1], "title": match[2], "source_line": start + 1,
                  "level": fields.get("Level", {}).get("text", "Unresolved"),
                  "platform": fields.get("Platform", {}).get("text", "Unresolved"),
                  "source_ids": [s.strip() for s in fields.get("Sources", {}).get("text", "").split(",")],
                  "oracle_refs": [field_ref(fields, name) for name in ("Scenario", "Verification")],
                  "setup_refs": [field_ref(fields, name) for name in ("Preconditions", "Test data", "Cleanup")],
                  "journey_ref": field_ref(fields, "Expected UI"),
                  "subrows": table_rows(lines, start + 1, next_section, match[1]),
                  "result": "Pending", "pending_reason": PENDING}
        if match[1] in result:
            duplicates.append(match[1])
        result[match[1]] = record
    return result, duplicates


def table_rows(lines, start, end, case):
    result, table, row = [], 0, 0
    for number in range(start, end):
        values = cells(lines[number])
        separator = values and all(re.fullmatch(r":?-+:?", value.replace(" ", "")) for value in values)
        if separator:
            table += 1
            row = 0
        elif table and values and number > start and cells(lines[number - 1]):
            row += 1
            result.append({"id": f"{case}-T{table}-R{row:02}", "line": number + 1,
                           "row_sha256": digest(lines[number].encode()), "table": table, "row": row})
    return result


def tracked_paths():
    output = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT)
    return output.decode().rstrip("\0").split("\0")


def task_sources(path, hints, tracked):
    lines = read_lines(path)
    surfaces = [values[2].strip("`") for _, line in section(lines, "## Change surface")
                if len(values := cells(line)) == 4 and values[0] == "app"]
    declared = sorted(set(surfaces + hints))
    actual = sorted({name for surface in declared for name in tracked
                     if name == surface.rstrip("/") or name.startswith(surface.rstrip("/") + "/")})
    return declared, actual


def tasks(assessment, tracked, verification):
    result = {}
    for task in assessment["tasks"]:
        path = task["task_path"]
        declared, actual = task_sources(path, task["production_paths"], tracked)
        lines = read_lines(path)
        assigned = [{"id": cells(line)[0], "role": cells(line)[1], "line": number}
                    for number, line in section(lines, "## Traceability")
                    if len(cells(line)) == 2 and cells(line)[1] in {"implements", "verifies", "both"}]
        acceptance = [{"id": behavior["id"], "task_path": path, "result": "Pending", "pending_reason": PENDING}
                      for behavior in task["required_behavior"]]
        result[task["task_id"]] = {"group_id": task["group_id"], "task_path": path,
                                   "acceptance_obligations": acceptance,
                                   "declared_source_surfaces": declared, "actual_source_paths": actual,
                                   "source_mapping": "Task-owned candidates; exact obligation-to-symbol wiring remains Pending.",
                                   "assignments": assigned, "remote_runner_candidates": runner_candidates(
                                       verification.get(task["task_id"], {})),
                                   "result": "Pending", "pending_reason": PENDING}
    return result


def runner_candidates(task):
    return [{"level": level, "id": command["id"], "argv": command.get("argv"),
             "declared_kind": command.get("kind", "executable"), "pending_reason": command.get("reason"),
             "report": command.get("report"), "selection_status": "Pending exact obligation-to-assertion mapping"}
            for level in ("unit", "integration") for command in task.get(level, [])]


def case_matches(row, cases):
    parent = CASE_ID.match(row["id"])
    if parent and parent[0] in cases:
        return [parent[0]]
    return [name for name, case in cases.items() if row["id"] in case["source_ids"]]


def pending_reasons(row, linked, actual, task_map):
    reasons = [PENDING]
    if row["disposition"] != "Covered":
        reasons.append("Approved scope disposition retained: " + row["scope_reason"])
    if not actual:
        reasons.append("No actual file resolved from the approved task surfaces; exact source mapping is Pending.")
    if not linked:
        reasons.append("No exact ATDD Sources association; read the source clause and owned task oracle before selection.")
    unknown = sorted(set(row["implementation_owners"] + row["verification_owners"]) - set(task_map))
    if unknown:
        reasons.append("Unknown task owners: " + ", ".join(unknown))
    return reasons


def result_row(row, cases, task_map, sha):
    linked = case_matches(row, cases)
    owners = row["implementation_owners"]
    actual = sorted({path for owner in owners for path in task_map.get(owner, {}).get("actual_source_paths", [])})
    boundaries = [{"case_id": case, "level": cases[case]["level"], "platform": cases[case]["platform"],
                   "oracle_ref": f"#/cases/{case}/oracle_refs"} for case in linked]
    return {**row, "source_ref": source_locator(row), "case_ids": linked,
            "actual_source_path_refs": [{"task_id": owner, "paths_ref": f"#/tasks/{owner}/actual_source_paths"}
                                        for owner in owners],
            "source_mapping": "Task-owned candidate files, not certified implementation of this individual obligation.",
            "required_proof_boundaries": boundaries or [{"level": "Unresolved", "platform": "Unresolved",
                                                         "oracle": "Read exact source_ref and task acceptance; mapping Pending."}],
            "remote_execution_plan": {"workflow": ".github/workflows/remote-acceptance.yml",
                                      "runner_requirement": "Verified hosted remote runner; no MacBook or unknown self-hosted runner.",
                                      "task_runner_refs": [f"#/tasks/{owner}/remote_runner_candidates"
                                                           for owner in row["verification_owners"]],
                                      "journey_refs": [f"#/cases/{case}/journey_ref" for case in linked],
                                      "selection_status": "Pending exact case assertions and required real platform availability"},
            "result": "Pending", "pending_reasons": pending_reasons(row, linked, actual, task_map),
            "source_complete": "Unestablished", "runtime_verified": False, "formally_accepted": False,
            "assessed_sha": sha, "evidence_links": [], "run_metadata": None}


def duplicate_ids(rows):
    return sorted(name for name, count in collections.Counter(row["id"] for row in rows).items() if count > 1)


def assignment_diagnostics(rows, task_map):
    duplicates = {name: duplicate_ids(task["assignments"]) for name, task in task_map.items()}
    mismatches = []
    for row in rows:
        for owners, roles in (("implementation_owners", {"implements", "both"}),
                              ("verification_owners", {"verifies", "both"})):
            declared = {name for name, task in task_map.items() if any(
                assignment["id"] == row["id"] and assignment["role"] in roles
                for assignment in task["assignments"])}
            if declared != set(row[owners]):
                mismatches.append({"id": row["id"], "role": owners, "root": row[owners],
                                   "task_traceability": sorted(declared)})
    return {"task_duplicate_assignment_ids": {name: ids for name, ids in duplicates.items() if ids},
            "reciprocal_assignment_mismatches": mismatches}


def reconciliation(rows, cases, case_duplicates, task_map):
    root_ids = {row["id"] for row in rows}
    assigned = {assignment["id"] for task in task_map.values() for assignment in task["assignments"]}
    subrows = {row["id"] for case in cases.values() for row in case["subrows"]}
    root_subrows = {row["id"] for row in rows if re.fullmatch(r"ATDD-\d{3}-T\d+-R\d+", row["id"])}
    unassigned = [row for row in rows if row["id"] not in assigned]
    shared = [row["id"] for row in rows if row["source_type"] == "ATDD"
              and row["id"] not in cases and row["id"] not in root_subrows]
    diagnostics = assignment_diagnostics(rows, task_map)
    return {**diagnostics, "root_register_count": len(rows), "root_unique_ids": len(root_ids),
            "task_assigned_unique_ids": len(assigned), "disposition_counts": dict(collections.Counter(
                row["disposition"] for row in rows)), "root_duplicate_ids": duplicate_ids(rows),
            "case_duplicate_ids": case_duplicates, "atdd_root_cases": len(cases),
            "atdd_explicit_table_rows": len(subrows), "shared_atdd_fixture_recipe_ids": shared,
            "task_ids": sorted(task_map),
            "root_unassigned_rows": [{"id": row["id"], "disposition": row["disposition"],
                                      "reason": row["scope_reason"]} for row in unassigned],
            "task_assignments_absent_from_root": sorted(assigned - root_ids),
            "cases_absent_from_root": sorted(set(cases) - root_ids),
            "root_cases_absent_from_source": sorted({name for name in root_ids if CASE_ID.fullmatch(name)} - set(cases)),
            "table_rows_absent_from_root": sorted(subrows - root_subrows),
            "root_table_rows_absent_from_source": sorted(root_subrows - subrows),
            "count_difference_explanation": f"{len(rows)} register rows minus {len(assigned)} task-assigned unique IDs "
                                            f"leaves {len(unassigned)} explicit unassigned rows. Approved dispositions "
                                            "and reasons are retained; no row is discarded or accepted."}


def build():
    rows = register(read_lines(PLAN))
    cases, case_duplicates = atdd_cases(read_lines(ATDD))
    assessment = json.loads((ROOT / ASSESSMENT).read_text())
    verification = json.loads((ROOT / VERIFY_MAP).read_text())["tasks"]
    task_map = tasks(assessment, tracked_paths(), verification)
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    inputs = {PLAN, ATDD, ASSESSMENT, VERIFY_MAP} | {task["task_path"] for task in task_map.values()}
    inputs |= {location["path"] for row in rows if (location := source_locator(row)).get("status") == "located"}
    add_assessment_metadata(cases, task_map, sha)
    candidate_paths = sorted({path for task in task_map.values() for path in task["actual_source_paths"]})
    return {"schema_version": 1, "assessed_sha": sha,
            "scope": "Exhaustive declared-obligation inventory. No source completeness, runtime or formal acceptance inferred.",
            "input_sha256": {name: digest((ROOT / name).read_bytes()) for name in sorted(inputs)},
            "candidate_source_sha256": {name: digest((ROOT / name).read_bytes()) for name in candidate_paths},
            "reconciliation": reconciliation(rows, cases, case_duplicates, task_map),
            "totals": {"obligation_rows": len(rows), "pending": len(rows), "runtime_verified": 0,
                       "formally_accepted": 0, "source_complete_established": 0},
            "run_metadata_contract": ["tested_sha", "workflow_id", "run_id", "job_id", "runner_os",
                                      "runner_image", "browser_version", "seed_manifest_sha256", "configuration_digest",
                                      "assertion_ids", "artifact_links", "retention", "inspected_by"],
            "tasks": task_map, "cases": cases,
            "obligations": [result_row(row, cases, task_map, sha) for row in rows]}


def add_assessment_metadata(cases, task_map, sha):
    for name, case in cases.items():
        case.update({"assessed_sha": sha, "evidence_links": [], "runtime_verified": False,
                     "formally_accepted": False, "root_obligation_ref": f"#/obligations/id={name}"})
        for subrow in case["subrows"]:
            subrow.update({"assessed_sha": sha, "result": "Pending", "evidence_links": [],
                           "root_obligation_ref": f"#/obligations/id={subrow['id']}"})
    for task in task_map.values():
        task["assessed_sha"] = sha
        task["evidence_links"] = []
        for acceptance in task["acceptance_obligations"]:
            acceptance["assessed_sha"] = sha
            acceptance["evidence_links"] = []


def check(path):
    data = json.loads(path.read_text())
    current = register(read_lines(PLAN))
    if [row["id"] for row in data["obligations"]] != [row["id"] for row in current]:
        raise ValueError("Obligation identity/order differs from approved root register")
    for name, expected in (data["input_sha256"] | data["candidate_source_sha256"]).items():
        if digest((ROOT / name).read_bytes()) != expected:
            raise ValueError(f"Inventory input changed: {name}")
    if (data["reconciliation"]["root_duplicate_ids"] or data["reconciliation"]["case_duplicate_ids"]
            or data["reconciliation"]["task_duplicate_assignment_ids"]):
        raise ValueError("Duplicate IDs require explicit reconciliation")
    for key in ("task_assignments_absent_from_root", "cases_absent_from_root", "root_cases_absent_from_source",
                "table_rows_absent_from_root", "root_table_rows_absent_from_source", "reciprocal_assignment_mismatches"):
        if data["reconciliation"][key]:
            raise ValueError(f"Unreconciled inventory association: {key}")
    print("Inventory identity and source inputs match; this does not verify application acceptance.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check retained identities and input hashes only")
    parser.add_argument("--output", default=OUTPUT)
    args = parser.parse_args()
    output = ROOT / args.output
    if args.check:
        check(output)
    else:
        data = build()
        with output.open("x") as stream:
            json.dump(data, stream, ensure_ascii=False, indent=2)
            stream.write("\n")
        print(json.dumps(data["totals"]))


if __name__ == "__main__":
    main()
