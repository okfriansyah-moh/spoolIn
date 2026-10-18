"""Actual PostgreSQL driver-boundary observations in the owned capacity installation only."""
import hashlib
import json
import re
import time

from capacity_http import ApplicationClient
from capacity_runtime import ROOT, checked
from capacity_workload import TITLE_FIELDS

OWNERS = ("core", "catalog", "media")
METADATA_TABLES = r"(?:title_genre|title|genre|credit_role|credit|person|provenance_record)"


def inventory():
    definition = json.loads((ROOT / 'ops/ci/capacity-sql-inventory.json').read_text())
    required = {'rare', 'common', 'unfiltered', 'multi-genre', 'unknown-date', 'private-credit',
                'worker-due', 'worker-exhausted', 'work-purge', 'people', 'account-lookup',
                'nonce', 'rate-admission', 'rate-purge', 'draft-purge'}
    if len(definition['families']) != len(required) or {item['id'] for item in definition['families']} != required:
        raise RuntimeError('Approved SQL family inventory is incomplete or contains duplicates')
    for item in definition['families']:
        item['sources'] = [{"path": path, "sha256": hashlib.sha256((ROOT / path).read_bytes()).hexdigest()}
                           for path in item['sources']]
    definition['emittedSqlSources'] = [{"path": path, "sha256": hashlib.sha256((ROOT / path).read_bytes()).hexdigest()}
                                       for path in definition['emittedSqlSources']]
    return definition


def enable_observation(stack):
    checked([*stack.compose, 'stop', 'mcp', 'core', 'catalog', 'media'], stack.environment, timeout=150)
    available = stack.database('core', "SELECT count(*) FROM pg_available_extensions WHERE name='pg_stat_statements';", 'postgres')
    if available != '1':
        raise RuntimeError('Actual PostgreSQL image does not supply pg_stat_statements')
    # This is an owned disposable PostgreSQL process, with no application writers running.
    current = stack.database('core', 'SHOW shared_preload_libraries;', 'postgres')
    libraries = [name.strip() for name in current.split(',') if name.strip()]
    if any(not re.fullmatch(r'[a-z_]+', name) for name in libraries):
        raise RuntimeError('Unrecognized preload configuration; refusing to overwrite it')
    if 'pg_stat_statements' not in libraries:
        libraries.append('pg_stat_statements')
    stack.database('core', "ALTER SYSTEM SET shared_preload_libraries='" + ','.join(libraries) + "';", 'postgres')
    checked([*stack.compose, 'restart', 'postgres'], stack.environment, timeout=90)
    deadline = time.monotonic() + 60
    while True:
        try:
            stack.database('core', 'SELECT 1;', 'postgres', timeout=5)
            break
        except RuntimeError:
            if time.monotonic() >= deadline:
                raise
            time.sleep(1)
    for owner in OWNERS:
        stack.database(owner, 'CREATE EXTENSION pg_stat_statements WITH SCHEMA public;', 'postgres')
    version = stack.database('core', "SELECT json_build_object('postgres',current_setting('server_version'),"
                             "'extension',(SELECT extversion FROM pg_extension WHERE extname='pg_stat_statements'),"
                             "'track',current_setting('pg_stat_statements.track'));", 'postgres')
    stack.write('sql-observer.json', {'actual': json.loads(version), 'scope': 'owned disposable installation',
                                    'source': 'PostgreSQL completed top-level statements; no parameter values'})
    stack.start('startup-sql-observer')


def statements(stack):
    sql = """SELECT COALESCE(json_agg(json_build_object('queryId',queryid::text,'query',query,
      'calls',calls,'rows',rows,'totalExecutionMs',total_exec_time) ORDER BY queryid),'[]')
      FROM public.pg_stat_statements WHERE dbid=(SELECT oid FROM pg_database WHERE datname=current_database())
      AND userid=(SELECT oid FROM pg_roles WHERE rolname='spoolin_catalog_runtime') AND toplevel;"""
    return json.loads(stack.database('catalog', sql, 'postgres', maximum=1048576))


def deltas(before, after):
    previous = {row['queryId']: row for row in before}
    result = []
    for row in after:
        prior = previous.get(row['queryId'], {'calls': 0, 'rows': 0, 'totalExecutionMs': 0})
        calls = row['calls'] - prior['calls']
        if calls < 0:
            raise RuntimeError('PostgreSQL statement statistics reset during observation')
        if not calls:
            continue
        query = row['query']
        metadata = bool(re.search(r'\b(?:FROM|JOIN)\s+(?:appdata\.)?' + METADATA_TABLES + r'\b', query, re.I))
        result.append({'queryId': row['queryId'], 'sql': query, 'calls': calls,
                       'rows': row['rows'] - prior['rows'],
                       'executionMs': row['totalExecutionMs'] - prior['totalExecutionMs'],
                       'metadataSelect': metadata and bool(re.match(r'\s*(?:SELECT|WITH)\b', query, re.I))})
    return result


def query_counts(stack, label, fields=TITLE_FIELDS):
    client = ApplicationClient(stack.environment)
    observations = []
    try:
        for page_size in (1, 20, 1, 20):
            query = 'query{titles(filter:{},first:' + str(page_size) + '){nodes{' + fields + '} pageInfo{hasNextPage}}}'
            before = statements(stack)
            response = client.request(query, {})
            after = statements(stack)
            rows = response.body.get('data', {}).get('titles', {}).get('nodes', [])
            observed = deltas(before, after)
            metadata = [row for row in observed if row['metadataSelect']]
            observations.append({'requestedRows': page_size, 'returnedRows': len(rows),
                                 'response': response.safe_result(), 'statements': observed,
                                 'metadataSelectCalls': sum(row['calls'] for row in metadata),
                                 'metadataReturnedRows': sum(row['rows'] for row in metadata)})
            if response.status != 200 or response.body.get('errors') or len(rows) != page_size or not metadata:
                raise RuntimeError('Actual application query-count request or driver observation failed')
    finally:
        client.close()
        stack.write(label + '-emitted-sql.json', {'observations': observations,
                    'boundary': 'serialized actual HTTPS Core→Catalog→PostgreSQL runtime-role top-level statements',
                    'attribution': 'metadata SELECTs only; concurrent work/health statements retained separately',
                    'limits': 'no other metadata client runs in this exclusively owned installation'})
    counts = [row['metadataSelectCalls'] for row in observations]
    result = {'result': 'PASS' if len(set(counts)) == 1 else 'FAIL',
              'pageSizes': [1, 20, 1, 20], 'metadataSelectCalls': counts,
              'artifact': label + '-emitted-sql.json'}
    if result['result'] != 'PASS':
        result['failureCode'] = 'N_PLUS_ONE_OBSERVED' if counts[1] > counts[0] and counts[3] > counts[2] else 'UNSTABLE_QUERY_COUNTS'
    return result
