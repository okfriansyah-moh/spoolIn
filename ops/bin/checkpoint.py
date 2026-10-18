#!/usr/bin/env python3
"""Coordinated installation checkpoints. Execute only in the operator-selected environment."""
import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import tarfile
import tempfile

OWNERS = ("core", "catalog", "media")
ROOT = Path(__file__).resolve().parents[2]


def run(argv, *, output=None, input_file=None):
    result = subprocess.run(argv, stdin=input_file, stdout=output or subprocess.PIPE,
                            stderr=subprocess.PIPE, check=False)
    if result.returncode:
        detail = result.stderr.decode(errors="replace")[:6000]
        detail = re.sub(r"(?i)(password|token|authorization)[=: ]+[^\s,;]+", r"\1=[redacted]", detail)
        raise RuntimeError(f"{Path(argv[0]).name} failed with exit {result.returncode}: {detail}")
    return result.stdout.decode().strip() if output is None else None


def protected(path, directory=False):
    state = path.lstat()
    expected = 0o700 if directory else 0o600
    kind = stat.S_ISDIR if directory else stat.S_ISREG
    if not kind(state.st_mode) or stat.S_IMODE(state.st_mode) != expected:
        raise ValueError("Protected path type or mode is invalid")
    return path


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(65536), b""):
            value.update(block)
    return value.hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, sort_keys=True, indent=2) + "\n")
    path.chmod(0o600)


class Installation:
    def __init__(self):
        self.name = os.environ["SPOOLIN_INSTALLATION"]
        if not re.fullmatch(r"[a-z][a-z0-9-]{2,63}", self.name):
            raise ValueError("Invalid installation identifier")
        self.locks = protected(Path(os.environ["SPOOLIN_LOCK_DIR"]), True)
        if self.locks.stat().st_uid != os.getuid():
            raise ValueError("The authenticated OS user must own the installation lock directory")
        self.compose = ["docker", "compose", "--project-name", self.name, "-f", str(ROOT / "ops/compose.yaml")]
        self.config = json.loads(run(self.compose + ["config", "--format", "json"]))
        if not all(owner in self.config["services"] for owner in OWNERS):
            raise ValueError("All three production owner services are required")

    def postgres(self, owner, role, argv, output=None, input_file=None):
        if owner not in OWNERS or role not in ("backup", "postgres"):
            raise ValueError("Unsupported checkpoint database role")
        file = "postgres-password" if role == "postgres" else f"{owner}-backup-password"
        script = 'secret="$1"; shift; export PGPASSWORD="$(cat "/run/spoolin/$secret")"; exec "$@"'
        return run(self.compose + ["exec", "-T", "postgres", "sh", "-c", script, "checkpoint", file] + argv,
                   output=output, input_file=input_file)

    def sql(self, owner, sql):
        user = f"spoolin_{owner}_backup"
        return self.postgres(owner, "backup", ["psql", "-X", "-A", "-t", "-v", "ON_ERROR_STOP=1", "-U", user,
                                                  "-d", f"spoolin_{owner}", "-c", sql])

    def capture(self, service, path, target):
        if service not in (*OWNERS, "mcp", "postgres"):
            raise ValueError("Unsupported checkpoint service")
        command = self.compose + ["run", "--rm", "--no-deps", "-T", "--user", "0:0", "--entrypoint", "tar", service,
                                  "-C", path, "-cf", "-", "."]
        if service == "postgres":
            command = self.compose + ["exec", "-T", "postgres", "tar", "-C", path, "-cf", "-", "."]
        with target.open("xb") as output:
            run(command, output=output)
        target.chmod(0o600)

    def writer_names(self):
        active = run(self.compose + ["ps", "--status", "running", "--services"]).splitlines()
        return sorted(service for service in active if service != "postgres")

    def assert_quiescent(self):
        for owner in OWNERS:
            count = self.postgres(owner, "postgres", ["psql", "-X", "-A", "-t", "-U", "postgres", "-d", "postgres", "-c",
                "SELECT count(*) FROM pg_stat_activity WHERE usename='spoolin_" + owner + "_runtime'"])
            if count != "0":
                raise RuntimeError(f"{owner} still has live runtime database connections")


@contextmanager
def installation_lock(installation):
    path = installation.locks / f"operator-{installation.name}.lock"
    path.mkdir(mode=0o700)
    try:
        yield
    finally:
        path.rmdir()


