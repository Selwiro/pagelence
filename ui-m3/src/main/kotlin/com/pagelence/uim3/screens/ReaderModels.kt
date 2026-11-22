package com.pagelence.uim3.screens

// ReaderModels：从 ReaderScreen.kt 拆出；只做展示，不含业务规则。

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.reading.SentencePage

internal val SentenceTranslateIconSize = 14.dp
internal val SentenceAnalysisIconSize = 14.dp

/**
 * Page-turn tap zones live in the 22dp text margin only. A 48dp zone would
 * cover the last words of every line and steal their taps, so the zone matches
 * the margin; full height keeps it easy to hit.
 */
internal val PageTurnZoneWidth = 22.dp

/**
 * Extra hit area around the 14dp inline icons. The placeholder stays 14dp so
 * pagination does not move; taps land in this slop instead of on a word.
 */
internal val SentenceIconSlop = 14.dp

internal const val SentenceTranslateInlineId = "sentence-translate"
internal const val SentenceAnalysisInlineId = "sentence-analysis"

/** One page in the reading sequence: guide → text → quiz → summary → grammar → review. */
internal enum class ProgressKind { GUIDE, TEXT, QUIZ, GRAMMAR }

internal data class ProgressSegment(
    val kind: ProgressKind,
    val label: String,
    val value: Float,
    val active: Boolean,
)


internal data class PagePosition(
    val phase: ReaderPhase,
    val textPage: Int,
    val textProgress: Float,
)

internal data class ExpandedTranslation(
    val paragraphId: String,
    val sentenceNumber: Int,
    val sentence: String,
    val zh: String,
)
