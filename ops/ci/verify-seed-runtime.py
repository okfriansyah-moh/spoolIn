#!/usr/bin/env python3
"""Compare the complete frozen seed with live database and HTTPS GraphQL results."""
import importlib.util
import json
import os
from pathlib import Path
import ssl
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
TABLES = ('title', 'person', 'genre', 'credit_role', 'credit', 'title_genre', 'provenance_record', 'seed_member')
TITLE_FIELDS = '''id kind state title originalTitle synopsis synopsisLanguage originalLanguage countries
release {year month day precision} tvEnd {year month day precision} tvStatus releaseContext
runtimeMinutes genres {id label} artwork {assetId url width height}
credits {nodes {id personId personName titleId titleName role {id label category allowsCharacters} characters version}
pageInfo {hasNextPage}} provenance {source externalId fieldSources locallyChanged}'''


def require(value, message):
    if not value:
        raise ValueError(message)


def compose(*arguments, **options):
    command = ['docker', 'compose', '--project-name', os.environ['SPOOLIN_INSTALLATION'],
               '-f', str(ROOT / 'ops/compose.yaml'), *arguments]
    return subprocess.run(command, check=True, timeout=180, **options)


def snapshot():
    fields = [f"'{table}',(SELECT COALESCE(json_agg(t ORDER BY to_jsonb(t)::text),'[]') FROM appdata.{table} t)" for table in TABLES]
    fields.append("'receipt',(SELECT json_agg(t) FROM (SELECT state,seed_version,"
                  "encode(seed_snapshot_hash,'hex') hash,seed_movie_count,seed_tv_count "
                  "FROM appdata.operation_receipt WHERE receipt_type='CATALOG_INITIALIZATION') t)")
    sql = 'BEGIN READ ONLY; SELECT json_build_object(' + ','.join(fields) + '); COMMIT;'
    shell = 'export PGPASSWORD="$(cat /run/secrets/catalog-backup-password)"; exec psql -X -qAt -v ON_ERROR_STOP=1 -h /var/run/postgresql -U spoolin_catalog_backup -d spoolin_catalog'
    result = compose('exec', '-T', 'postgres', '/bin/sh', '-ec', shell,
                     input=sql, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return json.loads(result.stdout)


def compare_fields(actual, expected, label):
    for field, value in expected.items():
        require(actual.get(field) == value, f'{label}: {field} differs from frozen seed')


def title_database(node):
    values = node['normalized']
    result = {'kind': node['kind'], 'status': 'ACTIVE', 'display_title': values['title'],
              'synopsis': values['synopsis'], 'synopsis_language': values['synopsisLanguage'],
              'release_context': 'FIRST_AIR' if node['kind'] == 'TV' else 'ORIGINAL_RELEASE',
              'tv_status': values.get('tvStatus'), 'asset_id': None, 'attachment_id': None, 'version': 0}
    for prefix, date in (('release', values['release']), ('end', values.get('end') or {})):
        result.update({f'{prefix}_{part}': date.get(part) for part in ('year', 'month', 'day')})
    return result


def verify_titles(manifest, database):
    titles = {row['id']: row for row in database['title']}
    require(len(titles) == 200, 'Initial live title total must be exactly200')
    for kind in ('MOVIE', 'TV'):
        require(sum(row['kind'] == kind for row in titles.values()) == 100, f'Expected100 distinct{kind}')
    genres = {row['id']: row for row in database['genre']}
    links = {(row['title_id'], row['genre_id']) for row in database['title_genre']}
    expected_links = set()
    for node in manifest['records']:
        compare_fields(titles[node['stableUUID']], title_database(node), node['stableUUID'])
        for genre in node['genres']:
            compare_fields(genres[genre['stableUUID']], {'label': genre['label']}, genre['stableUUID'])
            expected_links.add((node['stableUUID'], genre['stableUUID']))
    require(links == expected_links, 'Complete title/genre association set differs')


def verify_people_credits(manifest, database, validator):
    people = {row['id']: row for row in database['person']}
    require(set(people) == {row['stableUUID'] for row in manifest['people']}, 'Person identity set differs')
    for person in manifest['people']:
        compare_fields(people[person['stableUUID']], {'name': person['name'], 'status': 'ACTIVE', 'version': 0}, person['stableUUID'])
    roles = {row['id']: row for row in database['credit_role']}
    starter_ids = dict(zip(('P161', 'P57', 'P58', 'P162'),
                       (f'b0000001-0000-4000-8000-{number:012d}' for number in range(1, 5))))
    role_ids = {}
    for role in manifest['roles']:
        expected_identity = starter_ids.get(role['externalId'], role['stableUUID'])
        actual = roles[expected_identity]
        compare_fields(actual, {'label': role['label'], 'category': role['category'],
                                'allows_characters': role['allowsCharacters']}, role['externalId'])
        role_ids[role['externalId']] = actual['id']
    expected = {}
    for title in manifest['records']:
        for credit in title['credits']:
            person = validator.identity(manifest['namespaceUUID'], 'wikidata', 'PERSON', credit['personExternalId'])
            expected[credit['stableUUID']] = {'title_id': title['stableUUID'], 'person_id': person,
                                             'role_id': role_ids[credit['roleExternalId']], 'characters': [], 'version': 0}
    actual = {row['id']: row for row in database['credit']}
    require(actual.keys() == expected.keys(), 'Complete credit identity set differs')
    for identity, fields in expected.items():
        compare_fields(actual[identity], fields, identity)
    return role_ids


def seed_members(manifest):
    result = [(node, 'TITLE') for node in manifest['records']]
    result.extend((node, 'PERSON') for node in manifest['people'])
    result.extend((node, 'ROLE') for node in manifest['roles'])
    genres = {node['stableUUID']: node for title in manifest['records'] for node in title['genres']}
    result.extend((node, 'GENRE') for node in genres.values())
    result.extend((node, 'CREDIT') for title in manifest['records'] for node in title['credits'])
    return result


def verify_provenance(manifest, database, validator, role_ids):
    members = {row['member_key']: row for row in database['seed_member']}
    expected = seed_members(manifest)
    require(len(members) == len(expected), 'Complete seed membership total differs')
    for node, kind in expected:
        key = f"{kind}/{node['source']}/{node['externalId']}"
        identity = role_ids[node['externalId']] if kind == 'ROLE' else node['stableUUID']
        compare_fields(members[key], {'subject_id': identity, 'subject_kind': kind, 'removed_at': None,
                                      'seed_id': manifest['manifestVersion'], 'content_hash': '\\x' + validator.checksum(node)}, key)
    sources = {(row['subject_id'], row['subject_kind']): row for row in database['provenance_record']}
    expected_sources = [(node, kind) for node, kind in expected if kind in ('TITLE', 'PERSON')]
    require(len(sources) == len(expected_sources), 'Complete provenance total differs')
    for node, kind in expected_sources:
        compare_fields(sources[(node['stableUUID'], kind)], {'source': node['source'], 'external_id': node['externalId'],
                       'deleted_at': None, 'locally_changed': [], 'field_sources': {row['field']: row for row in node['sources']}}, node['stableUUID'])
    require(len(database['receipt']) == 1, 'Expected one initialization receipt')
    compare_fields(database['receipt'][0], {'state': 'SUCCEEDED', 'seed_version': manifest['manifestVersion'],
                   'hash': manifest['snapshotSHA256'], 'seed_movie_count': 100, 'seed_tv_count': 100}, 'initialization receipt')


def graphql(query, variables):
    context = ssl.create_default_context(cafile=os.environ['SERVICE_CA_FILE'])
    endpoint = 'https://localhost:' + os.environ.get('CORE_HTTPS_PORT', '8443') + '/graphql'
    payload = json.dumps({'query': query, 'variables': variables}).encode()
    request = urllib.request.Request(endpoint, data=payload, headers={'Content-Type': 'application/json'})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, context=context, timeout=20) as response:
                body = json.load(response)
            require(not body.get('errors'), 'GraphQL returned errors during full seed comparison')
            return body['data']
        except urllib.error.HTTPError as error:
            if error.code != 429 or attempt == 2:
                raise
            delay = int(error.headers.get('Retry-After', '60'))
            require(0 < delay <= 120, 'Invalid admission retry delay')
            time.sleep(delay)
    raise RuntimeError('Seed API request did not complete')