def metadata(installation):
    owners = {}
    for owner in OWNERS:
        tables = installation.sql(owner, "SELECT tablename FROM pg_tables WHERE schemaname='appdata' ORDER BY tablename").splitlines()
        if not tables or any(not re.fullmatch(r"[a-z][a-z0-9_]*", table) for table in tables):
            raise RuntimeError("Missing or invalid migrated owner tables")
        counts = {table: int(installation.sql(owner, f"SELECT count(*) FROM appdata.{table}")) for table in tables}
        migrations = json.loads(installation.sql(owner,
            "SELECT jsonb_agg(jsonb_build_object('version',version,'checksum',checksum,'success',success) ORDER BY installed_rank) "
            "FROM appdata.flyway_schema_history"))
        if not migrations or any(not row["success"] for row in migrations):
            raise RuntimeError("Owner migration evidence is incomplete")
        owners[owner] = {"counts": counts, "migrations": migrations}
    return owners


def runtime_images(installation):
    result = {}
    for service in ("postgres", *OWNERS, "mcp"):
        ids = run(installation.compose + ["ps", "--all", "--quiet", service]).splitlines()
        if not ids:
            continue
        records = json.loads(run(["docker", "inspect", *ids]))
        for record in records:
            if record["Config"]["Labels"].get("com.docker.compose.project") != installation.name:
                raise RuntimeError("Container belongs to another installation")
        result[service] = [{"image": row["Image"], "exitCode": row["State"]["ExitCode"]} for row in records]
    return result


def pack(directory, target):
    with tarfile.open(target, "w") as archive:
        for path in sorted(directory.rglob("*")):
            if path.is_symlink() or not (path.is_file() or path.is_dir()):
                raise ValueError("Checkpoint contains an unsupported filesystem entry")
            archive.add(path, arcname=path.relative_to(directory).as_posix(), recursive=False)
    target.chmod(0o600)


def extract(source, destination):
    destination.mkdir(mode=0o700, parents=True, exist_ok=True)
    with tarfile.open(source) as archive:
        for member in archive:
            relative = Path(member.name)
            if relative.is_absolute() or ".." in relative.parts or member.sparse is not None or not (member.isdir() or member.isfile()):
                raise ValueError("Unsafe checkpoint archive member")
            path = destination / relative
            path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
            if member.isdir():
                path.mkdir(mode=0o700, exist_ok=True)
            else:
                with archive.extractfile(member) as incoming, path.open("xb") as output:
                    shutil.copyfileobj(incoming, output)
                path.chmod(0o600)


def encrypt(source, target, recipient):
    run(["openssl", "cms", "-encrypt", "-binary", "-aes-256-gcm", "-outform", "DER", "-in", str(source),
         "-out", str(target), "-recip", str(recipient)])
    target.chmod(0o600)


def decrypt(source, target, certificate, key):
    protected(key)
    run(["openssl", "cms", "-decrypt", "-binary", "-inform", "DER", "-in", str(source), "-out", str(target),
         "-recip", str(certificate), "-inkey", str(key)])
    target.chmod(0o600)


def checkpoint_files(installation, data, keys):
    for owner in OWNERS:
        with (data / f"{owner}.dump").open("xb") as output:
            installation.postgres(owner, "backup", ["pg_dump", "--no-password", "--format=custom", "--no-owner",
                "-U", f"spoolin_{owner}_backup", "-d", f"spoolin_{owner}"], output=output)
        installation.capture(owner, "/run/secrets", keys / f"{owner}-secrets.tar")
    with (data / "roles.sql").open("xb") as output:
        installation.postgres("core", "postgres", ["pg_dumpall", "--roles-only", "--no-role-passwords", "-U", "postgres"], output=output)
    installation.capture("media", "/var/spoolin/media", data / "artwork.tar")
    installation.capture("core", "/run/spoolin-config", keys / "runtime.tar")
    with tarfile.open(keys / "runtime.tar") as archive:
        if any(Path(member.name).name == "restore-pending-validation" for member in archive):
            raise RuntimeError("Cannot checkpoint an incompletely restored installation")
    installation.capture("postgres", "/run/secrets", keys / "postgres-secrets.tar")
    if "mcp" in installation.config["services"]:
        installation.capture("mcp", "/run/secrets", keys / "mcp-secrets.tar")


