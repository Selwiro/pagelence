#!/usr/bin/env python3
"""Build the compact read-only lexicon index shipped with the app.

The app used to parse three generated JSON tables on the first word tap:

    third-party/dictionaries/ecdict.json   8.3 MB, 38,229 entries
    third-party/word-levels.json           1.9 MB, 33,210 words
    third-party/word-frequencies.json      0.6 MB, 37,897 words

Parsing them into `Map<String, DictionaryEntry>` and nested level maps cost
about 95 MB of heap on a device (80 MB → 180 MB PSS), and the graph was kept
for the whole process. This script turns the same data into one binary index
whose entries stay as JSON text: the app reads the file once (about 10 MB),
binary-searches the key table in place and decodes only the tapped entry.

    python3 tools/build_lexicon.py           # write content/third-party/lexicon.bin
    python3 tools/build_lexicon.py --check   # verify the committed file is current

Format (`lexicon.bin`, little endian):

    magic           4 bytes  "PLX1"
    version         int32    1
    sectionCount    int32    3
    per section     5 × int32 (kind, count, keysLen, valuesLen, blobLen)
    sections        keys, values, blob (in header order)

    kind 1 dictionary: key = term, value = (blobOffset, blobLength) of the
        entry JSON object (`phonetic`, `senses`, `en`, `lemma`, `form`, `kind`,
        `note`; the unused ECDICT `tag` column is dropped).
    kind 2 levels: key = word, value = (blobOffset, blobLength) of
        `{"l":{pos:level},"s":{pos:source}}`.
    kind 3 frequencies: key = term, value = float64 zipf; the frequency band
        cut points live in the blob as three float64 (high, common, normal).

Keys are stored as `int32 length` + UTF-8 bytes, sorted by encoded bytes.
All current keys are ASCII, so the Kotlin side compares bytes directly; the
build asserts that and fails loudly if a future table grows a non-ASCII key.
"""

from __future__ import annotations

import argparse
import json
import struct
import sys
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[1]
CONTENT = ANDROID / "content"
DICT = CONTENT / "third-party" / "dictionaries" / "ecdict.json"
LEVELS = CONTENT / "third-party" / "word-levels.json"
FREQUENCIES = CONTENT / "third-party" / "word-frequencies.json"
OUTPUT = CONTENT / "third-party" / "lexicon.bin"

MAGIC = b"PLX1"
VERSION = 1
KIND_DICTIONARY = 1
KIND_LEVELS = 2
KIND_FREQUENCIES = 3

# Fields `DictionaryEntry` decodes; `tag` is an ECDICT build-time filter that the
# app never reads, so it is not worth carrying in the APK.
ENTRY_FIELDS = ("phonetic", "senses", "en", "lemma", "form", "kind", "note")


def compact_json(value: object, *, sort_keys: bool = True) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=sort_keys,
    ).encode("utf-8")


def require_ascii(keys: list[str], table: str) -> None:
    bad = [key for key in keys if any(ord(ch) > 127 for ch in key)]
    if bad:
        sample = ", ".join(repr(key) for key in bad[:5])
        raise SystemExit(
            f"{table}: {len(bad)} non-ASCII keys ({sample}); the binary key "
            "comparison assumes ASCII — extend the Kotlin comparator first."
        )


