#!/usr/bin/env python3
"""Build `content/store.json` from the editorial shelf and books index.

Sources:
    content-src/shelf.json     order, status, monogram, summary; planned identity
    content/books-index.json   which books actually ship

Usage:
    python3 tools/build_shelf.py          # build
    python3 tools/build_shelf.py --check  # verify freshness
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "lib"))

from content import REPO, dump  # noqa: E402
from shelf import STORE, build_store, load_index, load_shelf, store_errors  # noqa: E402


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify store.json is current")
    args = parser.parse_args()

    shelf = load_shelf()
    index = load_index()
    try:
        store = build_store(shelf, index)
    except ValueError as error:
        raise SystemExit(f"error: {error}")
    errors = store_errors(store, index)
    if errors:
        raise SystemExit("error: " + "; ".join(errors))
    text = dump(store)
    if args.check:
        if not STORE.is_file() or STORE.read_text(encoding="utf-8") != text:
            print(f"stale: {STORE.relative_to(REPO)}")
            raise SystemExit(1)
        print("store ok")
        return
    STORE.write_text(text, encoding="utf-8")
    print(f"wrote {STORE.relative_to(REPO)}")


if __name__ == "__main__":
    main()
