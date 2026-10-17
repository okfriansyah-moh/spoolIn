#!/usr/bin/env python3
"""Validate data/seed/manifest.json: 100 unique movie + 100 unique series IDs and required fields."""
from __future__ import annotations

import json
import sys
from pathlib import Path

REQUIRED = ("kind", "sourceId", "title", "synopsis", "releaseYear", "genreLabels", "contentHash")


def main() -> int:
    root = Path(__file__).resolve().parents[2]
    manifest_path = root / "data" / "seed" / "manifest.json"
    data = json.loads(manifest_path.read_text(encoding="utf-8"))
    movies = data.get("movies", [])
    series = data.get("series", [])
    errors: list[str] = []
    if len(movies) != 100:
        errors.append(f"expected 100 movies, found {len(movies)}")
    if len(series) != 100:
        errors.append(f"expected 100 series, found {len(series)}")
    for label, rows in ("movies", movies), ("series", series):
        ids = [row.get("sourceId") for row in rows]
        if len(ids) != len(set(ids)):
            errors.append(f"duplicate sourceId in {label}")
        for index, row in enumerate(rows):
            for key in REQUIRED:
                if key not in row or row[key] in (None, "", []):
                    errors.append(f"{label}[{index}] missing {key}")
    if errors:
        for err in errors:
            print(err, file=sys.stderr)
        return 1
    print(f"manifest ok: {len(movies)} movies, {len(series)} series")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
