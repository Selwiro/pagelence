"""Shared helpers for the Pagelence Android content pipeline.

Sentence splitting is frozen here: the migration script (Python), the content
validator and the Kotlin :core reader must all split paragraphs the same way.
Keep this module the single source of truth for the rule; the Kotlin copy in
core/SentenceSplitter.kt is checked against it by a unit test.

Usage:
    from content import dump, split_sentences, build_manifest
"""

from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

from taxonomy import category_errors

#: 仓库根目录。两种布局都成立：monorepo 里的 `work/android/`，或把本目录直接作为仓库根。
ANDROID = Path(__file__).resolve().parents[2]
REPO = ANDROID
assert (REPO / "content").is_dir(), f"unexpected repo root: {REPO}"
CONTENT = ANDROID / "content"
BOOKS = CONTENT / "books"
BOOKS_INDEX = CONTENT / "books-index.json"

#: `book.json` format.
BOOK_SCHEMA_VERSION = 3
#: Built-in index / package manifest format, independent from `book.json`.
INDEX_SCHEMA_VERSION = 2
#: Editorial source metadata in `content-src/<book>/meta.json`.
META_SCHEMA_VERSION = 2

#: Backwards-compatible alias for callers that mean the book format.
SCHEMA_VERSION = BOOK_SCHEMA_VERSION

#: Package-level metadata lives in `content-src/<book>/meta.json`. These are
#: the values the packager copies into the `.pagelence` manifest; `book.json`
#: stays content-only.
CEFR_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"}
CONTENT_STATUSES = {"skeleton", "sample", "complete"}
SEMVER = re.compile(r"^\d+\.\d+\.\d+$")
ISO_DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
REGION = re.compile(r"^[A-Z]{2}$")
SPDX = re.compile(r"^[A-Za-z0-9][A-Za-z0-9.+-]*$")
SOURCE_LICENCE_TYPES = {"public-domain", "permission", "original"}
CREATOR_SPECIAL_TYPES = {"custom", "all-rights-reserved"}

META_REQUIRED = (
    "schemaVersion",
    "id",
    "creator",
    "version",
    "language",
    "level",
    "contentStatus",
    "wordCount",
    "updatedAt",
    "minAppVersionCode",
)

#: A period/exclamation/question mark, optional closing quote, then if followed
#: by whitespace and an opening quote plus an uppercase letter or digit.
_BOUNDARY = re.compile(r"[.!?][”’\"]?(?=\s+(?:[“\"]?[A-Z0-9]))")
_TRAILING_DOT_TOKEN = re.compile(r"([A-Za-z](?:[A-Za-z.]*)\.)[”’\"]?$")
_INITIAL = re.compile(r"[a-z]\.")
WORD_RE = re.compile(r"[A-Za-z]+(?:['’][A-Za-z]+)?")


def split_sentences(text: str) -> list[str]:
    """Split a paragraph into sentences using the frozen rule.

    Rules:
      1. A boundary is . ! or ? (plus an optional closing quote) followed by
         whitespace and an uppercase letter or digit, or the end of the text.
      2. A boundary after a single-letter initial ("J.") is not a sentence end.
         Dotted abbreviations before a capital ("Mr. Smith") *do* split; the
         frozen rule keeps that limitation because the existing translations
         were authored against it.
      3. Whatever follows the last boundary is the final sentence.

    The rule is deliberately simple and stable. It does not attempt dialogue
    attribution or style-specific handling; content authoring must follow it.
    """
    sentences: list[str] = []
    start = 0
    for match in _BOUNDARY.finditer(text):
        end = match.end()
        before = text[start:end].rstrip()
        tail = _TRAILING_DOT_TOKEN.search(before)
        if tail and _INITIAL.fullmatch(tail.group(1).lower()):
            continue
        sentence = text[start:end].strip()
        if sentence:
            sentences.append(sentence)
        start = end
    rest = text[start:].strip()
    if rest:
        sentences.append(rest)
    return sentences


def word_count(text: str) -> int:
    return len(WORD_RE.findall(text))


def meta_errors(meta: dict) -> list[str]:
    """Check the package metadata in `content-src/<book>/meta.json`.

    Returns human-readable messages without a file prefix; callers add the
    path. The word count and level are cross-checked against the built content
    and the shelf by the book builders and `validate_content.py`.
    """
    errors: list[str] = []
    for field in META_REQUIRED:
        if field not in meta:
            errors.append(f"{field} is required")

    if meta.get("schemaVersion") != META_SCHEMA_VERSION:
        errors.append(f"schemaVersion must be {META_SCHEMA_VERSION}")
    if not str(meta.get("id") or "").strip():
        errors.append("id is empty")
    if not str(meta.get("creator") or "").strip():
        errors.append("creator is empty")
    version = str(meta.get("version") or "")
    if not SEMVER.fullmatch(version):
        errors.append(f"version {version!r} must be MAJOR.MINOR.PATCH")
    if meta.get("language") != "en":
        errors.append("language must be 'en' (only English books are supported)")
    level = str(meta.get("level") or "")
    if level not in CEFR_LEVELS:
        errors.append(f"level {level!r} must be one of {sorted(CEFR_LEVELS)}")
    if meta.get("contentStatus") not in CONTENT_STATUSES:
        errors.append(f"contentStatus must be one of {sorted(CONTENT_STATUSES)}")
    count = meta.get("wordCount")
    if type(count) is not int or count <= 0:
        errors.append("wordCount must be a positive integer")
    updated = str(meta.get("updatedAt") or "")
    if not ISO_DATE.fullmatch(updated):
        errors.append(f"updatedAt {updated!r} must be YYYY-MM-DD")
    min_version = meta.get("minAppVersionCode")
    if type(min_version) is not int or min_version < 1:
        errors.append("minAppVersionCode must be a positive integer")
    if "levelNote" in meta and not str(meta["levelNote"] or "").strip():
        errors.append("levelNote is present but empty")
    errors.extend(category_errors(meta.get("category"), where="category", required=True))
    errors.extend(licence_errors(meta.get("licenses")))
    return errors


