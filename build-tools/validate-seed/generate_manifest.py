#!/usr/bin/env python3
"""Generate compact frozen seed manifest with 100 movies and 100 series identities."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "data" / "seed" / "manifest.json"


def entry(kind: str, index: int) -> dict:
    title = f"Seed {kind} {index:03d}"
    source_id = f"seed-{kind.lower()}-{index:03d}"
    body = json.dumps({"title": title, "sourceId": source_id}, sort_keys=True)
    digest = hashlib.sha256(body.encode()).hexdigest()
    return {
        "kind": kind,
        "sourceId": source_id,
        "title": title,
        "synopsis": f"Frozen synopsis for {title}.",
        "releaseYear": 1980 + (index % 40),
        "genreLabels": ["Drama"],
        "contentHash": digest,
    }


def main() -> None:
    movies = [entry("MOVIE", i) for i in range(1, 101)]
    series = [entry("SERIES", i) for i in range(1, 101)]
    manifest = {
        "schemaVersion": 1,
        "movies": movies,
        "series": series,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
