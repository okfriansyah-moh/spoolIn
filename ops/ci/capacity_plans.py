"""Separate owned100k-title fixture and actual PostgreSQL18 EXPLAIN ANALYZE/BUFFERS evidence."""
import hashlib
import json
import re

from capacity_runtime import ROOT, checked
from capacity_workload import read_cases, assert_scope, TITLE_FIELDS
from capacity_http import ApplicationClient
from capacity_sql_observation import inventory, enable_observation, query_counts


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
        predicates.append(f"(casefold(t.display_title COLLATE pg_unicode_fast) LIKE casefold({pattern} COLLATE pg_unicode_fast) ESCAPE '\\' OR "
                          f"(t.original_title IS NOT NULL AND casefold(t.original_title COLLATE pg_unicode_fast) LIKE casefold({pattern} COLLATE pg_unicode_fast) ESCAPE '\\') OR "
                          "EXISTS (SELECT 1 FROM credit sc JOIN person sp ON sp.id=sc.person_id "
                          "JOIN credit_role sr ON sr.id=sc.role_id WHERE sc.title_id=t.id "
                          "AND sp.status='ACTIVE' AND sr.category IN ('CAST','CREATOR') "
                          f"AND casefold(sp.name COLLATE pg_unicode_fast) LIKE casefold({pattern} COLLATE pg_unicode_fast) ESCAPE '\\'))")

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


def explain(stack, owner, name, sql, types=None, values=None, role="runtime", private_values=False):
    prefix = "BEGIN; SET LOCAL statement_timeout='30s'; "
    if types:
        prefix += "PREPARE capacity_statement(" + ",".join(types) + ") AS " + sql + "; "
        selected = "EXECUTE capacity_statement(" + ",".join(values) + ")"
    else:
        selected = sql
    output = stack.database(owner, prefix + "EXPLAIN(ANALYZE,BUFFERS,FORMAT JSON) " + selected + "; ROLLBACK;",
                            role=role, maximum=1048576, timeout=45)
    plan = json.loads(output)
    if private_values:
        plan = redact_plan_literals(plan)
        if any(secret in json.dumps(plan) for secret in re.findall(r"[a-f0-9]{64}", str(values))):
            raise RuntimeError("Protected lookup parameter remains in plan output")
    if not isinstance(plan, list) or len(plan) != 1 or "Execution Time" not in plan[0]:
        raise RuntimeError("Actual PostgreSQL plan or executed timings missing")
    stack.write("plan-" + name + ".json", {"owner": owner, "role": role, "query": sql,
                                          "parameterTypes": types or [], "parameters": ["withheld"] * len(values or []) if private_values else values or [],
                                          "plan": plan, "transaction": "rolled back after actual EXPLAIN ANALYZE"})
    return {"name": name, "owner": owner, "executionMs": plan[0]["Execution Time"], "artifact": "plan-" + name + ".json"}


def redact_plan_literals(value):
    if isinstance(value, dict):
        return {key: redact_plan_literals(item) for key, item in value.items()}
    if isinstance(value, list):
        return [redact_plan_literals(item) for item in value]
    return re.sub(r"'(?:[^']|'')*'", "'[withheld]'", value) if isinstance(value, str) else value


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
    definition = inventory()
    stack.write('sql-inventory.json', definition)
    enable_observation(stack)
    cases = read_cases(manifest)
    counts_evidence = {'200': read_query_counts(stack, '200')}
    evidence = dataset_plans(stack, cases, '200')
    counts = stress_fixture(stack)
    stress_requests(stack, cases)
    stack.start('startup-stress-sql-observer')
    try:
        counts_evidence['100k'] = read_query_counts(stack, '100k')
    finally:
        checked([*stack.compose, 'stop', 'mcp', 'core', 'catalog', 'media'], stack.environment, timeout=150)
    evidence += dataset_plans(stack, cases, '100k')
    required = {item['id'] for item in definition['families']}
    assessed = {size: {item['family'] for item in evidence if item['dataset'] == size} for size in ('200', '100k')}
    if any(value != required for value in assessed.values()):
        raise RuntimeError('A required SQL family lacks an actually executed dataset plan')
    complete = all(observed['result'] == 'PASS' for group in counts_evidence.values() for observed in group.values())
    return {'result': 'PASS' if complete else 'FAIL', 'scope': 'TD7.4/7.5 SQL plan families and actual public title-list query counts only',
            'stressCounts': counts, 'plans': evidence, 'queryCounts': counts_evidence,
            'limitations': ['Source-bound parameterized EXPLAIN plans and emitted driver statements are distinct artifacts',
                            'No index-use threshold, both-architecture proof or deployment SLO is inferred']}


