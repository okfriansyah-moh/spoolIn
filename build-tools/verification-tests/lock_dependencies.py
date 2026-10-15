"""Explicit lock maintenance; normal verification must not write locks."""

from pathlib import Path
from datetime import datetime, timezone
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]


def relocate_local_catalog(root=ROOT):
    source = root / "settings-gradle.lockfile"
    if not source.exists():
        return
    entries = [line.strip() for line in source.read_text().splitlines()
               if line.strip() and not line.startswith("#")]
    if entries != ["empty=incomingCatalogForLibs0"]:
        raise ValueError("Unexpected external settings resolution: retain the file and review its graph")
    target = root / "gradle/locks/settings/gradle.lockfile"
    target.parent.mkdir(parents=True, exist_ok=True)
    source.replace(target)


def main():
    metadata = ROOT / "gradle/verification-metadata.xml"
    previous = metadata.read_bytes() if metadata.exists() else None
    if metadata.exists():
        archive = ROOT / ".ai/evidence/lock-maintenance"
        archive.mkdir(parents=True, exist_ok=True)
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S-%f")
        archive.joinpath(f"{stamp}-verification-metadata.xml").write_bytes(metadata.read_bytes())
        metadata.unlink()
    try:
        process = subprocess.run(["./gradlew", "resolveLocks", "--write-locks", "--write-verification-metadata", "sha256", "--no-daemon", "--console", "plain"], cwd=ROOT)
    except BaseException:
        if previous is not None:
            metadata.write_bytes(previous)
        raise
    if process.returncode:
        if previous is not None:
            metadata.write_bytes(previous)
        return process.returncode
    relocate_local_catalog(ROOT)
    return 0


if __name__ == "__main__":
    sys.exit(main())
