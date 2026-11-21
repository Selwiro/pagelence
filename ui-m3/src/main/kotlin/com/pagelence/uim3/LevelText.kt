package com.pagelence.uim3

import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.LevelSource

data class LevelChoice(
    val level: CefrLevel,
    val name: String,
    val description: String,
    /** Display-ready vocabulary line, e.g. "词汇量参考 2,000–4,000". */
    val vocabulary: String,
)

/** One place that maps CEFR levels to the user-facing copy. */
fun levelOptions(strings: Strings): List<LevelChoice> = listOf(
    LevelChoice(CefrLevel.A1, strings.levelA1Name, strings.levelA1Desc, vocabLine(strings, strings.levelA1Vocab)),
    LevelChoice(CefrLevel.A2, strings.levelA2Name, strings.levelA2Desc, vocabLine(strings, strings.levelA2Vocab)),
    LevelChoice(CefrLevel.B1, strings.levelB1Name, strings.levelB1Desc, vocabLine(strings, strings.levelB1Vocab)),
    LevelChoice(CefrLevel.B2, strings.levelB2Name, strings.levelB2Desc, vocabLine(strings, strings.levelB2Vocab)),
    LevelChoice(CefrLevel.C1, strings.levelC1Name, strings.levelC1Desc, vocabLine(strings, strings.levelC1Vocab)),
    LevelChoice(CefrLevel.C2, strings.levelC2Name, strings.levelC2Desc, vocabLine(strings, strings.levelC2Vocab)),
)

private fun vocabLine(strings: Strings, range: String): String =
    strings.levelVocabLine.format(range)

fun levelName(strings: Strings, level: CefrLevel): String =
    levelOptions(strings).first { it.level == level }.name

fun levelDescription(strings: Strings, level: CefrLevel): String =
    levelOptions(strings).first { it.level == level }.description

fun levelVocabulary(strings: Strings, level: CefrLevel): String =
    levelOptions(strings).first { it.level == level }.vocabulary

fun levelSourceLabel(strings: Strings, source: LevelSource): String = when (source) {
    LevelSource.SELF -> strings.levelSelf
    LevelSource.QUICK -> strings.levelQuick
    LevelSource.FULL -> strings.levelFull
}
