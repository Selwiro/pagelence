#!/usr/bin/env python3
"""Build the shipped CEFR word-level table in five layers (App content spec §3.4.4).

Layers, each filling only empty slots and never overwriting an earlier one:

1. `list`      CEFR-J Wordlist 1.6 + Octanove C1/C2 (archived Web table), plus
               the common contractions (`don't`, `I'm`, …) whose level follows
               the auxiliary or lexical verb they shorten.
2. `aligned`   CEFR-Annotated WordNet (CC BY 4.0 + WordNet 3.0 licence): a
               lemma takes the lowest level of its senses, mapped by WordNet
               part of speech.
3. `derived`   suffix stripping: a derived word inherits its root's level, with
               no level shift (`happiness` → `happy`). Only one hop, and the
               root must reconstruct the word exactly.
4. `tag`       ECDICT exam tags: gk → B1, cet4 → B1, cet6/ky → B2,
               toefl/ielts → C1, gre → C2; several tags take the lowest.
5. `freq`      wordfreq 3.0.2 zipf: per-part-of-speech cut points fitted on the
               `list` words only, by dynamic programming, monotonic by level.
               `zipf = 0` (wordfreq does not know the word) is never graded.

Outputs:

    content/third-party/word-levels.json          schemaVersion 2 (+ sources)
    content/reports/word-levels-coverage.json     coverage, distribution, gaps
    content/reports/levels-audit.json             210-word audit sample (seed 42)

The script needs `wordfreq` 3.0.2 (`python3 -m pip install wordfreq==3.0.2`).
`--check` compares every generated file with what is on disk.

Usage:
    python3 tools/build_word_levels.py
    python3 tools/build_word_levels.py --check
"""

from __future__ import annotations

import argparse
import json
import random
import os
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[1]
REPO = ANDROID


def external_input(env: str) -> "Path | None":
    """构建期需要、但不随本仓库分发的输入；用环境变量指到本地副本。"""
    value = os.environ.get(env)
    if not value:
        return None
    path = Path(value)
    if not path.is_file():
        raise SystemExit(f"{env} 指向的文件不存在：{path}")
    return path


#: CEFR-J + Octanove 的 `list` 层基线，取自封存的 Web 归档。
WEB_LIST = external_input("PAGELENCE_WEB_WORD_LEVELS")
#: CEFR-Annotated WordNet 的 sensekey 标注表，只作构建输入，不随包分发。
CEFR_WORDNET = external_input("PAGELENCE_CEFR_WORDNET_TSV")


def require(path: "Path | None", env: str, what: str) -> Path:
    if path is None:
        raise SystemExit(
            f"重建这份表需要{what}；用环境变量 {env} 指向该文件后再运行。"
            "已生成的结果随仓库分发，普通构建与校验不需要这两个文件。"
        )
    return path


ECDICT = ANDROID / "content" / "third-party" / "dictionaries" / "ecdict.json"
BOOKS = ANDROID / "content" / "books"
OUT_LEVELS = ANDROID / "content" / "third-party" / "word-levels.json"
OUT_REPORT = ANDROID / "content" / "reports" / "word-levels-coverage.json"
OUT_AUDIT = ANDROID / "content" / "reports" / "levels-audit.json"

LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]
RANK = {level: index for index, level in enumerate(LEVELS)}
SOURCES = ("list", "aligned", "derived", "tag", "freq")
LEXICAL_POS = [
    "noun",
    "verb",
    "adjective",
    "adverb",
    "pronoun",
    "preposition",
    "determiner",
    "conjunction",
    "number",
    "modal auxiliary",
    "be-verb",
    "do-verb",
    "have-verb",
    "interjection",
]

