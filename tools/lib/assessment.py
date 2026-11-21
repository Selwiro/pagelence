"""Placement-test bank helpers: load the editorial sources, build, validate.

The bank is authored as one JSON file per CEFR level under
`content-src/assessment/` and built into `content/assessment/bank.json`. The
build script and the content validator share this module so the shipped bank
and the sources are checked by the same rules.

Schema (see docs/content-format.md, section 7):
    { schemaVersion, updatedAt, source, license, passages[], questions[] }
    passage: { id, level, title, text }
    question: { id, level, skill, passageId?, prompt, options[4], answer,
                explanation: { zh, en } }
"""

from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[2]
SRC = Path("content-src") / "assessment"
OUT = Path("content") / "assessment" / "bank.json"

SCHEMA_VERSION = 2
LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]
# The bank has no A1: quick tests start at A2 (spec §3.5.1).
BANK_LEVELS = ["A2", "B1", "B2", "C1", "C2"]
SKILLS = {"vocab", "grammar", "reading"}
OPTION_COUNT = 4

SOURCE = (
    "Pagelence 项目编写：词汇、语法与十五篇原创短文；题目只用于应用内参考评级，"
    "不作认证。"
)
LICENSE = "CC BY-SA 4.0"


def load_sources(android: Path) -> tuple[list[dict], list[dict]]:
    """Merge every level file; return (passages, questions) sorted by id."""
    passages: list[dict] = []
    questions: list[dict] = []
    for path in sorted((android / SRC).glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        passages.extend(data.get("passages", []))
        questions.extend(data.get("questions", []))
    passages.sort(key=lambda p: p["id"])
    questions.sort(key=lambda q: q["id"])
    return passages, questions


def normalize_question(question: dict) -> dict:
    """Use `{{blank}}` for prompts and shuffle options deterministically by id."""
    out = dict(question)
    out["prompt"] = re.sub(r"_{2,}", "{{blank}}", str(out.get("prompt") or ""))
    options = list(out.get("options") or [])
    answer = out.get("answer")
    if options and isinstance(answer, int) and 0 <= answer < len(options):
        order = sorted(
            range(len(options)),
            key=lambda index: hashlib.sha256(
                f"{out.get('id')}:{index}".encode("utf-8")
            ).hexdigest(),
        )
        out["options"] = [options[index] for index in order]
        out["answer"] = order.index(answer)
    return out


def build_bank(android: Path) -> dict:
    passages, questions = load_sources(android)
    questions = [normalize_question(question) for question in questions]
    return {
        "schemaVersion": SCHEMA_VERSION,
        "updatedAt": "2026-11-21",
        "source": SOURCE,
        "license": LICENSE,
        "passages": passages,
        "questions": questions,
    }


def dump_bank(bank: dict) -> str:
    return json.dumps(bank, ensure_ascii=False, indent=2) + "\n"


def validate_bank(bank: dict) -> tuple[list[str], list[str]]:
    """Return (errors, warnings) for one bank document (spec §3.5)."""
    errors: list[str] = []
    warnings: list[str] = []
    if bank.get("schemaVersion") != SCHEMA_VERSION:
        errors.append(f"bank: schemaVersion is {bank.get('schemaVersion')!r}, want {SCHEMA_VERSION}")

    dictionary = _dictionary_entries()
    phrases = _phrase_targets()

    passage_ids: set[str] = set()
    passage_levels: dict[str, str] = {}
    passages_per_level: dict[str, int] = {level: 0 for level in LEVELS}
    for passage in bank.get("passages", []):
        pid = passage.get("id")
        where = f"bank/passage/{pid}"
        if not pid:
            errors.append(f"{where}: missing id")
            continue
        if pid in passage_ids:
            errors.append(f"{where}: duplicate id")
        passage_ids.add(pid)
        if not re.match(r"^passage-(a1|a2|b1|b2|c1|c2)-[a-z0-9-]+$", pid):
            errors.append(f"{where}: id must be passage-<level>-<slug>")
        level = passage.get("level")
        if level not in LEVELS:
            errors.append(f"{where}: bad level {level!r}")
        else:
            passages_per_level[level] += 1
            passage_levels[pid] = level
        for key in ("title", "text"):
            if not str(passage.get(key) or "").strip():
                errors.append(f"{where}: empty {key}")

    seen: set[str] = set()
    per_level: dict[str, int] = {level: 0 for level in LEVELS}
    per_level_direct: dict[str, int] = {level: 0 for level in LEVELS}
    per_level_reading: dict[str, int] = {level: 0 for level in LEVELS}
    skills: dict[str, int] = {skill: 0 for skill in SKILLS}
    points_by_level: dict[str, set[str]] = {level: set() for level in LEVELS}
    reading_per_passage: dict[str, int] = {}
    option_counts: dict[str, int] = {level: 0 for level in LEVELS}

    for question in bank.get("questions", []):
        qid = question.get("id")
        where = f"bank/question/{qid}"
        if not qid:
            errors.append(f"{where}: missing id")
            continue
        if qid in seen:
            errors.append(f"{where}: duplicate id")
        seen.add(qid)
        level = question.get("level")
        if level not in LEVELS:
            errors.append(f"{where}: bad level {level!r}")
        else:
            per_level[level] += 1
            if question.get("skill") != "reading":
                per_level_direct[level] += 1
            else:
                per_level_reading[level] += 1
        skill = question.get("skill")
        if skill not in SKILLS:
            errors.append(f"{where}: bad skill {skill!r}")
        else:
            skills[skill] += 1
        options = question.get("options")
        if not isinstance(options, list) or len(options) != OPTION_COUNT:
            errors.append(f"{where}: needs exactly {OPTION_COUNT} options")
            options = []
        elif len({str(option).strip() for option in options}) != len(options):
            errors.append(f"{where}: duplicate options")
        elif skill != "reading":
            option_counts[level] = option_counts.get(level, 0) + 1
            for option in options:
                ok, reason = _option_resolves(str(option), dictionary, phrases)
                if not ok:
                    errors.append(f"{where}: option {option!r}: {reason}")
                    break
            if skill == "vocab" and _shares_lemma(options, dictionary):
                errors.append(f"{where}: vocab options are inflections of one lemma; use a grammar item")
        answer = question.get("answer")
        if not isinstance(answer, int) or not 0 <= answer < max(len(options), 1):
            errors.append(f"{where}: answer index out of range")
        prompt = str(question.get("prompt") or "")
        if not prompt.strip():
            errors.append(f"{where}: empty prompt")
        if re.search(r"_{2,}", prompt):
            errors.append(f"{where}: prompt still uses ___; write {{{{blank}}}}")
        if skill == "reading":
            if "{{blank}}" in prompt:
                errors.append(f"{where}: reading prompt must not contain {{{{blank}}}}")
        elif prompt.count("{{blank}}") != 1:
            errors.append(f"{where}: non-reading prompt needs exactly one {{{{blank}}}}")
        if qid and not re.match(r"^q-(a1|a2|b1|b2|c1|c2)-[vgr]\d{2}$", qid):
            errors.append(f"{where}: id must be q-<level>-<v|g|r><nn>")
        explanation = question.get("explanation") or {}
        if not str(explanation.get("zh") or "").strip() or not str(explanation.get("en") or "").strip():
            errors.append(f"{where}: explanation needs zh and en")
        point = str(question.get("point") or "")
        if level in points_by_level:
            if not re.match(r"^(cefrj-\d+(?:-\d+)?|lex-[a-z][a-z'\-]*|read-[a-z0-9\-]+|focus-[a-z0-9\-]+)$", point):
                errors.append(f"{where}: bad point {point!r}")
            elif point in points_by_level[level]:
                errors.append(f"{where}: point {point!r} is used twice in {level}")
            else:
                points_by_level[level].add(point)
        passage_id = question.get("passageId")
        if skill == "reading":
            if not passage_id:
                errors.append(f"{where}: reading question needs a passageId")
            elif passage_id not in passage_ids:
                errors.append(f"{where}: unknown passageId {passage_id!r}")
            else:
                reading_per_passage[passage_id] = reading_per_passage.get(passage_id, 0) + 1
                if passage_levels.get(passage_id) != level:
                    errors.append(f"{where}: passage level does not match the question")
        elif passage_id:
            warnings.append(f"{where}: passageId on a non-reading question")

    if len(seen) != 210:
        errors.append(f"bank: {len(seen)} questions, want 210")
    for level in BANK_LEVELS:
        if per_level[level] != 42:
            errors.append(f"bank: {level} has {per_level[level]} questions, want 42")
        if per_level_direct[level] != 30:
            errors.append(f"bank: {level} has {per_level_direct[level]} non-reading questions, want 30")
        if per_level_reading[level] != 12:
            errors.append(f"bank: {level} has {per_level_reading[level]} reading questions, want 12")
        if passages_per_level[level] != 3:
            errors.append(f"bank: {level} has {passages_per_level[level]} passages, want 3")
    for passage_id, count in reading_per_passage.items():
        if count != 4:
            errors.append(f"bank/{passage_id}: {count} reading questions, want 4")
    if skills["reading"] != 60:
        errors.append(f"bank: only {skills['reading']} reading questions")

    positions = [0, 0, 0, 0]
    for question in bank.get("questions", []):
        answer = question.get("answer")
        if isinstance(answer, int) and 0 <= answer < 4:
            positions[answer] += 1
    total = max(1, sum(positions))
    shares = [count / total for count in positions]
    if any(share < 0.10 or share > 0.40 for share in shares):
        errors.append(
            "bank: answer positions are skewed: " + ", ".join(f"{share:.0%}" for share in shares)
        )

    return errors, warnings


def _dictionary_entries() -> dict[str, dict]:
    """The shipped ECDICT table, for the distractor check."""
    path = ANDROID / "content" / "third-party" / "dictionaries" / "ecdict.json"
    if not path.is_file():
        return {}
    return json.loads(path.read_text(encoding="utf-8")).get("entries") or {}


def _phrase_targets() -> set[str]:
    """Book hint targets double as the phrase table for the distractor check."""
    targets: set[str] = set()
    extra = ANDROID / "content-src" / "extra.json"
    if extra.is_file():
        targets.update(key.lower() for key in json.loads(extra.read_text(encoding="utf-8"))["entries"])
    for path in (ANDROID / "content-src").glob("*/**/*.json"):
        for match in re.finditer(r'"target":\s*"([^"]+)"', path.read_text(encoding="utf-8")):
            targets.add(match.group(1).lower())
    return targets


def _lemma_of(word: str, dictionary: dict[str, dict]) -> str:
    entry = dictionary.get(word) or {}
    return entry.get("lemma") or word


def _option_resolves(option: str, dictionary: dict[str, dict], phrases: set[str]) -> tuple[bool, str]:
    text = option.lower().strip()
    if not text:
        return False, "empty option"
    words = re.findall(r"[a-z][a-z'’\-]*", text)
    if not words:
        return False, "no word in option"
    for word in words:
        if word in dictionary or word in phrases or _lemma_of(word, dictionary) in dictionary:
            continue
        ok = False
        for suffix in ("est", "er", "ed", "ing", "s", "ly"):
            if word.endswith(suffix) and len(word) - len(suffix) >= 3:
                stem = word[: -len(suffix)]
                candidates = [stem, stem + "e"]
                if stem.endswith("i"):
                    candidates.append(stem[:-1] + "y")
                else:
                    candidates.append(stem + "y")
                if len(stem) > 2 and stem[-1] == stem[-2]:
                    candidates.append(stem[:-1])
                if any(candidate in dictionary or candidate in phrases for candidate in candidates):
                    ok = True
                    break
        if not ok:
            return False, f"'{word}' is not in the dictionary or phrase table"
    return True, ""


def _shares_lemma(options: list[str], dictionary: dict[str, dict]) -> bool:
    lemmas = []
    for option in options:
        words = re.findall(r"[a-z][a-z'’\-]*", option.lower())
        if len(words) != 1:
            return False
        lemmas.append(_lemma_of(words[0], dictionary))
    return len(lemmas) != len(set(lemmas))