def read_query_counts(stack, size):
    root_fields = TITLE_FIELDS.replace(
        'credits{nodes{id personId role{id allowsCharacters} characters version} pageInfo{hasNextPage}}\n', '')
    return {'rootList': query_counts(stack, size + '-root-list', root_fields),
            'nestedCredits': query_counts(stack, size + '-nested-credits')}


def dataset_plans(stack, cases, size):
    checked([*stack.compose, 'stop', 'mcp', 'core', 'catalog', 'media'], stack.environment, timeout=150)
    edge_fixture(stack, cases, size)
    controlled_work_fixture(stack)
    evidence = []
    try:
        for case, family in zip(cases[:4], ('unfiltered', 'rare', 'common', 'multi-genre')):
            selected = dict(case['variables']['filter'])
            if size == '100k' and family == 'common':
                selected = {'text': 'Capacity common'}
            sql, types, values, _ = title_statement(selected)
            evidence.append({**explain(stack, 'catalog', size + '-' + family, sql, types, values), 'family': family})
        evidence += supplementary_plans(stack, size)
        evidence += work_plans(stack, size)
        evidence += protected_query_plans(stack, size)
        for item in evidence:
            item['dataset'] = size
    finally:
        clear_work_fixture(stack)
    return evidence


def edge_fixture(stack, cases, size):
    excluded = [case['expectedIds'][0] for case in cases if case['name'] in {'rare-literal', 'title-detail'}]
    identities = ','.join(literal(value) for value in excluded)
    sql = ("UPDATE person SET status='ARCHIVED' WHERE id=(SELECT person_id FROM credit ORDER BY id LIMIT 1); "
           "UPDATE title SET status='ARCHIVED' WHERE id=(SELECT title_id FROM credit "
           "WHERE title_id NOT IN (" + identities + ") ORDER BY title_id LIMIT 1); "
           "UPDATE title SET release_year=NULL,release_month=NULL,release_day=NULL WHERE id="
           "(SELECT id FROM title WHERE kind='TV' AND id NOT IN (" + identities + ") ORDER BY id LIMIT 1); "
           "ANALYZE title; ANALYZE person; "
           "SELECT json_build_object('titles',(SELECT count(*) FROM title),'unknownDates',"
           "(SELECT count(*) FROM title WHERE release_year IS NULL),'archivedTitles',"
           "(SELECT count(*) FROM title WHERE status='ARCHIVED'),'archivedPeople',"
           "(SELECT count(*) FROM person WHERE status='ARCHIVED'));")
    counts = json.loads(stack.database('catalog', sql, 'postgres'))
    if counts['titles'] != int(size.replace('100k', '100000')) or min(counts[key] for key in
            ('unknownDates', 'archivedTitles', 'archivedPeople')) <= 0:
        raise RuntimeError('Actual plan fixture lacks its required unknown/private states')
    stack.write(size + '-plan-fixture.json', {'counts': counts,
                'boundary': 'controlled paused-writer SQL plan fixture; not seed acceptance', 'setupSQL': sql})


def supplementary_plans(stack, size):
    projection, _ = columns()
    statements = {
        'unknown-date': 'SELECT ' + projection + " FROM title t WHERE t.status='ACTIVE' AND t.release_year IS NULL "
                        'ORDER BY t.release_year DESC NULLS LAST,t.release_month DESC NULLS LAST,'
                        't.release_day DESC NULLS LAST,t.id ASC LIMIT 21',
        'private-credit': "SELECT c.id,c.title_id,c.person_id,c.role_id,c.characters,c.version,p.name,t.display_title,"
                          'r.category,r.label,r.allows_characters FROM credit c JOIN title t ON t.id=c.title_id '
                          'JOIN person p ON p.id=c.person_id JOIN credit_role r ON r.id=c.role_id '
                          "WHERE t.status='ACTIVE' AND p.status='ACTIVE' ORDER BY c.id LIMIT 21",
        'people': "SELECT p.id,p.name,p.biography,p.status,p.version FROM person p WHERE p.status='ACTIVE' "
                  "AND casefold(p.name COLLATE pg_unicode_fast) LIKE casefold('%a%' COLLATE pg_unicode_fast) ESCAPE '\\' "
                  'ORDER BY p.name ASC,p.id ASC LIMIT 21',
    }
    return [{**explain(stack, 'catalog', size + '-' + family, sql), 'family': family}
            for family, sql in statements.items()]


