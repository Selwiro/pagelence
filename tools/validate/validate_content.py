#!/usr/bin/env python3
"""Validate the Android content package.

Checks every built-in book, the manifest, the grammar library, the third-party
tables and the generated gap report. Exits non-zero when any error is found, so
the same command works locally and in CI before a build.

Rules are the frozen schema in docs/content-format.md. Missing sentence
translations are a *content gap*, not a schema error: they are reported by the
gap report and checked here only for consistency.

Usage:
    python3 tools/validate/validate_content.py
    python3 tools/validate/validate_content.py --quiet
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "lib"))

from content import (  # noqa: E402
    ANDROID,
    BOOKS,
    CONTENT,
    INDEX_SCHEMA_VERSION,
    REPO,
    SCHEMA_VERSION,
    build_manifest,
    dump,
    meta_errors,
    read_json,
    split_sentences,
)
from assessment import validate_bank  # noqa: E402
from gap import compute_gap  # noqa: E402
from shelf import build_store, load_shelf, store_errors  # noqa: E402
from taxonomy import category_errors, taxonomy_errors  # noqa: E402

THIRD_PARTY = CONTENT / "third-party"
EXTRA = ANDROID / "content-src" / "extra.json"
GRAMMAR = CONTENT / "grammar" / "grammar-library.json"
ASSESSMENT = CONTENT / "assessment" / "bank.json"
MANIFEST = CONTENT / "books-index.json"
STORE = CONTENT / "store.json"
REPORTS = ANDROID / "reports"
SRC = ANDROID / "content-src"

HINT_KINDS = {"word", "phrase", "sentence"}
GRAMMAR_TYPES = {"new", "review"}
ANALYSIS_KEYS = ("form", "explain", "focus")
ANALYSIS_ROLES = {
    "subordinate", "relative", "nonfinite",
    "prepositional", "appositive", "parenthetical",
}
CEFR_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"}
HTML_TAG = re.compile(r"<\s*/?\s*[a-zA-Z][^>]*>")


class Report:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: dict[str, list[str]] = {}
        self.checked = {"books": 0, "chapters": 0, "units": 0, "paragraphs": 0, "sentences": 0}

    def error(self, where: str, message: str) -> None:
        self.errors.append(f"{where}: {message}")

    def warn(self, where: str, message: str) -> None:
        self.warnings.setdefault(message, []).append(where)

    def expect(self, condition: bool, where: str, message: str) -> bool:
        if not condition:
            self.error(where, message)
        return condition


def text(value) -> str:
    return value.strip() if isinstance(value, str) else ""


def localized(value, where: str, field: str, report: Report, required: bool = False) -> None:
    if value is None:
        if required:
            report.error(where, f"{field} is required")
        return
    if not isinstance(value, dict):
        report.error(where, f"{field} must be an object with zh/en")
        return
    if not text(value.get("zh")) and not text(value.get("en")):
        report.error(where, f"{field} has no text")
    for language in value:
        if language not in {"zh", "en", "startParagraphId"}:
            report.warn(where, f"{field} has unknown language key {language}")


def plain(value: str, where: str, field: str, report: Report) -> None:
    if HTML_TAG.search(value):
        report.error(where, f"{field} contains HTML")


def validate_analysis(sentence_text: str, analysis, where: str, report: Report) -> None:
    if not isinstance(analysis, dict):
        report.error(where, "analysis must be an object")
        return
    if text(analysis.get("level")) not in CEFR_LEVELS:
        report.error(f"{where}/analysis", f"unknown level {analysis.get('level')!r}")
    if not text(analysis.get("core")):
        report.error(f"{where}/analysis", "core is required")
    parts = analysis.get("parts") or []
    if not isinstance(parts, list) or not 1 <= len(parts) <= 4:
        report.error(f"{where}/analysis", "parts must have 1–4 items")
        parts = []
    for index, part in enumerate(parts, start=1):
        pwhere = f"{where}/analysis/p{index}"
        part_text = text(part.get("text"))
        if not part_text or part_text not in sentence_text:
            report.error(pwhere, "part text is not in the sentence")
        if part.get("role") not in ANALYSIS_ROLES:
            report.error(pwhere, f"unknown role {part.get('role')!r}")
        if not text(part.get("zh")):
            report.error(pwhere, "zh is required")
        if part.get("en") is not None and not text(part.get("en")):
            report.error(pwhere, "en is present but empty")
    for field in ("order", "note"):
        localized(analysis.get(field), f"{where}/analysis", field, report)


def validate_grammar(grammar, unit_text: str, library: dict, where: str, report: Report) -> None:
    if grammar is None:
        return
    if not isinstance(grammar, dict):
        report.error(where, "grammar must be an object")
        return
    excerpt = text(grammar.get("excerpt"))
    report.expect(bool(excerpt), where, "grammar.excerpt is empty")
    if excerpt and excerpt not in unit_text:
        report.error(where, "grammar.excerpt is not in the unit text")
    if grammar.get("type") and grammar["type"] not in GRAMMAR_TYPES:
        report.error(where, f"unknown grammar.type {grammar['type']}")
    localized(grammar.get("focus"), where, "grammar.focus", report, required=True)
    for field in ("form", "explain"):
        localized(grammar.get(field), where, f"grammar.{field}", report)
    for removed in ("pointId", "function", "prompt", "note"):
        if removed in grammar:
            report.error(where, f"grammar.{removed} was removed in schema v3")
    point_ids = grammar.get("pointIds")
    if point_ids is None:
        point_ids = []
    if not isinstance(point_ids, list):
        report.error(where, "grammar.pointIds must be an array")
        point_ids = []
    if len(set(point_ids)) != len(point_ids):
        report.error(where, "grammar.pointIds has duplicates")
    unknown = [point for point in point_ids if point not in library]
    for point in unknown:
        report.warn(where, f"grammar.pointId {point} is not in the grammar library (ignored)")
    primary = point_ids[0] if point_ids else None
    inline_analysis = all(grammar.get(key) for key in ANALYSIS_KEYS)
    if primary is None and not inline_analysis:
        report.warn(where, "grammar analysis is incomplete (content gap, see gap report)")
    if primary and primary in library:
        point = library[primary]
        if not (point.get("form") and point.get("explain") and point.get("pitfalls")):
            report.warn(where, f"library point {primary} has incomplete fields")
    elif primary is None and (not grammar.get("explain") or not grammar.get("form")):
        report.error(where, "library-outside grammar needs explain + form + focus")


def validate_book(book: dict, library: dict, where: str, report: Report) -> None:
    report.checked["books"] += 1
    report.expect(
        book.get("schemaVersion") == SCHEMA_VERSION,
        where,
        f"schemaVersion must be {SCHEMA_VERSION}",
    )
    for field in ("id", "author"):
        report.expect(bool(text(book.get(field))), where, f"{field} is required")
    localized(book.get("title"), where, "title", report, required=True)
    localized(book.get("description"), where, "description", report, required=True)
    if not book.get("source"):
        report.error(where, "source is required")
    elif not isinstance(book["source"], dict):
        report.error(where, "source must be an object")
    elif "publicDomain" in book["source"]:
        report.error(where, "source.publicDomain was removed in schema v3; read manifest licenses")
    if "license" in book:
        report.error(where, "book.json.license was removed in schema v3; read manifest licenses")

    seen: set[str] = set()

    def claim(value: str, kind: str) -> None:
        if value in seen:
            report.error(where, f"duplicate id {value} ({kind})")
        seen.add(value)

    chapters = book.get("chapters") or []
    if not chapters:
        report.error(where, "a book needs at least one chapter")
        return
    for chapter_index, chapter in enumerate(chapters, start=1):
        cwhere = f"{where}/{chapter.get('id', '?')}"
        report.checked["chapters"] += 1
        claim(text(chapter.get("id")), "chapter")
        report.expect(bool(text(chapter.get("id"))), cwhere, "chapter id is empty")
        localized(chapter.get("title"), cwhere, "title", report, required=True)
        report.expect(
            chapter.get("number") == chapter_index,
            cwhere,
            f"chapter.number should be {chapter_index}",
        )
        units = chapter.get("units") or []
        if not units:
            report.error(cwhere, "chapter has no units")
        for unit_index, unit in enumerate(units, start=1):
            uwhere = f"{cwhere}/{unit.get('id', '?')}"
            report.checked["units"] += 1
            claim(text(unit.get("id")), "unit")
            report.expect(bool(text(unit.get("id"))), uwhere, "unit id is empty")
            if not unit.get("title"):
                report.warn(uwhere, "unit title is missing (skeleton content)")
            localized(unit.get("title"), uwhere, "title", report)
            localized(unit.get("introduction"), uwhere, "introduction", report)
            for removed in ("recap", "review"):
                if removed in unit:
                    report.error(uwhere, f"units[].{removed} was removed in schema v3")
            report.expect(
                unit.get("number") == unit_index,
                uwhere,
                f"unit.number should be chapter-local {unit_index}",
            )

            paragraphs = unit.get("paragraphs") or []
            if not paragraphs:
                report.error(uwhere, "unit has no paragraphs")
            unit_text = " ".join(text(p.get("text")) for p in paragraphs)
            all_unit_sentences = [
                text(sentence.get("text"))
                for paragraph in paragraphs
                for sentence in (paragraph.get("sentences") or [])
            ]
            sentence_count = 0
            hint_keys: set[tuple[str, int]] = set()
            for index, paragraph in enumerate(paragraphs):
                pwhere = f"{uwhere}/{paragraph.get('id', '?')}"
                report.checked["paragraphs"] += 1
                claim(text(paragraph.get("id")), "paragraph")
                body = text(paragraph.get("text"))
                report.expect(bool(body), pwhere, "paragraph text is empty")
                plain(body, pwhere, "text", report)
                if "guide" in paragraph:
                    report.error(pwhere, "paragraphs[].guide was removed in schema v3")

                sentences = paragraph.get("sentences") or []
                if not sentences:
                    # A skeleton book may ship text before its study edition.
                    report.warn(pwhere, "paragraph has no sentences (study edition pending)")
                else:
                    expected = split_sentences(body)
                    if [s.get("text", "") for s in sentences] != expected:
                        report.error(
                            pwhere,
                            "sentences do not match the frozen splitter "
                            f"({len(sentences)} stored, {len(expected)} split)",
                        )
                sentence_count += len(sentences)
                report.checked["sentences"] += len(sentences)
                for sentence_position, sentence in enumerate(sentences):
                    swhere = f"{pwhere}/s{sentence_position + 1}"
                    sentence_text = text(sentence.get("text"))
                    report.expect(bool(sentence_text), swhere, "sentence text is empty")
                    plain(sentence_text, swhere, "text", report)
                    if sentence.get("zh") is not None:
                        report.expect(
                            bool(text(sentence.get("zh"))),
                            swhere,
                            "sentence zh is present but empty",
                        )
                        plain(sentence.get("zh", ""), swhere, "zh", report)
                    if "glosses" in sentence:
                        report.error(swhere, "sentences[].glosses was removed in schema v3; use hints")
                    if sentence.get("analysis") is not None:
                        validate_analysis(sentence_text, sentence["analysis"], swhere, report)

                unit_sentences = all_unit_sentences
                for hint in paragraph.get("hints") or []:
                    hwhere = f"{pwhere}/hint:{hint.get('id', '?')}"
                    claim(text(hint.get("id")), "hint")
                    report.expect(bool(text(hint.get("id"))), hwhere, "hint id is empty")
                    report.expect(hint.get("kind") in HINT_KINDS, hwhere, f"bad hint kind {hint.get('kind')}")
                    target = text(hint.get("target"))
                    report.expect(bool(target), hwhere, "hint target is empty")
                    if target and target.lower() not in body.lower():
                        report.error(hwhere, "hint target is not in the paragraph")
                    for language in ("zh", "en"):
                        report.expect(bool(text(hint.get(language))), hwhere, f"{language} is required")
                        plain(hint.get(language, ""), hwhere, language, report)
                    for removed in ("italic", "gloss"):
                        if removed in hint:
                            report.error(hwhere, f"hints[].{removed} was removed in schema v3")
                    sentence_index = hint.get("sentence")
                    if sentence_index is not None:
                        report.expect(
                            isinstance(sentence_index, int) and 1 <= sentence_index <= len(unit_sentences),
                            hwhere,
                            "hint.sentence must be a 1-based unit index",
                        )
                        if isinstance(sentence_index, int) and 1 <= sentence_index <= len(unit_sentences):
                            if target.lower() not in unit_sentences[sentence_index - 1].lower():
                                report.error(hwhere, "hint target is not in the sentence it points at")
                    key = (target.lower(), sentence_index or 0)
                    if key in hint_keys:
                        report.error(hwhere, "duplicate hint for the same target and sentence")
                    hint_keys.add(key)

            quiz = unit.get("quiz") or []
            for index, item in enumerate(quiz, start=1):
                qwhere = f"{uwhere}/quiz{index}"
                report.expect(bool(text(item.get("question"))), qwhere, "question is empty")
                options = item.get("options") or []
                report.expect(len(options) >= 2, qwhere, "needs at least two options")
                answer = item.get("answer")
                report.expect(
                    isinstance(answer, int) and 0 <= answer < len(options),
                    qwhere,
                    "answer is out of range",
                )
                if item.get("sentence") is not None:
                    report.expect(
                        isinstance(item["sentence"], int) and 1 <= item["sentence"] <= sentence_count,
                        qwhere,
                        "sentence must be a 1-based unit sentence index",
                    )
                feedback = item.get("feedback")
                if not isinstance(feedback, dict):
                    report.error(qwhere, "feedback must be {zh, en?} in schema v3")
                else:
                    report.expect(bool(text(feedback.get("zh"))), qwhere, "feedback.zh is required")
                    if feedback.get("en") is not None and not text(feedback.get("en")):
                        report.error(qwhere, "feedback.en is present but empty")

            validate_grammar(unit.get("grammar"), unit_text, library, uwhere, report)



def validate_manifest(report: Report) -> None:
    if not MANIFEST.is_file():
        report.error("content/books-index.json", "missing")
        return
    manifest = read_json(MANIFEST)
    report.expect(
        manifest.get("schemaVersion") == INDEX_SCHEMA_VERSION,
        "books-index",
        "bad schemaVersion",
    )
    expected = build_manifest()
    if dump(expected) != MANIFEST.read_text(encoding="utf-8"):
        report.error("books-index", "does not match content/books; regenerate with the content scripts")
        return
    ids = {entry["id"] for entry in manifest["books"]}
    on_disk = {read_json(path)["id"] for path in BOOKS.glob("*/book.json")}
    for missing in on_disk - ids:
        report.error("books-index", f"book {missing} is on disk but not in the manifest")
    for extra in ids - on_disk:
        report.error("books-index", f"book {extra} is in the manifest but has no book.json")
    for entry in manifest.get("books") or []:
        book_id = entry.get("id", "?")
        for message in category_errors(
            entry.get("category"),
            where=f"books-index/{book_id}.category",
            required=True,
        ):
            report.error("books-index", message)


def validate_taxonomy(report: Report) -> None:
    for message in taxonomy_errors():
        report.error("content/taxonomy.json", message)


def validate_store(report: Report) -> None:
    """Shelf v2: available carries no identity, planned has no package."""
    if not STORE.is_file():
        report.error("content/store.json", "missing")
        return
    catalog = read_json(STORE)
    index = read_json(MANIFEST) if MANIFEST.is_file() else {"books": []}
    for message in store_errors(catalog, index):
        report.error("store", message)
    try:
        expected = build_store(load_shelf(), index)
    except ValueError as error:
        report.error("store", str(error))
    else:
        if dump(expected) != STORE.read_text(encoding="utf-8"):
            report.error("store", "does not match content-src/shelf.json; run build_shelf.py")
    for book in catalog.get("books") or []:
        where = f"store/{book.get('id', '?')}"
        if book.get("title") is not None:
            localized(book.get("title"), where, "title", report)
        if book.get("summary") is not None:
            localized(book.get("summary"), where, "summary", report)


def validate_source_meta(report: Report) -> None:
    """Editorial package metadata must agree with the generated content.

    `content-src/<book>/meta.json` is not shipped; a packager copies these
    fields into the `.pagelence` manifest. Checking them here catches a stale
    word count, level or content status before a package is cut.
    """
    meta_by_id: dict[str, dict] = {}
    meta_paths: dict[str, str] = {}
    for path in sorted(SRC.glob("*/meta.json")):
        where = path.relative_to(REPO).as_posix()
        meta = read_json(path)
        for message in meta_errors(meta):
            report.error(where, message)
        book_id = text(meta.get("id"))
        if not book_id:
            continue
        if book_id in meta_by_id:
            report.error(where, f"duplicate book id {book_id}")
        meta_by_id[book_id] = meta
        meta_paths[book_id] = where

    manifest = read_json(MANIFEST) if MANIFEST.is_file() else {"books": []}
    generated = {entry.get("id"): entry for entry in manifest.get("books") or []}

    for book_id, meta in meta_by_id.items():
        where = meta_paths[book_id]
        entry = generated.get(book_id)
        if not entry:
            report.warn(where, "no generated book for this meta.json")
        else:
            report.expect(
                meta.get("schemaVersion") == entry.get("schemaVersion"),
                where,
                "schemaVersion differs from book.json",
            )
            report.expect(
                meta.get("wordCount") == entry.get("words"),
                where,
                f"wordCount {meta.get('wordCount')} != manifest words {entry.get('words')}",
            )

        report.expect(
            meta.get("level") == entry.get("level"),
            where,
            f"level {meta.get('level')} != books-index level {entry.get('level')}",
        )

        gap_path = REPORTS / f"{book_id}.gap.json"
        if gap_path.is_file():
            units = read_json(gap_path).get("units") or []
            complete = sum(1 for unit in units if not unit.get("missing"))
            expected = (
                "complete" if units and complete == len(units)
                else "skeleton" if complete == 0
                else "sample"
            )
            report.expect(
                meta.get("contentStatus") == expected,
                where,
                f"contentStatus {meta.get('contentStatus')!r} != gap report {expected!r}",
            )

    for book_id in generated:
        if book_id not in meta_by_id:
            report.warn("content-src", f"book {book_id} has no meta.json")


#: 可选的本地对照表（不在仓库内）。设置后额外核对 CEFR-J 语法点编号是否都在表内；
#: 未设置时跳过这一项，其余校验照常。
TEACHER_IDS = Path(os.environ["GRAMMAR_TEACHER_IDS"]) if os.environ.get("GRAMMAR_TEACHER_IDS") else None
HANZI = re.compile(r"[\u3400-\u9fff]")
GRAMMAR_LIMITS = {"explain": 120, "form": 30, "pitfall": 30, "entry": 270}
LEVELS = ["A1.1", "A1.2", "A1.3", "A2.1", "A2.2", "B1.1", "B1.2", "B2.1", "B2.2"]


def teacher_ids() -> set[str]:
    if TEACHER_IDS is None or not TEACHER_IDS.is_file():
        return set()
    ids = set()
    for line in TEACHER_IDS.read_text(encoding="utf-8").splitlines():
        match = re.match(r"^\|\s*(\d+(?:-\d+)?)\s*\|", line)
        if match:
            ids.add(f"cefrj-{match.group(1)}")
    return ids


def validate_library(report: Report) -> dict:
    if not GRAMMAR.is_file():
        report.error("content/grammar/grammar-library.json", "missing")
        return {}
    library = read_json(GRAMMAR)
    report.expect(library.get("schemaVersion") == 2, "grammar", "schemaVersion must be 2")
    known = teacher_ids()
    points = {}
    for point in library.get("points") or []:
        pid = text(point.get("id"))
        where = f"grammar/{pid or '?'}"
        if not pid:
            report.error("grammar", "point without id")
            continue
        if pid in points:
            report.error(where, "duplicate point")
        points[pid] = point
        if not pid.startswith("cefrj-"):
            report.error(where, "id must start with cefrj-")
        elif known and pid not in known:
            report.error(where, "id is not one of the CEFR-J teacher-list ids")
        localized(point.get("name"), where, "name", report, required=True)
        start = text(point.get("levelStart"))
        end = text(point.get("levelEnd") or start)
        if start not in LEVELS:
            report.error(where, f"bad levelStart {start!r}")
        if end not in LEVELS:
            report.error(where, f"bad levelEnd {end!r}")
        if start in LEVELS and end in LEVELS and LEVELS.index(end) < LEVELS.index(start):
            report.error(where, "levelEnd is before levelStart")
        localized(point.get("explain"), where, "explain", report, required=True)
        localized(point.get("form"), where, "form", report, required=True)
        explain = point.get("explain") or {}
        form = point.get("form") or {}
        explain_hanzi = len(HANZI.findall(explain.get("zh") or ""))
        form_hanzi = len(HANZI.findall(form.get("zh") or ""))
        if not 1 <= explain_hanzi <= GRAMMAR_LIMITS["explain"]:
            report.error(where, f"explain.zh is {explain_hanzi} hanzi (max 120)")
        if not 1 <= form_hanzi <= GRAMMAR_LIMITS["form"]:
            report.error(where, f"form.zh is {form_hanzi} hanzi (max 30)")
        pitfalls = point.get("pitfalls")
        if not isinstance(pitfalls, list) or not 1 <= len(pitfalls) <= 3:
            report.error(where, "pitfalls must be 1–3 items")
            pitfalls = []
        pitfall_hanzi = 0
        for index, pitfall in enumerate(pitfalls):
            localized(pitfall, where, f"pitfalls[{index}]", report, required=True)
            hanzi = len(HANZI.findall((pitfall or {}).get("zh") or ""))
            pitfall_hanzi += hanzi
            if not 1 <= hanzi <= GRAMMAR_LIMITS["pitfall"]:
                report.error(where, f"pitfalls[{index}] is {hanzi} hanzi (max 30)")
        examples = point.get("examples")
        if not isinstance(examples, list) or not 1 <= len(examples) <= 2:
            report.error(where, "examples must be 1–2 items")
            examples = []
        for index, example in enumerate(examples):
            if not isinstance(example, dict):
                report.error(where, f"examples[{index}] must be an object")
                continue
            text_value = text(example.get("text"))
            if not text_value:
                report.error(where, f"examples[{index}].text is empty")
            elif HANZI.search(text_value):
                report.error(where, f"examples[{index}].text is not English")
            if not text(example.get("zh")):
                report.error(where, f"examples[{index}].zh is empty")
        if explain_hanzi + form_hanzi + pitfall_hanzi > GRAMMAR_LIMITS["entry"]:
            report.error(where, "zh copy is over 270 hanzi")
        freq = point.get("freqLevel")
        if freq is not None and freq not in LEVELS and freq not in ("A1", "A2", "B1", "B2", "C1", "C2"):
            report.error(where, f"bad freqLevel {freq!r}")
        for removed in ("summary", "function", "detail", "outline", "cefrjIds", "level"):
            if removed in point:
                report.error(where, f"{removed} was removed in schema 2")
        for related in point.get("relatedIds") or []:
            if related not in points and related not in known:
                report.warn(where, f"unknown relatedId {related}")
    if not points:
        report.error("grammar", "library has no points")
    if known:
        missing = sorted(known - set(points))
        extra = sorted(set(points) - known)
        if missing:
            report.error("grammar", f"library is missing {len(missing)} teacher entries, e.g. {', '.join(missing[:5])}")
        if extra:
            report.error("grammar", f"library has {len(extra)} ids outside the teacher list")
        if len(points) == 256:
            pass
        else:
            report.error("grammar", f"library has {len(points)} of 256 CEFR-J entries")
    return points


def validate_assessment(report: Report) -> None:
    if not ASSESSMENT.is_file():
        report.error("content/assessment/bank.json", "missing")
        return
    errors, warnings = validate_bank(read_json(ASSESSMENT))
    for message in errors:
        report.errors.append(message)
    for message in warnings:
        report.warnings.setdefault(message, []).append("assessment")


def validate_extra(report: Report) -> dict:
    if not EXTRA.is_file():
        return {}
    data = read_json(EXTRA)
    entries = data.get("entries") or {}
    if len(entries) > 200:
        report.error("extra.json", f"{len(entries)} entries, limit 200")
    ecdict = read_json(THIRD_PARTY / "dictionaries" / "ecdict.json") if (THIRD_PARTY / "dictionaries" / "ecdict.json").is_file() else {"entries": {}}
    for key, entry in entries.items():
        where = f"extra/{key}"
        if entry.get("kind") not in {"name", "place"}:
            report.error(where, "kind must be name or place")
        if not str(entry.get("note") or "").strip():
            report.error(where, "note is required")
        books = entry.get("books") or []
        if not books:
            report.error(where, "books is required")
        for book_id in books:
            book_path = BOOKS / book_id / "book.json"
            if not book_path.is_file():
                report.error(where, f"book {book_id} does not exist")
                continue
            text = book_path.read_text(encoding="utf-8").lower()
            if key.lower() not in text:
                report.error(where, f"book {book_id} never uses this spelling")
    return entries


def validate_third_party(report: Report) -> None:
    levels = read_json(THIRD_PARTY / "word-levels.json")
    if not levels.get("levels"):
        report.error("third-party/word-levels.json", "no levels")
    report.expect(
        levels.get("schemaVersion") == 2,
        "third-party/word-levels.json",
        "schemaVersion must be 2 (levels + sources)",
    )
    sources = levels.get("sources") or {}
    for word, poses in (levels.get("levels") or {}).items():
        if word not in sources:
            report.error("third-party/word-levels.json", f"{word}: missing sources")
            break
        for pos in poses:
            if sources[word].get(pos) not in {"list", "aligned", "derived", "tag", "freq"}:
                report.error("third-party/word-levels.json", f"{word}/{pos}: bad source")
                break
    frequencies = read_json(THIRD_PARTY / "word-frequencies.json")
    if not frequencies.get("entries"):
        report.error("third-party/word-frequencies.json", "no entries")
    bands = frequencies.get("bands") or {}
    if not all(isinstance(bands.get(key), (int, float)) for key in ("high", "common", "normal")):
        report.error("third-party/word-frequencies.json", "bands need high/common/normal")
    elif not (bands["high"] >= bands["common"] >= bands["normal"]):
        report.error("third-party/word-frequencies.json", "bands must be high >= common >= normal")
    for name in ("ecdict",):
        path = THIRD_PARTY / "dictionaries" / f"{name}.json"
        if not path.is_file():
            report.error(str(path.relative_to(REPO)), "missing")
            continue
        data = read_json(path)
        entries = data.get("entries") or {}
        if not entries:
            report.error(str(path.relative_to(REPO)), "no entries")
        elif data.get("termCount") != len(entries):
            report.error(
                str(path.relative_to(REPO)),
                f"termCount {data.get('termCount')} != entries {len(entries)}",
            )
        license_path = THIRD_PARTY / "dictionaries" / f"{name}-license.txt"
        if not license_path.is_file() or not license_path.read_text(encoding="utf-8").strip():
            report.error(str(license_path.relative_to(REPO)), "licence text is missing")
    # Book frequency and occurrence tables are no longer shipped: they are
    # scanned from the book text once on open (§3.4.3 #3).


def validate_gap_report(book: dict, library: dict, report: Report) -> None:
    path = REPORTS / f"{book['id']}.gap.json"
    if not path.is_file():
        report.error(str(path.relative_to(REPO)), "gap report is missing")
        return
    actual = read_json(path)
    expected = compute_gap(book, library)
    for key in ("totals", "chapters", "units"):
        if actual.get(key) != expected.get(key):
            report.error(
                str(path.relative_to(REPO)),
                f"{key} is out of date; regenerate with the book builder",
            )
            break
    if actual.get("schemaVersion") != SCHEMA_VERSION:
        report.error(str(path.relative_to(REPO)), "bad schemaVersion")


def report_structure(book: dict) -> list[dict]:
    return [unit for chapter in book.get("chapters", []) for unit in chapter.get("units", [])]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--quiet", action="store_true", help="only print the errors")
    args = parser.parse_args()

    report = Report()
    library = validate_library(report)
    validate_third_party(report)
    validate_extra(report)
    validate_assessment(report)
    validate_taxonomy(report)
    validate_store(report)
    validate_source_meta(report)

    for book_path in sorted(BOOKS.glob("*/book.json")):
        book = read_json(book_path)
        where = book_path.relative_to(REPO).as_posix()
        validate_book(book, library, where, report)
        validate_gap_report(book, library, report)

    validate_manifest(report)

    if not args.quiet:
        print(
            f"checked {report.checked['books']} books, {report.checked['chapters']} chapters, "
            f"{report.checked['units']} units, {report.checked['paragraphs']} paragraphs, "
            f"{report.checked['sentences']} sentences"
        )
        for message, places in report.warnings.items():
            print(f"warning: {len(places)} x {message} (first: {places[0]})")
    for error in report.errors:
        print(f"error: {error}")
    if report.errors:
        print(f"{len(report.errors)} error(s)")
        sys.exit(1)
    print("content ok")


if __name__ == "__main__":
    main()
