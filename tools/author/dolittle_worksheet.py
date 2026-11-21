#!/usr/bin/env python3
"""Print the authoring worksheet for one chapter of Doctor Dolittle.

The worksheet is the only place an author should read the paragraph/sentence
inventory from: it shows the frozen sentence split, the unit sentence numbers
used by `quiz[].sentence` and `hints[].sentence`, and the chapter paragraph ids
used as keys in the editorial overlay.

Usage:
    python3 tools/author/dolittle_worksheet.py 7
    python3 tools/author/dolittle_worksheet.py 7 --json
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "lib"))

from content import ANDROID, split_sentences, word_count  # noqa: E402

BOOK = ANDROID / "content" / "books" / "doctor-dolittle" / "book.json"


def load_chapter(number: int) -> dict:
    book = json.loads(BOOK.read_text(encoding="utf-8"))
    for chapter in book["chapters"]:
        if chapter["number"] == number:
            return chapter
    raise SystemExit(f"chapter {number} not found")


def worksheet(chapter: dict) -> dict:
    units = []
    for unit in chapter["units"]:
        # `sentence` is unit-wide (same scale as quiz.sentence), so the counter
        # resets for every unit; a chapter-wide counter misleads the author.
        sentence_number = 0
        sentences = []
        paragraphs = []
        for paragraph in unit["paragraphs"]:
            pieces = split_sentences(paragraph["text"])
            rows = []
            for piece in pieces:
                sentence_number += 1
                row = {"unitSentence": sentence_number, "text": piece}
                rows.append(row)
                sentences.append({"id": f"U{sentence_number}", **row})
            paragraphs.append({
                "id": paragraph["id"],
                "key": paragraph["id"],
                "words": word_count(paragraph["text"]),
                "text": paragraph["text"],
                "sentences": rows,
            })
        units.append({
            "id": unit["id"],
            "number": unit["number"],
            "words": word_count(" ".join(p["text"] for p in unit["paragraphs"])),
            "paragraphs": paragraphs,
            "sentenceCount": sum(len(p["sentences"]) for p in paragraphs),
        })
    return {
        "chapter": chapter["number"],
        "id": chapter["id"],
        "title": chapter["title"],
        "units": units,
        "sentenceCount": sentence_number,
    }


def render(data: dict) -> str:
    lines = [
        f"# {data['id']} · {data['title']['zh']} / {data['title']['en']}",
        f"units: {len(data['units'])}   sentences: {data['sentenceCount']}",
        "",
        "sentence numbers (U#) are unit-wide, 1-based: use them for quiz.sentence and hints.sentence.",
        "",
    ]
    for unit in data["units"]:
        lines.append(
            f"## unit {unit['number']}  {unit['id']}  {unit['words']} words  "
            f"{unit['sentenceCount']} sentences"
        )
        for paragraph in unit["paragraphs"]:
            lines.append(f"### paragraph {paragraph['id']} ({paragraph['words']} words)")
            for sentence in paragraph["sentences"]:
                lines.append(f"  [U{sentence['unitSentence']}] {sentence['text']}")
            lines.append("")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("chapter", type=int)
    parser.add_argument("--json", action="store_true", help="print the machine-readable worksheet")
    args = parser.parse_args()
    data = worksheet(load_chapter(args.chapter))
    if args.json:
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        print(render(data))


if __name__ == "__main__":
    main()