# `don't` is not in the CEFR-J wordlist, but it is one of the most frequent
# words in both books. The contraction takes the level of the word it shortens.
CONTRACTIONS = {
    "don't": "do",
    "doesn't": "do",
    "didn't": "do",
    "isn't": "be",
    "aren't": "be",
    "wasn't": "be",
    "weren't": "be",
    "i'm": "be",
    "i've": "have",
    "i'll": "will",
    "i'd": "would",
    "you're": "be",
    "you've": "have",
    "you'll": "will",
    "you'd": "would",
    "he's": "be",
    "she's": "be",
    "it's": "be",
    "we're": "be",
    "we've": "have",
    "we'll": "will",
    "we'd": "would",
    "they're": "be",
    "they've": "have",
    "they'll": "will",
    "they'd": "would",
    "can't": "can",
    "couldn't": "can",
    "won't": "will",
    "wouldn't": "would",
    "shouldn't": "should",
    "mustn't": "must",
    "haven't": "have",
    "hasn't": "have",
    "hadn't": "have",
    "let's": "let",
    "that's": "be",
    "there's": "be",
    "what's": "be",
    "who's": "be",
    "here's": "be",
}

SUFFIX_POS = {
    "ly": "adverb",
    "ward": "adverb",
    "ness": "noun",
    "ment": "noun",
    "ation": "noun",
    "tion": "noun",
    "sion": "noun",
    "ity": "noun",
    "ism": "noun",
    "ist": "noun",
    "er": "noun",
    "or": "noun",
    "less": "adjective",
    "ful": "adjective",
    "able": "adjective",
    "ible": "adjective",
    "al": "adjective",
    "ish": "adjective",
    "ive": "adjective",
    "ous": "adjective",
    "ic": "adjective",
    "ize": "verb",
    "en": "verb",
}

# CEFR-Annotated WordNet sense types: 1 noun, 2 verb, 3 adjective, 4 adverb,
# 5 adjective satellite.
WORDNET_POS = {"1": "noun", "2": "verb", "3": "adjective", "4": "adverb", "5": "adjective"}

TAG_LEVEL = {
    "gk": "B1",
    "cet4": "B1",
    "cet6": "B2",
    "ky": "B2",
    "toefl": "C1",
    "ielts": "C1",
    "gre": "C2",
}

# ECDICT part-of-speech abbreviations → the table's part-of-speech keys.
ECDICT_POS = {
    "n": "noun",
    "v": "verb",
    "vt": "verb",
    "vi": "verb",
    "adj": "adjective",
    "a": "adjective",
    "adv": "adverb",
    "ad": "adverb",
    "prep": "preposition",
    "pron": "pronoun",
    "conj": "conjunction",
    "num": "number",
    "int": "interjection",
    "aux": "modal auxiliary",
    "art": "determiner",
}

WORD = re.compile(r"[a-z][a-z'’\-]*")
SINGLE_WORD = re.compile(r"^[a-z][a-z'\-]*$")


def normalize(text: str) -> str:
    return text.strip().lower().replace("’", "'")


def load_levels() -> dict[str, dict[str, str]]:
    source = require(WEB_LIST, "PAGELENCE_WEB_WORD_LEVELS", "封存 Web 归档的 word-levels.json 作 `list` 基线")
    data = json.loads(source.read_text(encoding="utf-8"))
    levels: dict[str, dict[str, str]] = {}
    for word, poses in data["levels"].items():
        key = normalize(word)
        if not key:
            continue
        cleaned = {pos: level for pos, level in poses.items() if level in RANK}
        if cleaned:
            levels.setdefault(key, {}).update(cleaned)
    return levels


def load_dictionary() -> dict[str, dict]:
    data = json.loads(ECDICT.read_text(encoding="utf-8"))
    return data["entries"]