def verify_api_credits(actual, node, manifest, role_ids):
    people = {row['externalId']: row for row in manifest['people']}
    credits = {row['id']: row for row in actual['credits']['nodes']}
    require(not actual['credits']['pageInfo']['hasNextPage'], 'Seed credits require additional API pagination')
    require(set(credits) == {row['stableUUID'] for row in node['credits']}, 'API complete credit set differs')
    roles = {row['externalId']: row for row in manifest['roles']}
    for expected in node['credits']:
        person = people[expected['personExternalId']]
        role = roles[expected['roleExternalId']]
        compare_fields(credits[expected['stableUUID']], {'personId': person['stableUUID'], 'personName': person['name'],
                       'titleId': node['stableUUID'], 'titleName': node['normalized']['title'], 'characters': [],
                       'role': {'id': role_ids[expected['roleExternalId']], 'label': role['label'],
                                'category': role['category'], 'allowsCharacters': role['allowsCharacters']}}, expected['stableUUID'])


def verify_api_order(manifest):
    actual = []
    cursor = None
    while True:
        query = 'query SeedOrder($after:Cursor){titles(filter:{},first:100,after:$after){nodes{id} pageInfo{endCursor hasNextPage}}}'
        page = graphql(query, {'after': cursor})['titles']
        actual.extend(row['id'] for row in page['nodes'])
        require(len(actual) <= 200, 'Unexpected duplicate or extra API page results')
        if not page['pageInfo']['hasNextPage']:
            break
        require(page['pageInfo']['endCursor'] != cursor, 'API cursor did not advance')
        cursor = page['pageInfo']['endCursor']
    expected = sorted(manifest['records'], key=lambda row: tuple(-(row['normalized']['release'].get(part) or 0)
                      for part in ('year', 'month', 'day')) + (row['stableUUID'],))
    require(actual == [row['stableUUID'] for row in expected], 'Complete discovery ordering differs from release-date/UUID contract')


