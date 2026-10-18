#!/usr/bin/env python3
"""Pure diagnostic redaction/bounds and unsafe-container rejection; run on remote CI."""
import copy
import importlib.util
import json
from pathlib import Path
import sys
import unittest


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


DIAG = load('codec_diagnostics_tested', 'codec-diagnostics.py')
PROBE = load('codec_probe_tested', 'probe-media-codec.py')


class DiagnosticRules(unittest.TestCase):
    def test_only_finite_kernel_fields_are_retained(self):
        raw = '\n'.join((
            'apparmor="DENIED" operation="capable" profile="bwrap" capname="net_admin" pid=123 name="private-input" token="private-token"',
            'apparmor="DENIED" operation="capable" profile="bwrap" capname="net_admin" secret="private-value"',
            'apparmor="DENIED" operation="unrecognized-private" profile="secret-profile" capname="secret-capability"',
            'apparmor="ALLOWED" operation="capable" profile="unconfined" capname="sys_admin"',
        ))
        result = DIAG.denied_fields(raw)
        self.assertEqual([{'operation': 'capable', 'profile': 'bwrap', 'capability': 'net_admin', 'count': 2},
                          {'operation': 'other', 'profile': 'other', 'capability': 'other', 'count': 1}], result['events'])
        self.assertNotIn('private', json.dumps(result))
        self.assertNotIn('secret', json.dumps(result))
        self.assertNotIn('123', json.dumps(result))

    def test_context_and_loaded_profile_projection_excludes_arbitrary_values(self):
        status = DIAG.status_fields('Name:\tprivate\nCapEff:\t0000000000000000\nCapBnd:\t0000000000000000\nNoNewPrivs:\t1\nSeccomp:\t2\nUid:\t123\nCapEff:\tprivate')
        self.assertEqual({'CapEff': '0000000000000000', 'CapBnd': '0000000000000000', 'NoNewPrivs': '1', 'Seccomp': '2'}, status)
        self.assertEqual([{'profile': 'bwrap', 'mode': 'enforce'}], DIAG.loaded_profiles('/usr/bin/bwrap (enforce)\nsecret-profile (enforce)'))
        self.assertEqual('other', DIAG.profile_name('private-profile (enforce)'))

    def test_capture_is_bounded_and_timeout_is_an_observed_failure(self):
        state, content = DIAG.bounded_command([sys.executable, '-c', 'import sys;sys.stdout.write("x"*1000000)'], limit=512)
        self.assertTrue(state['truncated'])
        self.assertEqual(512, len(content))
        state, content = DIAG.bounded_command([sys.executable, '-c', 'import time;time.sleep(5)'], seconds=0.1)
        self.assertTrue(state['timedOut'])
        self.assertNotEqual(0, state['exit'])
        self.assertEqual('', content)

    def fixture(self):
        return {'image': 'sha256:' + 'a' * 64, 'user': '10001:10001', 'readOnly': True, 'capDrop': ['ALL'], 'capAdd': None,
                'privileged': False, 'securityOpt': ['no-new-privileges:true'], 'apparmor': 'docker-default',
                'networkMode': 'ci-123-1_private', 'mounts': [{'Destination': '/var/spoolin/media', 'Source': '/private/owned-volume',
                                                         'Type': 'volume', 'RW': True}]}

    def test_real_effective_config_must_match_and_no_source_paths_are_published(self):
        primary = self.fixture()
        actual = PROBE.effective_configuration(primary, copy.deepcopy(primary))
        self.assertNotIn('/private/owned-volume', json.dumps(actual))
        for field, value in (('user', '0:0'), ('privileged', True), ('readOnly', False), ('capAdd', ['NET_ADMIN']),
                             ('capDrop', []), ('securityOpt', ['seccomp=unconfined']), ('networkMode', 'host'),
                             ('image', 'other-image'), ('apparmor', 'unconfined')):
            broken = copy.deepcopy(primary)
            broken[field] = value
            with self.assertRaises(RuntimeError, msg=field):
                PROBE.effective_configuration(primary, broken)
        broken = copy.deepcopy(primary)
        broken['mounts'][0]['Source'] = '/another-volume'
        with self.assertRaises(RuntimeError):
            PROBE.effective_configuration(primary, broken)

    def test_namespace_diagnostics_reject_content_and_pipeline_uses_fixed_codec_arguments(self):
        self.assertEqual({'child-pid': 123, 'net-namespace': 42}, PROBE.namespaces('{"child-pid":123,"net-namespace":42}'))
        for raw in ('{"private":"secret"}', '{"net-namespace":0}', '{"net-namespace":"42"}', '[]'):
            with self.assertRaises((RuntimeError, AttributeError)):
                PROBE.namespaces(raw)
        script, stages = PROBE.pipeline('/var/spoolin/media/codec-proof-test')
        self.assertEqual(7, len(stages))
        self.assertIn('--unshare-all', script)
        self.assertIn('--remount-ro /', script)
        self.assertIn('--clearenv', script)
        self.assertNotIn('/bin/sh', script)
        self.assertNotIn('--share-net', script)
        self.assertNotIn('--cap-add', script)


if __name__ == '__main__':
    unittest.main()
