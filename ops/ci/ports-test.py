#!/usr/bin/env python3
"""Remote-only port contract regressions; actual bindings are inspected by make start."""
import importlib.util
import os
from pathlib import Path
import socket
import unittest

spec = importlib.util.spec_from_file_location("ports", Path(__file__).resolve().parents[1] / "bin/ports.py")
ports = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ports)


def container(project="owned", service="core", address="127.0.0.1", port=8443, target="8443/tcp", running=True):
    return {"State": {"Running": running}, "Config": {"Labels": {"com.docker.compose.project": project,
            "com.docker.compose.service": service}}, "NetworkSettings": {"Ports": {target: [{"HostIp": address, "HostPort": str(port)}]}}}


class PortsTest(unittest.TestCase):
    def test_bounds_duplicates_empty_whitespace_and_default_contract(self):
        expected = ports.expected_ports({})
        self.assertEqual({8443, 8787, 9090, 9093, 3000}, {pair[0] for pair in expected.values()})
        for value in ("", "0", "65536", " 8443", "8443 ", "01", "-1", "1.5"):
            with self.assertRaises(ValueError):
                ports.expected_ports({"CORE_HTTPS_PORT": value})
        with self.assertRaises(ValueError):
            ports.expected_ports({"CORE_HTTPS_PORT": "8787"})

    def test_owned_exact_binding_permits_idempotent_restart_but_not_foreign_or_changed_binding(self):
        expected = ports.expected_ports({})
        self.assertEqual({8443}, ports.check_existing([container()], "owned", expected))
        for row in (container(project="other"), container(service="mcp"), container(address="0.0.0.0"), container(target="8081/tcp")):
            with self.assertRaises(ValueError):
                ports.check_existing([row], "owned", expected)
        self.assertEqual(set(), ports.check_existing([container(project="other", running=False)], "owned", expected))

    def test_actual_publication_rejects_backend_or_wildcard_bindings(self):
        expected = ports.expected_ports({})
        rows = [container(service=service, port=pair[0], target=str(pair[1]) + "/tcp") for service, pair in expected.items()]
        self.assertEqual(set(expected), set(ports.actual_bindings(rows, "owned", expected)))
        for broken in (rows[:-1], rows + [container(service="postgres", port=5432, target="5432/tcp")],
                       rows + [container(service="catalog", port=18444, target="8081/tcp")], rows + [container(address="::")]):
            with self.assertRaises(ValueError):
                ports.actual_bindings(broken, "owned", expected)

    def test_actual_os_listener_collision_is_refused(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener:
            listener.bind(("127.0.0.1", 0))
            listener.listen(1)
            selected = listener.getsockname()[1]
            with self.assertRaises(ValueError):
                ports.check_available({"test": (selected, selected)}, set())

    def test_resolved_compose_must_publish_exactly_documented_ports(self):
        expected = ports.expected_ports({})
        config = {"networks": {"private": {"driver": "bridge"}}, "services": {
            service: {"ports": [{"published": str(pair[0]), "target": pair[1], "host_ip": "127.0.0.1"}]}
            for service, pair in expected.items()}}
        ports.validate_configuration(config, expected)
        config["networks"]["private"]["internal"] = True
        with self.assertRaises(ValueError):
            ports.validate_configuration(config, expected)
        config["networks"]["private"]["internal"] = False
        config["services"]["core"]["ports"][0]["host_ip"] = "0.0.0.0"
        with self.assertRaises(ValueError):
            ports.validate_configuration(config, expected)


if __name__ == "__main__":
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted":
        raise SystemExit("Requires GitHub-hosted remote execution")
    unittest.main()
