#!/usr/bin/env python3
"""Audit the built Doctor Dolittle book for content-quality smells.

Not a schema validator: `validate_content.py` covers structure. This script
looks for the mistakes that structural checks miss — untranslated or duplicated
sentences, quiz answers always at index 0, spoiler-ish introduction words,
hints that only repeat the dictionary, and grammar points used far too often.

Usage:
    python3 tools/author/audit_dolittle.py
"""

from __future__ import annotations

import collections
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "lib"))

from content import ANDROID  # noqa: E402

BOOK = ANDROID / "content" / "books" / "doctor-dolittle" / "book.json"
DICT = ANDROID / "content" / "third-party" / "dictionaries" / "ecdict.json"
GRAMMAR = ANDROID / "content" / "grammar" / "grammar-library.json"
HANZI = re.compile(r"[\u3400-\u9fff]")
WORD = re.compile(r"[A-Za-z]+")
SPOILER = re.compile(r"原来|结果|最后|终于|决定|答应|拒绝|发现|于是|就这样|从此")
ALT_NAMES = {
    "波莉": "Polynesia", "达布达布": "Dab-Dab", "达布": "Dab-Dab", "咕咕": "Gub-Gub",
    "奇奇": "Chee-Chee", "图图": "Too-Too", "吉普": "Jip", "邦波": "Bumpo",
    "波利尼西亚": "Polynesia", "杜立德": "Dolittle", "帕德尔比": "Puddleby",
}


def normalize(value: str) -> str:
    return re.sub(r"[\s　“”‘’\"'。，、！？；：…—\-（）()]", "", value or "")


def main() -> None:
    book = json.loads(BOOK.read_text(encoding="utf-8"))
    dictionary = json.loads(DICT.read_text(encoding="utf-8"))["entries"]
    library = {p["id"]: p for p in json.loads(GRAMMAR.read_text(encoding="utf-8"))["points"]}

    units = [u for c in book["chapters"] for u in c["units"]]
    sentences = [s for u in units for p in u["paragraphs"] for s in p.get("sentences") or []]
    missing_zh = [s["text"] for s in sentences if not (s.get("zh") or "").strip()]
    short = [
        (s["text"][:70], s.get("zh", ""))
        for s in sentences
        if len(WORD.findall(s["text"])) >= 8 and len(HANZI.findall(s.get("zh") or "")) < 4
    ]
    translated = collections.Counter(normalize(s.get("zh") or "") for s in sentences if s.get("zh"))
    duplicates = [(text[:70], count) for text, count in translated.items() if count >= 2]

    hints = [h for u in units for p in u["paragraphs"] for h in p.get("hints") or []]
    dict_duplicates = []
    for hint in hints:
        entry = dictionary.get(WordKey(hint["target"]))
        if not entry:
            continue
        senses = " ".join(" ".join(part) if isinstance(part, list) else str(part) for part in entry.get("senses") or [])
        target = normalize(hint.get("zh") or "")
        if target and target in normalize(senses):
            dict_duplicates.append((hint["id"], hint["target"], hint.get("zh", "")[:24]))
    hints_per_unit = [len(p.get("hints") or []) for u in units for p in u["paragraphs"]]

    quiz = [(u["id"], item) for u in units for item in u.get("quiz") or []]
    answers = collections.Counter(item.get("answer") for _, item in quiz)
    questions = collections.Counter(item.get("question") for _, item in quiz)
    repeated_questions = [(q[:80], n) for q, n in questions.items() if n >= 2]

    point_use = collections.Counter(
        (u.get("grammar") or {}).get("pointIds", [None])[0] for u in units if u.get("grammar")
    )
    unknown_points = [pid for pid in point_use if pid and pid not in library]
    long_focus = [
        (u["id"], len(HANZI.findall(u["grammar"]["focus"]["zh"])))
        for u in units
        if u.get("grammar") and len(HANZI.findall((u["grammar"].get("focus") or {}).get("zh", ""))) > 60
    ]

    intros = [u.get("introduction", {}).get("zh", "") for u in units]
    intro_flags = [(u["id"], intro) for u, intro in zip(units, intros) if SPOILER.search(intro)]
    intro_short = [(u["id"], len(HANZI.findall(intro))) for u, intro in zip(units, intros) if len(HANZI.findall(intro)) < 40]
    intro_long = [(u["id"], len(HANZI.findall(intro))) for u, intro in zip(units, intros) if len(HANZI.findall(intro)) > 90]

    name_counts = collections.Counter()
    for intro in intros:
        for name in ALT_NAMES:
            name_counts[name] += intro.count(name)

    report = {
        "units": len(units),
        "sentences": len(sentences),
        "paragraphs": sum(len(u["paragraphs"]) for u in units),
        "hints": len(hints),
        "hintsPerParagraph": {
            "min": min(hints_per_unit) if hints_per_unit else 0,
            "max": max(hints_per_unit) if hints_per_unit else 0,
            "average": round(sum(hints_per_unit) / len(hints_per_unit), 2) if hints_per_unit else 0,
        },
        "missingTranslations": len(missing_zh),
        "shortTranslations": short[:10],
        "duplicateTranslations": duplicates[:10],
        "dictionaryOverlap": dict_duplicates[:20],
        "dictionaryOverlapCount": len(dict_duplicates),
        "quizItems": len(quiz),
        "answerDistribution": dict(answers),
        "repeatedQuestions": repeated_questions,
        "grammarPoints": point_use.most_common(8),
        "unknownGrammarPoints": unknown_points,
        "focusOver60": long_focus,
        "introSpoilerWords": intro_flags[:10],
        "introShort": intro_short[:10],
        "introLong": intro_long[:10],
        "nameMentionsInIntro": dict(name_counts),
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if missing_zh:
        sys.exit(1)


def WordKey(target: str) -> str:
    target = target.lower().replace("’", "'")
    return target if " " not in target else target


if __name__ == "__main__":
    main()
