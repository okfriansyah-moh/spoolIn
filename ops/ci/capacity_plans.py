"""Separate owned100k-title fixture and actual PostgreSQL18 EXPLAIN ANALYZE/BUFFERS evidence."""
import hashlib
import json
import re

from capacity_runtime import ROOT, checked
from capacity_workload import read_cases, assert_scope
from capacity_http import ApplicationClient


def literal(value):
    return "'" + str(value).replace("'", "''") + "'"


def columns():
    path = ROOT / "services/catalog/src/main/kotlin/com/spoolin/catalog/features/search/TitleSearchSql.kt"
    tail = path.read_text().split("const val TITLE_COLUMNS =", 1)[1]
    value = "".join(re.findall(r'"([^"\n]*)"', tail))
    if not value.startswith("t.id, t.kind") or not value.endswith("t.version"):
        raise RuntimeError("Title projection changed; update the source-bound query-plan fixture")
    return value, hashlib.sha256(path.read_bytes()).hexdigest()


def title_statement(selected):
    projection, source_hash = columns()
    predicates = ["t.status='ACTIVE'"]
    values, types = [], []
    def parameter(value, kind):
        values.append(literal(value))
        types.append(kind)
        return "$" + str(len(values))
    if selected.get("text"):
        text = selected["text"].replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        pattern = parameter("%" + text + "%", "text")
        predicates.append(f"(casefold(t.display_title) LIKE casefold({pattern}) ESCAPE '\\' OR "
                          f"(t.original_title IS NOT NULL AND casefold(t.original_title) LIKE casefold({pattern}) ESCAPE '\\'))")
    for field, column, kind in (("kind", "kind", "text"), ("year", "release_year", "integer"),
                                ("month", "release_month", "integer")):
        if selected.get(field) is not None:
            predicates.append("t." + column + "=" + parameter(selected[field], kind))
    if selected.get("genreIds"):
        ids = ",".join(parameter(value, "uuid") for value in selected["genreIds"])
        predicates.append(f"EXISTS (SELECT 1 FROM title_genre tg WHERE tg.title_id=t.id AND tg.genre_id IN ({ids}))")
    sql = "SELECT " + projection + " FROM title t WHERE " + " AND ".join(predicates)
    sql += " ORDER BY t.release_year DESC NULLS LAST,t.release_month DESC NULLS LAST,t.release_day DESC NULLS LAST,t.id ASC LIMIT 21"
    return sql, types, values, source_hash


def explain(stack, owner, name, sql, types=None, values=None, role="runtime"):
    prefix = "BEGIN; SET LOCAL statement_timeout='30s'; "
    if types:
        prefix += "PREPARE capacity_statement(" + ",".join(types) + ") AS " + sql + "; "
        selected = "EXECUTE capacity_statement(" + ",".join(values) + ")"
    else:
        selected = sql
    output = stack.database(owner, prefix + "EXPLAIN(ANALYZE,BUFFERS,FORMAT JSON) " + selected + "; ROLLBACK;",
                            role=role, maximum=1048576, timeout=45)
    plan = json.loads(output)
    if not isinstance(plan, list) or len(plan) != 1 or "Execution Time" not in plan[0]:
        raise RuntimeError("Actual PostgreSQL plan or executed timings missing")
    stack.write("plan-" + name + ".json", {"owner": owner, "role": role, "query": sql,
                                          "parameterTypes": types or [], "parameters": values or [],
                                          "plan": plan, "transaction": "rolled back after actual EXPLAIN ANALYZE"})
    return {"name": name, "owner": owner, "executionMs": plan[0]["Execution Time"], "artifact": "plan-" + name + ".json"}