def section(
    kind: int,
    keys: list[str],
    *,
    blob_of=None,
    value_of=None,
    extra_blob: bytes = b"",
) -> tuple[bytes, bytes, bytes, bytes]:
    """Return (header, keys, values, blob) for one sorted table section.

    Value bytes are either an (offset, length) pair into the blob or, when
    `blob_of` is omitted, one float64 per key. `extra_blob` is the section's
    metadata (the frequency cut points) and always precedes entry blobs.
    """
    ordered = sorted(keys, key=lambda key: key.encode("utf-8"))
    keys_buf = bytearray()
    values_buf = bytearray()
    blob_buf = bytearray(extra_blob)
    for key in ordered:
        encoded = key.encode("utf-8")
        keys_buf += struct.pack("<I", len(encoded)) + encoded
        blob = blob_of(key) if blob_of is not None else None
        if blob is None:
            values_buf += struct.pack("<d", float(value_of(key)))
        else:
            offset = len(blob_buf)
            blob_buf += blob
            values_buf += struct.pack("<II", offset, len(blob))
    header = struct.pack(
        "<IIIII",
        kind,
        len(ordered),
        len(keys_buf),
        len(values_buf),
        len(blob_buf),
    )
    return header, keys_buf, values_buf, blob_buf


def build() -> bytes:
    dictionary = json.loads(DICT.read_text(encoding="utf-8"))["entries"]
    levels_doc = json.loads(LEVELS.read_text(encoding="utf-8"))
    levels = levels_doc["levels"]
    sources = levels_doc["sources"]
    frequencies_doc = json.loads(FREQUENCIES.read_text(encoding="utf-8"))
    frequencies = frequencies_doc["entries"]
    bands = frequencies_doc.get("bands", {"high": 4.5, "common": 3.5, "normal": 2.5})

    require_ascii(list(dictionary), "ecdict.json")
    require_ascii(list(levels), "word-levels.json")
    require_ascii(list(sources), "word-levels.json sources")
    require_ascii(list(frequencies), "word-frequencies.json")
    missing_sources = sorted(set(levels) - set(sources))
    if missing_sources:
        raise SystemExit(f"word-levels.json: {len(missing_sources)} words without sources")

    dict_header, dict_keys, dict_values, dict_blob = section(
        KIND_DICTIONARY,
        list(dictionary),
        blob_of=lambda key: compact_json(
            {field: dictionary[key][field] for field in ENTRY_FIELDS if field in dictionary[key]}
        ),
    )
    levels_header, levels_keys, levels_values, levels_blob = section(
        KIND_LEVELS,
        list(levels),
        # Level slots keep the source file's order: when two parts of speech
        # share the lowest level, WordQuery picks the first, so sorting the
        # inner map would silently change which slot (and source) wins.
        blob_of=lambda key: compact_json({"l": levels[key], "s": sources[key]}, sort_keys=False),
    )
    frequency_header, frequency_keys, frequency_values, frequency_blob = section(
        KIND_FREQUENCIES,
        list(frequencies),
        value_of=lambda key: frequencies[key],
        extra_blob=struct.pack(
            "<ddd",
            float(bands["high"]),
            float(bands["common"]),
            float(bands["normal"]),
        ),
    )

    headers = [dict_header, levels_header, frequency_header]
    head = MAGIC + struct.pack("<II", VERSION, len(headers))
    body = bytearray()
    for header, keys_buf, values_buf, blob_buf in (
        (dict_header, dict_keys, dict_values, dict_blob),
        (levels_header, levels_keys, levels_values, levels_blob),
        (frequency_header, frequency_keys, frequency_values, frequency_blob),
    ):
        body += keys_buf + values_buf + blob_buf
    return head + b"".join(headers) + bytes(body)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify the committed index")
    args = parser.parse_args()

    payload = build()
    if args.check:
        if not OUTPUT.is_file():
            print(f"error: {OUTPUT.relative_to(ANDROID)} is missing", file=sys.stderr)
            return 1
        current = OUTPUT.read_bytes()
        if current != payload:
            print(
                f"error: {OUTPUT.relative_to(ANDROID)} is stale "
                f"({len(current)} B on disk, {len(payload)} B expected); "
                "run tools/build_lexicon.py",
                file=sys.stderr,
            )
            return 1
        print(f"ok: lexicon.bin is current ({len(payload)} B)")
        return 0

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(payload)
    print(f"wrote {OUTPUT.relative_to(ANDROID)}: {len(payload)} B")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
