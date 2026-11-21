package com.pagelence.uim3.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Compact sentence-structure mark: a main clause branching into two parts,
 * each with two leaves. It replaces the old 「解析」 text marker at the end of a
 * long sentence, so the entry reads as an action instead of miniature copy.
 */
val AnalysisIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Analysis",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // Root, then the main-clause bar.
            moveTo(12f, 4.2f)
            lineTo(12f, 8f)
            moveTo(4.5f, 8f)
            lineTo(19.5f, 8f)
            // Two immediate parts.
            moveTo(4.5f, 8f)
            lineTo(4.5f, 12.5f)
            moveTo(19.5f, 8f)
            lineTo(19.5f, 12.5f)
            // Leaves under each part.
            moveTo(4.5f, 12.5f)
            lineTo(1.8f, 19.5f)
            moveTo(4.5f, 12.5f)
            lineTo(7.2f, 19.5f)
            moveTo(19.5f, 12.5f)
            lineTo(16.8f, 19.5f)
            moveTo(19.5f, 12.5f)
            lineTo(22.2f, 19.5f)
        }
    }.build()
}
