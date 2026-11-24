package com.pagelence.uim3.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.FontSize
import com.pagelence.uim3.Strings
import kotlin.math.roundToInt

private val SliderHeight = 30.dp
private val SliderThumbRadius = 7.5.dp

/**
 * Compact slider over a fixed list of presets: a hairline track, stop dots and
 * a 15dp ringed knob. Material 3's slider draws a thick dark bar with a tall
 * thumb, which reads as a progress bar inside a dense settings panel.
 *
 * Dragging snaps to the nearest preset and only reports when the stop actually
 * moves; tapping the track jumps straight to that preset. The row keeps the
 * current value on the right so the preset name is always visible.
 */
@Composable
fun PresetSlider(
    label: String,
    valueLabel: String,
    index: Int,
    steps: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(top = 2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        SliderTrack(
            label = label,
            index = index,
            steps = steps,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun SliderTrack(
    label: String,
    index: Int,
    steps: Int,
    onSelect: (Int) -> Unit,
) {
    if (steps <= 0) return
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val insetPx = with(density) { SliderThumbRadius.toPx() }
    var widthPx by remember { mutableIntStateOf(0) }

    fun pick(x: Float) {
        val usable = (widthPx - insetPx * 2).coerceAtLeast(1f)
        val fraction = ((x - insetPx) / usable).coerceIn(0f, 1f)
        val next = (fraction * steps).roundToInt().coerceIn(0, steps)
        if (next != index) onSelect(next)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(SliderHeight)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(steps) { detectTapGestures { pick(it.x) } }
            .pointerInput(steps) {
                detectDragGestures(
                    onDragStart = { pick(it.x) },
                    onDrag = { change, _ -> pick(change.position.x) },
                )
            }
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = index.toFloat(),
                    range = 0f..steps.toFloat(),
                    steps = steps - 1,
                )
                setProgress { target ->
                    val next = target.roundToInt().coerceIn(0, steps)
                    if (next != index) onSelect(next)
                    true
                }
            },
    ) {
        val cy = size.height / 2f
        val left = insetPx
        val right = size.width - insetPx
        val span = (right - left).coerceAtLeast(1f)
        val knobX = left + span * (index.toFloat() / steps)
        val trackWidth = 3.dp.toPx()

        drawLine(
            color = colors.outlineVariant,
            start = Offset(left, cy),
            end = Offset(right, cy),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round,
        )
        if (index > 0) {
            drawLine(
                color = colors.primary,
                start = Offset(left, cy),
                end = Offset(knobX, cy),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round,
            )
        }
        // Stop marks only ahead of the knob: a pale dot on the dark active
        // track reads as a gap in the line rather than a tick.
        for (stop in (index + 1) until steps) {
            val x = left + span * stop / steps
            drawCircle(
                color = colors.outline.copy(alpha = 0.5f),
                radius = 2.dp.toPx(),
                center = Offset(x, cy),
            )
        }
        // Knob: a filled surface disc with a primary ring, so it reads against
        // both the dark active track and the light inactive one.
        drawCircle(
            color = colors.surface,
            radius = SliderThumbRadius.toPx(),
            center = Offset(knobX, cy),
        )
        drawCircle(
            color = colors.primary,
            radius = (SliderThumbRadius - 1.25.dp).toPx(),
            center = Offset(knobX, cy),
            style = Stroke(width = 2.5.dp.toPx()),
        )
    }
}

/** Font-size presets in increasing order with their names. */
fun fontSizeOptions(strings: Strings): List<Pair<FontSize, String>> =
    FontSize.entries.map { it to fontSizeLabel(strings, it) }

fun fontSizeLabel(strings: Strings, size: FontSize): String = when (size) {
    FontSize.XSMALL -> strings.fontXSmall
    FontSize.SMALL -> strings.fontSmall
    FontSize.MEDIUM -> strings.fontMedium
    FontSize.LARGE -> strings.fontLarge
    FontSize.XLARGE -> strings.fontXLarge
    FontSize.XXLARGE -> strings.fontXXLarge
}
