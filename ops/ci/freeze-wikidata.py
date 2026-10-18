#!/usr/bin/env python3
"""Freeze permitted Wikidata facts on a hosted GitHub runner; never used by application startup."""
from __future__ import annotations
import hashlib
import json
import os
import struct
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from collections import Counter
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NAMESPACE = uuid.UUID('9da61864-80f5-5c70-bc79-3914da94079e')
API = 'https://www.wikidata.org/w/api.php'
LICENSE = 'CC0-1.0'
SOURCE = 'wikidata'
HEADERS = {'User-Agent': 'SpoolIn-seed-freezer/1.0 (educational frozen CC0 metadata; GitHub Actions)', 'Accept': 'application/json'}
ROLES = {'P161': ('Actor', True), 'P57': ('Director', False), 'P58': ('Writer', False), 'P162': ('Producer', False)}


def serialized(value):
    if isinstance(value, dict):
        return {key: serialized(item) for key, item in sorted(value.items()) if key not in {'generatedAt', 'retrievedAt', 'snapshotSHA256', 'contentSHA256'}}
    if isinstance(value, list):
        return [serialized(item) for item in value]
    return value


def digest(value):
    return hashlib.sha256(json.dumps(serialized(value), ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def identity(kind, external):
    name = b''.join(struct.pack('>I', len(part.encode())) + part.encode() for part in (SOURCE, kind, external))
    raw = bytearray(hashlib.sha1(NAMESPACE.bytes + name).digest()[:16])
    raw[6] = (raw[6] & 15) | 80
    raw[8] = (raw[8] & 63) | 128
    return str(uuid.UUID(bytes=bytes(raw)))


LAST_QUERY = 0.0


def retry_delay(failure, attempt, query_service):
    minimum = 65 if query_service and getattr(failure, 'code', None) == 429 else 2 ** attempt
    header = getattr(failure, 'headers', {}).get('Retry-After')
    if not header:
        return minimum
    try:
        supplied = float(header)
    except ValueError:
        supplied = (parsedate_to_datetime(header) - datetime.now(timezone.utc)).total_seconds()
    return max(minimum, supplied)


def request(url):
    global LAST_QUERY
    started = time.monotonic()
    query_service = urllib.parse.urlparse(url).hostname == 'query.wikidata.org'
    for attempt in range(3):
        if query_service:
            wait = max(0, 65 - (time.monotonic() - LAST_QUERY))
            if wait:
                time.sleep(wait)
            LAST_QUERY = time.monotonic()
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=60) as response:
                body = response.read(16 * 1024 * 1024 + 1)
            if len(body) > 16 * 1024 * 1024:
                raise RuntimeError('Wikidata response exceeded 16 MiB bound')
            return json.loads(body)
        except (urllib.error.URLError, TimeoutError) as failure:
            status = getattr(failure, 'code', 'transport')
            if attempt == 2:
                raise RuntimeError(f'Wikidata endpoint {urllib.parse.urlparse(url).hostname} unavailable after three bounded attempts; status={status}') from failure
            delay = retry_delay(failure, attempt, query_service)
            if time.monotonic() - started + delay + 60 > 360:
                raise RuntimeError(f'Wikidata retry guidance exceeds six-minute request budget; status={status}') from failure
            print(f'Wikidata request status={status}; bounded remote retry in {delay:.0f}s', flush=True)
            time.sleep(delay)


def candidates(kind):
    entity = 'Q11424' if kind == 'MOVIE' else 'Q5398426'
    # Fetch a bounded indexed type set; validate date, genre and English labels from frozen entity facts.
    query = f'SELECT DISTINCT ?item WHERE {{ ?item wdt:P31 wd:{entity} }} LIMIT 500'
    url = 'https://query.wikidata.org/sparql?' + urllib.parse.urlencode({'query': query, 'format': 'json'})
    found = {row['item']['value'].rsplit('/', 1)[1] for row in request(url)['results']['bindings']}
    print(f'Wikidata bounded candidate response: kind={kind}, distinct={len(found)}', flush=True)
    return sorted(found, key=lambda item: int(item[1:]))


def entities(ids):
    result = {}
    ordered = sorted(set(ids))
    for offset in range(0, len(ordered), 40):
        parameters = {'action': 'wbgetentities', 'ids': '|'.join(ordered[offset:offset + 40]), 'props': 'labels|claims|info', 'languages': 'en', 'format': 'json'}
        response = request(API + '?' + urllib.parse.urlencode(parameters))
        result.update(response.get('entities', {}))
        time.sleep(0.2)
    return result


def values(entity, property_id):
    claims = entity.get('claims', {}).get(property_id, [])
    return [claim['mainsnak']['datavalue']['value'] for claim in claims if claim.get('rank') != 'deprecated' and 'datavalue' in claim.get('mainsnak', {})]


def label(entity):
    return entity.get('labels', {}).get('en', {}).get('value')


def date(entity, property_id):
    facts = [value for value in values(entity, property_id) if isinstance(value, dict) and value.get('precision', 0) >= 9 and value.get('calendarmodel', '').endswith('Q1985727')]
    facts.sort(key=lambda value: value['time'])
    if not facts:
        return None
    fact = facts[0]
    parts = fact['time'].lstrip('+').split('T')[0].split('-')
    year = int(parts[0])
    if not 1 <= year <= 9999:
        return None
    precision = min(fact['precision'], 11)
    return {'year': year, 'month': int(parts[1]) if precision >= 10 else None, 'day': int(parts[2]) if precision >= 11 else None, 'precision': {9: 'YEAR', 10: 'MONTH', 11: 'DAY'}[precision]}


def source(entity_id, retrieved, fields):
    return [{'field': field, 'url': f'https://www.wikidata.org/wiki/{"Property:" if entity_id.startswith("P") else ""}{entity_id}', 'retrievedAt': retrieved, 'license': LICENSE, 'permissionBasis': 'Wikidata structured facts released under CC0; synopsis is original factual wording.'} for field in fields]


def record(kind, external, entity, linked, retrieved):
    title = label(entity)
    release = date(entity, 'P577') or (date(entity, 'P580') if kind == 'TV' else None)
    genre_ids = sorted({value['id'] for value in values(entity, 'P136') if isinstance(value, dict) and label(linked.get(value.get('id'), {}))})
    if not title or len(title) > 255 or not release or not genre_ids:
        return None
    genres = [{'source': SOURCE, 'externalId': item, 'stableUUID': identity('GENRE', item), 'label': label(linked[item]), 'sources': source(item, retrieved, ['label'])} for item in genre_ids[:20]]
    credits = []
    for property_id, (role, _) in ROLES.items():
        for person_id in sorted({value['id'] for value in values(entity, property_id) if isinstance(value, dict) and value.get('id')})[:4]:
            person = linked.get(person_id, {})
            if not label(person) or len(label(person)) > 255:
                continue
            credit_id = f'{external}/{property_id}/{person_id}'
            credits.append({'source': SOURCE, 'externalId': credit_id, 'stableUUID': identity('CREDIT', credit_id), 'personExternalId': person_id, 'roleExternalId': property_id, 'characters': [], 'sources': source(external, retrieved, ['person', 'role'])})
    noun = 'film' if kind == 'MOVIE' else 'television series'
    normalized = {'title': title, 'originalTitle': None, 'synopsis': f'{title} is a {noun} first released in {release["year"]}.', 'synopsisLanguage': 'en', 'release': release, 'genreIds': [item['stableUUID'] for item in genres], 'originalLanguage': None, 'countries': [], 'runtimeMinutes': None}
    if kind == 'TV':
        normalized['end'] = date(entity, 'P582')
        normalized['tvStatus'] = 'ENDED' if normalized['end'] else 'UNKNOWN'
    result = {'kind': kind, 'source': SOURCE, 'externalId': external, 'stableUUID': identity(kind, external), 'normalized': normalized, 'genres': genres, 'credits': credits, 'sources': source(external, retrieved, ['title', 'synopsis', 'release', 'genres'] + (['end', 'tvStatus'] if kind == 'TV' else [])), 'artwork': None}
    result['contentSHA256'] = digest(result)
    return result


def main():
    if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted':
        raise SystemExit('Seed freezing is restricted to a verified GitHub-hosted remote runner')
    retrieved = datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')
    selected = {kind: candidates(kind) for kind in ('MOVIE', 'TV')}
    titles = entities(selected['MOVIE'] + selected['TV'])
    linked_ids = []
    for entity in titles.values():
        for property_id in ['P136', *ROLES]:
            linked_ids.extend(value['id'] for value in values(entity, property_id) if isinstance(value, dict) and value.get('id'))
    linked = entities(linked_ids)
    records = []
    for kind in ('MOVIE', 'TV'):
        rows = [row for external in selected[kind] if (row := record(kind, external, titles[external], linked, retrieved)) is not None][:100]
        if len(rows) != 100:
            raise RuntimeError(f'Only {len(rows)} compatible {kind} entries; refusing an incomplete snapshot')
        records.extend(rows)
    used_people = {credit['personExternalId'] for row in records for credit in row['credits']}
    people = [{'kind': 'PERSON', 'source': SOURCE, 'externalId': item, 'stableUUID': identity('PERSON', item), 'name': label(linked[item]), 'sources': source(item, retrieved, ['name'])} for item in sorted(used_people)]
    for person in people:
        person['contentSHA256'] = digest(person)
    roles = [{'kind': 'ROLE', 'source': SOURCE, 'externalId': property_id, 'stableUUID': identity('ROLE', property_id), 'label': name, 'category': 'CAST' if characters else 'CREATOR', 'allowsCharacters': characters, 'sources': source(property_id, retrieved, ['label', 'category', 'allowsCharacters'])} for property_id, (name, characters) in ROLES.items()]
    distribution = {'years': dict(sorted(Counter(str(row['normalized']['release']['year']) for row in records).items())), 'months': dict(sorted(Counter(str(row['normalized']['release']['month'] or 'unknown') for row in records).items()))}
    manifest = {'schemaVersion': 2, 'manifestVersion': 'wikidata-2026-10-v1', 'sourcePolicyVersion': 'TD10.1-CC0-v1', 'namespaceUUID': str(NAMESPACE), 'generatedAt': retrieved, 'licenseSummary': 'Wikidata structured facts CC0-1.0; original factual synopses; no licensed artwork bundled; application placeholders are intentional.', 'expectedMovieCount': 100, 'expectedTVCount': 100, 'distribution': distribution, 'records': records, 'people': people, 'roles': roles, 'files': []}
    manifest['snapshotSHA256'] = digest(manifest)
    sources = {'source': SOURCE, 'license': LICENSE, 'licenseURL': 'https://www.wikidata.org/wiki/Wikidata:Licensing', 'retrievedAt': retrieved, 'snapshotSHA256': manifest['snapshotSHA256'], 'artwork': {'included': False, 'reason': 'No individual image redistribution license has been reviewed. Approved optional artwork uses application placeholders.'}, 'entities': [{'externalId': item, 'revision': titles.get(item, linked.get(item, {})).get('lastrevid'), 'url': f'https://www.wikidata.org/wiki/{item}'} for item in sorted({row['externalId'] for row in records} | used_people | {genre['externalId'] for row in records for genre in row['genres']})]}
    for relative, value in [('data/seed/manifest.json', manifest), ('data/seed/sources/wikidata.json', sources)]:
        path = ROOT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'snapshotSHA256': manifest['snapshotSHA256'], 'movies': 100, 'series': 100, 'people': len(people), 'credits': sum(len(row['credits']) for row in records), 'artworkFiles': 0}))


if __name__ == '__main__':
    main()