PROPER_ZH = (
    "人名", "地名", "男子名", "女子名", "姓氏", "[人名]", "[地名]",
    "的异体", "(f.)", "(m.)", "（名）", "（姓）",
)
ABBREV_POS = ("abbr", "aux.", "pl.", "comb", "comb.")
PROPER_EN = re.compile(
    r"^(a (?:city|town|village|river|county|state|mountain|lake|island|country|region|port|"
    r"provincial capital|capital) (?:in|of|on)|the (?:provincial )?capital|capital of|a former|"
    r"united states|an? (?:American|English|French|German|Greek|Roman|Norse) "
    r"(?:poet|writer|playwright|novelist|statesman|philosopher|king|emperor|colonist|film))"
)
PROPER_ZH_PLACE = re.compile(r"[（(][^）)]*(?:城市|首都|省名|州名|国名|古罗马|公元前|（缩）)[^）)]*[）)]")


def is_proper_or_abbrev(word: str, entry: dict) -> bool:
    """Names, abbreviations and one-letter tokens never get a CEFR level."""
    if len(word) <= 2:
        return True
    if not entry.get("senses"):
        return True  # no Chinese sense: a name, a code or undigested noise
    for sense in entry.get("senses") or []:
        pos = (sense[0] or "").strip().lower().rstrip(".")
        if pos.startswith("abbr") or pos in ABBREV_POS:
            return True
        text = sense[1] or ""
        if any(marker in text for marker in PROPER_ZH):
            return True
        if PROPER_ZH_PLACE.search(text):
            return True
    for sense in entry.get("en") or []:
        if PROPER_EN.match((sense[1] or "").strip().lower()):
            return True
    return False


def base_pos(entry: dict) -> str | None:
    """The first sense's part of speech, used by the tag layer."""
    for sense in entry.get("senses") or []:
        pos = (sense[0] or "").strip().rstrip(".")
        if pos in ECDICT_POS:
            return ECDICT_POS[pos]
    return None


def add_contractions(levels: dict[str, dict[str, str]], sources: dict[str, dict[str, str]]) -> int:
    added = 0
    for contraction, base in CONTRACTIONS.items():
        if contraction in levels:
            continue
        base_levels = levels.get(base)
        if not base_levels:
            continue
        target_pos = base_pos_for_contraction(base, base_levels)
        level = min(base_levels.values(), key=lambda item: RANK[item])
        levels[contraction] = {target_pos: level}
        sources[contraction] = {target_pos: "list"}
        added += 1
    return added


def base_pos_for_contraction(base: str, base_levels: dict[str, str]) -> str:
    for candidate in ("be-verb", "have-verb", "do-verb", "modal auxiliary", "verb"):
        if candidate in base_levels:
            return candidate
    return next(iter(base_levels))


def add_aligned(
    levels: dict[str, dict[str, str]], sources: dict[str, dict[str, str]]
) -> dict[str, int]:
    added = 0
    per_pos: Counter = Counter()
    source = require(CEFR_WORDNET, "PAGELENCE_CEFR_WORDNET_TSV", "CEFR-Annotated WordNet 的标注表")
    for line in source.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        key, _, level = line.partition("\t")
        level = level.strip()
        if level not in RANK:
            continue
        lemma, _, rest = key.partition("%")
        raw = lemma.replace("_", " ")
        if raw[:1].isupper():
            continue  # WordNet capitalises proper nouns; they get no level.
        lemma = normalize(raw)
        if " " in lemma or not SINGLE_WORD.fullmatch(lemma):
            continue
        sense_type = rest[:1]
        pos = WORDNET_POS.get(sense_type)
        if pos is None:
            continue
        slot = levels.setdefault(lemma, {})
        if pos in slot:
            continue
        slot[pos] = level
        sources.setdefault(lemma, {})[pos] = "aligned"
        added += 1
        per_pos[pos] += 1
    return {"slots": added, **per_pos}


def strip_suffix(word: str, suffix: str) -> str | None:
    stem = word[: -len(suffix)]
    if len(stem) < 4:
        return None
    candidates = [stem]
    if stem.endswith("i"):
        candidates.append(stem[:-1] + "y")
    candidates.append(stem + "e")
    for base in candidates:
        if not SINGLE_WORD.fullmatch(base):
            continue
        if (
            base + suffix == word
            or (base.endswith("e") and base[:-1] + suffix == word)
            or (base.endswith("y") and base[:-1] + "i" + suffix == word)
        ):
            return base
    return None


