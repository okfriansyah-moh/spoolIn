"""Remote-executable rules for bounded seed HTTP failure evidence."""
import importlib.util
import io
import json
import os
from pathlib import Path
import unittest
from unittest.mock import patch
import urllib.error

spec = importlib.util.spec_from_file_location('seed_runtime', Path(__file__).with_name('verify-seed-runtime.py'))
seed = importlib.util.module_from_spec(spec)
spec.loader.exec_module(seed)


class SeedHttpEvidenceRulesTest(unittest.TestCase):
    def setUp(self):
        self.previous = dict(seed.PROGRESS)
        seed.PROGRESS.update(phase='api-order', apiRequests=0, completedRecords=0, throttledRequests=0)

    def tearDown(self):
        seed.PROGRESS.clear()
        seed.PROGRESS.update(self.previous)

    def test_all_frozen_records_use_only_declared_public_source_properties(self):
        manifest = json.loads((seed.ROOT / 'data/seed/manifest.json').read_text())
        self.assertEqual(200, len(manifest['records']))
        with_end = 0
        for record in manifest['records']:
            projected = seed.public_field_sources(record)
            self.assertEqual({row['field'] for row in record['sources']}, set(projected))
            with_end += 'end' in projected
            for original in record['sources']:
                actual = projected[original['field']]
                self.assertEqual({'url', 'retrievedAt', 'reuseBasis', 'precision'}, set(actual))
                self.assertEqual(original['url'], actual['url'])
                self.assertEqual(original['permissionBasis'], actual['reuseBasis'])
                self.assertEqual(original['retrievedAt'], actual['retrievedAt'])
                self.assertEqual(original.get('precision'), actual['precision'])
        self.assertEqual(100, with_end)

    def test_public_source_timestamp_matches_instant_fraction_and_preserves_storage_row(self):
        row = {'field': 'end', 'url': 'https://example.invalid/evidence', 'retrievedAt': '2025-01-01T00:00:00.120000Z',
               'permissionBasis': 'approved facts', 'license': 'private-ledger-license', 'precision': 'YEAR'}
        before = dict(row)
        actual = seed.public_field_sources({'sources': [row]})['end']
        self.assertEqual('2025-01-01T00:00:00.120Z', actual['retrievedAt'])
        self.assertEqual(before, row)
        self.assertNotIn('license', actual)
        with self.assertRaises(ValueError):
            seed.public_field_sources({'sources': [row, dict(row)]})

    def test_http_failure_retains_status_phase_and_public_code_without_private_data(self):
        body = json.dumps({'errors': [{'message': 'private credential', 'extensions': {'code': 'UNAVAILABLE'}}]}).encode()
        failure = urllib.error.HTTPError('https://private.example/graphql?bearer=secret', 503, 'private', {}, io.BytesIO(body))
        with patch.dict(os.environ, {'SERVICE_CA_FILE': '/protected/ca'}), \
                patch.object(seed.ssl, 'create_default_context'), \
                patch.object(seed.urllib.request, 'urlopen', side_effect=failure):
            with self.assertRaises(seed.SeedApiFailure) as raised:
                seed.graphql('query SeedOrder { titles(filter:{},first:100){nodes{id}} }', {})
        observed = raised.exception.observed
        self.assertEqual(503, observed['httpStatus'])
        self.assertEqual('api-order', observed['phase'])
        self.assertEqual('SeedOrder', observed['operation'])
        self.assertEqual(['UNAVAILABLE'], observed['errorCodes'])
        self.assertEqual(1, observed['apiRequests'])
        self.assertNotIn('private', json.dumps(observed))
        self.assertNotIn('secret', json.dumps(observed))

    def test_unknown_codes_and_non_scalar_extension_values_do_not_escape(self):
        observed = seed.api_observation(400, {'errors': [
            {'extensions': {'code': ['private']}}, {'extensions': {'code': 'secret'}},
            {'extensions': {'code': 'FIELD_INVALID'}, 'path': ['private']},
        ], 'problem': {'code': {'private': 'value'}}})
        self.assertEqual(['FIELD_INVALID'], observed['errorCodes'])
        self.assertFalse(observed['rawBodyRetained'])
        self.assertNotIn('private', json.dumps(observed))
        self.assertIsNone(seed.api_observation(True, None)['httpStatus'])

    def test_oversized_and_non_json_bodies_remain_bounded_and_are_not_published(self):
        oversized = seed.bounded_json(io.BytesIO(b'x' * 131073))
        self.assertEqual({'problem': {'code': 'REQUEST_TOO_LARGE'}}, oversized)
        self.assertIsNone(seed.bounded_json(io.BytesIO(b'private non-JSON framework response')))

    def test_success_status_with_graphql_errors_still_fails_full_comparison(self):
        class Response(io.BytesIO):
            status = 200
        response = Response(json.dumps({'errors': [{'extensions': {'code': 'FIELD_INVALID'}}]}).encode())
        seed.PROGRESS['phase'] = 'api-records'
        seed.PROGRESS['completedRecords'] = 7
        with patch.dict(os.environ, {'SERVICE_CA_FILE': '/protected/ca'}), \
                patch.object(seed.ssl, 'create_default_context'), \
                patch.object(seed.urllib.request, 'urlopen', return_value=response):
            with self.assertRaises(seed.SeedApiFailure) as raised:
                seed.graphql('query SeedTitle($id:UUID!){title(id:$id){id}}', {'id': 'public-fixture-id'})
        self.assertEqual(200, raised.exception.observed['httpStatus'])
        self.assertEqual('SeedTitle', raised.exception.observed['operation'])
        self.assertEqual(7, raised.exception.observed['completedRecords'])


if __name__ == '__main__':
    unittest.main()