def backup(installation, arguments):
    output = Path(arguments.output).absolute()
    recipient = Path(os.environ.get("SPOOLIN_BACKUP_RECIPIENT_CERT_FILE", os.environ.get("SPOOLIN_OPERATOR_CERT_FILE", "")))
    if not recipient.is_file() or recipient.is_symlink():
        raise ValueError("Supply a trusted operator backup-recipient certificate")
    if not arguments.execute:
        print("Backup plan: quiesce writers; checkpoint all owners/artwork; encrypt data and separate key recovery material")
        return
    output.mkdir(mode=0o700)
    with installation_lock(installation), tempfile.TemporaryDirectory(prefix="spoolin-checkpoint-") as temporary:
        stage = Path(temporary)
        data, keys = stage / "data", stage / "keys"
        data.mkdir(mode=0o700)
        keys.mkdir(mode=0o700)
        writers = installation.writer_names()
        if writers:
            run(installation.compose + ["stop", "--timeout", "30", *writers])
        try:
            installation.assert_quiescent()
            before = metadata(installation)
            checkpoint_files(installation, data, keys)
            if before != metadata(installation):
                raise RuntimeError("Owner data changed while checkpointing")
            finish_backup(installation, output, stage, data, keys, recipient, before)
        finally:
            if writers:
                run(installation.compose + ["start", *writers])
                wait_status()
    print("Encrypted coordinated checkpoint complete; protected key recovery archive is separate; operator off-site custody remains required")


def finish_backup(installation, output, stage, data, keys, recipient, owners):
    key_files = {path.name: digest(path) for path in keys.iterdir()}
    write_json(keys / "manifest.json", {"files": key_files, "installation": installation.name})
    pack(keys, stage / "keys.tar")
    encrypt(stage / "keys.tar", output / "keys.cms", recipient)
    manifest = {"format": 1, "installation": installation.name, "createdAt": datetime.now(timezone.utc).isoformat(),
        "checkoutSha": run(["git", "-C", str(ROOT), "rev-parse", "HEAD"]), "owners": owners, "images": runtime_images(installation),
        "roles": role_inventory(installation),
        "postgresVersion": installation.sql("core", "SELECT version()"),
        "files": {path.name: digest(path) for path in data.iterdir()}, "keysArchiveSha256": digest(output / "keys.cms")}
    write_json(data / "manifest.json", manifest)
    pack(data, stage / "checkpoint.tar")
    encrypt(stage / "checkpoint.tar", output / "checkpoint.cms", recipient)
    write_json(output / "encrypted-files.json", {name: digest(output / name) for name in ("checkpoint.cms", "keys.cms")})


def verify_images(installation, manifest):
    for service in ("postgres", *OWNERS, "mcp"):
        expected = {item["image"] for item in manifest["images"].get(service, [])}
        image = installation.config["services"][service]["image"]
        actual = json.loads(run(["docker", "image", "inspect", image]))[0]["Id"]
        if actual not in expected:
            raise ValueError("Restore runtime image differs from the checkpoint's actual application")


def role_inventory(installation):
    roles = installation.postgres("core", "postgres", ["psql", "-X", "-A", "-t", "-U", "postgres", "-d", "postgres", "-c",
        "SELECT jsonb_agg(jsonb_build_object('name',rolname,'login',rolcanlogin,'superuser',rolsuper,'createDb',rolcreatedb,"
        "'createRole',rolcreaterole,'replication',rolreplication,'bypassRls',rolbypassrls,'limit',rolconnlimit,'settings',rolconfig) "
        "ORDER BY rolname) FROM pg_roles WHERE rolname='postgres' OR rolname LIKE 'spoolin_%'"])
    memberships = installation.postgres("core", "postgres", ["psql", "-X", "-A", "-t", "-U", "postgres", "-d", "postgres", "-c",
        "SELECT coalesce(jsonb_agg(jsonb_build_object('role',r.rolname,'member',m.rolname,'admin',a.admin_option,"
        "'inherit',a.inherit_option,'set',a.set_option) ORDER BY r.rolname,m.rolname),'[]') FROM pg_auth_members a "
        "JOIN pg_roles r ON r.oid=a.roleid JOIN pg_roles m ON m.oid=a.member "
        "WHERE r.rolname LIKE 'spoolin_%' OR m.rolname LIKE 'spoolin_%'"])
    return {"roles": json.loads(roles), "memberships": json.loads(memberships)}


