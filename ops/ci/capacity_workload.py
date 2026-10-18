"""Fixed F-BENCHMARK workloads, independent expected scopes and retained client observations."""
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import json
import math
from pathlib import Path
import threading
import time
import uuid

from capacity_http import ApplicationClient

TITLE_FIELDS = """id kind state title originalTitle synopsis synopsisLanguage originalLanguage countries
release{year month day precision} genres{id label} runtimeMinutes tvStatus version
credits{nodes{id personId role{id allowsCharacters} characters version} pageInfo{hasNextPage}}
provenance{source externalId fieldSources locallyChanged} artwork{assetId width height url}"""
LIST_QUERY = "query($filter:SearchInput!){titles(filter:$filter,first:20){nodes{" + TITLE_FIELDS + "} pageInfo{hasNextPage}}}"
DETAIL_QUERY = "query($id:UUID!){title(id:$id){" + TITLE_FIELDS + "}}"


def utc_now():
    return datetime.now(timezone.utc).isoformat()


def nearest_rank(samples, percentile=0.95):
    if not samples or not 0 < percentile <= 1:
        raise ValueError("Nonempty samples and a valid percentile are required")
    ordered = sorted(samples)
    return ordered[math.ceil(percentile * len(ordered)) - 1]


def ordered_records(records):
    return sorted(records, key=lambda row: tuple(-(row["normalized"]["release"].get(part) or 0)
                  for part in ("year", "month", "day")) + (row["stableUUID"],))


def matching(records, selected, people=(), roles=()):
    text = selected.get("text", "").casefold()
    result = []
    people_by_id = {person["externalId"]: person for person in people}
    categories = {role["externalId"]: role["category"] for role in roles}
    for row in records:
        if row.get("status", "ACTIVE") != "ACTIVE":
            continue
        values = row["normalized"]
        names = [values["title"], values.get("originalTitle") or ""]
        for credit in row.get("credits", []):
            person = people_by_id.get(credit["personExternalId"])
            if person and person.get("status", "ACTIVE") == "ACTIVE" and categories.get(credit["roleExternalId"]) in {"CAST", "CREATOR"}:
                names.append(person["name"])
        if text and not any(text in name.casefold() for name in names):
            continue
        if selected.get("kind") and row["kind"] != selected["kind"]:
            continue
        if any(selected.get(part) is not None and values["release"].get(part) != selected[part] for part in ("year", "month")):
            continue
        if selected.get("genreIds") and not set(selected["genreIds"]) & set(values["genreIds"]):
            continue
        result.append(row)
    return ordered_records(result)


def read_cases(manifest):
    records = manifest["records"]
    def expected(selected):
        return matching(records, selected, manifest["people"], manifest["roles"])
    rare = next(row["normalized"]["title"] for row in records
                if len(expected({"text": row["normalized"]["title"]})) == 1)
    common = max("abcdefghijklmnopqrstuvwxyz", key=lambda text: len(expected({"text": text})))
    if len(expected({"text": common})) <= 20:
        raise RuntimeError("Approved fixture does not provide a common literal substring")
    dated = next(row for row in records if row["normalized"]["release"].get("month"))
    genres = dated["normalized"]["genreIds"][:2]
    if len(genres) == 1:
        genres += [value for row in records for value in row["normalized"]["genreIds"] if value not in genres][:1]
    if len(genres) != 2:
        raise RuntimeError("Approved fixture does not provide two distinct filter genres")
    date = dated["normalized"]["release"]
    filters = ({}, {"text": rare}, {"text": common},
               {"kind": dated["kind"], "genreIds": genres, "year": date["year"], "month": date["month"]})
    cases = [{"name": name, "root": "titles", "query": LIST_QUERY, "variables": {"filter": selected},
              "expectedIds": [row["stableUUID"] for row in expected(selected)[:20]],
              "hasNextPage": len(expected(selected)) > 20}
             for name, selected in zip(("no-filter", "rare-literal", "common-literal", "typed-two-genre-date"), filters)]
    cases.append({"name": "title-detail", "root": "title", "query": DETAIL_QUERY,
                  "variables": {"id": dated["stableUUID"]}, "expectedIds": [dated["stableUUID"]]})
    return cases


def assert_scope(response, case):
    if response.status != 200 or response.safe_result()["domainError"] or response.transport:
        return False
    data = response.body.get("data") or {}
    value = data.get(case["root"])
    if case["root"] == "title":
        return isinstance(value, dict) and value.get("id") == case["expectedIds"][0]
    return (isinstance(value, dict) and [row.get("id") for row in value.get("nodes", [])] == case["expectedIds"]
            and value.get("pageInfo", {}).get("hasNextPage") == case["hasNextPage"])


class SampleSink:
    def __init__(self, output):
        self.output = output.open("x")
        self.lock = threading.Lock()
        self.counts = Counter()
        self.bytes = 0

    def record(self, worker, name, response, assertion=True):
        value = {"worker": worker, "class": name, **response.safe_result(), "scopeAssertion": assertion}
        line = json.dumps(value, separators=(",", ":")) + "\n"
        with self.lock:
            self.bytes += len(line.encode())
            if self.bytes > 67108864:
                raise RuntimeError("Raw sample budget exceeded; no samples evicted")
            self.output.write(line)
            self.counts["offered"] += 1
            self.counts["transportErrors" if response.transport else "completed"] += 1
            self.counts["http200"] += int(response.status == 200)
            self.counts["throttled"] += int(response.status == 429 or value["problem"] == "RATE_LIMITED")
            self.counts["domainErrors"] += int(value["domainError"])
            self.counts["httpErrors"] += int(response.status not in {0, 200, 429})
            self.counts["assertionErrors"] += int(not assertion and response.status == 200)
            self.counts["class:" + name] += 1

    def close(self):
        self.output.close()