def add_derived(
    levels: dict[str, dict[str, str]],
    sources: dict[str, dict[str, str]],
    dictionary: dict[str, dict],
) -> int:
    added = 0
    for word in sorted(dictionary):
        if word in levels:
            continue
        if not SINGLE_WORD.fullmatch(word) or len(word) < 6:
            continue
        for suffix, pos in SUFFIX_POS.items():
            if not word.endswith(suffix):
                continue
            base = strip_suffix(word, suffix)
            if base is None or base not in dictionary:
                continue
            base_levels = levels.get(base)
            if not base_levels:
                continue
            level = min(base_levels.values(), key=lambda item: RANK[item])
            levels[word] = {pos: level}
            sources.setdefault(word, {})[pos] = "derived"
            added += 1
            break
    return added


def add_tags(
    levels: dict[str, dict[str, str]],
    sources: dict[str, dict[str, str]],
    dictionary: dict[str, dict],
) -> int:
    added = 0
    for word in sorted(dictionary):
        entry = dictionary[word]
        if word in levels or entry.get("kind"):
            continue
        if not SINGLE_WORD.fullmatch(word) or is_proper_or_abbrev(word, entry):
            continue
        tags = [tag for tag in (entry.get("tag") or "").split() if tag in TAG_LEVEL]
        if not tags:
            continue
        level = min((TAG_LEVEL[tag] for tag in tags), key=lambda item: RANK[item])
        pos = base_pos(entry)
        if pos is None:
            continue
        levels[word] = {pos: level}
        sources.setdefault(word, {})[pos] = "tag"
        added += 1
    return added


def fit_cutoffs(pairs: list[tuple[float, str]]) -> list[float]:
    """Fit five monotonic zipf cut points by dynamic programming.

    Zipf values are quantized to 0.1 bins. Frequency rises with zipf, so bins
    from low to high map to C2 … A1: a rare word is graded higher. The cost of
    a bin range is the number of training words whose level it mislabels.
    """
    bins = 81
    ordered = list(reversed(LEVELS))
    totals = [[0] * len(ordered) for _ in range(bins)]
    for zipf, level in pairs:
        index = min(bins - 1, max(0, int(zipf * 10)))
        totals[index][RANK[level]] += 1
    # cumulative counts: prefix[b][k] = words in bins [0, b) at level k
    prefix = [[0] * len(ordered) for _ in range(bins + 1)]
    for b in range(bins):
        for k in range(len(ordered)):
            prefix[b + 1][k] = prefix[b][k] + totals[b][k]

    def cost(left: int, right: int, level: str) -> int:
        k = RANK[level]
        total = prefix[right][k] - prefix[left][k]
        everything = sum(
            prefix[right][j] - prefix[left][j] for j in range(len(ordered))
        )
        return everything - total

    infinity = float("inf")
    dp = [[infinity] * (bins + 1) for _ in range(len(ordered))]
    back = [[0] * (bins + 1) for _ in range(len(ordered))]
    for b in range(1, bins + 1):
        dp[0][b] = cost(0, b, ordered[0])
    for k in range(1, len(ordered)):
        for b in range(k + 1, bins + 1):
            best = infinity
            best_left = k
            for left in range(k, b):
                value = dp[k - 1][left] + cost(left, b, ordered[k])
                if value < best:
                    best = value
                    best_left = left
            dp[k][b] = best
            back[k][b] = best_left
    cutoffs: list[float] = []
    b = bins
    for k in range(len(ordered) - 1, 0, -1):
        left = back[k][b]
        cutoffs.append(left / 10.0)
        b = left
    cutoffs.reverse()
    return cutoffs


