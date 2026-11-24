package com.pagelence.uim3.screens

// ReaderDock：阅读页底部菜单与内联阅读设置（T167）。
//
// 点击正文空白处弹出，参考常规阅读软件：底部一行从左到右是目录、书签与笔记、
// 设置、生词卡；点设置就地展开设置面板（字号、页边距、行距滑杆，字体、翻页
// 方式、词义语言、导读模式），不再跳转到设置页。本文件只做展示。

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.LineSpacing
import com.pagelence.core.prefs.PageMargin
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.prefs.ReaderLayout
import com.pagelence.core.prefs.ReadingFont
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.ChoiceRow
import com.pagelence.uim3.components.PagelenceIcons
import kotlin.math.roundToInt

/**
 * The panel above the icons takes at most this share of the screen and scrolls
 * above that, so the four actions never get pushed off a short screen.
 */
private const val SettingsPanelHeightFraction = 0.88f
private val SettingsPanelMaxHeight = 520.dp

/**
 * Reader chrome that only appears on demand: a light scrim over the page plus a
 * bottom surface holding the optional settings panel and the four actions.
 * Everything stays an overlay, so showing it never reflows or repaginates text.
 */
@Composable
internal fun ReaderDockOverlay(
    strings: Strings,
    visible: Boolean,
    settingsOpen: Boolean,
    preference: Preferences,
    annotationCount: Int,
    cardsCount: Int,
    onDismiss: () -> Unit,
    onOpenToc: () -> Unit,
    onOpenAnnotations: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenSettings: () -> Unit,
    onFontSize: (FontSize) -> Unit,
    onPageMargin: (PageMargin) -> Unit,
    onLineSpacing: (LineSpacing) -> Unit,
    onReadingFont: (ReadingFont) -> Unit,
    onReaderLayout: (ReaderLayout) -> Unit,
    onDefinitionMode: (DefinitionMode) -> Unit,
    onGuideMode: (GuideMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val panelMaxHeight = minOf(
                SettingsPanelMaxHeight,
                maxHeight * SettingsPanelHeightFraction,
            )
            val interactions = remember { MutableInteractionSource() }
            // A light scrim keeps the text readable while the panel is open; it
            // is also the "tap outside to close" target.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.16f))
                    .clickable(
                        interactionSource = interactions,
                        indication = null,
                        role = Role.Button,
                        onClick = onDismiss,
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    )
                    .padding(bottom = 2.dp),
            ) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                if (settingsOpen) {
                    ReaderSettingsPanel(
                        strings = strings,
                        preference = preference,
                        maxHeight = panelMaxHeight,
                        onOpenSettings = onOpenSettings,
                        onFontSize = onFontSize,
                        onPageMargin = onPageMargin,
                        onLineSpacing = onLineSpacing,
                        onReadingFont = onReadingFont,
                        onReaderLayout = onReaderLayout,
                        onDefinitionMode = onDefinitionMode,
                        onGuideMode = onGuideMode,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DockAction(
                        icon = PagelenceIcons.Contents,
                        label = strings.toc,
                        onClick = onOpenToc,
                    )
                    DockAction(
                        icon = PagelenceIcons.Bookmark,
                        label = strings.annotations,
                        count = annotationCount,
                        onClick = onOpenAnnotations,
                    )
                    DockAction(
                        icon = PagelenceIcons.Tune,
                        label = strings.settings,
                        active = settingsOpen,
                        onClick = onToggleSettings,
                    )
                    DockAction(
                        icon = PagelenceIcons.Cards,
                        label = strings.wordCards,
                        count = cardsCount,
                        onClick = onOpenCards,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DockAction(
    icon: ImageVector,
    label: String,
    count: Int = 0,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(23.dp),
            )
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-7).dp),
                ) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            // English labels such as "Bookmarks & notes" need a second line at
            // this width; Chinese ones stay on one.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReaderSettingsPanel(
    strings: Strings,
    preference: Preferences,
    maxHeight: Dp,
    onOpenSettings: () -> Unit,
    onFontSize: (FontSize) -> Unit,
    onPageMargin: (PageMargin) -> Unit,
    onLineSpacing: (LineSpacing) -> Unit,
    onReadingFont: (ReadingFont) -> Unit,
    onReaderLayout: (ReaderLayout) -> Unit,
    onDefinitionMode: (DefinitionMode) -> Unit,
    onGuideMode: (GuideMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.readerSettings,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                strings.readerSettingsMore,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onOpenSettings)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        StepSlider(
            label = strings.fontSize,
            options = listOf(
                FontSize.SMALL to strings.fontSmall,
                FontSize.MEDIUM to strings.fontMedium,
                FontSize.LARGE to strings.fontLarge,
                FontSize.XLARGE to strings.fontXLarge,
            ),
            selected = preference.fontSize,
            onSelect = onFontSize,
        )
        StepSlider(
            label = strings.pageMargin,
            options = listOf(
                PageMargin.NARROW to strings.pageMarginNarrow,
                PageMargin.NORMAL to strings.pageMarginNormal,
                PageMargin.WIDE to strings.pageMarginWide,
            ),
            selected = preference.pageMargin,
            onSelect = onPageMargin,
        )
        StepSlider(
            label = strings.lineSpacing,
            options = listOf(
                LineSpacing.COMPACT to strings.lineSpacingCompact,
                LineSpacing.NORMAL to strings.lineSpacingNormal,
                LineSpacing.RELAXED to strings.lineSpacingRelaxed,
            ),
            selected = preference.lineSpacing,
            onSelect = onLineSpacing,
        )
        ChoiceLine(
            label = strings.readingFont,
            options = listOf(
                ReadingFont.SANS to strings.fontSans,
                ReadingFont.SERIF to strings.fontSerif,
            ),
            selected = preference.readingFont,
            onSelect = onReadingFont,
        )
        ChoiceLine(
            label = strings.readingLayout,
            options = listOf(
                ReaderLayout.PAGED to strings.layoutPaged,
                ReaderLayout.SCROLL to strings.layoutScroll,
            ),
            selected = preference.readerLayout,
            onSelect = onReaderLayout,
        )
        ChoiceLine(
            label = strings.definitionMode,
            options = listOf(
                DefinitionMode.ZH to strings.definitionModeZh,
                DefinitionMode.MIXED to strings.definitionModeMixed,
                DefinitionMode.EN to strings.definitionModeEn,
            ),
            selected = preference.definitionMode,
            onSelect = onDefinitionMode,
        )
        ChoiceLine(
            label = strings.guideMode,
            options = listOf(
                GuideMode.BILINGUAL to strings.guideModeBilingual,
                GuideMode.EN to strings.guideModeEn,
                GuideMode.IMMERSIVE to strings.guideModeImmersive,
            ),
            selected = preference.guideMode,
            onSelect = onGuideMode,
        )
        Spacer(Modifier.height(6.dp))
    }
}

