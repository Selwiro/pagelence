#!/usr/bin/env python3
"""Record Web-reader progress behaviour as a parity fixture for :core.

The Web reader's `advance` / `restoreProgress` (`work/web/src/reading.mjs`) are
the reference for what "已读单元数" and the resume paragraph mean. This script
replays those rules over the shipped Android book structure and writes the
expected results to

    core/src/test/resources/parity/progress-parity.json

Replaying the same sequences through `ProgressSemantics` in the unit test keeps
the two implementations aligned without loading any book text that is not
shipped.

Usage:
    python3 tools/migrate/record_progress_parity.py
    python3 tools/migrate/record_progress_parity.py --check
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "lib"))

from content import ANDROID, CONTENT, REPO, dump, read_json, write_outputs  # noqa: E402

BOOK = CONTENT / "books" / "doctor-dolittle" / "book.json"
FIXTURE = ANDROID / "core" / "src" / "test" / "resources" / "parity" / "progress-parity.json"

SCHEMA_VERSION = 2
LEGACY_SCHEMA_VERSIONS = (1, 2)


class WebIndex:
    def __init__(self, book: dict) -> None:
        self.units = [unit for chapter in book["chapters"] for unit in chapter["units"]]
        self.unit_ids = {unit["id"] for unit in self.units}
        self.paragraph_ids = {
            paragraph["id"] for unit in self.units for paragraph in unit["paragraphs"]
        }


def restore_progress(saved: dict | None, index: WebIndex) -> dict:
    """`restoreProgress` from reading.mjs."""
    # Android migrates v1 to v2, so both versions restore; the version gate
    # lives in the state codec, not in the progress rules.
    valid = bool(saved) and saved.get("schemaVersion") in LEGACY_SCHEMA_VERSIONS
    read = []
    if valid and isinstance(saved.get("readUnitIds"), list):
        for unit_id in saved["readUnitIds"]:
            if unit_id in index.unit_ids and unit_id not in read:
                read.append(unit_id)
    paragraph_id = saved.get("paragraphId") if valid and saved else None
    return {
        "paragraphId": paragraph_id if paragraph_id in index.paragraph_ids else None,
        "readUnitIds": read,
    }


def advance(progress: dict, units: list[dict], unit_id: str) -> dict:
    """`advance` from reading.mjs."""
    position = next((i for i, unit in enumerate(units) if unit["id"] == unit_id), -1)
    if position == -1:
        return progress
    next_unit = units[position + 1] if position + 1 < len(units) else None
    read = list(progress["readUnitIds"])
    if unit_id not in read:
        read.append(unit_id)
    return {
        "paragraphId": next_unit["paragraphs"][0]["id"] if next_unit else progress["paragraphId"],
        "readUnitIds": read,
    }


def sequential_cases(index: WebIndex) -> list[dict]:
    progress = {"paragraphId": None, "readUnitIds": []}
    cases = []
    for unit in index.units:
        progress = advance(progress, index.units, unit["id"])
        cases.append({
            "after": unit["id"],
            "readUnitCount": len(progress["readUnitIds"]),
            "paragraphId": progress["paragraphId"],
        })
    return cases


def shuffled_cases(index: WebIndex) -> list[dict]:
    ids = [unit["id"] for unit in index.units]
    alternating = []
    head, tail = 0, len(ids) - 1
    while head <= tail:
        alternating.append(ids[head])
        if head != tail:
            alternating.append(ids[tail])
        head += 1
        tail -= 1
    orders = {"reverse": list(reversed(ids)), "alternating": alternating}
    cases = []
    for name, order in orders.items():
        progress = {"paragraphId": None, "readUnitIds": []}
        for unit_id in order:
            progress = advance(progress, index.units, unit_id)
            cases.append({
                "order": name,
                "after": unit_id,
                "readUnitCount": len(progress["readUnitIds"]),
                "paragraphId": progress["paragraphId"],
            })
    return cases


def restore_cases(index: WebIndex) -> list[dict]:
    first = index.units[0]
    second = index.units[1]
    saved_cases = [
        None,
        {
            "schemaVersion": SCHEMA_VERSION,
            "paragraphId": "not-a-paragraph",
            "readUnitIds": ["not-a-unit", first["id"], first["id"]],
        },
        {
            "schemaVersion": SCHEMA_VERSION,
            "paragraphId": first["paragraphs"][0]["id"],
            "readUnitIds": [second["id"], first["id"]],
        },
        {
            "schemaVersion": 1,
            "paragraphId": first["paragraphs"][0]["id"],
            "readUnitIds": [second["id"], first["id"]],
        },
    ]
    cases = []
    for saved in saved_cases:
        restored = restore_progress(saved, index)
        cases.append({
            "saved": saved,
            "expectedParagraphId": restored["paragraphId"],
            "expectedReadUnitIds": restored["readUnitIds"],
        })
    return cases


def build_fixture(book: dict) -> dict:
    index = WebIndex(book)
    return {
        "schemaVersion": 1,
        "source": {
            "reference": "work/web/src/reading.mjs#advance, #restoreProgress",
            "book": "content/books/doctor-dolittle/book.json",
            "recordedBy": "tools/migrate/record_progress_parity.py",
        },
        "bookId": book["id"],
        "unitCount": len(index.units),
        "sequential": sequential_cases(index),
        "shuffled": shuffled_cases(index),
        "restore": restore_cases(index),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify the fixture is current")
    args = parser.parse_args()

    fixture = build_fixture(read_json(BOOK))
    stale = write_outputs({FIXTURE: dump(fixture)}, check=args.check)
    if args.check:
        if stale:
            print("stale: " + ", ".join(str(path.relative_to(REPO)) for path in stale))
            sys.exit(1)
        print("up to date")


if __name__ == "__main__":
    main()
