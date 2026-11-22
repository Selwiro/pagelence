#!/usr/bin/env python3
"""Bump the single-source app version.

Usage:
    python3 tools/bump_version.py patch|minor|major
    python3 tools/bump_version.py --show

`version.properties` is read by `:app-phone/build.gradle.kts`; the about page
shows `BuildConfig.VERSION_NAME`. Keep this file the only place a release
number is written by hand.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "version.properties"
NAME = re.compile(r"^versionName=(\d+)\.(\d+)\.(\d+)\s*$", re.MULTILINE)
CODE = re.compile(r"^versionCode=(\d+)\s*$", re.MULTILINE)


def read() -> tuple[int, int, int, int]:
    text = VERSION_FILE.read_text(encoding="utf-8")
    name = NAME.search(text)
    code = CODE.search(text)
    if not name or not code:
        raise SystemExit(f"version.properties 格式不对：{VERSION_FILE}")
    return int(name.group(1)), int(name.group(2)), int(name.group(3)), int(code.group(1))


def write(major: int, minor: int, patch: int, code: int) -> None:
    text = VERSION_FILE.read_text(encoding="utf-8")
    text = NAME.sub(f"versionName={major}.{minor}.{patch}", text)
    text = CODE.sub(f"versionCode={code}", text)
    VERSION_FILE.write_text(text, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="递增 version.properties 的版本号")
    parser.add_argument("part", nargs="?", choices=["patch", "minor", "major"])
    parser.add_argument("--show", action="store_true", help="只打印当前版本")
    args = parser.parse_args()

    major, minor, patch, code = read()
    if args.show or not args.part:
        print(f"{major}.{minor}.{patch} ({code})")
        return

    if args.part == "major":
        major, minor, patch = major + 1, 0, 0
    elif args.part == "minor":
        minor, patch = minor + 1, 0
    else:
        patch += 1
    write(major, minor, patch, code + 1)
    print(f"{major}.{minor}.{patch} ({code + 1})")


if __name__ == "__main__":
    sys.exit(main())
