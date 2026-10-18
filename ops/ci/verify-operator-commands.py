#!/usr/bin/env python3
"""Remote-only operator boundary checks against the existing disposable make-start installation."""
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import tempfile
import uuid


def invoke(script, *arguments, expected=0):
    result = subprocess.run(["bash", str(Path("ops/bin") / script), *arguments],
                            text=True, capture_output=True, timeout=90, check=False)
    if result.returncode != expected:
        raise RuntimeError(f"{script} returned {result.returncode}; expected {expected}; output withheld")
    return result.stdout, result.stderr


def checked_metadata(output, secret):
    if secret and secret in output:
        raise RuntimeError("Connector credential leaked into operator metadata")
    return json.loads(output)


def protocol(mode, token_file):
    output = Path('artifacts/runtime') / ('mcp-' + mode + '.json')
    output.parent.mkdir(parents=True, exist_ok=True)
    result = subprocess.run(['node', 'ops/ci/verify-mcp-runtime.mjs', mode, str(token_file), str(output)],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=150, check=False)
    if result.returncode:
        raise RuntimeError('Actual production MCP protocol assertions failed; inspect redacted report')
    return str(output)


def connector(account, parent):
    secret_file = parent / "operator-connector"
    base = ("--account", account, "--client", "CLAUDE")
    output, _ = invoke("connector.sh", "issue", *base, "--output-file", str(secret_file), "--ttl-seconds", "60")
    secret = secret_file.read_text()
    if not re.fullmatch(r"[A-Za-z0-9_-]{43}", secret):
        raise RuntimeError("Unexpected protected connector credential format")
    info = secret_file.lstat()
    if stat.S_IMODE(info.st_mode) != 0o600 or info.st_uid != os.getuid():
        raise RuntimeError("Connector credential ownership or mode differs")
    issued = checked_metadata(output, secret)
    credential = str(uuid.UUID(issued["id"]))
    listing, _ = invoke("connector.sh", "list", *base)
    assert any(row["id"] == credential and not row["revoked"]
               for row in checked_metadata(listing, secret)["credentials"])
    invoke("connector.sh", "issue", *base, "--output-file", str(secret_file), expected=2)
    assert secret_file.read_text() == secret
    protocol_report = protocol('positive', secret_file)
    for _ in range(2):
        revoked, _ = invoke("connector.sh", "revoke", *base, "--credential-id", credential)
        assert checked_metadata(revoked, secret)["revoked"] is True
    listing, _ = invoke("connector.sh", "list", *base)
    assert any(row["id"] == credential and row["revoked"]
               for row in checked_metadata(listing, secret)["credentials"])
    other, _ = invoke("connector.sh", "list", "--account", account, "--client", "CHATGPT")
    assert all(row["id"] != credential for row in checked_metadata(other, secret)["credentials"])
    revoked_report = protocol('revoked', secret_file)
    secret_file.unlink()
    return {"result": "PASS", "boundary": "actual protected CLI and owner database",
            "checks": ["issue", "0600/owner", "metadata redaction", "list", "existing-file refusal",
                       "repeated revoke", "client binding"], "hostCompatibility": "PENDING", "protocolReports": [protocol_report, revoked_report]}


def tunnel():
    for action in ("start", "stop", "status"):
        output, error = invoke("tunnel.sh", action, expected=78)
        assert not output and "no verified registered Secure MCP Tunnel runner" in error
    invoke("tunnel.sh", "unsupported", expected=2)
    return {"result": "PENDING", "refusalRegression": "PASS",
            "unmetRequirement": "approved pinned registered Secure MCP Tunnel runner and protected identity"}


def main():
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted":
        raise RuntimeError("Requires a verified GitHub-hosted disposable runner")
    account = str(uuid.UUID(os.environ["SPOOLIN_OPERATOR_ACCOUNT_ID"]))
    report = {"sha": os.environ["TESTED_SHA"], "runId": os.environ["GITHUB_RUN_ID"],
              "runnerOS": os.environ["RUNNER_OS"], "connector": None, "tunnel": None}
    with tempfile.TemporaryDirectory(prefix="operator-check-", dir=os.environ["RUNNER_TEMP"]) as temporary:
        parent = Path(temporary)
        parent.chmod(0o700)
        report["connector"] = connector(account, parent)
    report["tunnel"] = tunnel()
    target = Path(os.environ.get("SPOOLIN_OPERATOR_REPORT", ".ai/reports/operator-commands.json"))
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2) + "\n")
    raise SystemExit(78)  # Preserve the real tunnel obligation as pending.


if __name__ == "__main__":
    main()
