#!/usr/bin/env python3
"""Validate the one distro profile selected for the disposable codec runner."""
import argparse
import hashlib
import json
from pathlib import Path

PACKAGE = "apparmor-profiles"
VERSION = "4.0.1really4.0.1-0ubuntu0.24.04.9"
PACKAGE_BYTES = 39592
PACKAGE_SHA = "90b02aa006eea7702cd4e851343e469e41365dda42145a3cb035de1d6c773b8c"
PROFILE_SHA = "11d39094f044f0cda0febb3ad517b830301da6b2ce929664af09ee9e4dd264f9"
PAYLOAD = "usr/share/apparmor/extra-profiles/bwrap-userns-restrict"
FILENAME = f"pool/main/a/apparmor/{PACKAGE}_{VERSION}_all.deb"
SOURCE = "https://archive.ubuntu.com/ubuntu"


def paragraphs(text):
    """Read bounded Deb822 output; reject duplicate fields rather than guess."""
    if len(text) > 1048576:
        raise ValueError("APT metadata exceeds its inspection bound")
    for block in text.strip().split("\n\n"):
        row = {}
        for line in block.splitlines():
            if not line or line[0].isspace():
                continue
            key, separator, value = line.partition(":")
            if not separator or key in row:
                raise ValueError("Invalid or duplicate APT metadata field")
            row[key] = value.strip()
        if row:
            yield row


def validate_metadata(text):
    rows = [row for row in paragraphs(text) if row.get("Package") == PACKAGE]
    expected = {
        "Package": PACKAGE, "Version": VERSION, "Architecture": "all",
        "Filename": FILENAME, "Size": str(PACKAGE_BYTES), "SHA256": PACKAGE_SHA,
    }
    if len(rows) != 1 or any(rows[0].get(key) != value for key, value in expected.items()):
        raise ValueError("Signed APT metadata does not match the pinned profile package")
    return expected


def validate_package(path):
    if path.is_symlink() or not path.is_file() or path.stat().st_size != PACKAGE_BYTES:
        raise ValueError("Unexpected profile package type or size")
    if hashlib.sha256(path.read_bytes()).hexdigest() != PACKAGE_SHA:
        raise ValueError("Profile package checksum differs from the inspected distro payload")


def validate_profile(path):
    if path.is_symlink() or not path.is_file() or path.stat().st_size > 16384:
        raise ValueError("Unexpected profile payload type or size")
    data = path.read_bytes()
    if hashlib.sha256(data).hexdigest() != PROFILE_SHA:
        raise ValueError("Restrictive profile checksum differs from the inspected distro payload")
    required = (
        "profile bwrap /usr/bin/bwrap flags=(attach_disconnected)",
        "allow px /** -> bwrap//&unpriv_bwrap,",
        "profile unpriv_bwrap flags=(attach_disconnected)",
        "allow pix /** -> &unpriv_bwrap,", "audit deny capability,",
    )
    text = data.decode("utf-8")
    if any(value not in text for value in required):
        raise ValueError("Expected attachment, child transition or capability denial is missing")


def validate_loaded(text):
    relevant = [line.strip() for line in text.splitlines()
                if line.split(" ", 1)[0] in {"bwrap", "unpriv_bwrap"}]
    if sorted(relevant) != ["bwrap (enforce)", "unpriv_bwrap (enforce)"]:
        raise ValueError("Both distro profiles must be loaded exactly once in enforce mode")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["metadata", "package", "profile", "loaded", "report"])
    parser.add_argument("path", type=Path)
    parser.add_argument("--sha")
    parser.add_argument("--run-id")
    parser.add_argument("--attempt")
    parser.add_argument("--image-version")
    parser.add_argument("--controls-file", type=Path)
    args = parser.parse_args()
    validators = {"metadata": lambda: validate_metadata(args.path.read_text()),
                  "package": lambda: validate_package(args.path),
                  "profile": lambda: validate_profile(args.path),
                  "loaded": lambda: validate_loaded(args.path.read_text())}
    if args.action != "report":
        validators[args.action]()
        return
    report = {
        "status": "profile-loaded-probe-required", "assessedSha": args.sha,
        "runId": args.run_id, "attempt": args.attempt, "runner": "github-hosted",
        "runnerOs": "Linux", "imageOs": "ubuntu24", "imageVersion": args.image_version,
        "aptSource": SOURCE, "suite": "noble-updates", "component": "main",
        "signedBy": "/usr/share/keyrings/ubuntu-archive-keyring.gpg",
        "package": PACKAGE, "packageVersion": VERSION, "packageBytes": PACKAGE_BYTES,
        "packageSha256": PACKAGE_SHA, "payloadPath": PAYLOAD, "profileSha256": PROFILE_SHA,
        "loadedProfiles": ["bwrap (enforce)", "unpriv_bwrap (enforce)"],
        "restrictedUnprivilegedUserNamespaces": 1,
        "unchangedCallerControls": args.controls_file.read_text().splitlines(),
        "hostSandboxProbe": "pending", "productionContainerSandboxProbe": "pending",
    }
    args.path.write_text(json.dumps(report, indent=2) + "\n")


if __name__ == "__main__":
    try:
        main()
    except (OSError, UnicodeError, ValueError) as error:
        raise SystemExit(str(error)) from error
