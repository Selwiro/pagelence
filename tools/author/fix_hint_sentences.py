#!/usr/bin/env python3
"""Point every hint's `sentence` at the unit sentence that contains its target.

`hints[].sentence` is a unit-wide 1-based sentence index (same scale as
`quiz[].sentence`), but authors usually know only the paragraph. This helper
finds the sentence in the unit that contains the target and rewrites the index.

Usage:
    python3 tools/author/fix_hint_sentences.py 7
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "lib"))

from content import ANDROID, split_sentences  # noqa: E402

SRC_DIR = ANDROID / "content-src" / "doctor-dolittle"
BOOK = ANDROID / "content" / "books" / "doctor-dolittle" / "book.json"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("chapter", type=int)
    args = parser.parse_args()

    number = args.chapter
    path = SRC_DIR / f"chapter-{number:02d}.json"
    overlay = json.loads(path.read_text(encoding="utf-8"))
    book = json.loads(BOOK.read_text(encoding="utf-8"))
    chapter = next(c for c in book["chapters"] if c["number"] == number)
    by_number = {unit["number"]: unit for unit in chapter["units"]}

    changed = 0
    unresolved = 0
    chapter_offset = 0
    for unit_overlay in overlay.get("units") or []:
        unit = by_number.get(unit_overlay.get("number"))
        if unit is None:
            continue
        unit_sentences = [
            piece
            for paragraph in unit["paragraphs"]
            for piece in split_sentences(paragraph["text"])
        ]
        # Some authors number sentences across the whole chapter. Accept that
        # and rewrite quiz indexes into the unit-wide scale.
        for item in unit_overlay.get("quiz") or []:
            index = item.get("sentence")
            if isinstance(index, int) and index > len(unit_sentences) and index <= chapter_offset + len(unit_sentences):
                item["sentence"] = index - chapter_offset
                changed += 1
                print(f"quiz {unit['id']}: sentence {index} -> {item['sentence']}")
        paragraphs = {p["id"]: p for p in unit["paragraphs"]}
        for paragraph_id, source in (unit_overlay.get("paragraphs") or {}).items():
            paragraph = paragraphs.get(paragraph_id)
            if paragraph is None:
                continue
            for hint in source.get("hints") or []:
                target = hint.get("target", "")
                index = hint.get("sentence")
                if target and index is not None:
                    if 1 <= index <= len(unit_sentences) and target.lower() in unit_sentences[index - 1].lower():
                        continue
                    matches = [
                        i for i, sentence in enumerate(unit_sentences, start=1)
                        if target.lower() in sentence.lower()
                    ]
                    if len(matches) == 1:
                        hint["sentence"] = matches[0]
                        changed += 1
                        print(f"{paragraph_id}: {hint.get('id')} sentence {index} -> {matches[0]}")
                    elif not matches:
                        unresolved += 1
                        print(f"{paragraph_id}: {hint.get('id')} target {target!r} not in any unit sentence")
        chapter_offset += len(unit_sentences)
    if changed:
        path.write_text(json.dumps(overlay, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"updated {changed} hint sentence index(es) in {path.name}")
    if unresolved:
        sys.exit(1)


if __name__ == "__main__":
    main()
