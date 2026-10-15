"""Test-only F-STACK/F-CLOCK/F-UNIT interfaces; real services remain pending."""

from dataclasses import dataclass
from pathlib import Path
import json
import os
import secrets
import shutil
import socket
import tempfile
from uuid import UUID, uuid4


class SetupError(ValueError):
    """A required test configuration or runtime is unavailable (exit 2)."""


def validate_namespace(value):
    if not value.startswith("atdd-"):
        raise SetupError("Refusing a normal installation name")
    try:
        run_id = UUID(value[5:])
    except (ValueError, AttributeError) as error:
        raise SetupError("Fixture namespace requires a lowercase run UUID") from error
    if str(run_id) != value[5:] or run_id.version != 4:
        raise SetupError("Fixture namespace requires a lowercase UUIDv4")
    return value


def protected_secret(path):
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, "w") as stream:
        stream.write(secrets.token_urlsafe(48))


@dataclass
class TestInstallation:
    namespace: str
    root: Path
    ports: list
    reservations: list

    @classmethod
    def create(cls, namespace=None):
        name = validate_namespace(namespace or f"atdd-{uuid4()}")
        root = Path(tempfile.mkdtemp(prefix=f"{name}-"))
        fixture = cls(name, root, [], [])
        try:
            fixture.prepare()
        except BaseException:
            fixture.reset()
            raise
        return fixture

    def prepare(self):
        self.root.joinpath("fixture.json").write_text(json.dumps({"namespace": self.namespace}))
        self.root.joinpath("private").mkdir(mode=0o700)
        self.root.joinpath("artwork").mkdir(mode=0o700)
        for owner in ("core", "catalog", "media"):
            protected_secret(self.root / "private" / f"{owner}-synthetic-token")
            reservation = socket.socket()
            reservation.bind(("127.0.0.1", 0))
            self.reservations.append(reservation)
            self.ports.append(reservation.getsockname()[1])

    def configuration(self):
        return {
            "namespace": self.namespace,
            "clock": "2026-10-12T03:00:00Z",
            "databases": [f"spoolin_{owner}" for owner in ("core", "catalog", "media")],
            "role_families": ["runtime", "migrator", "maintenance"],
            "ports": self.ports,
            "artwork": str(self.root / "artwork"),
        }

    def start_services(self):
        raise SetupError("Real PostgreSQL/Core/Catalog/Media fixture is pending its owning tasks")

    def actors(self):
        raise SetupError("F-ACTORS provisioning is pending the real identity fixture")

    def catalog(self):
        raise SetupError("F-CATALOG provisioning is pending the real catalog fixture")

    def reset(self):
        for reservation in self.reservations:
            reservation.close()
        self.reservations.clear()
        validate_namespace(self.namespace)
        marker = json.loads(self.root.joinpath("fixture.json").read_text())
        if marker != {"namespace": self.namespace} or not self.root.name.startswith(f"{self.namespace}-"):
            raise SetupError("Cleanup refused: fixture ownership marker differs")
        shutil.rmtree(self.root)


def preserve_failure(primary_exit, cleanup):
    try:
        cleanup()
    except (OSError, SetupError):
        return primary_exit or 2
    return primary_exit
