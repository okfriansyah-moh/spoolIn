#!/usr/bin/env python3
"""Provision isolated synthetic credentials on a GitHub-hosted Linux runner only."""
import base64
import hashlib
import hmac
import json
import os
from pathlib import Path
import secrets
import subprocess


def command(*args):
    subprocess.run(args, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def protected(path, value):
    path.write_bytes(value if isinstance(value, bytes) else value.encode())
    path.chmod(0o600)
    return path


def certificate(directory, name, names):
    key = directory / f"{name}-key"
    cert = directory / f"{name}-cert"
    request = directory / f"{name}.csr"
    extensions = protected(directory / f"{name}.ext", "subjectAltName=" + names + "\nextendedKeyUsage=serverAuth,clientAuth\n")
    command("openssl", "req", "-new", "-newkey", "rsa:2048", "-nodes", "-subj", f"/CN={name}", "-keyout", str(key), "-out", str(request))
    command("openssl", "x509", "-req", "-in", str(request), "-CA", str(directory / "service-ca"), "-CAkey", str(directory / "ca-key"), "-CAcreateserial", "-days", "2", "-extfile", str(extensions), "-out", str(cert))
    key.chmod(0o600)
    cert.chmod(0o600)
    request.unlink()
    extensions.unlink()
    return key, cert


def runtime_config(directory):
    mounts = {purpose.lower(): {"purpose": purpose, "file": f"/run/spoolin-config/{purpose.lower()}"} for purpose in ("PII", "DRAFT", "EMAIL_LOOKUP")}
    core = {"spoolin": {"account-protection": {"enabled": True, "keys": mounts}, "rate-limit": {"enabled": True, "agreement-file": "/run/spoolin-config/rate-agreement"}, "operations": {"enabled": True, "comparison-keys": {"active": "/run/spoolin-config/operation"}}, "actor": {"enabled": True}}, "CSRF_KEY_FILE": "/run/spoolin-config/csrf", "RATE_LIMIT_KEY_FILE": "/run/spoolin-config/rate", "OPERATION_HMAC_KEY_ID": "active", "OPERATION_HMAC_KEY_FILE": "/run/spoolin-config/operation", "ACTOR_SIGNING_KEY_ID": "active", "ACTOR_SIGNING_PRIVATE_KEY_FILE": "/run/spoolin-config/actor-key"}
    catalog = {"spoolin": {"catalog": {"write": {"enabled": True}}, "actor": {"enabled": True}}, "CATALOG_HMAC_KEY_ID": "active", "CATALOG_HMAC_KEY_FILE": "/run/spoolin-config/catalog-operation", "ACTOR_VERIFY_KEYS_FILE": "/run/spoolin-config/actor-trust"}
    media = {"spoolin": {"actor": {"enabled": True}}, "ACTOR_VERIFY_KEYS_FILE": "/run/spoolin-config/actor-trust"}
    for owner, config in (("core", core), ("catalog", catalog), ("media", media)):
        protected(directory / f"{owner}.json", json.dumps(config))


def rate_agreement(directory):
    # Derive defaults from the approved configuration source, retaining its exact revision calculation.
    import re
    source = Path("libraries/backend-support/src/main/kotlin/com/spoolin/support/policy/Configuration.kt").read_text()
    constants = dict(re.findall(r"private const val ([A-Z_]+) = ([0-9]+)", source))
    profiles = re.findall(r'"([A-Z_]+)" to \(([A-Z_]+) to ([A-Z_]+)\)', source)
    values = {}
    for profile, requests, seconds in profiles:
        values[f"RATE_LIMIT_{profile}_MAX_REQUESTS"] = constants[requests]
        values[f"RATE_LIMIT_{profile}_WINDOW_SECONDS"] = constants[seconds]
    values["MCP_RATE_LIMIT_MAX_BINDINGS"] = constants["MCP_RATE_LIMIT_MAX_BINDINGS_DEFAULT"]
    if len(values) != 11:
        raise RuntimeError("Expected all five rate profiles")
    revision = hashlib.sha256("\n".join(f"{key}={values[key]}" for key in sorted(values)).encode()).hexdigest()
    canary = hmac.new((directory / "rate").read_bytes(), b"spoolin-rate-agreement-v1", hashlib.sha256).hexdigest()
    protected(directory / "rate-agreement", json.dumps({"profileRevision": revision, "keyCanary": canary}))


def provision_identity_files(base, exports):
    for owner in ("postgres", "core", "catalog", "media", "mcp", "operator"):
        key, cert = certificate(base, owner, f"DNS:{owner},DNS:localhost,IP:127.0.0.1")
        prefix = "SPOOLIN_OPERATOR" if owner == "operator" else owner.upper() + ("_SERVICE" if owner != "postgres" else "")
        exports[prefix + "_KEY_FILE"] = str(key)
        exports[prefix + "_CERT_FILE"] = str(cert)
        if owner != "operator":
            command("sudo", "chown", "10001:10001", str(key), str(cert))
    exports["POSTGRES_PASSWORD_FILE"] = str(protected(base / "postgres-password", secrets.token_urlsafe(32)))
    for owner in ("core", "catalog", "media"):
        for role in ("runtime", "migrator", "maintenance", "backup"):
            file = protected(base / f"{owner}-{role}-password", secrets.token_urlsafe(32))
            exports[f"{owner.upper()}_DB_{role.upper()}_PASSWORD_FILE"] = str(file)
            command("sudo", "chown", "10001:10001", str(file))


def prepared_accounts(directory):
    roles = (("VIEWER", "00000000-0000-4000-8000-000000000001"),
             ("MAINTAINER", "00000000-0000-4000-8000-000000000002"))
    accounts = []
    for role, identity in roles:
        password_file = role.lower() + "-password"
        protected(directory / password_file, secrets.token_urlsafe(32))
        accounts.append({"id": identity, "name": "Prepared " + role.title(),
                         "email": role.lower() + "@example.invalid", "role": role,
                         "passwordFile": "/run/spoolin-config/" + password_file})
    keys = {purpose.lower(): {"purpose": purpose, "file": "/run/spoolin-config/" + purpose.lower()}
            for purpose in ("PII", "DRAFT", "EMAIL_LOOKUP")}
    protected(directory / "account-initialization.json", json.dumps({"setupVersion": "ci-v1", "accounts": accounts, "keys": keys}))


def main():
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted" or os.uname().sysname != "Linux":
        raise SystemExit("Refusing credentials provisioning outside GitHub-hosted Linux")
    base = Path(os.environ["RUNNER_TEMP"]) / ("spoolin-" + os.environ["GITHUB_RUN_ID"] + "-" + os.environ["GITHUB_RUN_ATTEMPT"])
    base.mkdir(mode=0o700)
    runtime = base / "runtime"
    runtime.mkdir(mode=0o700)
    exports = {"SPOOLIN_INSTALLATION": "ci-" + os.environ["GITHUB_RUN_ID"] + "-" + os.environ["GITHUB_RUN_ATTEMPT"], "SPOOLIN_LOCK_DIR": str(base / "locks"), "SPOOLIN_RUNTIME_CONFIG_DIR": str(runtime)}
    (base / "locks").mkdir(mode=0o700)
    command("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "2", "-subj", "/CN=Spoolin disposable CI CA", "-keyout", str(base / "ca-key"), "-out", str(base / "service-ca"))
    (base / "ca-key").chmod(0o600)
    exports["SERVICE_CA_FILE"] = str(base / "service-ca")
    provision_identity_files(base, exports)
    for name in ("pii", "draft", "email_lookup", "csrf", "rate", "operation", "catalog-operation", "mcp-ui"):
        protected(runtime / name, secrets.token_bytes(32))
    command("openssl", "genpkey", "-algorithm", "ED25519", "-outform", "DER", "-out", str(runtime / "actor-key"))
    public = subprocess.check_output(["openssl", "pkey", "-inform", "DER", "-in", str(runtime / "actor-key"), "-pubout", "-outform", "DER"])
    protected(runtime / "actor-trust", json.dumps({"active": base64.b64encode(public).decode()}))
    runtime_config(runtime)
    prepared_accounts(runtime)
    exports["SPOOLIN_BROWSER_MAINTAINER_PASSWORD_FILE"] = str(protected(base / "browser-maintainer-password", (runtime / "maintainer-password").read_bytes()))
    exports["SPOOLIN_ACCOUNT_INITIALIZATION_FILE"] = "/run/spoolin-config/account-initialization.json"
    rate_agreement(runtime)
    for file in runtime.iterdir():
        file.chmod(0o600)
    command("sudo", "chown", "-R", "10001:10001", str(runtime))
    with open(os.environ["GITHUB_ENV"], "a") as output:
        for key, value in exports.items():
            output.write(f"{key}={value}\n")
    print("Provisioned isolated credentials; exported paths only; synthetic material excluded from artifacts")


if __name__ == "__main__":
    main()
