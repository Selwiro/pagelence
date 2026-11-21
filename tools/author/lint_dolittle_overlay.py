#!/usr/bin/env python3
"""Lint one Doctor Dolittle editorial chapter overlay.

Checks the overlay against the built book structure and the book-package spec
before the book builder sees it. Run after every edit:

    python3 tools/author/lint_dolittle_overlay.py 7

Exit code 0 means the overlay is structurally ready; warnings are printed for
things the editor should look at (lengths, meta language, possible dictionary
duplicates are reported by the build, not here).
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "lib"))

from content import ANDROID, split_sentences  # noqa: E402

SRC_DIR = ANDROID / "content-src" / "doctor-dolittle"
BOOK = ANDROID / "content" / "books" / "doctor-dolittle" / "book.json"
GRAMMAR = ANDROID / "content" / "grammar" / "grammar-library.json"

HANZI = re.compile(r"[\u3400-\u9fff]")
HTML_TAG = re.compile(r"<\s*/?\s*[a-zA-Z][^>]*>")
SLUG = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
HINT_KINDS = {"word", "phrase", "sentence"}
GRAMMAR_ROLES = {"subordinate", "relative", "nonfinite", "prepositional", "appositive", "parenthetical"}
CEFR_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"}
META_PHRASES = ("本单元", "接下来", "作者认为", "你会发现", "请注意")


def hanzi_count(value: str) -> int:
    return len(HANZI.findall(value or ""))


def word_count(value: str) -> int:
    return len(re.findall(r"[A-Za-z0-9’'-]+", value or ""))


class Lint:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []

    def error(self, where: str, message: str) -> None:
        self.errors.append(f"{where}: {message}")

    def warn(self, where: str, message: str) -> None:
        self.warnings.append(f"{where}: {message}")

    def text(self, value, where: str, field: str, required: bool = True) -> str:
        if not isinstance(value, str):
            if required:
                self.error(where, f"{field} must be a string")
            return ""
        if not value.strip():
            if required:
                self.error(where, f"{field} is empty")
            return ""
        if HTML_TAG.search(value):
            self.error(where, f"{field} contains HTML")
        return value.strip()


def localized(lint: Lint, value, where: str, field: str, required: bool = True) -> tuple[str, str]:
    if not isinstance(value, dict):
        if required:
            lint.error(where, f"{field} must be {{zh, en}}")
        return "", ""
    zh = lint.text(value.get("zh"), where, f"{field}.zh", required)
    en = lint.text(value.get("en"), where, f"{field}.en", required)
    for key in value:
        if key not in {"zh", "en"}:
            lint.error(where, f"{field} has unknown language key {key}")
    return zh, en


def check_title(lint: Lint, title, where: str) -> None:
    zh, en = localized(lint, title, where, "title")
    if zh and hanzi_count(zh) > 16:
        lint.error(where, f"title.zh is {hanzi_count(zh)} hanzi (max 16)")
    if en and word_count(en) > 8:
        lint.error(where, f"title.en is {word_count(en)} words (max 8)")


def check_introduction(lint: Lint, intro, where: str, first_unit_of_book: bool) -> None:
    zh, en = localized(lint, intro, where, "introduction")
    zh_len = hanzi_count(zh)
    en_len = word_count(en)
    zh_max = 110 if first_unit_of_book else 90
    en_max = 60 if first_unit_of_book else 50
    if zh and zh_len > zh_max:
        lint.error(where, f"introduction.zh is {zh_len} hanzi (max {zh_max})")
    elif zh and zh_len < 40:
        lint.warn(where, f"introduction.zh is {zh_len} hanzi (want 40–90)")
    if en and en_len > en_max:
        lint.error(where, f"introduction.en is {en_len} words (max {en_max})")
    elif en and en_len < 25:
        lint.warn(where, f"introduction.en is {en_len} words (want 25–50)")
    for phrase in META_PHRASES:
        if phrase in zh or phrase.lower() in en.lower():
            lint.warn(where, f"introduction uses meta language: {phrase}")
    if "您" in zh:
        lint.warn(where, "introduction uses 您; the product voice uses 你")


def check_analysis(lint: Lint, analysis, sentence: str, where: str) -> None:
    if not isinstance(analysis, dict):
        lint.error(where, "analysis must be an object")
        return
    if analysis.get("level") not in CEFR_LEVELS:
        lint.error(where, f"analysis.level {analysis.get('level')!r} is not A1–C2")
    core = lint.text(analysis.get("core"), where, "analysis.core")
    if core and core not in sentence:
        lint.warn(where, "analysis.core is not a verbatim run of the sentence (check it keeps the original wording)")
    parts = analysis.get("parts")
    if not isinstance(parts, list) or not 1 <= len(parts) <= 4:
        lint.error(where, "analysis.parts must have 1–4 items")
        parts = []
    for index, part in enumerate(parts, start=1):
        pwhere = f"{where}/parts{index}"
        text = lint.text(part.get("text"), pwhere, "text")
        if text and text not in sentence:
            lint.error(pwhere, "text is not verbatim from the sentence")
        if part.get("role") not in GRAMMAR_ROLES:
            lint.error(pwhere, f"role {part.get('role')!r} is not one of {sorted(GRAMMAR_ROLES)}")
        lint.text(part.get("zh"), pwhere, "zh")
        if part.get("en") is not None:
            lint.text(part.get("en"), pwhere, "en")
    for field in ("order", "note"):
        if analysis.get(field) is not None:
            localized(lint, analysis[field], where, field, required=True)


def check_grammar(lint: Lint, grammar, unit_text: str, library: dict, where: str) -> None:
    if not isinstance(grammar, dict):
        lint.error(where, "grammar must be an object")
        return
    excerpt = lint.text(grammar.get("excerpt"), where, "grammar.excerpt")
    if excerpt and excerpt not in unit_text:
        lint.error(where, "grammar.excerpt is not verbatim in the unit text")
    focus_zh, focus_en = localized(lint, grammar.get("focus"), where, "grammar.focus")
    focus_len = hanzi_count(focus_zh)
    if focus_zh and focus_len > 60:
        lint.error(where, f"grammar.focus.zh is {focus_len} hanzi (max 60)")
    if not focus_en:
        lint.warn(where, "grammar.focus.en is missing (English UI shows the library text only)")
    point_ids = grammar.get("pointIds")
    if point_ids is None:
        point_ids = []
    if not isinstance(point_ids, list):
        lint.error(where, "grammar.pointIds must be an array")
        point_ids = []
    if len(set(point_ids)) != len(point_ids):
        lint.error(where, "grammar.pointIds has duplicates")
    for point_id in point_ids:
        if point_id not in library:
            lint.error(where, f"grammar.pointId {point_id!r} is not in the grammar library")
    if grammar.get("type") not in {None, "new", "review"}:
        lint.error(where, "grammar.type must be new or review")
    if point_ids:
        # A library point supplies explain/form; the book must not repeat them.
        for field in ("explain", "form"):
            if grammar.get(field) is not None:
                lint.warn(where, f"grammar.{field} is ignored because pointIds resolves; remove it")
    else:
        if not grammar.get("explain") or not grammar.get("form"):
            lint.error(where, "library-outside grammar needs explain + form + focus")
        else:
            localized(lint, grammar["explain"], where, "grammar.explain")
            localized(lint, grammar["form"], where, "grammar.form")


def check_hints(lint: Lint, hints, paragraph_text: str, unit_sentences: list[str],
                unit_hint_ids: set[str], where: str) -> int:
    if hints is None:
        return 0
    if not isinstance(hints, list):
        lint.error(where, "hints must be an array")
        return 0
    seen: set[tuple[str, int]] = set()
    for index, hint in enumerate(hints, start=1):
        hwhere = f"{where}/hint{index}"
        if not isinstance(hint, dict):
            lint.error(hwhere, "hint must be an object")
            continue
        hint_id = lint.text(hint.get("id"), hwhere, "id")
        if hint_id and not SLUG.fullmatch(hint_id):
            lint.error(hwhere, f"id {hint_id!r} must be lowercase kebab-case")
        if hint_id in unit_hint_ids:
            lint.error(hwhere, f"duplicate hint id {hint_id!r} in the unit")
        unit_hint_ids.add(hint_id)
        kind = hint.get("kind")
        if kind not in HINT_KINDS:
            lint.error(hwhere, f"kind {kind!r} is not word/phrase/sentence")
        target = lint.text(hint.get("target"), hwhere, "target")
        if target and target.lower() not in paragraph_text.lower():
            lint.error(hwhere, "target is not in the paragraph text")
        lint.text(hint.get("zh"), hwhere, "zh")
        lint.text(hint.get("en"), hwhere, "en")
        sentence = hint.get("sentence")
        if sentence is not None:
            if not isinstance(sentence, int) or not 1 <= sentence <= len(unit_sentences):
                lint.error(hwhere, "sentence must be a 1-based unit sentence index")
            elif target and target.lower() not in unit_sentences[sentence - 1].lower():
                lint.error(hwhere, "target is not in the sentence it points at")
        key = (target.lower(), sentence or 0)
        if key in seen:
            lint.error(hwhere, "duplicate hint target/sentence in this paragraph")
        seen.add(key)
        if hint.get("context") is not None:
            localized(lint, hint["context"], hwhere, "context", required=False)
        for optional in ("ipa", "pos", "lemma", "example"):
            if optional in hint:
                lint.text(hint[optional], hwhere, optional, required=False)
    return len(hints)


def check_quiz(lint: Lint, quiz, sentence_count: int, where: str) -> int:
    if not isinstance(quiz, list) or not quiz:
        lint.error(where, "quiz must be a non-empty array")
        return 0
    if len(quiz) > 3:
        lint.error(where, f"{len(quiz)} quiz items (max 3)")
    for index, item in enumerate(quiz, start=1):
        qwhere = f"{where}/quiz{index}"
        lint.text(item.get("question"), qwhere, "question")
        options = item.get("options")
        if not isinstance(options, list) or not 2 <= len(options) <= 4:
            lint.error(qwhere, "options must have 2–4 items")
            options = []
        for option in options:
            lint.text(option, qwhere, "option")
        answer = item.get("answer")
        if not isinstance(answer, int) or not 0 <= answer < len(options):
            lint.error(qwhere, "answer is out of range")
        sentence = item.get("sentence")
        if sentence is not None:
            if not isinstance(sentence, int) or not 1 <= sentence <= sentence_count:
                lint.error(qwhere, "sentence must be a 1-based unit sentence index")
        feedback = item.get("feedback")
        if not isinstance(feedback, dict):
            lint.error(qwhere, "feedback must be {zh, en?}")
        else:
            zh = lint.text(feedback.get("zh"), qwhere, "feedback.zh")
            if zh and hanzi_count(zh) > 90:
                lint.warn(qwhere, f"feedback.zh is {hanzi_count(zh)} hanzi (keep it to 1–2 sentences)")
            if not feedback.get("en"):
                lint.warn(qwhere, "feedback.en is missing (English UI falls back to generic copy)")
            elif feedback.get("en") is not None:
                lint.text(feedback["en"], qwhere, "feedback.en")
    return len(quiz)


def lint_chapter(number: int) -> Lint:
    lint = Lint()
    book = json.loads(BOOK.read_text(encoding="utf-8"))
    chapter = next((c for c in book["chapters"] if c["number"] == number), None)
    if chapter is None:
        lint.error(f"chapter {number}", "not found in book.json")
        return lint
    path = SRC_DIR / f"chapter-{number:02d}.json"
    if not path.is_file():
        lint.error(path.name, "file does not exist")
        return lint
    try:
        overlay = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        lint.error(path.name, f"invalid JSON: {error}")
        return lint
    if overlay.get("chapter") != number:
        lint.error(path.name, f"chapter field is {overlay.get('chapter')!r}, want {number}")
    units = overlay.get("units")
    if not isinstance(units, list) or not units:
        lint.error(path.name, "units must be a non-empty array")
        return lint
    expected_units = [u["number"] for u in chapter["units"]]
    got_units = [u.get("number") for u in units]
    if got_units != expected_units:
        lint.error(path.name, f"unit numbers {got_units} != {expected_units}")

    library = {p["id"]: p for p in json.loads(GRAMMAR.read_text(encoding="utf-8"))["points"]}
    by_number = {u["number"]: u for u in units}
    total_hints = 0
    total_quiz = 0
    total_sentences = 0
    hint_targets: set[str] = set()
    for unit in chapter["units"]:
        uwhere = f"{chapter['id']}/{unit['id']}"
        overlay_unit = by_number.get(unit["number"])
        if overlay_unit is None:
            continue
        check_title(lint, overlay_unit.get("title"), uwhere)
        first_of_book = number == 1 and unit["number"] == 1
        check_introduction(lint, overlay_unit.get("introduction"), uwhere, first_of_book)

        unit_sentences = [
            piece
            for paragraph in unit["paragraphs"]
            for piece in split_sentences(paragraph["text"])
        ]
        total_sentences += len(unit_sentences)
        paragraph_overlays = overlay_unit.get("paragraphs")
        if not isinstance(paragraph_overlays, dict):
            lint.error(uwhere, "paragraphs must be an object keyed by paragraph id")
            continue
        expected_keys = [p["id"] for p in unit["paragraphs"]]
        missing = [key for key in expected_keys if key not in paragraph_overlays]
        extra = [key for key in paragraph_overlays if key not in expected_keys]
        if missing:
            lint.error(uwhere, f"missing paragraphs: {', '.join(missing)}")
        if extra:
            lint.error(uwhere, f"unknown paragraph keys: {', '.join(extra)}")

        unit_hint_ids: set[str] = set()
        unit_text = " ".join(p["text"] for p in unit["paragraphs"])
        for paragraph in unit["paragraphs"]:
            pwhere = f"{uwhere}/{paragraph['id']}"
            source = paragraph_overlays.get(paragraph["id"])
            if source is None:
                continue
            pieces = split_sentences(paragraph["text"])
            translations = source.get("sentences")
            if not isinstance(translations, list):
                lint.error(pwhere, "sentences must be an array")
                translations = []
            if len(translations) != len(pieces):
                lint.error(
                    pwhere,
                    f"{len(translations)} sentence translations for {len(pieces)} split sentences",
                )
            for index, (piece, zh) in enumerate(zip(pieces, translations), start=1):
                swhere = f"{pwhere}/s{index}"
                if not isinstance(zh, str) or not zh.strip():
                    lint.error(swhere, "zh is empty")
                    continue
                if not hanzi_count(zh):
                    lint.error(swhere, "zh has no Chinese text")
                if zh.strip() == piece.strip():
                    lint.warn(swhere, "zh is identical to the English sentence")
                if HTML_TAG.search(zh):
                    lint.error(swhere, "zh contains HTML")
            analyses = source.get("analysis") or {}
            if not isinstance(analyses, dict):
                lint.error(pwhere, "analysis must be an object keyed by sentence number")
                analyses = {}
            for key, analysis in analyses.items():
                try:
                    sentence_number = int(key)
                except (TypeError, ValueError):
                    lint.error(pwhere, f"analysis key {key!r} is not a sentence number")
                    continue
                if not 1 <= sentence_number <= len(pieces):
                    lint.error(pwhere, f"analysis key {key} is outside 1..{len(pieces)}")
                    continue
                check_analysis(
                    lint, analysis, pieces[sentence_number - 1],
                    f"{pwhere}/analysis/{key}",
                )
            count = check_hints(
                lint, source.get("hints"), paragraph["text"], unit_sentences,
                unit_hint_ids, pwhere,
            )
            total_hints += count
            for hint in source.get("hints") or []:
                if isinstance(hint, dict) and isinstance(hint.get("target"), str):
                    hint_targets.add(hint["target"].lower())

        check_grammar(lint, overlay_unit.get("grammar"), unit_text, library, uwhere)
        total_quiz += check_quiz(
            lint, overlay_unit.get("quiz"), len(unit_sentences), uwhere,
        )

    # Names should carry their book translation somewhere in the chapter.
    glossary = {
        "jip": "吉普", "polynesia": "波利尼西亚", "dab-dab": "达布达布",
        "gub-gub": "咕咕", "too-too": "图图", "chee-chee": "奇奇",
        "bumpo": "邦波", "dolittle": "杜立德", "puddleby": "帕德尔比",
        "jolliginki": "乔利金基", "ermintrude": "埃明特鲁德", "ben ali": "本·阿里",
    }
    chapter_text = " ".join(
        p["text"] for u in chapter["units"] for p in u["paragraphs"]
    ).lower()
    for name, zh in glossary.items():
        if name in chapter_text and not any(name in target for target in hint_targets):
            lint.warn(chapter["id"], f"{name!r} appears but has no hint giving {zh}")

    print(
        f"{path.name}: {len(units)} units, {total_sentences} sentences, "
        f"{total_hints} hints, {total_quiz} quiz items"
    )
    return lint


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("chapter", type=int)
    parser.add_argument("--warnings", action="store_true", help="also print warnings")
    args = parser.parse_args()
    lint = lint_chapter(args.chapter)
    for warning in lint.warnings:
        if args.warnings:
            print(f"warning: {warning}")
    for error in lint.errors:
        print(f"error: {error}")
    if lint.errors:
        print(f"{len(lint.errors)} error(s), {len(lint.warnings)} warning(s)")
        sys.exit(1)
    print(f"ok ({len(lint.warnings)} warning(s))")


if __name__ == "__main__":
    main()
