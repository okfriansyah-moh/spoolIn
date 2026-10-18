"""Owned remote capacity installation, protected setup, actual Make startup and bounded evidence."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import platform
import subprocess
import time

ROOT = Path(__file__).resolve().parents[2]


def checked(arguments, environment=None, timeout=180, input_text=None, maximum=262144):
    result = subprocess.run(arguments, env=environment, input=input_text, text=True,
                            capture_output=True, check=False, timeout=timeout, cwd=ROOT)
    if result.returncode or len(result.stdout.encode()) > maximum:
        raise RuntimeError("Remote capacity command failed or exceeded bounded output; raw output withheld")
    return result.stdout


def require_hosted():
    values = os.environ
    valid = values.get("GITHUB_ACTIONS") == "true" and values.get("RUNNER_ENVIRONMENT") == "github-hosted"
    valid = valid and values.get("RUNNER_OS") == "Linux" and platform.system() == "Linux"
    if not valid or values.get("SPOOLIN_SKIP_COMPOSE") == "1":
        raise RuntimeError("Capacity execution requires GitHub-hosted Linux and actual Compose")
    for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT"):
        if not values.get(key, "").isdigit():
            raise RuntimeError("Actual numeric hosted run identity is required")
    path = Path(values["RUNNER_TEMP"])
    if not path.is_absolute() or not path.is_dir():
        raise RuntimeError("Runner-owned absolute temporary directory required")


def host_record():
    require_hosted()
    memory = next(row.split()[1] for row in Path("/proc/meminfo").read_text().splitlines() if row.startswith("MemTotal:"))
    return {"os": platform.platform(), "architecture": platform.machine(), "cpuCount": os.cpu_count(),
            "cpuModel": next((line.split(":", 1)[1].strip() for line in Path("/proc/cpuinfo").read_text().splitlines()
                              if line.startswith("model name")), "reported-in-cpuinfo"),
            "memoryBytes": int(memory) * 1024, "runnerOS": os.environ["RUNNER_OS"],
            "imageOS": os.environ.get("ImageOS"), "imageVersion": os.environ.get("ImageVersion"),
            "diskFreeBytes": __import__("shutil").disk_usage(ROOT).free,
            "runId": os.environ["GITHUB_RUN_ID"], "runAttempt": os.environ["GITHUB_RUN_ATTEMPT"],
            "workflow": os.environ.get("GITHUB_WORKFLOW"), "job": os.environ.get("GITHUB_JOB"),
            "runUrl": os.environ["GITHUB_SERVER_URL"] + "/" + os.environ["GITHUB_REPOSITORY"] + "/actions/runs/" + os.environ["GITHUB_RUN_ID"],
            "jdk": checked([str(Path(os.environ["JAVA_HOME"]) / "bin/java"), "--version"]).strip(),
            "testedSha": checked(["git", "rev-parse", "HEAD"]).strip()}


class CapacityStack:
    def __init__(self, phase, repetition, output):
        require_hosted()
        if phase not in {"startup", "reader", "writer", "plans"} or repetition not in {1, 2, 3}:
            raise ValueError("Fixed capacity phase/repetition required")
        if phase == "plans" and repetition != 1:
            raise ValueError("One separately owned stress fixture is specified")
        self.phase = phase
        self.namespace = f"capacity-{phase}-{repetition}"
        self.installation = f"ci-{os.environ['GITHUB_RUN_ID']}-{os.environ['GITHUB_RUN_ATTEMPT']}-{self.namespace}"
        self.output = output
        self.environment = None
        self.compose = ["docker", "compose", "--project-name", self.installation, "-f", str(ROOT / "ops/compose.yaml")]

    def prepare(self):
        self.require_serialized()
        protected = Path(os.environ["RUNNER_TEMP"]) / (self.installation + "-environment")
        descriptor = os.open(protected, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        os.close(descriptor)
        environment = {**os.environ, "GITHUB_ENV": str(protected)}
        checked(["python3", "ops/ci/prepare-environment.py", "--namespace", self.namespace], environment, timeout=180)
        updates = dict(line.split("=", 1) for line in protected.read_text().splitlines() if line)
        self.environment = {**os.environ, **updates}
        if updates.get("SPOOLIN_INSTALLATION") != self.installation:
            raise RuntimeError("Preparation did not provision the exact capacity namespace")
        self.verify_quotas()
        if self.phase in {"reader", "writer"} and "core-timing.jfc" not in self.environment.get("CORE_JAVA_TOOL_OPTIONS", ""):
            raise RuntimeError("Actual Core custom-only JFR configuration is required")

    def require_serialized(self):
        prefix = f"ci-{os.environ['GITHUB_RUN_ID']}-{os.environ['GITHUB_RUN_ATTEMPT']}"
        rows = checked(["docker", "ps", "--format", '{{.Label "com.docker.compose.project"}}']).splitlines()
        if any(name == prefix or name.startswith(prefix + "-") for name in rows):
            raise RuntimeError("Pause the source and restored stacks before serialized capacity execution")
        stale = checked([*self.compose, "ps", "--all", "--quiet"]).strip()
        if stale:
            raise RuntimeError("Independent capacity repetition already has retained containers; refusing reuse")

    def verify_quotas(self):
        names = ("RATE_LIMIT_READ_MAX_REQUESTS", "RATE_LIMIT_MUTATION_MAX_REQUESTS")
        for name in names:
            if self.environment.get(name) != "1000000":
                raise RuntimeError("Preparation must select validated positive benchmark quota profiles")
            if self.environment.get(name.replace("MAX_REQUESTS", "WINDOW_SECONDS")) != "60":
                raise RuntimeError("Preparation must preserve the60-second benchmark window")

    def start(self, label="startup"):
        before = self.image_ids()
        started = time.monotonic_ns()
        checked(["make", "start"], self.environment, timeout=1500, maximum=1048576)
        elapsed = time.monotonic_ns() - started
        status = json.loads(checked(["make", "status"], self.environment, timeout=90))
        result = {"command": ["make", "start"], "elapsedNanos": elapsed, "exit": 0,
                  "imagesBefore": before, "imagesAfter": self.image_ids(), "readiness": status}
        self.write(label + ".json", result)
        self.verify_running_quotas()
        return result

    def image_ids(self):
        config = json.loads(checked([*self.compose, "config", "--format", "json"], self.environment, maximum=1048576))
        services = ("postgres", "core", "catalog", "media", "mcp", "prometheus", "alertmanager", "grafana")
        return {name: checked(["docker", "image", "inspect", "--format", "{{.Id}}", config["services"][name]["image"]]).strip()
                for name in services}

    def verify_running_quotas(self):
        rows = json.loads(checked([*self.compose, "config", "--format", "json"], self.environment, maximum=1048576))
        core = rows["services"]["core"]["environment"]
        keys = ("RATE_LIMIT_READ_MAX_REQUESTS", "RATE_LIMIT_READ_WINDOW_SECONDS",
                "RATE_LIMIT_MUTATION_MAX_REQUESTS", "RATE_LIMIT_MUTATION_WINDOW_SECONDS")
        actual = {name: str(core.get(name)) for name in keys}
        if any(actual[name] != self.environment[name] for name in keys):
            raise RuntimeError("Actual Compose admission quota differs from isolated profile")
        source = Path(self.environment["SPOOLIN_RUNTIME_CONFIG_DIR"])
        revision = checked(["sudo", "-n", "python3", "-c",
                            "import json,sys;print(json.load(open(sys.argv[1]))['profileRevision'])",
                            str(source / "rate-agreement")]).strip()
        if not __import__("re").fullmatch("[0-9a-f]{64}", revision):
            raise RuntimeError("Protected active quota agreement revision is invalid")
        enabled = checked(["sudo", "-n", "python3", "-c",
                           "import json,sys;print(json.load(open(sys.argv[1]))['spoolin']['rate-limit']['enabled'])",
                           str(source / "core.json")]).strip()
        if enabled != "True":
            raise RuntimeError("Actual protected Core rate admission is disabled")
        self.write("admission.json", {"profiles": actual, "enabled": True, "profileRevision": revision,
                                     "configurationSource": "isolated environment through strict owner configuration",
                                     "dimensions": {"reader": "READ/SOURCE", "writer": "MUTATION/CREDENTIAL"},
                                     "agreement": "Core startup/readiness must validate the protected profile revision and key canary"})

    def database(self, owner, sql, role="runtime", maximum=1048576, timeout=30):
        if owner not in {"core", "catalog", "media"} or role not in {"runtime", "backup", "maintenance", "postgres"}:
            raise ValueError("Fixed owner and role required")
        user = "postgres" if role == "postgres" else f"spoolin_{owner}_{role}"
        secret = "/run/spoolin/postgres-password" if role == "postgres" else f"/run/secrets/{owner}-{role}-password"
        shell = 'export PGPASSWORD="$(cat ' + secret + ')"; exec psql -X -qAt -v ON_ERROR_STOP=1 '
        shell += '--set=VERBOSITY=sqlstate -h /var/run/postgresql -U ' + user + ' -d spoolin_' + owner
        return checked([*self.compose, "exec", "-T", "--user", "0:0", "postgres", "sh", "-ec", shell],
                       self.environment, timeout, "SET search_path=appdata; " + sql, maximum).strip()

    def resources(self, label):
        names = checked([*self.compose, "ps", "--quiet"], self.environment).splitlines()
        rows = [json.loads(line) for line in checked(["docker", "stats", "--no-stream", "--format", "{{json .}}", *names]).splitlines()]
        safe = [{key: row.get(key) for key in ("Name", "CPUPerc", "MemUsage", "MemPerc", "PIDs", "BlockIO")} for row in rows]
        states = []
        for container in names:
            raw = json.loads(checked(["docker", "inspect", "--format", "{{json .State}}", container]))
            states.append({key: raw.get(key) for key in ("Running", "OOMKilled", "ExitCode", "StartedAt", "FinishedAt")})
        self.write("resources-" + label + ".json", {"containers": safe, "states": states})
        if any(row["OOMKilled"] for row in states):
            raise RuntimeError("Actual capacity workload triggered a container out-of-memory failure")

    def write(self, name, value):
        (self.output / name).write_text(json.dumps(value, indent=2) + "\n")

    def failure_evidence(self):
        if not self.environment:
            return
        command = [*self.compose, "logs", "--no-color", "--tail", "100", "core", "catalog", "media", "mcp"]
        result = subprocess.run(command, env=self.environment, capture_output=True, text=True, timeout=60, check=False)
        details = []
        for line in result.stdout.splitlines():
            if len(line) > 16384 or "{" not in line:
                continue
            try:
                value = json.loads(line[line.index("{"):])
            except ValueError:
                continue
            attributes = value.get("attributes") or {}
            safe = {key: attributes[key] for key in ("event", "error_class", "code", "operation") if key in attributes}
            # These are the production structured classifier's fixed fields, never raw messages/stacks or arguments.
            details.append({"level": value.get("level"), "attributes": safe})
        self.write("failure-diagnostics.json", {"structuredOwnerRecords": details, "rawMessagesRetained": False})
        result = subprocess.run(["make", "status"], env=self.environment, capture_output=True, text=True, timeout=90, check=False)
        if len(result.stdout) <= 262144:
            try:
                status = json.loads(result.stdout)
            except ValueError:
                status = {"reason": "readiness did not return bounded JSON"}
            self.write("failure-readiness.json", {"exit": result.returncode, "status": status})

    def cleanup(self):
        if self.environment:
            checked([*self.compose, "down", "--volumes", "--timeout", "65"], self.environment, timeout=150)


def seed_baseline(stack):
    path = ROOT / "ops/ci/verify-seed-runtime.py"
    spec = importlib.util.spec_from_file_location("capacity_seed_boundary", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    validator_path = ROOT / "build-tools/validate-seed/validate_manifest.py"
    spec = importlib.util.spec_from_file_location("capacity_seed_validator", validator_path)
    validator = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(validator)
    old = dict(os.environ)
    try:
        os.environ.update(stack.environment)
        manifest = json.loads((ROOT / "data/seed/manifest.json").read_text())
        snapshot = module.snapshot()
        module.verify_titles(manifest, snapshot)
        roles = module.verify_people_credits(manifest, snapshot, validator)
        module.verify_provenance(manifest, snapshot, validator, roles)
    finally:
        os.environ.clear()
        os.environ.update(old)
    stack.write("baseline.json", {"result": "PASS", "movies": 100, "series": 100,
                                  "manifestHash": manifest["snapshotSHA256"], "fields": "all imported database/provenance checks",
                                  "sourceVerifierSha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                                  "proof": "database baseline only; artwork/browser seed acceptance remains separate"})
    return manifest