def stress_fixture(stack):
    checked([*stack.compose, "stop", "mcp", "core", "catalog", "media"], stack.environment, timeout=150)
    sql = """
    BEGIN;
    INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,release_year,release_month,tv_status,release_context,version)
    SELECT md5('capacity-title-'||i::text)::uuid,CASE WHEN i%2=0 THEN 'MOVIE' ELSE 'TV' END,
      CASE WHEN i%17=0 THEN 'ARCHIVED' ELSE 'ACTIVE' END,'Capacity common title '||i::text,
      'Controlled capacity fixture metadata','en',CASE WHEN i%11=0 THEN NULL ELSE 2000+i%25 END,
      CASE WHEN i%11=0 THEN NULL ELSE 1+i%12 END,
      CASE WHEN i%2=0 THEN NULL ELSE 'UNKNOWN' END,
      CASE WHEN i%2=0 THEN 'ORIGINAL_RELEASE' ELSE 'FIRST_AIR' END,0 FROM generate_series(1,99800) i;
    INSERT INTO title_genre(title_id,genre_id)
    SELECT md5('capacity-title-'||i::text)::uuid,g.id FROM generate_series(1,99800) i
      CROSS JOIN (SELECT id FROM genre ORDER BY id LIMIT 2) g;
    UPDATE person SET status='ARCHIVED' WHERE id=(SELECT id FROM person ORDER BY id LIMIT 1);
    COMMIT; ANALYZE title; ANALYZE title_genre; ANALYZE credit;
    SELECT json_build_object('titles',(SELECT count(*) FROM title),'unknownDates',
      (SELECT count(*) FROM title WHERE release_year IS NULL),'archivedTitles',
      (SELECT count(*) FROM title WHERE status='ARCHIVED'),'archivedPeople',
      (SELECT count(*) FROM person WHERE status='ARCHIVED'));
    """
    counts = json.loads(stack.database("catalog", sql, "postgres", timeout=60))
    if counts["titles"] != 100000 or min(counts[key] for key in ("unknownDates", "archivedTitles", "archivedPeople")) <= 0:
        raise RuntimeError("Controlled100k fixture or required edge states missing")
    stack.write("stress-fixture.json", {"counts": counts, "boundary": "controlled PostgreSQL fixture setup",
                                       "writers": "paused", "seedAcceptance": False})
    return counts


def plans(stack, manifest):
    cases = read_cases(manifest)
    evidence = []
    for case in cases[:4]:
        sql, types, values, source_hash = title_statement(case["variables"]["filter"])
        evidence.append(explain(stack, "catalog", "seed-" + case["name"], sql, types, values))
    counts = stress_fixture(stack)
    stress_requests(stack, cases)
    controlled_work_fixture(stack)
    for case in cases[:4]:
        selected = dict(case["variables"]["filter"])
        if case["name"] == "common-literal":
            selected = {"text": "Capacity common"}
        sql, types, values, _ = title_statement(selected)
        evidence.append(explain(stack, "catalog", "100k-" + case["name"], sql, types, values))
    evidence.append(explain(stack, "catalog", "100k-unknown-date",
                            "SELECT id FROM title WHERE status='ACTIVE' AND release_year IS NULL ORDER BY id LIMIT 21"))
    evidence.append(explain(stack, "catalog", "100k-public-private-credit",
                            "SELECT c.id FROM credit c JOIN title t ON t.id=c.title_id JOIN person p ON p.id=c.person_id "
                            "WHERE t.status='ACTIVE' AND p.status='ACTIVE' ORDER BY c.id LIMIT 100"))
    evidence += work_plans(stack)
    evidence += protected_query_plans(stack)
    return {"result": "OBSERVED", "stressCounts": counts, "plans": evidence, "searchSourceSha256": source_hash,
            "limitations": ["Plans execute source-derived parameterized query shapes, not a claim that JPA/gRPC emitted identical SQL",
                            "N+1 SQL counting, complete consequential query inventory and both CPU architectures remain required",
                            "No index-use threshold or deployment service-level objective is invented"]}


def work_plans(stack):
    due = ("SELECT id,operation_id,kind,payload::text,attempt_count,max_attempts,deadline_at,lease_epoch,correlation_id,"
           "base_delay_ms,cap_delay_ms,backoff_multiplier,generation FROM durable_work WHERE state='READY' "
           "AND next_attempt_at<=CURRENT_TIMESTAMP AND deadline_at>GREATEST(CURRENT_TIMESTAMP,clock_timestamp()) "
           "AND attempt_count<max_attempts ORDER BY next_attempt_at,id LIMIT 10 FOR UPDATE SKIP LOCKED")
    exhausted = "SELECT id FROM durable_work WHERE state='EXHAUSTED' ORDER BY updated_at,id LIMIT 100"
    path = ROOT / "libraries/backend-support/src/main/kotlin/com/spoolin/support/maintenance/WorkRetention.kt"
    clause = re.search(r'private fun resolved\(\): String =\s*"""(.*?)"""\.trimIndent\(\)', path.read_text(), re.S)
    if clause is None:
        raise RuntimeError("Source retention eligibility changed; update the plan fixture")
    purge = "SELECT w.id FROM durable_work w WHERE " + clause.group(1) + " AND w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '90 days' ORDER BY w.updated_at,w.id LIMIT 100"
    return [explain(stack, "catalog", "100k-worker-due", due),
            explain(stack, "catalog", "100k-worker-exhausted", exhausted),
            explain(stack, "catalog", "100k-resolved-purge", purge)]