def predict(level_cutoffs: list[float], zipf: float) -> str:
    """Map a zipf value to a level: low zipf (rare) is high, high is A1."""
    ordered = list(reversed(LEVELS))
    index = 0
    for cutoff in level_cutoffs:
        if zipf >= cutoff:
            index += 1
        else:
            break
    return ordered[min(index, len(ordered) - 1)]


def add_frequency(
    levels: dict[str, dict[str, str]],
    sources: dict[str, dict[str, str]],
    dictionary: dict[str, dict],
) -> tuple[int, dict[str, list[float]]]:
    try:
        from wordfreq import zipf_frequency
    except ImportError:  # pragma: no cover - build environment check
        raise SystemExit(
            "wordfreq is required: python3 -m pip install wordfreq==3.0.2"
        )

    # Train only on pure `list` slots; never on values this layer predicts.
    training: dict[str, list[tuple[float, str]]] = defaultdict(list)
    all_training: list[tuple[float, str]] = []
    for word, poses in levels.items():
        zipf = zipf_frequency(word, "en")
        if zipf <= 0:
            continue
        for pos, level in poses.items():
            if sources.get(word, {}).get(pos) != "list":
                continue
            training[pos if pos in LEXICAL_POS else "other"].append((zipf, level))
            all_training.append((zipf, level))

    cutoffs: dict[str, list[float]] = {}
    global_cutoffs = fit_cutoffs(all_training)
    for pos, pairs in training.items():
        cutoffs[pos] = fit_cutoffs(pairs) if len(pairs) >= 200 else global_cutoffs
    cutoffs["other"] = global_cutoffs

    added = 0
    for word in sorted(dictionary):
        entry = dictionary[word]
        if not SINGLE_WORD.fullmatch(word) or entry.get("kind"):
            continue
        if is_proper_or_abbrev(word, entry):
            continue
        zipf = zipf_frequency(word, "en")
        if zipf <= 0:
            continue
        pos = base_pos(entry) or "other"
        slot = levels.setdefault(word, {})
        if pos in slot:
            continue
        level = predict(cutoffs.get(pos, global_cutoffs), zipf)
        slot[pos] = level
        sources.setdefault(word, {})[pos] = "freq"
        added += 1
    return added, cutoffs


def book_tokens(book: dict) -> list[str]:
    tokens: list[str] = []
    for chapter in book.get("chapters") or []:
        for unit in chapter.get("units") or []:
            for paragraph in unit.get("paragraphs") or []:
                tokens.extend(WORD.findall(normalize(paragraph.get("text") or "")))
    return tokens


def book_coverage(dictionary: dict[str, dict], levels: dict[str, dict[str, str]]) -> list[dict]:
    rows = []
    for path in sorted(BOOKS.glob("*/book.json")):
        book = json.loads(path.read_text(encoding="utf-8"))
        tokens = book_tokens(book)
        covered = 0
        for token in tokens:
            if token in levels:
                covered += 1
                continue
            lemma = (dictionary.get(token) or {}).get("lemma")
            if lemma and lemma in levels:
                covered += 1
        rows.append(
            {
                "id": book["id"],
                "tokens": len(tokens),
                "withLevel": covered,
                "share": round(covered / len(tokens), 4) if tokens else 0.0,
            }
        )
    return rows


def audit_sample(
    levels: dict[str, dict[str, str]],
    sources: dict[str, dict[str, str]],
    dictionary: dict[str, dict],
) -> list[dict]:
    by_level: dict[str, list[tuple[str, str]]] = defaultdict(list)
    for word, poses in levels.items():
        entry = dictionary.get(word)
        if entry is None or entry.get("lemma") or entry.get("kind"):
            continue
        if not SINGLE_WORD.fullmatch(word) or len(word) < 3 or "'" in word:
            continue
        if not (entry.get("senses") or entry.get("en")):
            continue
        if is_proper_or_abbrev(word, entry):
            continue
        level = min(poses.values(), key=lambda item: RANK[item])
        origin = sources.get(word, {}).get(
            next(pos for pos, value in poses.items() if value == level), "list"
        )
        by_level[level].append((word, origin))
    samples = []
    rng = random.Random(42)
    for level in LEVELS:
        pool = sorted(by_level.get(level, []))
        picked = rng.sample(pool, min(35, len(pool)))
        for word, origin in picked:
            samples.append({"word": word, "level": level, "source": origin, "verdict": None})
    return samples