/**
 * Compact slider over a fixed list of presets: a hairline track, small stop
 * dots and a 16dp knob. The Material 3 slider's thick dark bar and tall thumb
 * read as a progress bar in a settings panel this dense, so the reader panel
 * draws its own. Dragging snaps to the nearest preset and only reports a
 * change when the stop actually moves.
 */
@Composable
private fun <T> StepSlider(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                options[index].second,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        PresetSlider(
            label = label,
            index = index,
            steps = options.lastIndex,
            onPick = { next -> onSelect(options[next].first) },
        )
    }
}

private val SliderHeight = 30.dp
private val SliderThumbRadius = 7.5.dp

@Composable
private fun PresetSlider(
    label: String,
    index: Int,
    steps: Int,
    onPick: (Int) -> Unit,
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
        if (next != index) onPick(next)
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
                    if (next != index) onPick(next)
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

/** A labelled row of single-choice chips. The label sits above the chips so
 * "Meaning language" and "双语带读" both fit without squeezing the chips. */
@Composable
private fun <T> ChoiceLine(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        ChoiceRow(
            options = options,
            selected = selected,
            onSelect = onSelect,
        )
    }
}

/**
 * Fires [onTap] when a tap on the reading surface was not handled by anything
 * inside it — no word, sentence icon or button consumed the up event.
 *
 * Consumption is checked on the Main pass: by the time this node sees the up,
 * its descendants (the word spans, icons, buttons) have already had their turn,
 * while ancestors such as the pager and `verticalScroll` have not consumed it
 * yet. A plain `detectTapGestures` cannot be used here because those ancestors
 * swallow the tap on a scrollable page.
 */
@Composable
internal fun Modifier.readerBlankTap(onTap: () -> Unit): Modifier {
    val current = rememberUpdatedState(onTap)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = down.position
            val slop = viewConfiguration.touchSlop
            var moved = false
            var up: PointerInputChange? = null
            while (up == null) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if ((change.position - start).getDistance() > slop) moved = true
                if (!change.pressed) up = change
            }
            if (up != null && !moved && !up.isConsumed) current.value()
        }
    }
}
