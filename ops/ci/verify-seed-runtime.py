#!/usr/bin/env python3
"""Compare the complete frozen seed with live database and HTTPS GraphQL results."""
import datetime
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
PROGRESS = {'phase': 'initialization', 'apiRequests': 0, 'completedRecords': 0, 'throttledRequests': 0}
SAFE_CODES = {'FIELD_INVALID', 'UNAUTHENTICATED', 'FORBIDDEN', 'NOT_FOUND', 'STALE_VERSION',
              'POLICY_CHANGED', 'IDEMPOTENCY_CONFLICT', 'RESOURCE_EXHAUSTED', 'DEADLINE_EXCEEDED',
              'UNAVAILABLE', 'RATE_LIMITED', 'GRAPHQL_VALIDATION_FAILED', 'BAD_USER_INPUT',
              'INVALID_REQUEST', 'REQUEST_TOO_LARGE', 'INTERNAL_ERROR', 'INTERNAL'}
TABLES = ('title', 'person', 'genre', 'credit_role', 'credit', 'title_genre', 'provenance_record', 'seed_member', 'seed_artwork_attribution')
TITLE_FIELDS = '''id kind state title originalTitle synopsis synopsisLanguage originalLanguage countries
release {year month day precision} tvEnd {year month day precision} tvStatus releaseContext
runtimeMinutes genres {id label} artwork {assetId url width height}
credits {nodes {id personId personName titleId titleName role {id label category allowsCharacters} characters version}
pageInfo {hasNextPage}} provenance {source externalId recordUrl reuseNote fieldSources locallyChanged}'''


class SeedApiFailure(RuntimeError):
    def __init__(self, observed):
        super().__init__('Seed API comparison failed; inspect bounded report')
        self.observed = observed


def api_observation(status, body):
    codes = set()
    if isinstance(body, dict):
        errors = body.get('errors')
        if isinstance(errors, list):
            for error in errors[:200]:
                extensions = error.get('extensions') if isinstance(error, dict) else None
                value = extensions.get('code') if isinstance(extensions, dict) else None
                if isinstance(value, str) and value in SAFE_CODES:
                    codes.add(value)
        problem = body.get('problem')
        if isinstance(problem, dict) and isinstance(problem.get('code'), str) and problem['code'] in SAFE_CODES:
            codes.add(problem['code'])
    status = status if isinstance(status, int) and not isinstance(status, bool) and 100 <= status <= 599 else None
    operation = {'api-order': 'SeedOrder', 'api-records': 'SeedTitle'}.get(PROGRESS['phase'], 'NONE')
    return {'httpStatus': status, 'operation': operation, 'errorCodes': sorted(codes), 'rawBodyRetained': False,
            'phase': PROGRESS['phase'], 'apiRequests': PROGRESS['apiRequests'],
            'completedRecords': PROGRESS['completedRecords']}


def bounded_json(response):
    try:
        raw = response.read(131073)
    except OSError:
        return None
    if len(raw) > 131072:
        return {'problem': {'code': 'REQUEST_TOO_LARGE'}}
    try:
        return json.loads(raw)
    except (ValueError, UnicodeDecodeError):
        return None


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
              'tv_status': values.get('tvStatus'), 'asset_id': node['artwork']['assetId'],
              'attachment_id': node['artwork']['holdId'], 'version': 1,
              'original_title': values.get('originalTitle'), 'original_language': values.get('originalLanguage'),
              'runtime_minutes': values.get('runtimeMinutes'), 'countries': values.get('countries', [])}
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
    attribution = {row['title_id']: row for row in database['seed_artwork_attribution']}
    require(len(attribution) == 200, 'Expected200 poster attribution records')
    for node in manifest['records']:
        compare_fields(attribution[node['stableUUID']], {'asset_id': node['artwork']['assetId'],
                       'attribution': node['artwork']}, node['stableUUID'])


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
    request = urllib.request.Request(endpoint, data=payload, headers={'Content-Type': 'application/json', 'Origin': endpoint.removesuffix('/graphql'), 'X-Spoolin-Client': 'web'})
    for attempt in range(3):
        try:
            PROGRESS['apiRequests'] += 1
            with urllib.request.urlopen(request, context=context, timeout=20) as response:
                body = bounded_json(response)
                observed = api_observation(response.status, body)
            if not isinstance(body, dict) or body.get('errors') or not isinstance(body.get('data'), dict):
                raise SeedApiFailure(observed)
            return body['data']
        except urllib.error.HTTPError as error:
            try:
                observed = api_observation(error.code, bounded_json(error))
            finally:
                error.close()
            observed['attempt'] = attempt + 1
            if error.code == 429:
                PROGRESS['throttledRequests'] += 1
            if error.code != 429 or attempt == 2:
                raise SeedApiFailure(observed) from None
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
    PROGRESS['phase'] = 'api-order'
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


def public_field_sources(node):
    result = {}
    for row in node['sources']:
        at = datetime.datetime.fromisoformat(row['retrievedAt'].replace('Z', '+00:00'))
        at = at.astimezone(datetime.timezone.utc)
        timestamp = at.strftime('%Y-%m-%dT%H:%M:%S')
        fraction = f'{at.microsecond:06d}'
        if at.microsecond:
            timestamp += '.' + (fraction[:3] if at.microsecond % 1000 == 0 else fraction)
        result[row['field']] = {'url': row['url'], 'retrievedAt': timestamp + 'Z',
                                'reuseBasis': row.get('permissionBasis', row.get('reuseBasis')),
                                'precision': row.get('precision')}
    require(len(result) == len(node['sources']), 'Seed source fields must be unique')
    return result


