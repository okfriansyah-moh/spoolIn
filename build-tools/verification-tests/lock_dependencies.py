"""Explicit lock maintenance; normal verification must not write locks."""

from pathlib import Path
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
    process = subprocess.run(["./gradlew", "resolveLocks", "--write-locks", "--write-verification-metadata", "sha256", "--no-daemon", "--console", "plain"], cwd=ROOT)
    if process.returncode:
        return process.returncode
    relocate_local_catalog()
    return 0


if __name__ == "__main__":
    sys.exit(main())