def work_plans(stack, size):
    due = ("SELECT id,operation_id,kind,payload::text,attempt_count,max_attempts,deadline_at,lease_epoch,correlation_id,"
           "base_delay_ms,cap_delay_ms,backoff_multiplier,generation FROM durable_work WHERE state='READY' "
           "AND next_attempt_at<=CURRENT_TIMESTAMP AND deadline_at>GREATEST(CURRENT_TIMESTAMP,clock_timestamp()) "
           "AND attempt_count<max_attempts ORDER BY next_attempt_at,id LIMIT 10 FOR UPDATE SKIP LOCKED")
    exhausted = "SELECT state,count(*) FROM durable_work WHERE kind<>'NOTIFICATION' OR notification_mode<>'database' GROUP BY state"
    path = ROOT / "libraries/backend-support/src/main/kotlin/com/spoolin/support/maintenance/WorkRetention.kt"
    clause = re.search(r'private fun resolved\(\): String =\s*"""(.*?)"""\.trimIndent\(\)', path.read_text(), re.S)
    if clause is None:
        raise RuntimeError("Source retention eligibility changed; update the plan fixture")
    purge = ("SELECT w.id FROM appdata.durable_work w WHERE " + clause.group(1) +
             " AND ((w.kind<>'NOTIFICATION' AND w.payload<>'{}'::jsonb AND w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '7 days') "
             "OR w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '90 days') ORDER BY w.updated_at,w.id LIMIT 100")
    statements = {'worker-due': due, 'worker-exhausted': exhausted, 'work-purge': purge}
    return [{**explain(stack, 'catalog', size + '-' + family, sql,
                       role='maintenance' if family == 'work-purge' else 'runtime'), 'family': family}
            for family, sql in statements.items()]


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


def protected_query_plans(stack, size):
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
    alias = json.loads(stack.database('core', "SELECT json_build_object('key',key_id,'digest',encode(digest,'hex')) "
                                     'FROM account_email_lookup ORDER BY key_id,account_id LIMIT 1;'))
    lookup = ('SELECT a.id,a.password_hash,a.auth_epoch FROM appdata.account_email_lookup e '
              'JOIN appdata.user_account a ON a.id=e.account_id WHERE e.key_id=$1 AND e.digest=$2')
    draft = ("WITH expired AS (SELECT id FROM appdata.form_draft WHERE expires_at<=CURRENT_TIMESTAMP "
             "ORDER BY expires_at,id LIMIT 100) DELETE FROM appdata.form_draft d USING expired e "
             "WHERE d.id=e.id AND (expires_at<=CURRENT_TIMESTAMP)")
    return [{**explain(stack, 'core', size + '-nonce', nonce, ['text', 'text'], [literal(key), literal('PII')]), 'family': 'nonce'},
            {**explain(stack, 'core', size + '-rate-admission', rate), 'family': 'rate-admission'},
            {**explain(stack, 'core', size + '-rate-purge', expiry, role='maintenance'), 'family': 'rate-purge'},
            {**explain(stack, 'core', size + '-account-lookup', lookup, ['text', 'bytea'],
                       [literal(alias['key']), "decode(" + literal(alias['digest']) + ",'hex')"], private_values=True), 'family': 'account-lookup'},
            {**explain(stack, 'core', size + '-draft-purge', draft, role='maintenance'), 'family': 'draft-purge'}]


def clear_work_fixture(stack):
    # Only rows inserted as inert EXPLAIN fixtures; no live worker was allowed to claim them.
    history = stack.database('catalog', "SELECT count(*) FROM operational_event e JOIN durable_work w ON w.id=e.work_id "
                             "WHERE w.kind='CAPACITY_PLAN';", 'postgres')
    if history != '0':
        raise RuntimeError('A plan fixture acquired real history; refusing destructive cleanup')
    stack.database('catalog', "DELETE FROM durable_work WHERE kind='CAPACITY_PLAN' AND id IN "
                   "(SELECT md5('capacity-work-'||i::text)::uuid FROM generate_series(1,1000) i);", 'postgres')