def run_readers(environment, cases, seconds, sink):
    barrier = threading.Barrier(11)
    deadline = [0]
    def reader(worker):
        client = ApplicationClient(environment)
        index = 0
        try:
            barrier.wait(timeout=30)
            while time.monotonic_ns() < deadline[0]:
                case = cases[index % len(cases)]
                response = client.request(case["query"], case["variables"])
                sink.record(worker, case["name"], response, assert_scope(response, case))
                index += 1
        finally:
            client.close()
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(reader, worker) for worker in range(10)]
        started = utc_now()
        monotonic_start = time.monotonic_ns()
        deadline[0] = monotonic_start + seconds * 1000000000
        barrier.wait(timeout=30)
        for future in futures:
            future.result()
    return {"started": started, "offerSeconds": seconds, "drained": utc_now(), "concurrency": 10,
            "offerStartMonotonicNanos": monotonic_start, "offerDeadlineMonotonicNanos": deadline[0],
            "drainMonotonicNanos": time.monotonic_ns()}


class MetadataWriter:
    def __init__(self, environment, manifest):
        self.client = ApplicationClient(environment)
        self.client.sign_in(Path(environment["SPOOLIN_BROWSER_MAINTAINER_PASSWORD_FILE"]).read_text().strip())
        self.policy = self.client.data("query {policy{revision}}")["policy"]["revision"]
        roles = {row["externalId"]: row for row in manifest["roles"]}
        row = next(row for row in manifest["records"] if row["kind"] == "MOVIE" and
                   any(roles[credit["roleExternalId"]]["allowsCharacters"] for credit in row["credits"]))
        credit = next(credit for credit in row["credits"] if roles[credit["roleExternalId"]]["allowsCharacters"])
        person = next(item for item in manifest["people"] if item["externalId"] == credit["personExternalId"])
        self.ids = {"updateMovie": row["stableUUID"], "updatePerson": person["stableUUID"], "updateCredit": credit["stableUUID"]}
        self.versions = {name: 0 for name in self.ids}
        self.sequence = 0
        self.expected = {}

    def update(self, name):
        kind = {"updateMovie": "MoviePatch", "updatePerson": "PersonPatch", "updateCredit": "CreditPatch"}[name]
        patch = {"updateMovie": {"synopsis": f"Capacity movie metadata {self.sequence}"},
                 "updatePerson": {"biography": f"Capacity person metadata {self.sequence}"},
                 "updateCredit": {"characters": [f"Capacity Character {self.sequence}"]}}[name]
        query = (f"mutation($op:UUID!,$id:UUID!,$version:Long!,$policy:String!,$patch:{kind}!){{"
                 f"{name}(operationId:$op,id:$id,expectedVersion:$version,policyRevision:$policy,patch:$patch)"
                 "{operation{state version problem{code}} problem{code}}}")
        intent = str(uuid.uuid4())
        response = self.client.request(query, {"op": intent, "id": self.ids[name], "version": self.versions[name],
                                                "policy": self.policy, "patch": patch})
        result = (response.body.get("data") or {}).get(name) or {}
        operation = result.get("operation") or {}
        valid = (response.status == 200 and operation.get("state") == "SUCCEEDED" and not result.get("problem")
                 and not operation.get("problem") and isinstance(operation.get("version"), int))
        if valid:
            self.expected[name] = patch
            self.versions[name] = operation["version"]
            if name == "updateCredit":
                self.versions["updateMovie"] += 1
            self.sequence += 1
        return response, valid, intent

    def verify_persisted(self, stack):
        from capacity_plans import literal
        sql = "SELECT json_build_object("
        sql += "'updateMovie',(SELECT json_build_object('version',version,'synopsis',synopsis) FROM title WHERE id=" + literal(self.ids["updateMovie"]) + "),"
        sql += "'updatePerson',(SELECT json_build_object('version',version,'biography',biography) FROM person WHERE id=" + literal(self.ids["updatePerson"]) + "),"
        sql += "'updateCredit',(SELECT json_build_object('version',version,'characters',characters) FROM credit WHERE id=" + literal(self.ids["updateCredit"]) + "));"
        values = json.loads(stack.database("catalog", "BEGIN READ ONLY; " + sql + "COMMIT;"))
        for name in self.ids:
            expected = {"version": self.versions[name], **self.expected[name]}
            if values[name] != expected:
                raise RuntimeError("Fresh database state differs from confirmed metadata effects")
        stack.write("writer-persistence.json", {"result": "PASS", "freshDatabaseRead": True,
                                              "versions": self.versions, "classes": sorted(self.expected)})

    def run(self, seconds, sink):
        started = utc_now()
        deadline = time.monotonic_ns() + seconds * 1000000000
        index = 0
        while time.monotonic_ns() < deadline:
            name = ("updateMovie", "updatePerson", "updateCredit")[index % 3]
            response, valid, intent = self.update(name)
            sink.record(0, name, response, valid)
            if not valid:
                # Preserve the original operation identity; never retry a potentially committed write under a new UUID.
                return {"started": started, "offerSeconds": seconds, "drained": utc_now(), "concurrency": 1,
                        "aborted": True, "originalIntent": intent, "reason": "metadata-write-not-confirmed"}
            index += 1
        return {"started": started, "offerSeconds": seconds, "drained": utc_now(), "concurrency": 1}


def request_contract(cases):
    return [{"class": row["name"], "root": row["root"], "variables": row["variables"],
             "expectedIds": row["expectedIds"], "querySha256": hashlib.sha256(row["query"].encode()).hexdigest(),
             "payloadBytes": len(json.dumps({"query": row["query"], "variables": row["variables"]}, separators=(",", ":")).encode())}
            for row in cases]