def verify_api(manifest, role_ids):
    results = []
    query = 'query SeedTitle($id:UUID!){title(id:$id){' + TITLE_FIELDS + '}}'
    for node in manifest['records']:
        actual = graphql(query, {'id': node['stableUUID']})['title']
        values = node['normalized']
        compare_fields(actual, {'id': node['stableUUID'], 'kind': node['kind'], 'state': 'ACTIVE',
                       'title': values['title'], 'synopsis': values['synopsis'], 'synopsisLanguage': values['synopsisLanguage'],
                       'release': values['release'], 'tvStatus': values.get('tvStatus'), 'artwork': None,
                       'originalTitle': values.get('originalTitle'), 'originalLanguage': values.get('originalLanguage'),
                       'runtimeMinutes': values.get('runtimeMinutes'), 'countries': values.get('countries', [])}, node['stableUUID'])
        require({row['id'] for row in actual['genres']} == set(values['genreIds']), 'API genre identities differ')
        verify_api_credits(actual, node, manifest, role_ids)
        require(len(actual['provenance']) == 1, 'API seed provenance missing')
        compare_fields(actual['provenance'][0], {'source': node['source'], 'externalId': node['externalId'],
                       'fieldSources': {row['field']: row for row in node['sources']}, 'locallyChanged': []}, node['stableUUID'])
        results.append({'id': node['stableUUID'], 'kind': node['kind'], 'database': 'pass', 'https_api': 'pass',
                        'credit_count': len(node['credits']), 'artwork': 'not supplied; approved optional placeholders'})
        time.sleep(0.6)
    return results


def main():
    require(os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ.get('RUNNER_ENVIRONMENT') == 'github-hosted', 'Hosted CI required')
    require(os.environ['SPOOLIN_INSTALLATION'] == 'ci-' + os.environ['GITHUB_RUN_ID'] + '-' + os.environ['GITHUB_RUN_ATTEMPT'], 'Owned initial namespace required')
    manifest = json.loads((ROOT / 'data/seed/manifest.json').read_text())
    spec = importlib.util.spec_from_file_location('seed_validator', ROOT / 'build-tools/validate-seed/validate_manifest.py')
    validator = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(validator)
    report = {'tested_sha': os.environ['TESTED_SHA'], 'run_id': os.environ['GITHUB_RUN_ID'], 'snapshot': manifest['snapshotSHA256'], 'result': 'failed'}
    output = ROOT / 'artifacts/runtime/seed-verification.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    try:
        before = snapshot()
        verify_titles(manifest, before)
        roles = verify_people_credits(manifest, before, validator)
        verify_provenance(manifest, before, validator, roles)
        verify_api_order(manifest)
        report['records'] = verify_api(manifest, roles)
        for label, arguments in (('repeat-seed', ['seed']), ('seed-restart', ['restart'])):
            with (output.parent / (label + '.log')).open('w') as log:
                subprocess.run(['make', *arguments], env={**os.environ, 'SPOOLIN_SEED_APPLY': '1'}, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=1500)
        require(snapshot() == before, 'Full seed database snapshot changed after repeat seed/restart')
        report.update(result='passed', repeat_safe='passed', restart_persistence='passed', artwork_files=len(manifest['files']),
                      people=len(before['person']), credits=len(before['credit']), seed_members=len(before['seed_member']))
    except Exception as error:
        report['failure_type'] = type(error).__name__
        raise
    finally:
        output.write_text(json.dumps(report, indent=2) + '\n')


if __name__ == '__main__':
    main()
