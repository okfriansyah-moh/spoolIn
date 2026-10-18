#!/usr/bin/env python3
"""Validate configured loopback ports and actual Docker ownership without claiming application readiness."""
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import sys

PORTS = {"core": ("CORE_HTTPS_PORT", 8443, 8443), "mcp": ("MCP_HTTP_PORT", 8787, 8787),
         "prometheus": ("PROMETHEUS_HTTP_PORT", 9090, 9090), "alertmanager": ("ALERTMANAGER_HTTP_PORT", 9093, 9093),
         "grafana": ("GRAFANA_HTTPS_PORT", 3000, 3000)}


def expected_ports(environment):
    result = {}
    for service, (name, default, target) in PORTS.items():
        value = environment.get(name, str(default))
        if not re.fullmatch(r"[1-9][0-9]{0,4}", value) or not 1 <= int(value) <= 65535:
            raise ValueError("invalid-configured-port:" + name)
        result[service] = (int(value), target)
    if len({value[0] for value in result.values()}) != len(result):
        raise ValueError("configured-port-collision")
    return result


def validate_configuration(config, expected):
    network = config["networks"]["private"]
    if network.get("internal", False) or network.get("driver", "bridge") != "bridge":
        raise ValueError("private-user-defined-bridge-required")
    observed = {}
    for service, value in config["services"].items():
        if value.get("privileged") or value.get("network_mode") == "host":
            raise ValueError("unsupported-network-privilege")
        for binding in value.get("ports", []):
            if service not in expected or service in observed:
                raise ValueError("undocumented-or-duplicate-host-publication")
            pair = (int(binding["published"]), int(binding["target"]))
            if binding.get("host_ip") != "127.0.0.1" or binding.get("protocol", "tcp") != "tcp" or pair != expected[service]:
                raise ValueError("strict-loopback-port-contract-required")
            observed[service] = pair
    if observed != expected:
        raise ValueError("documented-operator-port-missing")


def published(container):
    for target, bindings in container.get("NetworkSettings", {}).get("Ports", {}).items():
        for binding in bindings or []:
            yield target, binding["HostIp"], int(binding["HostPort"])


def check_existing(containers, installation, expected):
    owned = set()
    for container in containers:
        if not container.get("State", {}).get("Running"):
            continue
        labels = container.get("Config", {}).get("Labels") or {}
        for target, address, port in published(container):
            if port not in {value[0] for value in expected.values()} or address not in {"", "0.0.0.0", "::", "127.0.0.1"}:
                continue
            service = labels.get("com.docker.compose.service")
            ours = labels.get("com.docker.compose.project") == installation and service in expected
            exact = ours and address == "127.0.0.1" and (port, int(target.split('/')[0])) == expected[service] and target.endswith('/tcp')
            if not exact or port in owned:
                raise ValueError("port-occupied-by-unrelated-or-mismatched-resource")
            owned.add(port)
    return owned


def check_available(expected, owned):
    for port, _ in expected.values():
        if port in owned:
            continue
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener:
            listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                listener.bind(("127.0.0.1", port))
            except OSError:
                raise ValueError("loopback-port-occupied") from None


def actual_bindings(containers, installation, expected):
    observed = {}
    for container in containers:
        labels = container.get("Config", {}).get("Labels") or {}
        if labels.get("com.docker.compose.project") != installation or not container.get("State", {}).get("Running"):
            continue
        service = labels.get("com.docker.compose.service")
        for target, address, port in published(container):
            if service not in expected or service in observed:
                raise ValueError("actual-undocumented-host-publication")
            if address != "127.0.0.1" or target != str(expected[service][1]) + "/tcp" or port != expected[service][0]:
                raise ValueError("actual-port-binding-contract-failed")
            observed[service] = {"host": address, "published": port, "target": target}
    if set(observed) != set(expected):
        raise ValueError("actual-required-host-publication-missing")
    return observed


def command(arguments):
    result = subprocess.run(arguments, text=True, capture_output=True, timeout=30, check=False)
    if result.returncode or len(result.stdout) > 8_388_608:
        raise ValueError("docker-network-inspection-unavailable")
    return result.stdout


def containers():
    identifiers = command(["docker", "ps", "-q"]).split()
    if len(identifiers) > 200:
        raise ValueError("docker-inspection-capacity-exceeded")
    return json.loads(command(["docker", "inspect", *identifiers])) if identifiers else []


def main():
    if sys.argv[1:] not in ([], ["--verify"]):
        raise ValueError("use-default-preflight-or-verify")
    installation = os.environ.get("SPOOLIN_INSTALLATION", "")
    if not re.fullmatch(r"[a-z][a-z0-9-]{2,63}", installation):
        raise ValueError("owned-installation-required")
    os.umask(0o077)
    expected = expected_ports(os.environ)
    compose = ["docker", "compose", "--project-name", installation, "-f", str(Path(__file__).resolve().parents[1] / "compose.yaml")]
    config = json.loads(command([*compose, "config", "--format", "json"]))
    validate_configuration(config, expected)
    active = containers()
    if not sys.argv[1:]:
        check_available(expected, check_existing(active, installation, expected))
        print(json.dumps({"result": "PASS", "boundary": "configured ports and current ownership; allocation still enforced by Docker"}))
    else:
        report = {"result": "PASS", "sha": os.environ.get("TESTED_SHA"), "runId": os.environ.get("GITHUB_RUN_ID"),
                  "bindings": actual_bindings(active, installation, expected)}
        report["network"] = verify_network(config)
        target = Path(os.environ.get("SPOOLIN_NETWORK_REPORT", ".ai/reports/network-bindings.json"))
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(report, indent=2) + "\n")
        print(json.dumps(report))


def verify_network(config):
    network = json.loads(command(["docker", "network", "inspect", config["networks"]["private"]["name"]]))
    if len(network) != 1 or network[0]["Driver"] != "bridge" or network[0]["Internal"]:
        raise ValueError("actual-private-bridge-contract-failed")
    return {"driver": network[0]["Driver"], "internal": network[0]["Internal"]}


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError, subprocess.TimeoutExpired):
        print("ports: configured loopback ports, occupancy or actual network bindings failed validation", file=sys.stderr)
        raise SystemExit(2)