def runtime_snapshot(installation, stage):
    archive = stage / "target-runtime.tar"
    installation.capture("core", "/run/spoolin-config", archive)
    target = stage / "target-runtime"
    extract(archive, target)
    return target


def install_runtime(installation, directory, stage):
    archive = stage / "runtime-update.tar"
    with tarfile.open(archive, "w") as output:
        for path in sorted(directory.rglob("*")):
            if path.is_symlink() or not (path.is_file() or path.is_dir()):
                raise ValueError("Unsafe runtime recovery entry")
            member = output.gettarinfo(str(path), arcname=path.relative_to(directory).as_posix())
            member.uid, member.gid = 10001, 10001
            member.mode = 0o700 if path.is_dir() else 0o600
            if path.is_file():
                with path.open("rb") as value:
                    output.addfile(member, value)
            else:
                output.addfile(member)
    mount = os.environ["SPOOLIN_RUNTIME_CONFIG_DIR"] + ":/run/spoolin-config:rw"
    with archive.open("rb") as value:
        run(installation.compose + ["run", "--rm", "--no-deps", "-T", "--user", "0:0", "--volume", mount,
            "--entrypoint", "tar", "core", "-C", "/run/spoolin-config", "-xpf", "-"], input_file=value)


def save_marker(installation, marker, phase, stage):
    marker["phase"] = phase
    marker.setdefault("history", []).append({"phase": phase, "at": datetime.now(timezone.utc).isoformat()})
    directory = stage / "marker-update"
    directory.mkdir(mode=0o700, exist_ok=True)
    write_json(directory / "restore-pending-validation", marker)
    install_runtime(installation, directory, stage)


def verified_archives(arguments, stage):
    directory = protected(Path(arguments.input).absolute(), True)
    envelope = json.loads((directory / "encrypted-files.json").read_text())
    if set(envelope) != {"checkpoint.cms", "keys.cms"}:
        raise ValueError("Checkpoint envelope is incomplete")
    for name, expected in envelope.items():
        if digest(protected(directory / name)) != expected:
            raise ValueError("Encrypted checkpoint checksum differs")
    certificate = Path(os.environ.get("SPOOLIN_BACKUP_RECIPIENT_CERT_FILE") or os.environ["SPOOLIN_OPERATOR_CERT_FILE"])
    key = Path(os.environ.get("SPOOLIN_BACKUP_PRIVATE_KEY_FILE") or os.environ["SPOOLIN_OPERATOR_KEY_FILE"])
    decrypt(directory / "checkpoint.cms", stage / "data.tar", certificate, key)
    decrypt(directory / "keys.cms", stage / "keys.tar", certificate, key)
    data, keys = stage / "data", stage / "keys"
    extract(stage / "data.tar", data)
    extract(stage / "keys.tar", keys)
    manifest = verify_content(data)
    key_manifest = verify_content(keys)
    required = {"core.dump", "catalog.dump", "media.dump", "artwork.tar", "roles.sql"}
    if not required.issubset(manifest["files"]) or "runtime.tar" not in key_manifest["files"]:
        raise ValueError("Checkpoint does not cover every required owner and recovery material")
    if key_manifest["installation"] != manifest["installation"] or digest(directory / "keys.cms") != manifest["keysArchiveSha256"]:
        raise ValueError("Separate key archive does not belong to this checkpoint")
    return data, keys, manifest, envelope["checkpoint.cms"]


def verify_content(directory):
    manifest = json.loads(protected(directory / "manifest.json").read_text())
    if not manifest.get("files"):
        raise ValueError("Empty checkpoint manifest")
    for name, expected in manifest["files"].items():
        if Path(name).name != name or digest(protected(directory / name)) != expected:
            raise ValueError("Checkpoint content checksum differs")
    return manifest


def ensure_empty_destination(installation):
    for owner in OWNERS:
        tables = installation.sql(owner, "SELECT count(*) FROM pg_tables WHERE schemaname='appdata'")
        if tables != "0":
            raise ValueError("Restore destination is not an empty owned database; replacement is refused")
    name = installation.config["volumes"]["database"]["name"]
    state = json.loads(run(["docker", "volume", "inspect", name]))[0]
    if state["Labels"].get("com.docker.compose.project") != installation.name:
        raise ValueError("Restore database volume is not owned by this installation")