def merge_audit(samples: list[dict]) -> dict:
    """Keep earlier verdicts/reasons when the sample is regenerated."""
    previous: dict[str, dict] = {}
    if OUT_AUDIT.is_file():
        try:
            old = json.loads(OUT_AUDIT.read_text(encoding="utf-8"))
            for row in old.get("samples", []):
                if row.get("word"):
                    previous[row["word"]] = row
        except json.JSONDecodeError:
            previous = {}
    for row in samples:
        old = previous.get(row["word"])
        if old and old.get("level") == row["level"]:
            row["verdict"] = old.get("verdict")
            if old.get("reason"):
                row["reason"] = old["reason"]
    counts = Counter(row.get("verdict") for row in samples if row.get("verdict"))
    judged = sum(counts.values())
    levels = {}
    for level in LEVELS:
        rows = [row for row in samples if row["level"] == level and row.get("verdict")]
        if rows:
            off = sum(1 for row in rows if row["verdict"] == "离谱")
            levels[level] = {"judged": len(rows), "off": off, "rate": round(off / len(rows), 4)}
    return {
        "schemaVersion": 1,
        "generatedAt": "2026-11-21",
        "method": "按预测级分层，每级 35 条，种子 42；评审只看词 + 预测级，不告诉来源。",
        "seed": 42,
        "samples": samples,
        "summary": {
            "sampled": len(samples),
            "judged": judged,
            "reasonable": counts.get("合理", 0),
            "close": counts.get("偏差", 0),
            "off": counts.get("离谱", 0),
            "unsure": counts.get("存疑", 0),
            "offRate": round(counts.get("离谱", 0) / judged, 4) if judged else None,
            "byLevel": levels,
        },
    }


