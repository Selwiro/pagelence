#!/usr/bin/env python3
"""Check the Android string catalog for pairing and terminology mistakes.

Usage:
    python3 tools/check_strings.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[1]
UI = ANDROID / "presentation" / "src" / "main" / "kotlin" / "com" / "pagelence" / "uim3"
INTERFACE = UI / "Strings.kt"
ZH = UI / "StringsZh.kt"
EN = UI / "StringsEn.kt"

KEY = re.compile(r"^\s+(?:override )?val (\w+): String", re.MULTILINE)


def keys(path: Path) -> list[str]:
    return KEY.findall(path.read_text(encoding="utf-8"))


def main() -> None:
    required = keys(INTERFACE)
    zh = keys(ZH)
    en = keys(EN)
    errors: list[str] = []
    if set(zh) != set(required):
        errors.append(f"StringsZh keys differ: missing {sorted(set(required) - set(zh))[:5]}, extra {sorted(set(zh) - set(required))[:5]}")
    if set(en) != set(required):
        errors.append(f"StringsEn keys differ: missing {sorted(set(required) - set(en))[:5]}, extra {sorted(set(en) - set(required))[:5]}")
    if "您" in ZH.read_text(encoding="utf-8"):
        errors.append("StringsZh uses 您; the product voice is 你")
    for message in errors:
        print(f"error: {message}")
    if errors:
        sys.exit(1)
    print(f"strings ok: {len(required)} keys, zh/en paired")


if __name__ == "__main__":
    main()
