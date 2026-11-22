package com.pagelence.uieink.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pagelence.uieink.theme.InkColors
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 墨水屏版的基础组件：只有文字、描边、留白与反色块。
 *
 * 不用 ripple / 阴影 / 圆角 / 转场——墨水屏上这些要么看不见，要么留下残影；
 * 选中与按下用 1.5–2dp 描边和字重表达，确保单色设备也能区分。
 */
@Composable
fun InkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalInkTypography.current.ui,
    color: Color = LocalInkColors.current.ink,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = color, textAlign = textAlign ?: TextAlign.Unspecified)),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
    )
}

/** 1px 分隔线；唯一的分层手段。 */
@Composable
fun InkDivider(modifier: Modifier = Modifier) {
    val colors = LocalInkColors.current
    Box(modifier.fillMaxWidth().height(1.dp).background(colors.rule))
}

/**
 * 按钮：主按钮反色填充，次按钮 1.5dp 描边；最小高度 48dp（验收要求），
 * 翻页键另用更大的区域，见 [InkPageTurnZone]。
 */
@Composable
fun InkButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    minHeight: Dp = 48.dp,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val interaction = remember { MutableInteractionSource() }
    val background = when {
        !enabled -> colors.fill
        primary -> colors.inversePaper
        else -> colors.paper
    }
    val foreground = when {
        !enabled -> colors.inkMuted
        primary -> colors.inverseInk
        else -> colors.ink
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .then(if (primary) Modifier else Modifier.border(1.5.dp, colors.ink))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        InkText(label, style = typography.uiStrong, color = foreground, textAlign = TextAlign.Center)
    }
}

/** 纯文字按钮（返回、更多），同样保证 48dp 触控。 */
@Composable
fun InkTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        InkText(
            label,
            style = typography.ui,
            color = if (enabled) colors.ink else colors.inkMuted,
        )
    }
}

/** 顶栏：返回 + 标题/副标题 + 右侧动作；下缘一条 1px 线。 */
@Composable
fun InkTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backLabel: String? = null,
    onBack: (() -> Unit)? = null,
    rightLabel: String? = null,
    onRight: (() -> Unit)? = null,
) {
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                InkTextButton(backLabel ?: "←", onClick = onBack)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp)) {
                InkText(title, style = typography.title, maxLines = 2)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    InkText(subtitle, style = typography.caption, color = LocalInkColors.current.inkMuted, maxLines = 2)
                }
            }
            if (onRight != null && rightLabel != null) {
                InkTextButton(rightLabel, onClick = onRight)
            }
        }
        InkDivider()
    }
}

/** 常驻提示条，替代 Snackbar（墨水屏不做浮层）。 */
@Composable
fun InkNotice(text: String, modifier: Modifier = Modifier) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxWidth()) {
        InkDivider()
        Box(Modifier.fillMaxWidth().background(colors.fill).padding(horizontal = 16.dp, vertical = 10.dp)) {
            InkText(text, style = typography.caption, color = colors.inkSecondary)
        }
        InkDivider()
    }
}

/**
 * 测验选项行：A/B/C/D + 文字。选中加粗描边，提交后正确/错误用正文里可见的
 * ✓ / ✗ 标记（不只靠线宽，避免灰度差异看不出来）。
 */
@Composable
fun InkChoiceRow(
    index: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    revealed: Boolean = false,
    correct: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val interaction = remember { MutableInteractionSource() }
    val width = if (selected) 2.dp else 1.dp
    val marker = when {
        !revealed -> ""
        correct -> "  ✓"
        else -> "  ✗"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(width, if (selected || revealed) colors.ink else colors.rule)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        InkText(
            "${('A' + index)}",
            style = typography.uiStrong,
            modifier = Modifier.width(24.dp),
        )
        InkText(
            label + marker,
            style = typography.ui,
            modifier = Modifier.weight(1f),
            color = when {
                !enabled && !revealed -> colors.inkMuted
                revealed && !correct -> colors.inkSecondary
                else -> colors.ink
            },
        )
    }
}

/** 页脚：左（返回提示）— 中（页码）— 右（键位提示）。 */
@Composable
fun InkPageFooter(left: String?, center: String?, right: String?, modifier: Modifier = Modifier) {
    val typography = LocalInkTypography.current
    val muted = LocalInkColors.current.inkMuted
    Column(modifier.fillMaxWidth()) {
        InkDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InkText(left.orEmpty(), style = typography.caption, color = muted, modifier = Modifier.weight(1f))
            InkText(center.orEmpty(), style = typography.caption, color = muted, textAlign = TextAlign.Center)
            InkText(
                right.orEmpty(),
                style = typography.caption,
                color = muted,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
    }
}

/** 小节标签，例如「已读完」。 */
@Composable
fun InkSectionLabel(text: String, modifier: Modifier = Modifier) {
    InkText(
        text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        style = LocalInkTypography.current.uiStrong,
        color = LocalInkColors.current.inkSecondary,
    )
}
