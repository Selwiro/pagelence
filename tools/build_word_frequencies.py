#!/usr/bin/env python3
"""Build `content/third-party/word-frequencies.json` with wordfreq 3.0.2.

The table is a single app-wide lemma → zipf map, not a per-book table: book
occurrence counts and per-paragraph frequencies are now derived at runtime by
scanning the book once (App content spec §3.4.3 #1 and #3).

Zipf values are kept to one decimal. The four display bands are cut at the
90th / 70th / 40th percentiles of the shipped table, so the wording is
「高频 / 较常见 / 一般 / 少见」 regardless of the source.

Requires `wordfreq==3.0.2` (`python3 -m pip install wordfreq==3.0.2`).

Usage:
    python3 tools/build_word_frequencies.py
    python3 tools/build_word_frequencies.py --check
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[1]
REPO = ANDROID
ECDICT = ANDROID / "content" / "third-party" / "dictionaries" / "ecdict.json"
OUT = ANDROID / "content" / "third-party" / "word-frequencies.json"

SOURCE = "wordfreq 3.0.2, Robyn Speer, CC BY-SA 4.0, https://github.com/rspeer/wordfreq"
LICENSE = "CC BY-SA 4.0（wordfreq 3.0.2 数据，摘录沿用同一协议）"


def percentile(sorted_values: list[float], share: float) -> float:
    """Nearest-rank percentile; `share` is the fraction of values below it."""
    index = min(len(sorted_values) - 1, int(round(share * (len(sorted_values) - 1))))
    return sorted_values[index]


def build() -> str:
    try:
        from wordfreq import zipf_frequency
    except ImportError:  # pragma: no cover - build environment check
        raise SystemExit("wordfreq is required: python3 -m pip install wordfreq==3.0.2")

    entries = json.loads(ECDICT.read_text(encoding="utf-8"))["entries"]
    values: dict[str, float] = {}
    for word in sorted(entries):
        if not word or " " in word:
            continue
        zipf = round(zipf_frequency(word, "en"), 1)
        if zipf <= 0:
            continue
        values[word] = zipf

    ranked = sorted(values.values())
    bands = {
        "high": percentile(ranked, 0.90),
        "common": percentile(ranked, 0.70),
        "normal": percentile(ranked, 0.40),
    }
    payload = {
        "schemaVersion": 1,
        "source": SOURCE,
        "license": LICENSE,
        "bands": bands,
        "entries": dict(sorted(values.items())),
    }
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify generated file is current")
    args = parser.parse_args()

    text = build()
    current = OUT.read_text(encoding="utf-8") if OUT.is_file() else None
    if args.check:
        if current != text:
            print(
                f"{OUT.relative_to(REPO)} is stale; rerun tools/build_word_frequencies.py"
            )
            sys.exit(1)
        print("word frequencies ok")
        return
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text, encoding="utf-8")
    payload = json.loads(text)
    print(
        f"wrote {OUT.relative_to(REPO)}: {len(payload['entries'])} words, "
        f"bands {payload['bands']}, "
        f"{len(text.encode('utf-8')) / 1048576:.1f} MiB"
    )


if __name__ == "__main__":
    main()
