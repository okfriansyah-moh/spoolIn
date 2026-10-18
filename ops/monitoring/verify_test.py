"""Remote-executable unit checks for bounded alert evidence projection."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("monitoring_verify", Path(__file__).with_name("verify.py"))
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)


class AlertEvidenceRulesTest(unittest.TestCase):
    def test_actual_status_and_fixed_codes_are_retained_without_private_response(self):
        result = verify.safe_alert_probe({"httpStatus": 403, "publicCode": "FORBIDDEN", "body": "private credential",
                                          "message": "private URL", "transportCode": "private hostname"})
        self.assertEqual({"httpStatus": 403, "publicCode": "FORBIDDEN"}, result)
        self.assertEqual({}, verify.safe_alert_probe({"httpStatus": True, "publicCode": "private"}))
        self.assertEqual({"transportCode": "ETIMEDOUT"}, verify.safe_alert_probe({"transportCode": "ETIMEDOUT"}))

    def test_actual_metrics_aggregate_only_observed_finite_counters_without_receiver_labels(self):
        body = '\n'.join([
            'alertmanager_notifications_failed_total{integration="webhook",receiver="private"} 2',
            'alertmanager_notifications_failed_total{integration="email",receiver="secret"} 3',
            'alertmanager_notifications_total{integration="webhook"} 8',
            'alertmanager_notifications_requests_total NaN',
            'private_metric{credential="secret"} 999',
        ])
        observed = verify.alertmanager_metric_values(body)
        self.assertEqual({"alertmanager_notifications_failed_total": 5, "alertmanager_notifications_total": 8},
                         observed["values"])
        self.assertIn("alertmanager_notifications_requests_total", observed["absent"])
        self.assertNotIn("private", str(observed))
        self.assertNotIn("secret", str(observed))
        self.assertEqual({}, verify.alertmanager_metric_values('unrelated_metric 1')["values"])

    def test_failed_case_retains_actual_counts_and_does_not_become_a_pass(self):
        previous = list(verify.CASES)
        verify.CASES.clear()
        observed = {"durableCounts": {"firing": 0, "resolved": 0, "keys": 0, "recorded": None}}
        try:
            def fail():
                raise verify.AlertEvidenceFailure("Actual episode absent", observed)
            verify.case("MON-ALERT-EPISODE", "actual durable database", fail)
            self.assertEqual("failed", verify.CASES[0]["result"])
            self.assertEqual(observed, verify.CASES[0]["observed"])
        finally:
            verify.CASES[:] = previous

    def test_timeout_retains_observed_counts_and_real_failed_delivery_counters(self):
        counts = {"firing": 0, "resolved": 0, "keys": 0, "recorded": None}
        metrics = {"values": {"alertmanager_notifications_failed_total": 1}, "absent": []}
        with patch.object(verify.time, "monotonic", side_effect=[0, 1, 66]), \
                patch.object(verify.time, "sleep"), patch.object(verify, "evidence_counts", return_value=counts), \
                patch.object(verify, "alertmanager_metrics", return_value=metrics):
            with self.assertRaises(verify.AlertEvidenceFailure) as raised:
                verify.wait_counts("2026-10-18T00:00:00Z", {"firing": 1})
        self.assertEqual(counts, raised.exception.observed["durableCounts"])
        self.assertEqual(metrics, raised.exception.observed["alertmanagerMetrics"])


if __name__ == '__main__':
    unittest.main()
