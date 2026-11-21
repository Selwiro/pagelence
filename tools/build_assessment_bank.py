#!/usr/bin/env python3
"""Build the placement-test bank for Pagelence Android.

Merges the editorial sources in content-src/assessment/*.json into
content/assessment/bank.json and refuses to write an invalid bank.

Usage:
    python3 tools/build_assessment_bank.py          # build
    python3 tools/build_assessment_bank.py --check   # verify freshness
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "lib"))

from assessment import build_bank, dump_bank, validate_bank  # noqa: E402

ANDROID = Path(__file__).resolve().parents[1]
REPO = ANDROID
OUT = ANDROID / "content" / "assessment" / "bank.json"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify generated file is current")
    args = parser.parse_args()

    bank = build_bank(ANDROID)
    errors, warnings = validate_bank(bank)
    for message in warnings:
        print(f"warning: {message}")
    if errors:
        for message in errors:
            print(f"error: {message}")
        print(f"{len(errors)} error(s); bank not written")
        sys.exit(1)

    text = dump_bank(bank)
    current = OUT.read_text(encoding="utf-8") if OUT.is_file() else None
    if args.check:
        if current != text:
            print(f"{OUT.relative_to(REPO)} is stale; rerun tools/build_assessment_bank.py")
            sys.exit(1)
        print("assessment bank ok")
        return

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text, encoding="utf-8")
    levels: dict[str, int] = {}
    for question in bank["questions"]:
        levels[question["level"]] = levels.get(question["level"], 0) + 1
    print(
        f"wrote {OUT.relative_to(REPO)}: {len(bank['questions'])} questions, "
        f"{len(bank['passages'])} passages, levels "
        + ", ".join(f"{level} {count}" for level, count in sorted(levels.items()))
    )


if __name__ == "__main__":
    main()
