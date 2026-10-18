#!/usr/bin/env python3
"""Require every private service to report readiness using mutual TLS."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import MagicMock, patch

SPEC = importlib.util.spec_from_file_location('local_readiness', Path(__file__).with_name('readiness.py'))
READINESS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(READINESS)


class ReadinessTest(unittest.TestCase):
    def test_checks_all_three_service_endpoints(self):
        response = MagicMock()
        response.__enter__.return_value.read.return_value = b'{"status":"UP"}'
        with patch.object(READINESS.ssl, 'create_default_context') as context, patch.object(READINESS.urllib.request, 'urlopen', return_value=response) as request:
            READINESS.main()
        context.assert_called_once_with(cafile='/run/secrets/service-ca')
        context.return_value.load_cert_chain.assert_called_once_with('/run/secrets/core-cert', '/run/secrets/core-key')
        self.assertEqual(['https://core:8081/actuator/health/readiness', 'https://catalog:8081/actuator/health/readiness', 'https://media:8081/actuator/health/readiness'], [call.args[0].full_url for call in request.call_args_list])

    def test_down_service_prevents_success(self):
        response = MagicMock()
        response.__enter__.return_value.read.return_value = b'{"status":"DOWN"}'
        with patch.object(READINESS.ssl, 'create_default_context'), patch.object(READINESS.urllib.request, 'urlopen', return_value=response):
            with self.assertRaisesRegex(RuntimeError, 'core is not ready'):
                READINESS.main()


if __name__ == '__main__':
    unittest.main()
