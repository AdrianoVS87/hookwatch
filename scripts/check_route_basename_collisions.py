#!/usr/bin/env python3
"""
Fail-fast guard for static route basename collisions.

Some deploy targets (e.g., Vercel static routing) can treat files that differ
only by extension as conflicting route paths.

Example conflict:
  static/swagger-ui/hookwatch.js
  static/swagger-ui/hookwatch.css

Both map to the same extensionless route stem: static/swagger-ui/hookwatch
"""

from __future__ import annotations

from collections import defaultdict
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
ROUTE_DIRS = [
    ROOT / "api" / "src" / "main" / "resources" / "static",
    ROOT / "web" / "public",
]


def main() -> int:
    buckets: dict[str, list[Path]] = defaultdict(list)

    for route_dir in ROUTE_DIRS:
        if not route_dir.exists():
            continue

        for p in route_dir.rglob("*"):
            if not p.is_file():
                continue

            rel = p.relative_to(ROOT)

            # extensionless route key (path minus extension)
            stem_key = rel.with_suffix("").as_posix().lower()
            buckets[stem_key].append(rel)

    conflicts = {k: v for k, v in buckets.items() if len(v) > 1}

    if not conflicts:
        print("✅ No extension-based route basename collisions found.")
        return 0

    print("❌ Route basename collisions detected (same path, different extensions):")
    for key, files in sorted(conflicts.items()):
        print(f"\n- Route stem: {key}")
        for f in sorted(files):
            print(f"  • {f.as_posix()}")

    print("\nFix by renaming files so extensionless route stems are unique.")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
