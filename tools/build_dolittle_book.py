#!/usr/bin/env python3
"""Build the Doctor Dolittle book package for Pagelence Android.

Reads the Project Gutenberg text plus the editorial sources in
content-src/doctor-dolittle/ and writes content/books/doctor-dolittle/book.json
and content/books-index.json.

Usage:
    python3 tools/build_dolittle_book.py          # build
    python3 tools/build_dolittle_book.py --check  # verify freshness
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
)
from gap import compute_gap, render_markdown  # noqa: E402

ANDROID = Path(__file__).resolve().parents[1]
REPO = ANDROID
SRC_DIR = ANDROID / "content-src" / "doctor-dolittle"
CONTENT_DIR = ANDROID / "content"
BOOK_DIR = CONTENT_DIR / "books" / "doctor-dolittle"
DICTIONARY = CONTENT_DIR / "third-party" / "dictionaries" / "ecdict.json"
WORD_LEVELS = CONTENT_DIR / "third-party" / "word-levels.json"

START_MARKER = "*** START OF THE PROJECT GUTENBERG EBOOK THE STORY OF DOCTOR DOLITTLE ***"
END_MARKER = "*** END OF THE PROJECT GUTENBERG EBOOK THE STORY OF DOCTOR DOLITTLE ***"
CHAPTER_RE = re.compile(r"^_THE (.*) CHAPTER_$")
WORD_RE = re.compile(r"[A-Za-z]+(?:['’][A-Za-z]+)?")

# Small-caps name emphasis in the 1920 printing; plain text keeps the caps
# otherwise, so restore normal name casing for these four words only.
SMALL_CAPS_NAMES = {
    "DOLITTLE": "Dolittle",
    "ERMINTRUDE": "Ermintrude",
    "CHEE": "Chee",
    "PULLYUS": "Pullyus",
}

_ILLUSTRATION = "[Illustration"
_SEPARATOR = re.compile(r"(\*\s*)+")


def load_source_text(path: Path) -> str:
    raw = path.read_text(encoding="utf-8-sig")
    body = raw[raw.index(START_MARKER) + len(START_MARKER): raw.index(END_MARKER)]
    # Transcriber notes and trailing rule lines are not part of the story.
    for stop in ("Transcriber’s Notes:", "Transcriber's Notes:"):
        if stop in body:
            body = body[: body.index(stop)]
    return body


def normalize_paragraph(lines: list[str]) -> str:
    text = " ".join(line.strip() for line in lines)
    text = re.sub(r"_([^_]+)_", r"\1", text)
    text = re.sub(
        r"\b(" + "|".join(SMALL_CAPS_NAMES) + r")\b",
        lambda match: SMALL_CAPS_NAMES[match.group(1)],
        text,
    )
    text = re.sub(r"\s+", " ", text).strip()
    match = re.match(r"^([A-Z]{2,})\b", text)
    if match and re.search(r"[a-z]", text[match.end():]):
        text = match.group(1).capitalize() + text[match.end():]
    return text


def parse_chapters(text: str) -> list[dict]:
    """Split the Gutenberg text into chapters.

    A chapter starts at a `_THE ... CHAPTER_` heading; the next non-empty line
    is the chapter title. Paragraphs are blank-line separated groups of lines.
    Illustration captions are not part of the body: a line beginning with
    `[Illustration` is dropped (a caption that wraps onto a second line leaves
    that tail behind, exactly like the printed source), and `* * *` rule lines
    are dropped too.
    """
    lines = text.splitlines()
    headings = [index for index, line in enumerate(lines) if CHAPTER_RE.match(line.strip())]
    chapters: list[dict] = []
    for position, start in enumerate(headings):
        title_index = start + 1
        while title_index < len(lines) and not lines[title_index].strip():
            title_index += 1
        end = headings[position + 1] if position + 1 < len(headings) else len(lines)

        blocks: list[list[str]] = []
        current: list[str] = []
        for line in lines[title_index + 1:end]:
            if line.strip():
                current.append(line)
            elif current:
                blocks.append(current)
                current = []
        if current:
            blocks.append(current)

        paragraphs = []
        for block in blocks:
            # An illustration block is a caption over one or two lines; the
            # first line starts with `[Illustration`, the wrapped tail does
            # not. Drop the whole block, not just the first line, or the tail
            # becomes a stray paragraph such as `branches to greet him”]`.
            if block and block[0].strip().startswith(_ILLUSTRATION):
                continue
            paragraph = normalize_paragraph(block)
            if not paragraph or _SEPARATOR.fullmatch(paragraph):
                continue
            paragraphs.append(paragraph)
        chapters.append({
            "title_source": lines[title_index].strip() if title_index < len(lines) else "",
            "paragraphs": paragraphs,
        })
    return chapters



def word_count(text: str) -> int:
    return len(WORD_RE.findall(text))


# --- title helpers ----------------------------------------------------------

SMALL_WORDS = {"a", "an", "and", "as", "at", "by", "for", "from", "in", "of", "on", "the", "to", "with"}


def title_case(upper: str) -> str:
    words = re.split(r"(\s+)", upper)
    out: list[str] = []
    index = 0
    for word in words:
        if not word.strip():
            out.append(word)
            continue
        lower = word.lower()
        if "-" in lower:
            out.append("-".join(part[:1].upper() + part[1:] for part in lower.split("-")))
        elif index > 0 and lower.strip(",.;:") in SMALL_WORDS:
            out.append(lower)
        else:
            out.append(lower[:1].upper() + lower[1:])
        index += 1
    return "".join(out)


# --- unit segmentation ------------------------------------------------------

def segment_chapter(paragraphs: list[str], target: int) -> list[list[int]]:
    """Choose paragraph ranges whose sizes sit close to `target` words.

    Dynamic programming over paragraph boundaries; every unit keeps whole
    paragraphs. Sizes below ~0.65x or above ~1.3x the target are penalized so
    that a single long paragraph is the only way to get an oversized unit.
    """
    counts = [word_count(p) for p in paragraphs]
    n = len(counts)
    prefix = [0]
    for count in counts:
        prefix.append(prefix[-1] + count)
    total = prefix[-1]

    def cost(size: int) -> float:
        value = abs(size - target)
        if size > target * 1.3:
            value += 200 + 4 * (size - target * 1.3)
        if size < target * 0.65:
            value += 200 + 4 * (target * 0.65 - size)
        return value

    kmin = max(1, round(total / (target * 1.35)))
    kmax = max(kmin, round(total / (target * 0.7)))
    infinity = float("inf")
    dp = [[infinity] * (n + 1) for _ in range(kmax + 1)]
    prev = [[-1] * (n + 1) for _ in range(kmax + 1)]
    dp[0][0] = 0
    for k in range(1, kmax + 1):
        for j in range(1, n + 1):
            for i in range(j):
                if dp[k - 1][i] == infinity:
                    continue
                size = prefix[j] - prefix[i]
                value = dp[k - 1][i] + cost(size)
                if value < dp[k][j]:
                    dp[k][j] = value
                    prev[k][j] = i
    best_k = min(range(kmin, kmax + 1), key=lambda k: dp[k][n])
    if dp[best_k][n] == infinity:
        raise SystemExit("could not segment chapter into units")

    ranges: list[list[int]] = []
    j = n
    for k in range(best_k, 0, -1):
        i = prev[k][j]
        ranges.append([i, j - 1])
        j = i
    ranges.reverse()
    return ranges


# --- assembly ---------------------------------------------------------------

def slug(value: str) -> str:
    value = value.lower()
    value = re.sub(r"[^a-z0-9]+", "-", value)
    return value.strip("-")


def balance_options(unit_id: str, quiz: list[dict]) -> list[dict]:
    """Rotate quiz options deterministically so the answer is not always first.

    Authors naturally write the correct option first; the shipped book would
    then teach readers to tap the first line. The rotation is content-neutral:
    it moves the option texts and updates `answer` to the same option, keyed by
    the unit id so a rebuild is byte-stable.
    """
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
    source_text = load_source_text(REPO / meta["sourceText"])
    parsed = parse_chapters(source_text)
    target = int(meta["unitTargetWords"])

    chapters = []
    word_total = 0
    unit_total = 0
    for chapter_index, chapter in enumerate(parsed, start=1):
        chapter_id = f"chapter-{chapter_index:02d}"
        paragraphs = chapter["paragraphs"]
        word_total += sum(word_count(p) for p in paragraphs)

        ranges = meta["unitRanges"].get(str(chapter_index))
        if ranges:
            ordered = [[a - 1, b - 1] for a, b in ranges]
        else:
            ordered = segment_chapter(paragraphs, target)

        units = []
        overlay = overlays.get(chapter_index, {})
        overlay_units = {unit["number"]: unit for unit in overlay.get("units", [])}
        covered: set[int] = set()
        for unit_index, (a, b) in enumerate(ordered, start=1):
            unit_id = f"{chapter_id}-u{unit_index:02d}"
            unit = {
                "id": unit_id,
                "number": unit_index,
                "title": None,
                "paragraphs": [],
            }
            overlay_unit = overlay_units.get(unit_index)
            if overlay_unit:
                unit["title"] = overlay_unit["title"]
                unit["introduction"] = overlay_unit["introduction"]
                covered.add(unit_index)

            for index in range(a, b + 1):
                covered.add(index + 1)
                paragraph_id = f"{chapter_id}-p{index + 1:03d}"
                paragraph = {"id": paragraph_id, "text": paragraphs[index]}
                if overlay_unit:
                    paragraph_overlays = overlay_unit.get("paragraphs", {})
                    paragraph_src = (
                        paragraph_overlays.get(paragraph_id)
                        or paragraph_overlays.get(str(index + 1))
                    )
                    if paragraph_src:
                        add_learning(paragraph, paragraph_src, paragraph_id)
                unit["paragraphs"].append(paragraph)

            if overlay_unit:
                if overlay_unit.get("quiz"):
                    unit["quiz"] = balance_options(unit_id, overlay_unit["quiz"])
                if overlay_unit.get("grammar"):
                    unit["grammar"] = overlay_unit["grammar"]
            units.append(unit)
            unit_total += 1

        missing = set(range(1, len(paragraphs) + 1)) - covered
        if missing:
            raise SystemExit(f"chapter {chapter_index}: paragraphs not in any unit: {sorted(missing)}")

        chapters.append({
            "id": chapter_id,
            "number": chapter_index,
            "title": {
                "zh": meta["chapterTitles"][str(chapter_index)],
                "en": title_case(chapter["title_source"]),
            },
            "units": units,
        })

    if len(chapters) != 21:
        raise SystemExit(f"expected 21 chapters, parsed {len(chapters)}")

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


def add_learning(paragraph: dict, source: dict, paragraph_id: str) -> None:
    sentences = split_sentences(paragraph["text"])
    translations = source.get("sentences", [])
    if len(translations) != len(sentences):
        raise SystemExit(
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


# --- validation -------------------------------------------------------------

def validate(book: dict, stats: dict) -> None:
    seen: set[str] = set()
    quiz_checked = 0
    sentence_checked = 0

    def claim(value: str, kind: str) -> None:
        if value in seen:
            raise SystemExit(f"duplicate {kind} id: {value}")
        seen.add(value)

    for chapter in book["chapters"]:
        claim(chapter["id"], "chapter")
        for unit in chapter["units"]:
            claim(unit["id"], "unit")
            unit_text = " ".join(p["text"] for p in unit["paragraphs"])
            unit_sentences = 0
            for paragraph in unit["paragraphs"]:
                claim(paragraph["id"], "paragraph")
                if "sentences" in paragraph:
                    sentence_checked += len(paragraph["sentences"])
                    unit_sentences += len(paragraph["sentences"])
                    for sentence in paragraph["sentences"]:
                        if not sentence["text"].strip() or not sentence["zh"].strip():
                            raise SystemExit(f"{paragraph['id']}: empty sentence or translation")
                for hint in paragraph.get("hints", []):
                    claim(hint["id"], "hint")
                    if hint["target"].lower() not in paragraph["text"].lower():
                        raise SystemExit(f"{hint['id']}: target not in paragraph text")
                    if hint["kind"] not in {"word", "phrase", "sentence"}:
                        raise SystemExit(f"{hint['id']}: unknown hint kind {hint['kind']}")
            for item in unit.get("quiz", []):
                quiz_checked += 1
                if not 0 <= item["answer"] < len(item["options"]):
                    raise SystemExit(f"{unit['id']}: quiz answer out of range")
                if "sentence" in item and not 0 < item["sentence"] <= unit_sentences:
                    raise SystemExit(f"{unit['id']}: quiz sentence index out of range")
                feedback = item.get("feedback")
                if not isinstance(feedback, dict) or not str(feedback.get("zh", "")).strip():
                    raise SystemExit(f"{unit['id']}: quiz feedback must be {{zh, en?}}")
            grammar = unit.get("grammar")
            if grammar:
                excerpt = grammar["excerpt"]
                if excerpt not in unit_text:
                    raise SystemExit(f"{unit['id']}: grammar excerpt not found in unit text")
                if not grammar.get("focus", {}).get("zh"):
                    raise SystemExit(f"{unit['id']}: grammar focus is empty")
                point_ids = grammar.get("pointIds") or []
                if len(set(point_ids)) != len(point_ids):
                    raise SystemExit(f"{unit['id']}: grammar pointIds has duplicates")
                if not point_ids and not (grammar.get("explain") and grammar.get("form")):
                    raise SystemExit(
                        f"{unit['id']}: library-outside grammar needs explain + form + focus"
                    )
        if not chapter["units"]:
            raise SystemExit(f"{chapter['id']}: no units")

    for paragraph in book["chapters"][0]["units"][0]["paragraphs"]:
        if "text" not in paragraph or not paragraph["text"].strip():
            raise SystemExit("empty paragraph text")
    if stats["chapters"] != 21 or stats["units"] < 80:
        raise SystemExit(f"unexpected structure: {stats}")
    print(
        f"ok: {stats['chapters']} chapters, {stats['units']} units, {stats['words']} words, "
        f"{sentence_checked} translated sentences, {quiz_checked} quiz items"
    )


# --- output -----------------------------------------------------------------

def _plain(value: str) -> str:
    value = unicodedata.normalize("NFKC", value or "").lower()
    return re.sub(r"[^0-9a-z\u3400-\u9fff]+", "", value)


_CLAUSE_MARKERS = re.compile(
    r"\b(?:which|who|whom|whose|when|while|because|although|though|if|unless|"
    r"after|before|since|where|whether)\b",
    re.IGNORECASE,
)
_ADVANCED = {"B1", "B2", "C1", "C2"}


def long_sentence_report(book: dict) -> dict:
    """Candidate long sentences for the optional `analysis` field (§5.8).

    The score is a filter for an editor, not a final level: word count, clause
    and non-finite markers, and the share of content words above A2. Only
    candidates are listed; `analysis` stays optional and never blocks a build.
    """
    levels = json.loads(WORD_LEVELS.read_text(encoding="utf-8"))["levels"]
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
                    for token in WORD_RE.findall(text):
                        lower = token.lower()
                        if lower in {"the", "a", "an", "and", "but", "or", "of", "to", "in", "on",
                                     "at", "for", "with", "he", "she", "it", "they", "i", "you",
                                     "was", "were", "is", "are", "had", "have", "his", "her",
                                     "their", "my", "your", "that", "this", "as", "not", "be"}:
                            continue
                        content += 1
                        word_levels = levels.get(lower) or {}
                        level = next(iter(word_levels.values()), None) if word_levels else None
                        if level in _ADVANCED:
                            advanced += 1
                    ratio = advanced / content if content else 0.0
                    if words < 25 and not (words >= 18 and markers >= 2):
                        continue
                    score = words + 5 * markers + 20 * ratio
                    level = "B1" if score < 45 else "B2" if score < 60 else "C1" if score < 80 else "C2"
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
                        "suggestedLevel": level,
                        "hasAnalysis": bool(sentence.get("analysis")),
                        "text": text,
                    })
    return {
        "schemaVersion": 1,
        "bookId": book["id"],
        "bookLevel": "A2",
        "rule": "candidate if words >= 25, or words >= 18 with at least 2 clause markers; score = words + 5*markers + 20*advancedRatio",
        "totals": {
            "candidates": len(rows),
            "withAnalysis": sum(1 for row in rows if row["hasAnalysis"]),
        },
        "sentences": rows,
    }


def hint_overlap_report(book: dict) -> dict:
    """Report how much of each hint repeats the general dictionary.

    Book-package spec §六: the report is produced by the book build, the
    threshold is decided later, and it never blocks a build. Hints that only
    repeat `senses` / `en` / `phonetic` / part of speech are the ones to trim.
    """
    entries = json.loads(DICTIONARY.read_text(encoding="utf-8"))["entries"]
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


def write_outputs(book: dict, stats: dict, check: bool) -> None:
    book_text = dump(book)
    library = {
        point["id"]: point
        for point in json.loads(
            (CONTENT_DIR / "grammar" / "grammar-library.json").read_text(encoding="utf-8")
        )["points"]
    }
    report = compute_gap(
        book,
        library,
        source={"book": "Project Gutenberg eBook #501", "builtBy": "tools/build_dolittle_book.py"},
        notes=[
            "全书 21 章 103 单元已按书籍包内容规范补齐标题、导读、句译、测验与语法。",
        ],
    )
    outputs = {
        BOOK_DIR / "book.json": book_text,
        CONTENT_DIR / "reports" / f"{book['id']}.gap.json": dump(report),
        CONTENT_DIR / "reports" / f"{book['id']}.hints.json": dump(hint_overlap_report(book)),
        CONTENT_DIR / "reports" / f"{book['id']}.long-sentences.json": dump(long_sentence_report(book)),
    }
    if check:
        # In check mode the on-disk book is the reference; stale book text is
        # already reported by the first entry.
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
    # The manifest hashes the book file, so it is computed after the book is on
    # disk; otherwise the first build after a content change is stale.
    manifest_path = CONTENT_DIR / "books-index.json"
    manifest_path.write_text(dump(build_manifest()), encoding="utf-8")
    print(f"wrote {manifest_path.relative_to(REPO)}")
    print(
        f"migrated {stats['chapters']} chapters, {stats['units']} units, "
        f"{report['totals']['unitsMissingTranslations']} units lack translations"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify generated files are current")
    args = parser.parse_args()

    meta = json.loads((SRC_DIR / "meta.json").read_text(encoding="utf-8"))
    errors = meta_errors(meta)
    if errors:
        raise SystemExit("content-src/doctor-dolittle/meta.json: " + "; ".join(errors))
    overlays = {}
    for path in sorted(SRC_DIR.glob("chapter-*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        overlays[int(data["chapter"])] = data

    book, stats = build_book(meta, overlays)
    if stats["words"] != meta["wordCount"]:
        raise SystemExit(
            f"meta.wordCount is {meta['wordCount']}, but the built text has {stats['words']} words; "
            "update content-src/doctor-dolittle/meta.json"
        )
    validate(book, stats)
    write_outputs(book, stats, args.check)


if __name__ == "__main__":
    main()
