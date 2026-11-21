"""Build and check `content/store.json` from the editorial shelf.

`content-src/shelf.json` is the only hand-written shelf source: it lists the
curation fields (order, status, monogram, summary) and, for planned books only,
the title/author/level shown before a package exists. The builder merges that
with `content/books-index.json`, drops identity fields from available books, and
writes the version 2 store the app reads.

Usage:
    from shelf import build_store, store_errors
"""

from __future__ import annotations

from pathlib import Path

from content import ANDROID, BOOKS_INDEX, CONTENT, INDEX_SCHEMA_VERSION, read_json
from taxonomy import category_errors

SHELF = ANDROID / "content-src" / "shelf.json"
STORE = CONTENT / "store.json"

STATUSES = {"available", "planned", "retired"}
CEFR_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"}
IDENTITY_FIELDS = ("title", "author", "level", "levelSource")


def load_shelf() -> dict:
    return read_json(SHELF)


def load_index() -> dict:
    return read_json(BOOKS_INDEX) if BOOKS_INDEX.is_file() else {"books": []}


def _summary(value) -> dict | None:
    if not isinstance(value, dict):
        return None
    zh = str(value.get("zh") or "").strip()
    en = str(value.get("en") or "").strip()
    if not zh and not en:
        return None
    return {"zh": value.get("zh"), "en": value.get("en")}


def build_store(shelf: dict | None = None, index: dict | None = None) -> dict:
    """Return the canonical `store.json` value.

    Available books keep only curation: `id`, `order`, `status`, optional
    `monogram`, optional `summary`. Planned books carry title/author/level plus
    `levelSource: "estimated"`. Retired books keep the entry and optional
    summary but no identity fields, so old progress can still be looked up.
    """
    shelf = shelf if shelf is not None else load_shelf()
    index = index if index is not None else load_index()
    indexed = {entry["id"] for entry in index.get("books", [])}
    books = []
    for source in sorted(shelf.get("books", []), key=lambda item: int(item.get("order", 0))):
        book_id = str(source.get("id") or "").strip()
        if not book_id:
            continue
        status = source.get("status")
        entry = {"id": book_id, "order": int(source.get("order", 0)), "status": status}
        if source.get("monogram"):
            entry["monogram"] = source["monogram"]
        summary = _summary(source.get("summary"))
        if summary:
            entry["summary"] = summary
        if status == "planned":
            entry["title"] = source.get("title")
            entry["author"] = source.get("author")
            entry["level"] = source.get("level")
            entry["levelSource"] = source.get("levelSource") or "estimated"
            if source.get("category"):
                entry["category"] = source["category"]
            if source.get("tags"):
                entry["tags"] = source["tags"]
        elif status == "available":
            if book_id not in indexed:
                raise ValueError(f"available book {book_id} is not in books-index.json")
        elif status == "retired":
            # A retired book may still have a package; it is hidden from the
            # shelf either way, and old progress stays readable.
            pass
        else:
            raise ValueError(f"book {book_id} has unknown status {status!r}")
        books.append(entry)
    return {
        "schemaVersion": INDEX_SCHEMA_VERSION,
        "updatedAt": shelf.get("updatedAt"),
        "books": books,
    }


def store_errors(store: dict, index: dict | None = None) -> list[str]:
    """Check the four shelf gates plus the curation fields."""
    index = index if index is not None else load_index()
    indexed = {entry["id"] for entry in index.get("books", [])}
    errors: list[str] = []
    if store.get("schemaVersion") != INDEX_SCHEMA_VERSION:
        errors.append(f"schemaVersion must be {INDEX_SCHEMA_VERSION}")
    books = store.get("books") or []
    if not books:
        errors.append("shelf has no books")
        return errors
    orders: set[int] = set()
    seen: set[str] = set()
    for book in books:
        book_id = str(book.get("id") or "").strip()
        where = f"store/{book_id or '?'}"
        if not book_id:
            errors.append(f"{where}: id is empty")
            continue
        if book_id in seen:
            errors.append(f"{where}: duplicate id")
        seen.add(book_id)
        order = book.get("order")
        if type(order) is not int or order < 1:
            errors.append(f"{where}: order must be a positive integer")
        elif order in orders:
            errors.append(f"{where}: duplicate order {order}")
        orders.add(order if isinstance(order, int) else 0)
        status = book.get("status")
        if status not in STATUSES:
            errors.append(f"{where}: unknown status {status!r}")
            continue
        summary = book.get("summary")
        if summary is not None:
            zh = str(summary.get("zh") or "").strip()
            en = str(summary.get("en") or "").strip()
            if not zh and not en:
                errors.append(f"{where}: summary has no text")
            if len(zh) > 40:
                errors.append(f"{where}: summary.zh is {len(zh)} characters, limit 40")
        if status == "available":
            for field in IDENTITY_FIELDS:
                if field in book:
                    errors.append(f"{where}: available book must not carry {field}")
            if book_id not in indexed:
                errors.append(f"{where}: available book is not in books-index.json")
        elif status == "planned":
            if book_id in indexed:
                errors.append(f"{where}: planned book already has a package")
            for field in ("title", "author", "level", "summary"):
                if not book.get(field):
                    errors.append(f"{where}: planned book needs {field}")
            if book.get("level") not in CEFR_LEVELS:
                errors.append(f"{where}: bad level {book.get('level')!r}")
            if book.get("levelSource") != "estimated":
                errors.append(f"{where}: planned levelSource must be 'estimated'")
            if book.get("category") is not None:
                errors.extend(
                    category_errors(
                        book.get("category"),
                        where=f"{where}.category",
                        required=False,
                    )
                )
        else:  # retired
            for field in IDENTITY_FIELDS:
                if field in book:
                    errors.append(f"{where}: retired book must not carry {field}")
    for book_id in indexed - seen:
        errors.append(f"store: shipped book {book_id} is not on the shelf")
    return errors