def restore_databases(installation, data):
    for owner in OWNERS:
        with (data / f"{owner}.dump").open("rb") as value:
            installation.postgres(owner, "postgres", ["pg_restore", "--no-password", "--exit-on-error", "--clean", "--if-exists",
                "--no-owner", "--role", f"spoolin_{owner}_owner", "-U", "postgres", "-d", f"spoolin_{owner}"], input_file=value)
        installation.postgres(owner, "postgres", ["psql", "-X", "-v", "ON_ERROR_STOP=1", "-U", "postgres", "-d",
            f"spoolin_{owner}", "-f", "/opt/spoolin/grants.sql"])


def restore_artwork(installation, data, stage):
    verified = stage / "verified-artwork"
    extract(data / "artwork.tar", verified)
    current = stage / "existing-artwork.tar"
    installation.capture("media", "/var/spoolin/media", current)
    existing = stage / "existing-artwork"
    extract(current, existing)
    for path in existing.rglob("*"):
        if path.is_file():
            expected = verified / path.relative_to(existing)
            if not expected.is_file() or digest(expected) != digest(path):
                raise ValueError("Restore artwork destination contains unrelated or changed files")
    with (data / "artwork.tar").open("rb") as value:
        run(installation.compose + ["run", "--rm", "--no-deps", "-T", "--user", "0:0", "--entrypoint", "tar",
            "media", "-C", "/var/spoolin/media", "--no-same-owner", "-xpf", "-"], input_file=value)
    run(installation.compose + ["run", "--rm", "--no-deps", "-T", "--user", "0:0", "--entrypoint", "chown",
        "media", "-R", "10001:10001", "/var/spoolin/media"])


def stage_new_epochs(installation, keys, marker, stage):
    import secrets
    runtime = stage / "restored-runtime"
    extract(keys / "runtime.tar", runtime)
    if (runtime / "restore-pending-validation").exists():
        raise ValueError("Source checkpoint was captured during an incomplete restore")
    core = json.loads(protected(runtime / "core.json").read_text())
    mounts = core["spoolin"]["account-protection"]["keys"]
    material = [path.read_bytes() for path in runtime.rglob("*") if path.is_file() and path.stat().st_size == 32]
    originals = json.loads(installation.sql("core", "SELECT jsonb_object_agg(purpose,id) FROM appdata.crypto_key_version WHERE state='WRITE'"))
    counters = json.loads(installation.sql("core", "SELECT jsonb_object_agg(id,next_nonce::text) FROM appdata.crypto_key_version"))
    targets = {}
    for purpose in ("PII", "DRAFT", "EMAIL_LOOKUP"):
        identifier = "restore-" + marker["epoch"].replace("-", "") + "-" + purpose.lower()
        value = secrets.token_bytes(32)
        while value in material:
            value = secrets.token_bytes(32)
        path = runtime / identifier
        path.write_bytes(value)
        path.chmod(0o600)
        material.append(value)
        mounts[identifier] = {"purpose": purpose, "file": "/run/spoolin-config/" + identifier}
        targets[purpose] = identifier
    core["BROWSER_ORIGIN"] = "https://localhost:" + os.environ.get("CORE_HTTPS_PORT", "8443")
    write_json(runtime / "core.json", core)
    write_json(runtime / "restore-keys.json", mounts)
    plan = {"checkpointSha256": marker["checkpointSha256"], "epoch": marker["epoch"], "originals": originals,
        "targets": targets, "originalCounters": counters, "keys": "/run/spoolin-config/restore-keys.json"}
    write_json(runtime / "restore-plan.json", plan)
    write_json(runtime / "restore-pending-validation", marker)
    install_runtime(installation, runtime, stage)


def establish_epochs(installation):
    mount = os.environ["SPOOLIN_RUNTIME_CONFIG_DIR"] + ":/run/spoolin-config:rw"
    run(installation.compose + ["run", "--rm", "--no-deps", "-T", "--volume", mount, "--entrypoint", "java", "core",
        "-Dloader.main=com.spoolin.core.features.restore.RestoreCheckpointCli", "-cp", "/app/service.jar",
        "org.springframework.boot.loader.launch.PropertiesLauncher", "--plan=/run/spoolin-config/restore-plan.json"])


