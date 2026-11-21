package com.pagelence.uim3.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Simple speaker glyph; the material-icons core set has none. */
val SpeakerIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Speaker",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 9f)
            lineTo(7f, 9f)
            lineTo(12f, 5f)
            lineTo(12f, 19f)
            lineTo(7f, 15f)
            lineTo(3f, 15f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(15f, 8.5f)
            curveTo(16.8f, 10f, 16.8f, 14f, 15f, 15.5f)
            moveTo(17.8f, 6.4f)
            curveTo(20.8f, 9f, 20.8f, 15f, 17.8f, 17.6f)
        }
    }.build()
}
