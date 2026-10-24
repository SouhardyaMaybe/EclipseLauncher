#!/usr/bin/env python3
"""Cheap documentation gate: required files, index coverage, relative links."""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
REQUIRED = [
    "README.md",
    "PROVENANCE.md",
    "docs/README.md",
    "docs/00-executive-brief.md",
    "docs/01-landscape.md",
    "docs/02-minecraft-platform.md",
    "docs/03-mod-loaders-and-content.md",
]
LINK = re.compile(r"\[[^\]]*\]\(([^)]+)\)")


def main() -> int:
    errors: list[str] = []
    for rel in REQUIRED:
        if not (ROOT / rel).is_file():
            errors.append(f"missing required file: {rel}")

    docs = sorted(p.name for p in (ROOT / "docs").glob("*.md") if p.name != "README.md")
    index = (ROOT / "docs/README.md").read_text(encoding="utf-8") if (ROOT / "docs/README.md").is_file() else ""
    for name in docs:
        if name not in index:
            errors.append(f"docs/README.md does not mention {name}")

    for md in [ROOT / "README.md", ROOT / "PROVENANCE.md", *sorted((ROOT / "docs").glob("*.md"))]:
        text = md.read_text(encoding="utf-8")
        for target in LINK.findall(text):
            target = target.strip()
            if target.startswith(("http://", "https://", "mailto:", "#")):
                continue
            path_part = target.split("#", 1)[0]
            if not path_part:
                continue
            resolved = (md.parent / path_part).resolve()
            if not resolved.exists():
                errors.append(f"{md.relative_to(ROOT)}: broken relative link -> {target}")

    for err in errors:
        print("FAIL " + err, file=sys.stderr)
    if errors:
        print(f"docs gate: {len(errors)} failure(s)", file=sys.stderr)
        return 1
    print(f"docs gate: passed ({len(docs)} chapters)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