def validate_owner_data(installation, manifest):
    current = metadata(installation)
    if role_inventory(installation) != manifest["roles"]:
        raise RuntimeError("Restored role attributes or memberships differ from the retained source inventory")
    for owner in OWNERS:
        before, after = manifest["owners"][owner], current[owner]
        if before["migrations"] != after["migrations"]:
            raise RuntimeError("Restored migration versions or checksums differ")
        mutable = {"crypto_key_version", "operation_receipt", "operational_event", "account_email_lookup"} if owner == "core" else set()
        for table, count in before["counts"].items():
            if table not in mutable and after["counts"].get(table) != count:
                raise RuntimeError(f"Restored {owner} table row count differs: {table}")
            if table in mutable and after["counts"].get(table, -1) < count:
                raise RuntimeError("Restore lost retained cryptographic or operational evidence")
    for table in ("browser_session", "mcp_credential"):
        if installation.sql("core", f"SELECT count(*) FROM appdata.{table} WHERE revoked_at IS NULL") != "0":
            raise RuntimeError("Restored authentication material remains usable")
    return current


def validate_artwork(installation, stage):
    archive = stage / "validation-artwork.tar"
    installation.capture("media", "/var/spoolin/media", archive)
    directory = stage / "validation-artwork"
    extract(archive, directory)
    assets = json.loads(installation.sql("media",
        "SELECT coalesce(jsonb_agg(jsonb_build_object('id',id,'key',storage_key,'bytes',output_bytes,'sha256',encode(content_hash,'hex'),"
        "'width',width,'height',height,'format',format) ORDER BY id),'[]') FROM appdata.asset WHERE state='READY'"))
    for asset in assets:
        relative = Path(asset["key"])
        if relative.is_absolute() or ".." in relative.parts or relative.parts[0] != "output":
            raise RuntimeError("Restored artwork mapping is unsafe")
        path = directory / relative
        if not path.is_file() or path.stat().st_size != asset["bytes"] or digest(path) != asset["sha256"]:
            raise RuntimeError("Restored artwork availability, size or checksum differs")
    return assets


def validate_holds(installation):
    holds = json.loads(installation.sql("media", "SELECT coalesce(jsonb_agg(jsonb_build_object('id',id,'asset',asset_id,"
        "'title',title_id,'version',catalog_version) ORDER BY id),'[]') FROM appdata.asset_reference WHERE state='ACTIVE'"))
    for hold in holds:
        identifier = str(__import__("uuid").UUID(hold["title"]))
        rows = json.loads(installation.sql("catalog", "SELECT coalesce(jsonb_agg(jsonb_build_object('hold',attachment_id,'asset',asset_id,"
            f"'version',version)),'[]') FROM appdata.title WHERE id='{identifier}'::uuid"))
        if not rows or rows[0]["hold"] != hold["id"] or rows[0]["asset"] != hold["asset"] or rows[0]["version"] < hold["version"]:
            raise RuntimeError("Restored active artwork hold requires destination reconciliation; no automatic deletion or resend permitted")
    return len(holds)


def api_client():
    import http.cookiejar
    import ssl
    import urllib.request
    trust = ssl.create_default_context(cafile=os.environ["SERVICE_CA_FILE"])
    cookies = http.cookiejar.CookieJar()
    client = urllib.request.build_opener(urllib.request.HTTPSHandler(context=trust), urllib.request.HTTPCookieProcessor(cookies))
    return client


def graphql(client, origin, query, variables=None, csrf=None):
    import urllib.request
    headers = {"Content-Type": "application/json", "Origin": origin, "X-Spoolin-Client": "web",
               "Accept": "application/graphql-response+json, application/json"}
    if csrf:
        headers["X-CSRF-Token"] = csrf
    body = json.dumps({"query": query, "variables": variables or {}}).encode()
    request = urllib.request.Request(origin + "/graphql", data=body, headers=headers, method="POST")
    with client.open(request, timeout=10) as response:
        raw = response.read(1048577)
        if len(raw) > 1048576:
            raise RuntimeError("Restored API returned an oversized response")
        value = json.loads(raw)
        if value.get("errors") or not value.get("data"):
            raise RuntimeError("Restored application API assertion failed")
        return value["data"]


