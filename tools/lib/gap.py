"""Content gap report shared by every book builder.

A unit has three content axes that the Web pilot only filled for its first ten
units: sentence translations, a unit quiz, and grammar analysis (form, use and
this sentence — or a grammar-library point plus the book's focus).

The report lists what is missing per unit so content work can be scheduled
without opening each book. It is generated, never hand-edited.
"""

from __future__ import annotations

from content import SCHEMA_VERSION, book_stats

ANALYSIS_KEYS = ("form", "explain", "focus")

LABELS = {
    "sentenceTranslations": "句译",
    "quiz": "测验",
    "grammarAnalysis": "语法解析",
}

RULES = {
    "sentenceTranslations": "单元内每一句都要有 sentences[].zh；缺一句即计入缺口。",
    "quiz": "unit.quiz 至少 1 题，每题至少 2 个选项、answer 合法、sentence 指回单元内 1 起的句序。",
    "grammarAnalysis": "grammar 解析齐全：书内写全 form/explain/focus，或 pointIds 首个命中语法库的 form+function+pitfalls 且书中给出 focus。",
}


def analysis_complete(grammar: dict | None, library: dict) -> bool:
    if not grammar:
        return False
    if all(grammar.get(key) for key in ANALYSIS_KEYS):
        return True
    primary = (grammar.get("pointIds") or [None])[0]
    point = library.get(primary or "")
    if not point or not grammar.get("focus"):
        return False
    return bool(point.get("form") and point.get("explain") and point.get("pitfalls"))


def unit_missing(unit: dict, library: dict) -> list[str]:
    missing = []
    sentences = [sentence for paragraph in unit.get("paragraphs", []) for sentence in paragraph.get("sentences", [])]
    translated = [sentence for sentence in sentences if (sentence.get("zh") or "").strip()]
    if len(translated) != len(sentences) or not sentences:
        missing.append("sentenceTranslations")
    if not unit.get("quiz"):
        missing.append("quiz")
    if not analysis_complete(unit.get("grammar"), library):
        missing.append("grammarAnalysis")
    return missing


def compute_gap(book: dict, library: dict, source: dict | None = None, notes: list[str] | None = None) -> dict:
    units = []
    for chapter in book.get("chapters", []):
        for unit in chapter.get("units", []):
            sentences = [
                sentence
                for paragraph in unit.get("paragraphs", [])
                for sentence in paragraph.get("sentences", [])
            ]
            translated = [sentence for sentence in sentences if (sentence.get("zh") or "").strip()]
            units.append({
                "id": unit["id"],
                "chapterId": chapter["id"],
                "unitNumber": unit.get("number"),
                "title": unit.get("title") or {"zh": "", "en": ""},
                "paragraphs": len(unit.get("paragraphs", [])),
                "sentences": len(sentences),
                "translatedSentences": len(translated),
                "quizItems": len(unit.get("quiz") or []),
                "hints": sum(len(paragraph.get("hints") or []) for paragraph in unit.get("paragraphs", [])),
                "missing": unit_missing(unit, library),
            })

    chapters = []
    for chapter in book.get("chapters", []):
        rows = [row for row in units if row["chapterId"] == chapter["id"]]
        chapters.append({
            "id": chapter["id"],
            "number": chapter.get("number"),
            "title": chapter.get("title") or {"zh": "", "en": ""},
            "units": len(rows),
            "missingTranslations": sum(1 for row in rows if "sentenceTranslations" in row["missing"]),
            "missingQuiz": sum(1 for row in rows if "quiz" in row["missing"]),
            "missingGrammarAnalysis": sum(1 for row in rows if "grammarAnalysis" in row["missing"]),
        })

    totals = {
        **book_stats(book),
        "unitsMissingTranslations": sum(1 for row in units if "sentenceTranslations" in row["missing"]),
        "unitsMissingQuiz": sum(1 for row in units if "quiz" in row["missing"]),
        "unitsMissingGrammarAnalysis": sum(1 for row in units if "grammarAnalysis" in row["missing"]),
        "unitsMissingAll": sum(1 for row in units if len(row["missing"]) == 3),
        "sentencesWithoutTranslation": sum(row["sentences"] - row["translatedSentences"] for row in units),
        "hints": sum(row["hints"] for row in units),
    }
    return {
        "schemaVersion": SCHEMA_VERSION,
        "bookId": book["id"],
        "source": source or {},
        "rules": RULES,
        "notes": notes or [],
        "totals": totals,
        "chapters": chapters,
        "units": units,
    }


def render_markdown(report: dict, title: str, data_path: str) -> str:
    totals = report["totals"]
    lines = [
        f"# 内容缺口报告 · {title}",
        "",
        "> 本页由构建脚本生成，不手工编辑。",
        f"> 机器可读版本：[`{data_path}`]({data_path})。",
        "",
        "## 总量",
        "",
        "| 项 | 数量 |",
        "| --- | --- |",
        f"| 章 | {totals['chapters']} |",
        f"| 单元 | {totals['units']} |",
        f"| 自然段 | {totals['paragraphs']} |",
        f"| 句子 | {totals['sentences']} |",
        f"| 提示 | {totals['hints']} |",
        f"| 缺句译的单元 | {totals['unitsMissingTranslations']} |",
        f"| 缺测验的单元 | {totals['unitsMissingQuiz']} |",
        f"| 缺语法解析的单元 | {totals['unitsMissingGrammarAnalysis']} |",
        f"| 三项全缺的单元 | {totals['unitsMissingAll']} |",
        f"| 未译句子 | {totals['sentencesWithoutTranslation']} |",
        "",
        "判定规则：",
        "",
    ]
    for key, rule in report["rules"].items():
        lines.append(f"- **{LABELS[key]}**：{rule}")
    if report.get("notes"):
        lines.append("")
        for note in report["notes"]:
            lines.append(note)
    lines += [
        "",
        "## 按章缺口",
        "",
        "| 章 | 单元 | 缺句译 | 缺测验 | 缺语法解析 |",
        "| --- | --- | --- | --- | --- |",
    ]
    for chapter in report["chapters"]:
        lines.append(
            f"| {chapter['title']['zh']} | {chapter['units']} | {chapter['missingTranslations']} "
            f"| {chapter['missingQuiz']} | {chapter['missingGrammarAnalysis']} |"
        )
    lines += [
        "",
        "## 逐单元明细",
        "",
        "| 单元 | 标题 | 段 | 句 | 已译 | 测验 | 提示 | 缺 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for unit in report["units"]:
        missing = "、".join(LABELS[key] for key in unit["missing"]) or "—"
        lines.append(
            f"| `{unit['id']}` | {unit['title']['zh']} | {unit['paragraphs']} | {unit['sentences']} "
            f"| {unit['translatedSentences']} | {unit['quizItems']} | {unit['hints']} | {missing} |"
        )
    lines.append("")
    return "\n".join(lines)