def verify_poster(actual, expected):
    require(isinstance(actual, dict), 'Seed poster missing from public title')
    compare_fields(actual, {'assetId': expected['assetId'], 'width': expected['width'],
                           'height': expected['height'], 'url': '/artwork/' + expected['assetId']}, 'poster')
    endpoint = 'https://localhost:' + os.environ.get('CORE_HTTPS_PORT', '8443') + actual['url']
    context = ssl.create_default_context(cafile=os.environ['SERVICE_CA_FILE'])
    with urllib.request.urlopen(endpoint, context=context, timeout=20) as response:
        require(response.status == 200, 'Poster response unsuccessful')
        require(response.headers.get_content_type() == 'image/jpeg', 'Poster response type differs')
        body = response.read(5242881)
        require(0 < len(body) <= 5242880, 'Poster response exceeds bounded image size')
        require(body.startswith(b'\xff\xd8\xff') and body.endswith(b'\xff\xd9'), 'Poster response is not a complete JPEG')


def verify_api(manifest, role_ids, results=None):
    PROGRESS['phase'] = 'api-records'
    if results is None:
        results = []
    query = 'query SeedTitle($id:UUID!){title(id:$id){' + TITLE_FIELDS + '}}'
    for node in manifest['records']:
        actual = graphql(query, {'id': node['stableUUID']})['title']
        values = node['normalized']
        compare_fields(actual, {'id': node['stableUUID'], 'kind': node['kind'], 'state': 'ACTIVE',
                       'title': values['title'], 'synopsis': values['synopsis'], 'synopsisLanguage': values['synopsisLanguage'],
                       'release': values['release'], 'tvStatus': values.get('tvStatus'),
                       'originalTitle': values.get('originalTitle'), 'originalLanguage': values.get('originalLanguage'),
                       'runtimeMinutes': values.get('runtimeMinutes'), 'countries': values.get('countries', [])}, node['stableUUID'])
        require({row['id'] for row in actual['genres']} == set(values['genreIds']), 'API genre identities differ')
        verify_api_credits(actual, node, manifest, role_ids)
        require(len(actual['provenance']) == 2, 'API metadata/poster provenance missing')
        source = next(row for row in actual['provenance'] if row['source'] == node['source'])
        compare_fields(source, {'source': node['source'], 'externalId': node['externalId'],
                       'fieldSources': public_field_sources(node), 'locallyChanged': []}, node['stableUUID'])
        poster = next(row for row in actual['provenance'] if row['source'] == 'poster')
        compare_fields(poster, {'externalId': node['artwork']['assetId'],
                       'recordUrl': node['artwork']['sourceUrl'],
                       'fieldSources': public_field_sources({'sources': [{'field': 'artwork', 'url': node['artwork']['sourceUrl'], 'retrievedAt': node['artwork']['retrievedAt'], 'reuseBasis': 'Public poster source recorded; no open-license claim.'}]})}, node['stableUUID'])
        verify_poster(actual['artwork'], node['artwork'])
        results.append({'id': node['stableUUID'], 'kind': node['kind'], 'database': 'pass', 'https_api': 'pass',
                        'credit_count': len(node['credits']), 'artwork': 'packaged poster served over HTTPS'})
        PROGRESS['completedRecords'] = len(results)
        time.sleep(1.1)
    return results


def main():
    require(os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ.get('RUNNER_ENVIRONMENT') == 'github-hosted', 'Hosted CI required')
    require(os.environ['SPOOLIN_INSTALLATION'] == 'ci-' + os.environ['GITHUB_RUN_ID'] + '-' + os.environ['GITHUB_RUN_ATTEMPT'], 'Owned initial namespace required')
    manifest = json.loads((ROOT / 'data/seed/manifest.json').read_text())
    spec = importlib.util.spec_from_file_location('seed_validator', ROOT / 'build-tools/validate-seed/validate_manifest.py')
    validator = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(validator)
    report = {'tested_sha': os.environ['TESTED_SHA'], 'run_id': os.environ['GITHUB_RUN_ID'], 'snapshot': manifest['snapshotSHA256'], 'result': 'failed', 'records': []}
    output = ROOT / 'artifacts/runtime/seed-verification.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    try:
        PROGRESS['phase'] = 'database-comparison'
        before = snapshot()
        verify_titles(manifest, before)
        roles = verify_people_credits(manifest, before, validator)
        verify_provenance(manifest, before, validator, roles)
        report['database_comparison'] = {'result': 'passed', 'movies': 100, 'series': 100,
                                         'people': len(before['person']), 'credits': len(before['credit']),
                                         'seed_members': len(before['seed_member'])}
        verify_api_order(manifest)
        verify_api(manifest, roles, report['records'])
        for label, arguments in (('repeat-seed', ['seed']), ('seed-restart', ['restart'])):
            PROGRESS['phase'] = label
            with (output.parent / (label + '.log')).open('w') as log:
                subprocess.run(['make', *arguments], env={**os.environ, 'SPOOLIN_SEED_APPLY': '1'}, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=1500)
        PROGRESS['phase'] = 'restart-database-comparison'
        require(snapshot() == before, 'Full seed database snapshot changed after repeat seed/restart')
        report.update(result='passed', repeat_safe='passed', restart_persistence='passed', artwork_files=len(manifest['files']),
                      people=len(before['person']), credits=len(before['credit']), seed_members=len(before['seed_member']))
    except Exception as error:
        report['failure_type'] = type(error).__name__
        if isinstance(error, SeedApiFailure):
            report['api_failure'] = error.observed
        raise RuntimeError('Full seed verification failed; inspect bounded report') from None
    finally:
        report['progress'] = dict(PROGRESS)
        output.write_text(json.dumps(report, indent=2) + '\n')


if __name__ == '__main__':
    main()