def smoke_application(installation, artwork):
    import uuid
    client = api_client()
    origin = "https://localhost:" + os.environ.get("CORE_HTTPS_PORT", "8443")
    session = graphql(client, origin, "query { session { authenticated csrfToken } titles(filter:{},first:1) { nodes { id } } }")
    if session["session"]["authenticated"] or not isinstance(session["titles"]["nodes"], list):
        raise RuntimeError("Restored public metadata/session assertion failed")
    served = serve_artwork(installation, client, origin, artwork)
    email = os.environ.get("SPOOLIN_RESTORE_CANARY_EMAIL")
    password_path = os.environ.get("SPOOLIN_RESTORE_CANARY_PASSWORD_FILE")
    if not email or not password_path:
        raise ValueError("Supply current source-account canary email and protected password file; no password reset is permitted")
    password = protected(Path(password_path)).read_text()
    variables = {"operationId": str(uuid.uuid4()), "email": email, "password": password}
    result = graphql(client, origin, (ROOT / "frontend/src/lib/features/session/sign-in.graphql").read_text(), variables,
                     session["session"]["csrfToken"])
    if result["signIn"]["operation"]["state"] != "SUCCEEDED" or result["signIn"].get("problem"):
        raise RuntimeError("Current source credential did not authenticate after restore")
    protected_result = graphql(client, origin, "query { session { authenticated role csrfToken } managedTitles(state:ACTIVE,first:1) { nodes { id } } }")
    if not protected_result["session"]["authenticated"] or protected_result["session"]["role"] != "MAINTAINER":
        raise RuntimeError("Restored protected account/API/downstream integration assertion failed")
    signout = graphql(client, origin, (ROOT / "frontend/src/lib/features/session/sign-out.graphql").read_text(),
        {"operationId": str(uuid.uuid4())}, protected_result["session"]["csrfToken"])
    if signout["signOut"]["operation"]["state"] != "SUCCEEDED":
        raise RuntimeError("Restored smoke session did not revoke correctly")
    return {"tlsVerified": True, "publicCatalog": True, "sourcePasswordPreserved": True,
            "protectedRoleAndCatalogRpc": True, "smokeSessionRevoked": True, "verifiedServedArtwork": served}


def restore(installation, arguments):
    if not arguments.execute:
        print("Restore plan: distinct empty destination; authenticated archives; fresh key epochs; credential revocation; all-row/artwork/API verification")
        return
    if not (installation.name.startswith("ci-") or installation.name.startswith("restore-")):
        raise ValueError("This command restores only an explicitly owned disposable namespace")
    with installation_lock(installation), tempfile.TemporaryDirectory(prefix="spoolin-restore-") as temporary:
        stage = Path(temporary)
        data, keys, manifest, checksum = verified_archives(arguments, stage)
        if manifest["installation"] == installation.name or manifest["checkoutSha"] != run(["git", "-C", str(ROOT), "rev-parse", "HEAD"]):
            raise ValueError("Source/destination installation must differ and exact source revision must match")
        verify_images(installation, manifest)
        runtime = runtime_snapshot(installation, stage)
        path = runtime / "restore-pending-validation"
        marker = json.loads(path.read_text()) if path.exists() else {
            "checkpointSha256": checksum, "source": manifest["installation"], "destination": installation.name,
            "epoch": str(__import__("uuid").uuid4()), "phase": "new", "history": []}
        if marker["checkpointSha256"] != checksum or marker["destination"] != installation.name:
            raise ValueError("Restore marker belongs to another checkpoint or destination")
        writers = installation.writer_names()
        if writers:
            run(installation.compose + ["stop", "--timeout", "30", *writers])
        try:
            prepare_restore(installation, marker, data, keys, stage)
            os.environ["ACCOUNT_READ_KEYS_FILE"] = "/run/spoolin-config/restore-keys.json"
            establish_epochs(installation)
            save_marker(installation, marker, "crypto-verified", stage)
            counts = marker.get("ownerDataVerified")
            if counts is None:
                counts = validate_owner_data(installation, manifest)
                marker["ownerDataVerified"] = counts
                save_marker(installation, marker, "database-verified", stage)
            artwork = validate_artwork(installation, stage)
            holds = validate_holds(installation)
            proof = start_and_validate(installation, artwork, marker, stage)
            finish_restore(installation, marker, checksum, counts, artwork, holds, proof, arguments, stage)
        except Exception:
            current = installation.writer_names()
            if current:
                run(installation.compose + ["stop", "--timeout", "30", *current])
            raise


