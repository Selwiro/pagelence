#!/usr/bin/env python3
"""Import the shared, third-party reference tables used by the Android app.

The app needs read-only reference files that are not part of any single book:

    content/third-party/dictionaries/        ECDICT licence text
    content/grammar/grammar-library.json     grammar points shared by books

`word-levels.json` is no longer copied from the Web archive: it is built by
`tools/build_word_levels.py` from five layers, with `--check` (T121).

`ecdict.json` itself is **not** imported here: it is a general core dictionary
built from the full upstream ECDICT table by `tools/build_dictionaries.py`.
This script only carries the licence text over.

They are imported once from `work/web/content/` (the archived Web sources) and
then owned by this repository; Web changes no longer flow here.

Book texts are **not** imported by this script. Only books whose copyright
status is clear for distribution are shipped (currently Doctor Dolittle and
Alice); each book is built by its own `tools/build_*.py` script.

Usage:
    python3 tools/migrate/import_reference_data.py
    python3 tools/migrate/import_reference_data.py --check
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "lib"))

from content import ANDROID, CONTENT, REPO, dump, read_json, write_outputs  # noqa: E402

GRAMMAR_SOURCE = ANDROID / "content-src" / "grammar" / "points.json"
#: 许可原文随仓库分发，构建时按原样写回（幂等，不再从 Web 归档取）。
DICTIONARIES = CONTENT / "third-party" / "dictionaries"

THIRD_PARTY = CONTENT / "third-party"
GRAMMAR_OUT = CONTENT / "grammar" / "grammar-library.json"


def build_outputs() -> dict[Path, str]:
    outputs = {}
    outputs[THIRD_PARTY / "dictionaries" / "ecdict-license.txt"] = (
        (DICTIONARIES / "ecdict-license.txt").read_text(encoding="utf-8")
    )

    # Grammar is Android-editorial content now: one schema-2 source, copied
    # verbatim to the shipped library. The archived Web library is no longer
    # merged, so an id or field change happens in one place.
    outputs[GRAMMAR_OUT] = dump(read_json(GRAMMAR_SOURCE))
    return outputs


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify generated files are current")
    args = parser.parse_args()

    stale = write_outputs(build_outputs(), check=args.check)
    if args.check:
        if stale:
            print("stale: " + ", ".join(str(path.relative_to(REPO)) for path in stale))
            sys.exit(1)
        print("up to date")
        return
    print("reference tables imported")


if __name__ == "__main__":
    main()
