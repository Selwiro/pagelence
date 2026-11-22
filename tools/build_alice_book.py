#!/usr/bin/env python3
"""Build the Alice book package for Pagelence Android.

Sources:
    content-src/alice/meta.json             package metadata, chapter titles, frozen unit ranges
    content-src/alice/source/chapters.json  English paragraphs per chapter (1866 Wikisource text)
    content-src/alice/chapter-NN.json       editorial overlays: titles, introductions,
                                            sentence translations, hints, quizzes, grammar

The unit boundaries are frozen in `meta.json:unitRanges` (1-based paragraph
ranges per chapter). `book.json`, the gap report, the hint-overlap report and
the long-sentence report are generated; never hand-edit them.

Usage:
    python3 tools/build_alice_book.py          # build
    python3 tools/build_alice_book.py --check   # verify freshness
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "lib"))

from content import (  # noqa: E402
    BOOK_SCHEMA_VERSION,
    build_manifest,
    dump,
    meta_errors,
    split_sentences,
    word_count,
)
from gap import compute_gap  # noqa: E402
from outline import build_outline  # noqa: E402

ANDROID = Path(__file__).resolve().parents[1]
REPO = ANDROID
SRC_DIR = ANDROID / "content-src" / "alice"
SOURCE = SRC_DIR / "source" / "chapters.json"
CONTENT_DIR = ANDROID / "content"
# 报告是给编辑看的产物，不进 APK，所以放在 content/ 之外的 reports/。
REPORTS_DIR = ANDROID / "reports"
BOOK_ID = "alice-1866"
BOOK_DIR = CONTENT_DIR / "books" / BOOK_ID
GRAMMAR = CONTENT_DIR / "grammar" / "grammar-library.json"
DICTIONARY = CONTENT_DIR / "third-party" / "dictionaries" / "ecdict.json"
WORD_LEVELS = CONTENT_DIR / "third-party" / "word-levels.json"

CHAPTER_COUNT = 12
UNIT_COUNT = 104
HINT_KINDS = {"word", "phrase", "sentence"}


class BuildError(Exception):
    pass


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def normalize(text: str) -> str:
    return " ".join(text.split())


def source_chapters() -> list[dict]:
    """Return the 1866 chapters with whitespace-normalised paragraphs.

    The Wikisource transcription keeps the line breaks of the printed poems;
    the reader treats a paragraph as one flowing text, so lines are joined with
    a single space. Boundaries and the frozen sentence split are unaffected.
    """
    data = load_json(SOURCE)
    chapters = []
    for chapter in data:
        chapters.append({
            "number": chapter["number"],
            "subtitle": chapter.get("subtitle", ""),
            "paragraphs": [normalize(p) for p in chapter["paragraphs"]],
        })
    if len(chapters) != CHAPTER_COUNT:
        raise BuildError(f"source has {len(chapters)} chapters, want {CHAPTER_COUNT}")
    return chapters


def structure(meta: dict) -> list[dict]:
    """The frozen shape of the book: chapters, units and paragraph ids."""
    chapters = source_chapters()
    result = []
    for chapter in chapters:
        number = chapter["number"]
        chapter_id = f"chapter-{number:02d}"
        ranges = meta["unitRanges"].get(str(number))
        if not ranges:
            raise BuildError(f"meta.unitRanges is missing chapter {number}")
        expected = list(range(1, len(chapter["paragraphs"]) + 1))
        covered = [index for a, b in ranges for index in range(a, b + 1)]
        if covered != expected:
            raise BuildError(f"chapter {number}: unitRanges do not cover 1..{len(expected)} exactly")
        units = []
        for unit_index, (a, b) in enumerate(ranges, start=1):
            paragraphs = []
            for index in range(a, b + 1):
                text = chapter["paragraphs"][index - 1]
                paragraphs.append({
                    "id": f"{chapter_id}-p{index:03d}",
                    "index": index,
                    "text": text,
                    "sentences": split_sentences(text),
                })
            units.append({
                "id": f"{chapter_id}-u{unit_index:02d}",
                "number": unit_index,
                "words": sum(word_count(p["text"]) for p in paragraphs),
                "paragraphs": paragraphs,
            })
        title = meta["chapters"][number - 1]["title"]
        result.append({
            "id": chapter_id,
            "number": number,
            "title": title,
            "units": units,
        })
    total = sum(len(u["paragraphs"]) for c in result for u in c["units"])
    if total != sum(len(c["paragraphs"]) for c in chapters):
        raise BuildError("unit ranges do not cover every paragraph")
    return result


def add_learning(paragraph: dict, source: dict, paragraph_id: str) -> None:
    sentences = split_sentences(paragraph["text"])
    translations = source.get("sentences", [])
    if len(translations) != len(sentences):
        raise BuildError(
            f"{paragraph_id}: {len(translations)} translations for {len(sentences)} sentences"
        )
    hints = []
    for hint in source.get("hints", []):
        entry = dict(hint)
        entry["id"] = f"{paragraph_id}-{hint['id']}"
        hints.append(entry)

    analysis_by_sentence = source.get("analysis") or {}
    sentence_objects = []
    for number, (text, zh) in enumerate(zip(sentences, translations), start=1):
        entry = {"text": text, "zh": zh}
        analysis = analysis_by_sentence.get(str(number))
        if analysis:
            entry["analysis"] = analysis
        sentence_objects.append(entry)
    paragraph["sentences"] = sentence_objects
    if hints:
        paragraph["hints"] = hints


def slug(value: str) -> str:
    value = value.lower()
    value = re.sub(r"[^a-z0-9]+", "-", value)
    return value.strip("-")


def balance_options(unit_id: str, quiz: list[dict]) -> list[dict]:
    """Rotate quiz options deterministically so the answer is not always first."""
    balanced = []
    for index, item in enumerate(quiz):
        item = dict(item)
        options = list(item.get("options") or [])
        answer = item.get("answer")
        count = len(options)
        if count >= 2 and isinstance(answer, int) and 0 <= answer < count:
            shift = (sum(ord(char) for char in unit_id) + index * 2) % count
            if shift:
                item["options"] = options[-shift:] + options[:-shift]
                item["answer"] = (answer + shift) % count
        balanced.append(item)
    return balanced


def build_book(meta: dict, overlays: dict[int, dict]) -> tuple[dict, dict]:
    chapters = []
    word_total = 0
    unit_total = 0
    for chapter in structure(meta):
        overlay = overlays.get(chapter["number"], {})
        overlay_units = {unit["number"]: unit for unit in overlay.get("units", [])}
        units = []
        for unit in chapter["units"]:
            unit_obj = {
                "id": unit["id"],
                "number": unit["number"],
                "title": None,
                "paragraphs": [],
            }
            overlay_unit = overlay_units.get(unit["number"])
            if overlay_unit:
                unit_obj["title"] = overlay_unit["title"]
                unit_obj["introduction"] = overlay_unit["introduction"]
            for paragraph in unit["paragraphs"]:
                paragraph_obj = {"id": paragraph["id"], "text": paragraph["text"]}
                word_total += word_count(paragraph["text"])
                if overlay_unit:
                    paragraph_src = (overlay_unit.get("paragraphs") or {}).get(paragraph["id"])
                    if paragraph_src is not None:
                        add_learning(paragraph_obj, paragraph_src, paragraph["id"])
                unit_obj["paragraphs"].append(paragraph_obj)
            if overlay_unit:
                if overlay_unit.get("quiz"):
                    unit_obj["quiz"] = balance_options(unit["id"], overlay_unit["quiz"])
                if overlay_unit.get("grammar"):
                    unit_obj["grammar"] = overlay_unit["grammar"]
            units.append(unit_obj)
            unit_total += 1
        chapters.append({
            "id": chapter["id"],
            "number": chapter["number"],
            "title": chapter["title"],
            "units": units,
        })

    book = {
        "schemaVersion": BOOK_SCHEMA_VERSION,
        "id": meta["id"],
        "title": meta["title"],
        "author": meta["author"],
        "edition": meta["edition"],
        "description": meta["description"],
        "source": meta["source"],
        "chapters": chapters,
    }
    stats = {"chapters": len(chapters), "units": unit_total, "words": word_total}
    return book, stats


def validate(book: dict, stats: dict) -> None:
    seen: set[str] = set()
    quiz_checked = 0
    sentence_checked = 0

    def claim(value: str, kind: str) -> None:
        if value in seen:
            raise BuildError(f"duplicate {kind} id: {value}")
        seen.add(value)

    for chapter in book["chapters"]:
        claim(chapter["id"], "chapter")
        for unit in chapter["units"]:
            claim(unit["id"], "unit")
            unit_text = " ".join(p["text"] for p in unit["paragraphs"])
            unit_sentence_list = [
                piece
                for paragraph in unit["paragraphs"]
                for piece in split_sentences(paragraph["text"])
            ]
            unit_sentences = len(unit_sentence_list)
            for paragraph in unit["paragraphs"]:
                claim(paragraph["id"], "paragraph")
                if "sentences" in paragraph:
                    sentence_checked += len(paragraph["sentences"])
                    expected = split_sentences(paragraph["text"])
                    if [s["text"] for s in paragraph["sentences"]] != expected:
                        raise BuildError(f"{paragraph['id']}: stored sentences drifted from the splitter")
                    for sentence in paragraph["sentences"]:
                        if not sentence["text"].strip() or not sentence["zh"].strip():
                            raise BuildError(f"{paragraph['id']}: empty sentence or translation")
                for hint in paragraph.get("hints", []):
                    claim(hint["id"], "hint")
                    if hint["target"].lower() not in paragraph["text"].lower():
                        raise BuildError(f"{hint['id']}: target not in paragraph text")
                    if hint["kind"] not in HINT_KINDS:
                        raise BuildError(f"{hint['id']}: unknown hint kind {hint['kind']}")
                    index = hint.get("sentence")
                    if index is not None:
                        if not 1 <= index <= unit_sentences:
                            raise BuildError(f"{hint['id']}: sentence index out of range")
                        if hint["target"].lower() not in unit_sentence_list[index - 1].lower():
                            raise BuildError(f"{hint['id']}: target is not in the sentence it points at")
            for item in unit.get("quiz", []):
                quiz_checked += 1
                if not 0 <= item["answer"] < len(item["options"]):
                    raise BuildError(f"{unit['id']}: quiz answer out of range")
                if "sentence" in item and not 0 < item["sentence"] <= unit_sentences:
                    raise BuildError(f"{unit['id']}: quiz sentence index out of range")
                feedback = item.get("feedback")
                if not isinstance(feedback, dict) or not str(feedback.get("zh", "")).strip():
                    raise BuildError(f"{unit['id']}: quiz feedback must be {{zh, en?}}")
            grammar = unit.get("grammar")
            if grammar:
                if grammar.get("excerpt") not in unit_text:
                    raise BuildError(f"{unit['id']}: grammar excerpt not found in unit text")
                if not grammar.get("focus", {}).get("zh"):
                    raise BuildError(f"{unit['id']}: grammar focus is empty")
                point_ids = grammar.get("pointIds") or []
                if len(set(point_ids)) != len(point_ids):
                    raise BuildError(f"{unit['id']}: grammar pointIds has duplicates")
                if not point_ids and not (grammar.get("explain") and grammar.get("form")):
                    raise BuildError(
                        f"{unit['id']}: library-outside grammar needs explain + form + focus"
                    )
        if not chapter["units"]:
            raise BuildError(f"{chapter['id']}: no units")

    if stats["chapters"] != CHAPTER_COUNT or stats["units"] != UNIT_COUNT:
        raise BuildError(f"unexpected structure: {stats}")
    print(
        f"ok: {stats['chapters']} chapters, {stats['units']} units, {stats['words']} words, "
        f"{sentence_checked} translated sentences, {quiz_checked} quiz items"
    )


# --- reports ----------------------------------------------------------------

def _plain(value: str) -> str:
    value = unicodedata.normalize("NFKC", value or "").lower()
    return re.sub(r"[^0-9a-z\u3400-\u9fff]+", "", value)


_CLAUSE_MARKERS = re.compile(
    r"\b(?:which|who|whom|whose|when|while|because|although|though|if|unless|"
    r"after|before|since|where|whether)\b",
    re.IGNORECASE,
)
_ADVANCED = {"B1", "B2", "C1", "C2"}


def long_sentence_report(book: dict, level: str) -> dict:
    """Candidate long sentences for the optional `analysis` field (§5.8)."""
    levels = load_json(WORD_LEVELS)["levels"]
    rows = []
    for chapter in book["chapters"]:
        for unit in chapter["units"]:
            for paragraph in unit["paragraphs"]:
                for index, sentence in enumerate(paragraph.get("sentences") or [], start=1):
                    text = sentence["text"]
                    words = word_count(text)
                    markers = len(_CLAUSE_MARKERS.findall(text))
                    advanced = 0
                    content = 0
                    for token in re.findall(r"[A-Za-z]+(?:['’][A-Za-z]+)?", text):
                        lower = token.lower()
                        if lower in {"the", "a", "an", "and", "but", "or", "of", "to", "in", "on",
                                     "at", "for", "with", "he", "she", "it", "they", "i", "you",
                                     "was", "were", "is", "are", "had", "have", "his", "her",
                                     "their", "my", "your", "that", "this", "as", "not", "be"}:
                            continue
                        content += 1
                        word_levels = levels.get(lower) or {}
                        word_level = next(iter(word_levels.values()), None) if word_levels else None
                        if word_level in _ADVANCED:
                            advanced += 1
                    ratio = advanced / content if content else 0.0
                    if words < 25 and not (words >= 18 and markers >= 2):
                        continue
                    score = words + 5 * markers + 20 * ratio
                    suggested = "B1" if score < 45 else "B2" if score < 60 else "C1" if score < 80 else "C2"
                    rows.append({
                        "chapterId": chapter["id"],
                        "unitId": unit["id"],
                        "paragraphId": paragraph["id"],
                        "sentenceIndex": index,
                        "words": words,
                        "markers": markers,
                        "advancedWords": advanced,
                        "advancedRatio": round(ratio, 2),
                        "score": round(score, 1),
                        "suggestedLevel": suggested,
                        "hasAnalysis": bool(sentence.get("analysis")),
                        "text": text,
                    })
    return {
        "schemaVersion": 1,
        "bookId": book["id"],
        "bookLevel": level,
        "rule": "candidate if words >= 25, or words >= 18 with at least 2 clause markers; "
                "score = words + 5*markers + 20*advancedRatio",
        "totals": {
            "candidates": len(rows),
            "withAnalysis": sum(1 for row in rows if row["hasAnalysis"]),
        },
        "sentences": rows,
    }


def hint_overlap_report(book: dict) -> dict:
    """Report how much of each hint repeats the general dictionary."""
    entries = load_json(DICTIONARY)["entries"]
    rows = []
    totals = {"hints": 0, "matched": 0, "zh": 0, "en": 0, "ipa": 0, "pos": 0, "any": 0}
    for chapter in book["chapters"]:
        for unit in chapter["units"]:
            for paragraph in unit["paragraphs"]:
                for hint in paragraph.get("hints") or []:
                    totals["hints"] += 1
                    key = hint["target"].lower().replace("’", "'")
                    entry = entries.get(key) or entries.get((hint.get("lemma") or "").lower())
                    if not entry:
                        continue
                    totals["matched"] += 1
                    duplicate = []
                    senses = " ".join(sense[1] for sense in entry.get("senses") or [] if len(sense) > 1)
                    if _plain(hint.get("zh")) and _plain(hint["zh"]) in _plain(senses):
                        duplicate.append("zh")
                    english = " ".join(part[1] for part in entry.get("en") or [] if len(part) > 1)
                    if _plain(hint.get("en")) and _plain(hint["en"]) in _plain(english):
                        duplicate.append("en")
                    phonetic = _plain(entry.get("phonetic"))
                    if hint.get("ipa") and _plain(hint["ipa"]) == phonetic:
                        duplicate.append("ipa")
                    pos = hint.get("pos")
                    if pos and any((sense[0] or "").strip(".") == pos for sense in entry.get("senses") or []):
                        duplicate.append("pos")
                    for field in duplicate:
                        totals[field] += 1
                    if duplicate:
                        totals["any"] += 1
                        rows.append({
                            "id": hint["id"],
                            "target": hint["target"],
                            "duplicates": duplicate,
                        })
    return {
        "schemaVersion": 1,
        "bookId": book["id"],
        "dictionary": "ECDICT 39cfb96 (core dictionary)",
        "sources": {"hints": "book.json", "dictionary": "content/third-party/dictionaries/ecdict.json"},
        "totals": totals,
        "duplicateHints": rows,
    }


def write_outputs(book: dict, stats: dict, meta: dict, check: bool) -> None:
    library = {point["id"]: point for point in load_json(GRAMMAR)["points"]}
    report = compute_gap(
        book,
        library,
        source={
            "book": "Wikisource: Alice's Adventures in Wonderland (1866)",
            "url": "https://en.wikisource.org/wiki/Alice%27s_Adventures_in_Wonderland_(1866)",
            "builtBy": "tools/build_alice_book.py",
        },
        notes=[
            "全书 12 章 104 单元按书籍包内容规范补齐标题、导读、句译、测验与语法。",
            "unitRanges 冻结在 content-src/alice/meta.json，重建不重算单元边界。",
        ],
    )
    outputs = {
        BOOK_DIR / "book.json": dump(book),
        BOOK_DIR / "outline.json": dump(build_outline(book)),
        REPORTS_DIR / f"{book['id']}.gap.json": dump(report),
        REPORTS_DIR / f"{book['id']}.hints.json": dump(hint_overlap_report(book)),
        REPORTS_DIR / f"{book['id']}.long-sentences.json": dump(
            long_sentence_report(book, meta["level"])
        ),
    }
    if check:
        outputs[CONTENT_DIR / "books-index.json"] = dump(build_manifest())
        stale = []
        for path, text in outputs.items():
            if not path.is_file() or path.read_text(encoding="utf-8") != text:
                stale.append(path)
        if stale:
            print("stale: " + ", ".join(str(p.relative_to(REPO)) for p in stale))
            sys.exit(1)
        print("up to date")
        return
    for path, text in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        print(f"wrote {path.relative_to(REPO)}")
    manifest_path = CONTENT_DIR / "books-index.json"
    manifest_path.write_text(dump(build_manifest()), encoding="utf-8")
    print(f"wrote {manifest_path.relative_to(REPO)}")
    print(
        f"built {stats['chapters']} chapters, {stats['units']} units, "
        f"{report['totals']['unitsMissingTranslations']} units lack translations"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify generated files are current")
    args = parser.parse_args()

    meta = load_json(SRC_DIR / "meta.json")
    errors = meta_errors(meta)
    if errors:
        raise SystemExit("content-src/alice/meta.json: " + "; ".join(errors))
    overlays = {}
    for path in sorted(SRC_DIR.glob("chapter-*.json")):
        data = load_json(path)
        overlays[int(data["chapter"])] = data

    book, stats = build_book(meta, overlays)
    if stats["words"] != meta["wordCount"]:
        raise SystemExit(
            f"meta.wordCount is {meta['wordCount']}, but the built text has {stats['words']} words; "
            "update content-src/alice/meta.json"
        )
    validate(book, stats)
    write_outputs(book, stats, meta, args.check)


if __name__ == "__main__":
    main()