def prepare_restore(installation, marker, data, keys, stage):
    phase = marker["phase"]
    if phase == "new":
        save_marker(installation, marker, "destination-check", stage)
        run(installation.compose + ["up", "-d", "--wait", "--wait-timeout", "120", "postgres"])
        ensure_empty_destination(installation)
        save_marker(installation, marker, "database-restoring", stage)
        phase = "database-restoring"
    installation.assert_quiescent()
    if phase == "destination-check":
        raise RuntimeError("Earlier destination preflight failed; inspect the retained marker and use a fresh owned namespace")
    if phase == "database-restoring":
        restore_databases(installation, data)
        save_marker(installation, marker, "database-restored", stage)
        phase = "database-restored"
    if phase == "database-restored":
        restore_artwork(installation, data, stage)
        stage_new_epochs(installation, keys, marker, stage)
        save_marker(installation, marker, "keys-prepared", stage)


def wait_status():
    import time
    deadline = time.monotonic() + 300
    while True:
        try:
            run(["make", "-C", str(ROOT), "status"])
            return
        except RuntimeError:
            if time.monotonic() >= deadline:
                raise RuntimeError("Real application readiness deadline exceeded")
            time.sleep(2)


def start_and_validate(installation, artwork, marker, stage):
    run(installation.compose + ["up", "-d", *OWNERS, "mcp", "prometheus", "alertmanager", "grafana"])
    wait_status()
    save_marker(installation, marker, "application-ready", stage)
    return smoke_application(installation, artwork)


def serve_artwork(installation, client, origin, assets):
    expected = {asset["id"]: asset for asset in assets}
    active = json.loads(installation.sql("catalog", "SELECT coalesce(jsonb_agg(DISTINCT asset_id),'[]') "
        "FROM appdata.title WHERE status='ACTIVE' AND asset_id IS NOT NULL"))
    for identifier in active:
        asset = expected.get(identifier)
        if asset is None:
            raise RuntimeError("Active Catalog artwork lacks a verified restored Media asset")
        with client.open(origin + "/artwork/" + str(__import__("uuid").UUID(identifier)), timeout=10) as response:
            body = response.read(8388609)
            if len(body) != asset["bytes"] or hashlib.sha256(body).hexdigest() != asset["sha256"]:
                raise RuntimeError("Restored application served incorrect artwork bytes")
            if response.headers.get_content_type() != "image/" + asset["format"].lower():
                raise RuntimeError("Restored artwork content type differs")
    return len(active)


def finish_restore(installation, marker, checksum, counts, artwork, holds, proof, arguments, stage):
    report = {"checkpointSha256": checksum, "destination": installation.name, "epoch": marker["epoch"],
        "preStartupOwners": counts, "currentOwners": metadata(installation), "verifiedStoredArtwork": artwork, "verifiedActiveHolds": holds, "actualApplication": proof,
        "checkedAt": datetime.now(timezone.utc).isoformat(), "checkoutSha": run(["git", "-C", str(ROOT), "rev-parse", "HEAD"])}
    directory = stage / "completion"
    directory.mkdir(mode=0o700)
    marker["phase"] = "verified"
    write_json(directory / "restore-completed.json", {**marker, "proof": report})
    install_runtime(installation, directory, stage)
    if arguments.report:
        target = Path(arguments.report).absolute()
        if target.exists():
            raise ValueError("Restore report already exists; preserve prior evidence")
        target.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
        write_json(target, report)
    mount = os.environ["SPOOLIN_RUNTIME_CONFIG_DIR"] + ":/run/spoolin-config:rw"
    run(installation.compose + ["run", "--rm", "--no-deps", "-T", "--user", "0:0", "--volume", mount,
        "--entrypoint", "rm", "core", "--", "/run/spoolin-config/restore-pending-validation"])
    if os.environ.get("GITHUB_ACTIONS") == "true" and os.environ.get("GITHUB_ENV"):
        with open(os.environ["GITHUB_ENV"], "a") as output:
            output.write("ACCOUNT_READ_KEYS_FILE=/run/spoolin-config/restore-keys.json\n")
    print("Coordinated restore verified through fresh write epochs, revoked restored credentials, all protected rows, artwork, and real TLS API reads")


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("backup", "restore"))
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--output")
    parser.add_argument("--input")
    parser.add_argument("--report")
    arguments = parser.parse_args()
    if arguments.mode == "backup" and not arguments.output:
        parser.error("backup requires --output DIRECTORY")
    if arguments.mode == "restore" and not arguments.input:
        parser.error("restore requires --input DIRECTORY")
    installation = Installation()
    if arguments.mode == "backup":
        backup(installation, arguments)
    else:
        restore(installation, arguments)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, KeyError) as failure:
        print(f"Checkpoint refused: {failure}", file=__import__("sys").stderr)
        raise SystemExit(4)