def build() -> dict[Path, str]:
    levels = load_levels()
    sources: dict[str, dict[str, str]] = {
        word: {pos: "list" for pos in poses} for word, poses in levels.items()
    }
    dictionary = load_dictionary()
    original_forms = [
        word
        for word, entry in dictionary.items()
        if not entry.get("lemma") and not entry.get("kind")
    ]

    contractions = add_contractions(levels, sources)
    aligned = add_aligned(levels, sources)
    derived = add_derived(levels, sources, dictionary)
    tagged = add_tags(levels, sources, dictionary)
    frequency, cutoffs = add_frequency(levels, sources, dictionary)

    # Counts are over slots (word × part of speech), so every layer is visible.
    layer_slots: Counter = Counter()
    for word, poses in sources.items():
        for pos in poses:
            layer_slots[sources[word][pos]] += 1
    covered_forms = sum(1 for word in original_forms if levels.get(word))
    uncovered = [word for word in original_forms if not levels.get(word)]
    level_distribution: Counter = Counter()
    for word, poses in levels.items():
        if word not in dictionary:
            continue
        if not poses:
            continue
        level_distribution[min(poses.values(), key=lambda item: RANK[item])] += 1

    # Sanity: every `list` slot survives untouched, and no level loses words
    # against the curated list (spec §3.4.4 acceptance 2–3).
    baseline = json.loads(
        require(WEB_LIST, "PAGELENCE_WEB_WORD_LEVELS", "封存 Web 归档的 word-levels.json 作核对基线")
        .read_text(encoding="utf-8")
    )["levels"]
    baseline_counts: Counter = Counter()
    for word, poses in baseline.items():
        key = normalize(word)
        if key in dictionary and poses:
            baseline_counts[min(poses.values(), key=lambda item: RANK.get(item, 99))] += 1
    sanity = {
        "listSlotsPreserved": all(
            sources.get(word, {}).get(pos) == "list"
            for word, poses in baseline.items()
            for pos in poses
            if normalize(word) in levels
        ),
        "distributionAtLeastList": {
            level: {
                "list": baseline_counts.get(level, 0),
                "now": level_distribution.get(level, 0),
                "ok": level_distribution.get(level, 0) >= baseline_counts.get(level, 0),
            }
            for level in LEVELS
        },
        "allLevelsAtLeastList": all(
            level_distribution.get(level, 0) >= baseline_counts.get(level, 0) for level in LEVELS
        ),
    }
    report = {
        "schemaVersion": 1,
        "generatedAt": "2026-11-21",
        "source": (
            "CEFR-J Wordlist 1.6 + Octanove C1/C2 (list); CEFR-Annotated WordNet "
            "(aligned); suffix inheritance (derived); ECDICT exam tags (tag); "
            "wordfreq 3.0.2 zipf (freq)"
        ),
        "dictionary": {
            "entries": len(dictionary),
            "originalForms": len(original_forms),
            "inflectedForms": sum(1 for entry in dictionary.values() if entry.get("lemma")),
            "properNouns": sum(1 for entry in dictionary.values() if entry.get("kind")),
        },
        "layers": {
            name: {"slots": layer_slots.get(name, 0)} for name in SOURCES
        }
        | {"contractions": contractions, "cutoffs": {key: [round(x, 2) for x in value] for key, value in cutoffs.items()}},
        "coverage": {
            "originalFormsWithLevel": covered_forms,
            "originalFormsShare": round(covered_forms / len(original_forms), 4)
            if original_forms
            else 0.0,
            "uncovered": len(uncovered),
        },
        "distribution": {level: level_distribution.get(level, 0) for level in LEVELS},
        "books": book_coverage(dictionary, levels),
        "uncoveredSamples": [
            {"word": word, "reason": "wordfreq=0 或无可还原词根"} for word in uncovered[:50]
        ],
        "sanity": sanity,
    }

    payload = {
        "schemaVersion": 2,
        "source": report["source"],
        "license": (
            "CEFR-J Wordlist v1.6（研究/教育/商用免费）；Octanove C1/C2 与 "
            "CEFR-Annotated WordNet 为 CC BY-SA 4.0 / CC BY 4.0；ECDICT MIT；"
            "wordfreq 3.0.2 CC BY-SA 4.0"
        ),
        "levels": {
            word: poses
            for word, poses in sorted(levels.items())
            if poses
        },
        "sources": {
            word: poses
            for word, poses in sorted(sources.items())
            if poses
        },
    }
    return {
        OUT_LEVELS: json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n",
        OUT_REPORT: json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        OUT_AUDIT: json.dumps(merge_audit(audit_sample(levels, sources, dictionary)), ensure_ascii=False, indent=2) + "\n",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify generated files are current")
    args = parser.parse_args()

    outputs = build()
    stale = []
    for path, text in outputs.items():
        current = path.read_text(encoding="utf-8") if path.is_file() else None
        if current != text:
            stale.append(path)
            if not args.check:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text, encoding="utf-8")
    payload = json.loads(outputs[OUT_LEVELS])
    report = json.loads(outputs[OUT_REPORT])
    if args.check:
        if stale:
            print(
                "stale: "
                + ", ".join(str(path.relative_to(REPO)) for path in stale)
                + "; rerun tools/build_word_levels.py"
            )
            sys.exit(1)
        print("word levels ok")
        return
    print(
        f"wrote word-levels.json: {len(payload['levels'])} words, "
        f"{sum(len(poses) for poses in payload['levels'].values())} slots; "
        f"original forms covered {report['coverage']['originalFormsShare']:.1%}; "
        + ", ".join(f"{level} {count}" for level, count in report["distribution"].items())
    )


if __name__ == "__main__":
    main()