def licence_errors(licenses) -> list[str]:
    """Check the two package licences (book-package spec §三).

    `source` covers the original text; `creator` covers the translations,
    introductions, quizzes, hints and grammar notes added by the package.
    Both are required and both are written by the packager into the package
    manifest; `book.json` never repeats them.
    """
    errors: list[str] = []
    if not isinstance(licenses, dict):
        return ["licenses must be an object with source and creator"]
    source = licenses.get("source")
    creator = licenses.get("creator")
    if not isinstance(source, dict):
        errors.append("licenses.source is required")
    else:
        kind = str(source.get("type") or "")
        if not kind:
            errors.append("licenses.source.type is required")
        elif kind not in SOURCE_LICENCE_TYPES and not SPDX.fullmatch(kind):
            errors.append(f"licenses.source.type {kind!r} is not a licence id")
        if kind == "public-domain":
            regions = source.get("publicDomain")
            if not isinstance(regions, list) or not regions:
                errors.append("licenses.source.publicDomain must be a non-empty list")
            else:
                for region in regions:
                    if not isinstance(region, str) or not REGION.fullmatch(region):
                        errors.append(f"licenses.source.publicDomain {region!r} must be ISO 3166-1 alpha-2")
        for field in ("url", "text", "file"):
            if field in source and not str(source[field] or "").strip():
                errors.append(f"licenses.source.{field} is present but empty")
    if not isinstance(creator, dict):
        errors.append("licenses.creator is required")
    else:
        kind = str(creator.get("type") or "")
        if not kind:
            errors.append("licenses.creator.type is required")
        elif kind not in CREATOR_SPECIAL_TYPES and not SPDX.fullmatch(kind):
            errors.append(f"licenses.creator.type {kind!r} is not a licence id")
        if kind in CREATOR_SPECIAL_TYPES and not str(creator.get("name") or "").strip():
            errors.append(f"licenses.creator.name is required for {kind}")
        if not any(str(creator.get(field) or "").strip() for field in ("url", "text", "file")):
            errors.append("licenses.creator needs at least one of url, text or file")
        for field in ("url", "text", "file", "scope", "name", "note"):
            if field in creator and not str(creator[field] or "").strip():
                errors.append(f"licenses.creator.{field} is present but empty")
    return errors


def dump(value) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2) + "\n"


def sha256_of(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def book_stats(book: dict) -> dict:
    """Counts used by the manifest and the gap report."""
    chapters = units = paragraphs = sentences = translated = words = 0
    for chapter in book.get("chapters", []):
        chapters += 1
        for unit in chapter.get("units", []):
            units += 1
            for paragraph in unit.get("paragraphs", []):
                paragraphs += 1
                words += word_count(paragraph.get("text", ""))
                for sentence in paragraph.get("sentences", []):
                    sentences += 1
                    if (sentence.get("zh") or "").strip():
                        translated += 1
    return {
        "chapters": chapters,
        "units": units,
        "paragraphs": paragraphs,
        "sentences": sentences,
        "translatedSentences": translated,
        "words": words,
    }


def build_manifest() -> dict:
    """Describe every book under content/books/, newest file wins nothing.

    The built-in index is derived, never hand-edited: both the Dolittle builder
    and the Web migration call this so neither clobbers the other's entry. The
    package-facing metadata (level, levelNote, contentStatus) comes from the
    editorial `content-src/<book>/meta.json`; the index itself is version 2.
    """
    meta_by_id = {}
    for path in sorted((ANDROID / "content-src").glob("*/meta.json")):
        meta = read_json(path)
        if meta.get("id"):
            meta_by_id[meta["id"]] = meta
    entries = []
    for book_path in sorted(BOOKS.glob("*/book.json")):
        data = book_path.read_bytes()
        book = json.loads(data.decode("utf-8"))
        stats = book_stats(book)
        meta = meta_by_id.get(book["id"], {})
        entries.append({
            "id": book["id"],
            "title": book.get("title"),
            "path": book_path.relative_to(CONTENT).as_posix(),
            "schemaVersion": INDEX_SCHEMA_VERSION,
            "sizeBytes": len(data),
            "sha256": sha256_of(data),
            "level": meta.get("level"),
            "levelNote": meta.get("levelNote"),
            "contentStatus": meta.get("contentStatus"),
            "creator": meta.get("creator"),
            "category": meta.get("category"),
            "tags": meta.get("tags") or [],
            "licenses": meta.get("licenses"),
            **stats,
        })
    return {"schemaVersion": INDEX_SCHEMA_VERSION, "books": entries}


def write_outputs(outputs: dict[Path, str], check: bool) -> list[Path]:
    """Write {path: text} or, with check=True, report stale files."""
    stale = []
    for path, text in outputs.items():
        if check:
            if not path.is_file() or path.read_text(encoding="utf-8") != text:
                stale.append(path)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
    if not check:
        for path in outputs:
            print(f"wrote {path.relative_to(REPO)}")
    return stale
