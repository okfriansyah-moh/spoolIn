"""Retain licenses and registry provenance for every resolved lock coordinate."""

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from functools import lru_cache
import hashlib
import json
from pathlib import Path
import subprocess
from urllib.parse import quote
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


@lru_cache(maxsize=None)
def fetch(url):
    argv = ["curl", "--location", "--silent", "--show-error", "--connect-timeout", "10", "--max-time", "30",
            "--write-out", "\nHTTP_STATUS:%{http_code}\n", url]
    result = subprocess.run(argv, capture_output=True)
    body, _, status = result.stdout.rpartition(b"\nHTTP_STATUS:")
    return body, {"argv": argv, "exit_code": result.returncode,
                  "http_status": status.decode().strip() or "000",
                  "body_sha256": hashlib.sha256(body).hexdigest(),
                  "stderr": result.stderr.decode(errors="replace")[:300]}


def pom_licenses(group, artifact, version, depth=0):
    path = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"
    body, provenance = fetch("https://repo.maven.apache.org/maven2/" + path)
    if provenance["http_status"] == "404":
        body, provenance = fetch("https://plugins.gradle.org/m2/" + path)
    if provenance["http_status"] != "200":
        return [], [provenance], "Registry metadata unavailable"
    try:
        document = ET.fromstring(body)
    except ET.ParseError:
        return [], [provenance], "Invalid POM response"
    licenses = [{"name": entry.findtext("m:name", namespaces=NS),
                 "url": entry.findtext("m:url", namespaces=NS)}
                for entry in document.findall("m:licenses/m:license", NS)]
    parent = document.find("m:parent", NS)
    if not licenses and parent is not None and depth < 5:
        coordinates = [parent.findtext("m:" + key, namespaces=NS) for key in ("groupId", "artifactId", "version")]
        if all(coordinates) and not any("${" in part for part in coordinates):
            inherited, sources, limitation = pom_licenses(*coordinates, depth + 1)
            return inherited, [provenance, *sources], limitation
    return licenses, [provenance], None if licenses else "POM has no license declaration"


def maven_record(coordinate):
    group, artifact, version = coordinate.split(":", 2)
    licenses, sources, limitation = pom_licenses(group, artifact, version)
    implementation = None
    if not licenses and artifact.endswith(".gradle.plugin") and sources:
        body, _ = fetch(sources[0]["argv"][-1])
        document = ET.fromstring(body)
        dependency = document.find("m:dependencies/m:dependency", NS)
        if dependency is not None:
            parts = [dependency.findtext("m:" + key, namespaces=NS) for key in ("groupId", "artifactId", "version")]
            if all(parts):
                implementation = ":".join(parts)
                resolved = maven_record(implementation)
                licenses = resolved["licenses"]
                sources.extend(resolved["sources"])
                limitation = resolved["limitation"]
    if not licenses and coordinate == "org.jlleitschuh.gradle:ktlint-gradle:13.1.0":
        body, source = fetch("https://api.github.com/repos/JLLeitschuh/ktlint-gradle/license?ref=v13.1.0")
        sources.append(source)
        if source["http_status"] == "200":
            declared = json.loads(body)["license"]
            licenses = [{"name": declared["name"], "spdx_id": declared["spdx_id"], "url": declared["url"]}]
            limitation = None
    return {"coordinate": coordinate, "licenses": licenses, "sources": sources,
            "limitation": limitation, "metadata_marker": artifact.endswith(".gradle.plugin"),
            "marker_implementation": implementation}


def npm_record(item):
    path, metadata = item
    name = path.rsplit("node_modules/", 1)[-1]
    version = metadata["version"]
    license_value = metadata.get("license")
    record = {"name": name, "version": version, "license": license_value,
              "integrity": metadata.get("integrity"), "resolved": metadata.get("resolved")}
    if not license_value:
        body, source = fetch(f"https://registry.npmjs.org/{quote(name, safe='@')}/{version}")
        record["source"] = source
        if source["http_status"] == "200":
            record["license"] = json.loads(body).get("license")
    return record


def main():
    coordinates = set()
    for lock in ROOT.joinpath("gradle").rglob("gradle.lockfile"):
        coordinates.update(line.split("=", 1)[0] for line in lock.read_text().splitlines()
                           if line and not line.startswith(("#", "empty=")))
    lock = json.loads(ROOT.joinpath("package-lock.json").read_text())
    packages = [(path, value) for path, value in lock["packages"].items()
                if "node_modules/" in path and not value.get("link")]
    with ThreadPoolExecutor(max_workers=10) as pool:
        maven = list(pool.map(maven_record, sorted(coordinates)))
        npm = list(pool.map(npm_record, packages))
    report = {"observed_at_utc": datetime.now(timezone.utc).isoformat(), "maven": maven, "npm": npm,
              "limitations": ["Registry license declarations are an inventory, not a legal opinion.",
                              "Plugin marker metadata can omit the implementation artifact license."]}
    output = ROOT / ".ai/evidence/t001-dependency-inventory.json"
    output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"maven": len(maven), "npm": len(npm),
                      "maven_without_declared_license": sum(not row["licenses"] for row in maven),
                      "npm_without_declared_license": sum(not row["license"] for row in npm)}))


if __name__ == "__main__":
    main()
