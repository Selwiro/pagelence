"""Generate the lightweight `outline.json` that ships next to `book.json`.

The shelf and the store only need identity, statistics and the paragraph→unit
mapping; parsing a 1 MB `book.json` for that on every cold start does not scale
past a couple of books. The outline carries exactly those fields and is checked
against `book.json` by `tools/validate/validate_content.py`.
"""

from __future__ import annotations


def build_outline(book: dict) -> dict:
    units = []
    for chapter in book["chapters"]:
        for unit in chapter["units"]:
            units.append(
                {
                    "id": unit["id"],
                    "chapterId": chapter["id"],
                    "number": unit["number"],
                    "title": unit["title"],
                    "paragraphIds": [paragraph["id"] for paragraph in unit["paragraphs"]],
                }
            )
    return {
        "schemaVersion": 1,
        "bookId": book["id"],
        "title": book["title"],
        "author": book.get("author"),
        "edition": book.get("edition"),
        "description": book.get("description"),
        "units": units,
    }