def stress_requests(stack, cases):
    stack.start("startup-stress")
    client = ApplicationClient(stack.environment)
    observed = []
    try:
        for original in cases:
            case = dict(original)
            if case["root"] == "titles":
                selected = dict(case["variables"]["filter"])
                if case["name"] == "common-literal":
                    selected = {"text": "Capacity common"}
                sql, types, values, _ = title_statement(selected)
                projection, _ = columns()
                sql = sql.replace("SELECT " + projection, "SELECT t.id", 1)
                sql = "SELECT COALESCE(json_agg(x.id),'[]') FROM (" + sql + ") x"
                command = ("PREPARE capacity_scope(" + ",".join(types) + ") AS " + sql + "; "
                           "EXECUTE capacity_scope(" + ",".join(values) + ");") if types else sql + ";"
                identities = json.loads(stack.database("catalog", command))
                case.update(expectedIds=identities[:20], hasNextPage=len(identities) > 20)
                case["variables"] = {"filter": selected}
            response = client.request(case["query"], case["variables"])
            valid = assert_scope(response, case)
            observed.append({"class": case["name"], "scopeAssertion": valid, **response.safe_result()})
            if not valid:
                raise RuntimeError("Actual100k application response differs from independent database scope")
    finally:
        client.close()
        stack.write("100k-application-probes.json", observed)
    stack.resources("100k-application-ready")
    checked([*stack.compose, "stop", "mcp", "core", "catalog", "media"], stack.environment, timeout=150)


def controlled_work_fixture(stack):
    sql = """
    BEGIN;
    INSERT INTO durable_work(id,kind,payload,state,max_attempts,deadline_at,correlation_id,
      base_delay_ms,cap_delay_ms,backoff_multiplier,next_attempt_at,updated_at)
    SELECT md5('capacity-work-'||i::text)::uuid,'CAPACITY_PLAN','{}',
      CASE WHEN i%3=0 THEN 'CANCELLED' WHEN i%2=0 THEN 'READY' ELSE 'EXHAUSTED' END,3,CURRENT_TIMESTAMP+INTERVAL '1 hour',
      md5('capacity-correlation-'||i::text)::uuid,500,5000,2,
      CURRENT_TIMESTAMP-INTERVAL '1 minute',CURRENT_TIMESTAMP-INTERVAL '100 days'
      FROM generate_series(1,1000) i;
    COMMIT; ANALYZE durable_work;
    """
    stack.database("catalog", sql, "postgres", timeout=60)


def protected_query_plans(stack):
    key = json.loads(stack.database("core", "SELECT json_build_object('id',id) FROM crypto_key_version WHERE purpose='PII' AND state='WRITE';"))["id"]
    nonce = ("UPDATE appdata.crypto_key_version SET next_nonce=next_nonce+1 WHERE id=$1 AND purpose=$2 "
             "AND state='WRITE' AND next_nonce<9223372036854775807 RETURNING next_nonce-1 AS allocated")
    rate = ("INSERT INTO appdata.request_rate_window(bucket_key,window_start,hits,expires_at) VALUES "
            "(decode(repeat('42',32),'hex'),CURRENT_TIMESTAMP,1,CURRENT_TIMESTAMP+INTERVAL '120 seconds') "
            "ON CONFLICT (bucket_key,window_start) DO UPDATE SET hits=request_rate_window.hits+1 "
            "WHERE request_rate_window.hits<1000000 RETURNING hits")
    expiry = ("WITH expired AS (SELECT bucket_key,window_start FROM appdata.request_rate_window "
              "WHERE expires_at<=CURRENT_TIMESTAMP ORDER BY expires_at,bucket_key,window_start LIMIT 100) "
              "DELETE FROM appdata.request_rate_window w USING expired e "
              "WHERE w.bucket_key=e.bucket_key AND w.window_start=e.window_start AND w.expires_at<=CURRENT_TIMESTAMP")
    return [explain(stack, "core", "nonce-reservation-rollback", nonce, ["text", "text"], [literal(key), literal("PII")]),
            explain(stack, "core", "rate-admission-rollback", rate),
            explain(stack, "core", "rate-expiry-rollback", expiry, role="maintenance")]
