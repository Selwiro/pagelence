"""Category vocabulary for book packages.

Only categories are frozen here; tags are still a draft and are not validated.
`content/taxonomy.json` is the machine-readable source of truth.

Usage:
    from taxonomy import category_errors, taxonomy_errors
"""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path

CONTENT = Path(__file__).resolve().parents[2] / "content"
TAXONOMY = CONTENT / "taxonomy.json"
EXPECTED_IDS = ("literature", "knowledge", "guides", "other")


@lru_cache(maxsize=1)
def load_taxonomy() -> dict:
    return json.loads(TAXONOMY.read_text(encoding="utf-8"))


def category_errors(
    value,
    *,
    where: str = "category",
    required: bool = False,
    require_id: bool = True,
) -> list[str]:
    """Check a `{id, zh, en}` category object against `taxonomy.json`.

    Built-in books must carry `id`. Imported packages without `id` can set
    `require_id=False`; their display name is matched against
    `categoryAliases`. An unmatched category is reported so the importer can
    fall back to `other` explicitly instead of guessing.
    """
    if value is None:
        return [f"{where} is required"] if required else []
    if not isinstance(value, dict):
        return [f"{where} must be an object with id, zh and en"]
    if not value:
        return [f"{where} has no zh or en"]

    try:
        taxonomy = load_taxonomy()
    except (OSError, ValueError) as error:
        return [f"{where}: taxonomy.json cannot be read: {error}"]
    by_id = {item["id"]: item for item in taxonomy.get("categories", [])}
    zh = str(value.get("zh") or "").strip()
    en = str(value.get("en") or "").strip()
    category_id = str(value.get("id") or "").strip()

    if not zh and not en:
        return [f"{where} has no zh or en"]

    if not category_id:
        if require_id:
            return [f"{where}.id is required"]
        match = None
        for alias in taxonomy.get("categoryAliases", []):
            if en and alias.get("en") == en:
                match = alias
                break
            if zh and alias.get("zh") == zh:
                match = alias
                break
        if match is None:
            return [f"{where} has no id and {zh or en!r} is not a known alias"]
        # An alias is a legacy display name; accept it and let the caller use
        # the canonical id instead of demanding the new display pair.
        return []

    if category_id not in by_id:
        return [f"{where}.id {category_id!r} is not one of {sorted(by_id)}"]

    canonical = by_id[category_id]
    errors: list[str] = []
    if not zh:
        errors.append(f"{where}.zh is required")
    elif zh != canonical["zh"]:
        errors.append(f"{where}.zh {zh!r} must be {canonical['zh']!r} for id {category_id}")
    if not en:
        errors.append(f"{where}.en is required")
    elif en != canonical["en"]:
        errors.append(f"{where}.en {en!r} must be {canonical['en']!r} for id {category_id}")
    return errors


def taxonomy_errors() -> list[str]:
    """Check `content/taxonomy.json` itself: four fixed categories and aliases."""
    if not TAXONOMY.is_file():
        return ["taxonomy.json is missing"]
    try:
        data = load_taxonomy()
    except (OSError, ValueError) as error:
        return [f"taxonomy.json cannot be read: {error}"]

    errors: list[str] = []
    if data.get("schemaVersion") != 1:
        errors.append(f"schemaVersion must be 1, got {data.get('schemaVersion')!r}")

    categories = data.get("categories")
    if not isinstance(categories, list):
        return errors + ["categories must be an array"]

    seen: set[str] = set()
    pairs: set[tuple[str, str]] = set()
    for index, item in enumerate(categories):
        where = f"categories[{index}]"
        if not isinstance(item, dict):
            errors.append(f"{where} must be an object")
            continue
        category_id = str(item.get("id") or "").strip()
        zh = str(item.get("zh") or "").strip()
        en = str(item.get("en") or "").strip()
        if category_id not in EXPECTED_IDS:
            errors.append(f"{where}.id {category_id!r} is not one of {list(EXPECTED_IDS)}")
        elif category_id in seen:
            errors.append(f"{where}.id {category_id!r} is duplicated")
        seen.add(category_id)
        if not zh or not en:
            errors.append(f"{where} needs zh and en")
        elif (zh, en) in pairs:
            errors.append(f"{where} duplicates {zh}/{en}")
        pairs.add((zh, en))

    missing = [category_id for category_id in EXPECTED_IDS if category_id not in seen]
    if missing:
        errors.append(f"categories missing {missing}")

    aliases = data.get("categoryAliases", [])
    if not isinstance(aliases, list):
        errors.append("categoryAliases must be an array")
        return errors
    known = {item["id"] for item in categories if isinstance(item, dict) and item.get("id")}
    for index, alias in enumerate(aliases):
        where = f"categoryAliases[{index}]"
        if not isinstance(alias, dict):
            errors.append(f"{where} must be an object")
            continue
        if alias.get("id") not in known:
            errors.append(f"{where}.id {alias.get('id')!r} is not a known category")
        if not str(alias.get("zh") or "").strip() and not str(alias.get("en") or "").strip():
            errors.append(f"{where} needs zh or en")
    return errors
