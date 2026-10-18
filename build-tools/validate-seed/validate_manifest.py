#!/usr/bin/env python3
"""Structural preflight for frozen seed identities, hashes, source rules and relationships.

Current effective input policy is enforced by CatalogSeedLoader for fresh application
and by the Catalog CLI --validate-only mode for preparation, never by guessed defaults here.
"""
from __future__ import annotations
import hashlib
import json
import struct
import sys
import uuid
from collections import Counter
from datetime import date, datetime
from pathlib import Path

EXCLUDED = {'generatedAt', 'retrievedAt', 'snapshotSHA256', 'contentSHA256'}


def stable(value):
    if isinstance(value, dict):
        return {key: stable(item) for key, item in sorted(value.items()) if key not in EXCLUDED}
    if isinstance(value, list):
        return [stable(item) for item in value]
    return value


def checksum(value):
    return hashlib.sha256(json.dumps(stable(value), sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()).hexdigest()


def identity(namespace, source, kind, external):
    values = [part.encode() for part in (source, kind, external)]
    name = b''.join(struct.pack('>I', len(part)) + part for part in values)
    raw = bytearray(hashlib.sha1(uuid.UUID(namespace).bytes + name).digest()[:16])
    raw[6], raw[8] = (raw[6] & 15) | 80, (raw[8] & 63) | 128
    return str(uuid.UUID(bytes=bytes(raw)))


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def text(value):
    require(isinstance(value, str) and bool(value.strip()), 'Missing seed text')
    require(not any(ord(char) == 0 or 0xD800 <= ord(char) <= 0xDFFF for char in value), 'Invalid seed Unicode')


def sources(node, expected):
    rows = node.get('sources', [])
    require(set(row['field'] for row in rows) >= expected, 'Missing field provenance')
    for row in rows:
        require(row['url'].startswith('https://'), 'Unsupported source URL')
        require(row['license'] in {'CC0-1.0', 'not-specified'} and row['permissionBasis'], 'Missing fact reuse permission')
        datetime.fromisoformat(row['retrievedAt'].replace('Z', '+00:00'))


def validate_date(value):
    year, month, day = value['year'], value['month'], value['day']
    require(1 <= year <= 9999, 'Invalid seed year')
    require(month is None or 1 <= month <= 12, 'Invalid seed month')
    require(day is None or month is not None, 'Day requires month')
    date(year, month or 1, day or 1)
    require(value['precision'] == ('DAY' if day else 'MONTH' if month else 'YEAR'), 'Incorrect date precision')


def validate_identity(root, row, kind):
    require(row['source'] == 'wikidata' and bool(row['externalId']), 'Missing Wikidata identity')
    require(row['stableUUID'] == identity(root['namespaceUUID'], row['source'], kind, row['externalId']), 'Stable UUID mismatch')


def validate_record(root, row, people, roles):
    validate_identity(root, row, row['kind'])
    require(row['contentSHA256'] == checksum(row), 'Record checksum mismatch')
    sources(row, {'title', 'synopsis', 'release', 'genres'})
    fields = row['normalized']
    text(fields['title'])
    text(fields['synopsis'])
    require(fields['synopsisLanguage'] == 'en', 'Invalid original synopsis language')
    validate_date(fields['release'])
    if row['kind'] == 'TV':
        require(fields['tvStatus'] in {'CONTINUING', 'ENDED', 'CANCELLED', 'UNKNOWN'}, 'Invalid TV status')
        if fields['end'] is not None:
            validate_date(fields['end'])
    genres = row['genres']
    require(1 <= len(genres) <= 20, 'Missing or excessive genres')
    require(fields['genreIds'] == [genre['stableUUID'] for genre in genres], 'Genre reference mismatch')
    require(len(set(fields['genreIds'])) == len(genres), 'Duplicate genre')
    for genre in genres:
        validate_identity(root, genre, 'GENRE')
        sources(genre, {'label'})
        require(bool(genre['label'].strip()), 'Missing genre label')
    for credit in row['credits']:
        validate_identity(root, credit, 'CREDIT')
        require(credit['personExternalId'] in people and credit['roleExternalId'] in roles, 'Dangling credit reference')
        sources(credit, {'person', 'role'})
        require(isinstance(credit['characters'], list), 'Invalid credit characters')
    if row['artwork'] is not None:
        validate_artwork(row['artwork'])
        for field, prefix in [('assetId', 'SEED_ARTWORK:'), ('holdId', 'SEED_ARTWORK_REFERENCE:')]:
            expected = uuid.uuid5(uuid.UUID(root['namespaceUUID']), prefix + row['stableUUID'] + ':' + row['artwork']['sha256'])
            require(row['artwork'][field] == str(expected), 'Poster identity mismatch')


def validate_artwork(artwork):
    require(artwork['file'].startswith('posters/') and '..' not in artwork['file'], 'Invalid poster path')
    require(artwork['mimeType'] == 'image/jpeg', 'Invalid poster format')
    require(64 == len(artwork['sha256']) and all(c in '0123456789abcdef' for c in artwork['sha256']), 'Invalid poster hash')
    require(0 < artwork['width'] < artwork['height'] <= 10000, 'Invalid poster dimensions')
    for field in ('assetId', 'holdId'):
        uuid.UUID(artwork[field])
    for field in ('sourceUrl', 'imageUrl'):
        require(artwork[field].startswith('https://'), 'Invalid poster source')
    text(artwork['sourceName'])
    datetime.fromisoformat(artwork['retrievedAt'].replace('Z', '+00:00'))


def jpeg_dimensions(data):
    offset = 2
    while offset + 4 <= len(data):
        require(data[offset] == 255, 'Invalid JPEG marker')
        marker = data[offset + 1]
        offset += 2
        if marker in {0xD8, 0xD9}:
            continue
        size = int.from_bytes(data[offset:offset + 2], 'big')
        require(size >= 2 and offset + size <= len(data), 'Invalid JPEG segment')
        if marker in {0xC0, 0xC1, 0xC2}:
            require(size >= 8, 'Invalid JPEG frame')
            return int.from_bytes(data[offset + 5:offset + 7], 'big'), int.from_bytes(data[offset + 3:offset + 5], 'big')
        offset += size
    raise ValueError('Missing JPEG frame')


def validate_files(root, directory):
    for item in root['files']:
        path = directory / item['file']
        require(path.is_file(), 'Missing poster file')
        require(path.resolve().is_relative_to(directory.resolve()), 'Poster escapes package')
        data = path.read_bytes()
        require(len(data) <= 8000000 and data.startswith(b'\xff\xd8') and data.endswith(b'\xff\xd9'), 'Invalid JPEG file')
        require(hashlib.sha256(data).hexdigest() == item['sha256'], 'Poster file checksum mismatch')
        require(jpeg_dimensions(data) == (item['width'], item['height']), 'Poster dimensions mismatch')


def validate(root, directory=None):
    require(root['schemaVersion'] == 2, 'Legacy synthetic seed schema is not accepted')
    require(root['sourcePolicyVersion'] and root['manifestVersion'] and root['licenseSummary'], 'Missing seed metadata')
    require(root['snapshotSHA256'] == checksum(root), 'Snapshot checksum mismatch')
    require(root['expectedMovieCount'] == 100 and root['expectedTVCount'] == 100, 'Expected counts differ')
    records = root['records']
    require(Counter(row['kind'] for row in records) == {'MOVIE': 100, 'TV': 100}, 'Requires exactly 100 movies and 100 TV entries')
    require(len({row['externalId'] for row in records}) == 200, 'Duplicate title source identity')
    people = {person['externalId']: person for person in root['people']}
    roles = {role['externalId']: role for role in root['roles']}
    require(len(people) == len(root['people']) and len(roles) == 4, 'Duplicate person or missing starter roles')
    require({role['label'] for role in roles.values()} == {'Actor', 'Director', 'Writer', 'Producer'}, 'Incorrect starter roles')
    for person in people.values():
        validate_identity(root, person, 'PERSON')
        require(person['contentSHA256'] == checksum(person), 'Person checksum mismatch')
        text(person['name'])
        sources(person, {'name'})
    for role in roles.values():
        validate_identity(root, role, 'ROLE')
        sources(role, {'label', 'category', 'allowsCharacters'})
        require(role['category'] == ('CAST' if role['allowsCharacters'] else 'CREATOR'), 'Invalid contribution category')
    for row in records:
        validate_record(root, row, people, roles)
    expected = {'years': dict(sorted(Counter(str(row['normalized']['release']['year']) for row in records).items())), 'months': dict(sorted(Counter(str(row['normalized']['release']['month'] or 'unknown') for row in records).items()))}
    require(root['distribution'] == expected, 'Distribution does not match complete dataset')
    artwork = [dict(row['artwork'], titleId=row['stableUUID']) for row in records if row['artwork'] is not None]
    require(root['files'] == artwork, 'Artwork inventory mismatch')
    require(not artwork or len(artwork) == 200, 'Requires complete poster package')
    if directory is not None:
        validate_files(root, Path(directory))
    return {'movies': 100, 'series': 100, 'people': len(people), 'credits': sum(len(row['credits']) for row in records), 'artworkFiles': len(artwork), 'snapshotSHA256': root['snapshotSHA256'], 'distribution': expected, 'validationScope': 'structure_provenance_hash', 'effectivePolicyVerified': False}


def main():
    arguments = [argument for argument in sys.argv[1:] if argument != '--structural-only']
    path = Path(arguments[0]) if arguments else Path(__file__).resolve().parents[2] / 'data/seed/manifest.json'
    if len(arguments) > 1:
        print('Seed structural preflight accepts one manifest path', file=sys.stderr)
        return 2
    try:
        result = validate(json.loads(path.read_text(encoding='utf-8')), path.parent)
    except (ValueError, KeyError, TypeError) as failure:
        print(f'Seed validation failed: {failure}', file=sys.stderr)
        return 1
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
