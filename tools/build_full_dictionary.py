#!/usr/bin/env python3
"""Build the optional full ECDICT table (`full-dict.sqlite`) + `dict-manifest.json`.

The app ships a core dictionary (38k headwords, `tools/build_dictionaries.py`).
This script produces the **complete** ECDICT table as a single SQLite file with
the same entry fields (`phonetic`, `senses`, `en`, `lemma`, `form`), plus the
manifest compiled into the APK (`content/dict-manifest.json`).

The upstream CSV is not in the repository. Fetch the pinned revision and pass
it in:

    curl -L -o /tmp/ecdict.csv \\
      https://raw.githubusercontent.com/skywind3000/ECDICT/39cfb9672c0906b63569e0453c2e6abfb9ab182a/ecdict.csv
    python3 tools/build_full_dictionary.py --ecdict-csv /tmp/ecdict.csv

Output (both ignored by git, both must be uploaded for the download to work):

    dist/dict/full-dict.sqlite
    dist/dict/dict-manifest.json
    dist/dict/LICENSE-ECDICT.txt
    content/dict-manifest.json      # compiled into the app

Parsing differences from the core table, on purpose:

* the full table keeps every row that has a translation, including senses that
  ECDICT tags with a domain (`[网络]`, `[医]`, `[化]`, …). The core build drops
  those lines because it only needs common words; the long tail is exactly what
  this download is for. The leading tag is stripped from the line, the sense
  text stays.
* rows without any translation, definition or phonetic are skipped (36 rows in
  the pinned revision).
* `lemma` is derived from the `exchange` column the same way as the core build
  (`0:` on an inflected row, base rows listing `s/p/d/i/3/r/t`), plus the same
  irregular overrides.

The SQLite file is not byte-reproducible across SQLite versions. Regenerate the
manifest **together with** the artifact you upload, never separately.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import re
import sqlite3
import sys
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[1]
CONTENT = ANDROID / "content"
OUT_DIR = ANDROID / "dist" / "dict"
MANIFEST = CONTENT / "dict-manifest.json"
LICENSE = CONTENT / "third-party" / "dictionaries" / "ecdict-license.txt"

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_dictionaries as core  # noqa: E402

SCHEMA_VERSION = 1
DEFAULT_DICT_VERSION = "39cfb96"
TAG = re.compile(r"^\s*[\[【][^\]】]*[\]】]\s*")


def full_lines(value: str) -> list[str]:
    """Split one ECDICT cell into lines, removing only the domain tag."""
    out: list[str] = []
    for line in value.replace("\\r", "").split("\\n"):
        line = TAG.sub("", line.strip())
        if line:
            out.append(line)
    return out


def json_senses(rows: list[list[str]]) -> str | None:
    if not rows:
        return None
    return json.dumps(rows, ensure_ascii=False, separators=(",", ":"))


def build(csv_path: Path, out_path: Path, dict_version: str) -> dict[str, int]:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    tmp = out_path.with_name(out_path.name + ".tmp")
    tmp.unlink(missing_ok=True)
    con = sqlite3.connect(tmp)
    con.execute("PRAGMA journal_mode=OFF")
    con.execute("PRAGMA synchronous=OFF")
    con.execute("PRAGMA temp_store=MEMORY")
    con.execute(
        "CREATE TABLE entries ("
        "term TEXT PRIMARY KEY, phonetic TEXT, senses TEXT, en TEXT, lemma TEXT, form TEXT)"
    )
    inserted = skipped = 0
    batch: list[tuple] = []
    with csv_path.open(encoding="utf-8-sig", newline="") as source:
        for row in csv.DictReader(source):
            term = core.normalize(row.get("word") or "")
            if not term:
                continue
            zh = core.senses(full_lines(row.get("translation") or ""))
            en = core.senses(full_lines(row.get("definition") or ""))
            phonetic = (row.get("phonetic") or "").strip()
            if not (zh or en or phonetic):
                skipped += 1
                continue
            form = None
            for kind, value in core.exchange_parts(row):
                if kind == "1":
                    form = value
                    break
            batch.append((term, phonetic or None, json_senses(zh), json_senses(en), None, form))
            if len(batch) >= 5000:
                inserted += insert_batch(con, batch)
                batch.clear()
    inserted += insert_batch(con, batch)

    # Second pass: morphology. `0:` names the base row; a base row lists its
    # inflected forms under s/p/d/i/3/r/t, so both directions set `lemma`.
    with csv_path.open(encoding="utf-8-sig", newline="") as source:
        for row in csv.DictReader(source):
            term = core.normalize(row.get("word") or "")
            if not term:
                continue
            for kind, value in core.exchange_parts(row):
                if kind == "0":
                    set_lemma(con, term=term, lemma=value)
                elif kind in core.BASE_KINDS:
                    set_lemma(con, term=value, lemma=term)
    for term, lemma in core.LEMMA_OVERRIDES.items():
        con.execute("UPDATE entries SET lemma=? WHERE term=?", (lemma, term))

    con.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)")
    con.executemany(
        "INSERT INTO meta(key, value) VALUES (?, ?)",
        [
            ("schemaVersion", str(SCHEMA_VERSION)),
            ("dictVersion", dict_version),
            ("source", f"ECDICT {dict_version}"),
        ],
    )
    con.execute("INSERT INTO meta(key, value) VALUES ('termCount', (SELECT COUNT(*) FROM entries))")
    con.commit()
    con.execute("VACUUM")
    con.commit()
    con.close()
    os.replace(tmp, out_path)
    return {"entries": inserted, "skipped": skipped}


def insert_batch(con: sqlite3.Connection, batch: list[tuple]) -> int:
    before = con.total_changes
    con.executemany(
        "INSERT OR IGNORE INTO entries(term, phonetic, senses, en, lemma, form) "
        "VALUES (?, ?, ?, ?, ?, ?)",
        batch,
    )
    return con.total_changes - before


def set_lemma(con: sqlite3.Connection, term: str, lemma: str) -> None:
    if term == lemma:
        return
    con.execute(
        "UPDATE entries SET lemma=? WHERE term=? AND COALESCE(lemma,'')=''",
        (lemma, term),
    )


def manifest_of(path: Path, dict_version: str, min_app: int) -> dict:
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
            size += len(chunk)
    return {
        "dictVersion": dict_version,
        "schemaVersion": SCHEMA_VERSION,
        "bytes": size,
        "sha256": digest.hexdigest(),
        "minAppVersionCode": min_app,
    }


def write_manifest(path: Path, manifest: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--ecdict-csv", type=Path)
    parser.add_argument("--out-dir", type=Path, default=OUT_DIR)
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    parser.add_argument("--dict-version", default=DEFAULT_DICT_VERSION)
    parser.add_argument("--min-app-version-code", type=int, default=1)
    parser.add_argument("--check", action="store_true", help="verify the manifest matches the built table")
    args = parser.parse_args()

    out_path = args.out_dir / "full-dict.sqlite"
    if args.check:
        if not out_path.is_file() or not args.manifest.is_file():
            print("skip: no built table or manifest to check")
            return
        manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
        actual = manifest_of(out_path, manifest.get("dictVersion", ""), manifest.get("minAppVersionCode", 0))
        if actual["bytes"] != manifest["bytes"] or actual["sha256"] != manifest["sha256"]:
            print(f"stale: {args.manifest} does not match {out_path}")
            sys.exit(1)
        print("full dictionary manifest up to date")
        return

    if args.ecdict_csv is None or not args.ecdict_csv.is_file():
        parser.error(f"ECDICT CSV not found: {args.ecdict_csv}")

    stats = build(args.ecdict_csv, out_path, args.dict_version)
    manifest = manifest_of(out_path, args.dict_version, args.min_app_version_code)
    write_manifest(args.out_dir / "dict-manifest.json", manifest)
    write_manifest(args.manifest, manifest)
    if LICENSE.is_file():
        (args.out_dir / "LICENSE-ECDICT.txt").write_bytes(LICENSE.read_bytes())
    size_mib = manifest["bytes"] / 1048576
    print(
        f"entries: {stats['entries']}  skipped: {stats['skipped']}  "
        f"size: {size_mib:.1f} MiB  dictVersion: {args.dict_version}"
    )
    print(f"wrote {out_path}")
    print(f"wrote {args.out_dir / 'dict-manifest.json'}")
    print(f"wrote {args.manifest}")
    print("upload full-dict.sqlite + dict-manifest.json + LICENSE-ECDICT.txt, then keep the manifest in sync")


if __name__ == "__main__":
    main()
