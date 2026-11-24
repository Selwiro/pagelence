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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * 按钮：一律白底黑字。主按钮用 2dp 描边 + 黑体，次按钮 1.5dp 描边 + 常规字重。
 *
 * 墨水屏禁止黑底（2026-11-22）：反色块在整页刷新时会留下明显残影，也会让
 * 「按钮」在灰阶里变成一个需要逐像素刷新的黑洞。分层只靠描边与字重。
 * 最小高度 48dp（验收要求）。
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
    val foreground = if (enabled) colors.ink else colors.inkMuted
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .border(if (primary) 2.dp else 1.5.dp, if (enabled) colors.ink else colors.rule)
            .background(colors.paper)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        InkText(
            label,
            style = if (primary) typography.uiStrong else typography.ui,
            color = foreground,
            textAlign = TextAlign.Center,
        )
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

/** 一行「标签 — 值」，用于统计、设置摘要与同步状态。 */
@Composable
fun InkKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InkText(label, style = typography.ui, color = colors.inkSecondary, modifier = Modifier.weight(1f))
        InkText(
            value,
            style = if (emphasis) typography.uiStrong else typography.ui,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
        )
    }
}

/** 可选行：方框标记，选中填实；用于单选（语言、目标、刷新强度）。 */
@Composable
fun InkSelectRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    alignTop: Boolean = false,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically,
    ) {
        InkText(
            if (selected) "■" else "□",
            style = typography.uiStrong,
            modifier = Modifier.width(24.dp).then(if (alignTop) Modifier.padding(top = 2.dp) else Modifier),
        )
        Column(Modifier.weight(1f)) {
            InkText(label, style = if (selected) typography.uiStrong else typography.ui)
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                InkText(description, style = typography.caption, color = colors.inkMuted)
            }
        }
    }
}

/** 通用列表行：标题 + 副标题 + 右侧说明；整行可点。 */
@Composable
fun InkListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            InkText(title, style = typography.ui, maxLines = 3)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                InkText(subtitle, style = typography.caption, color = colors.inkMuted, maxLines = 2)
            }
        }
        if (!trailing.isNullOrBlank()) {
            Spacer(Modifier.width(12.dp))
            InkText(trailing, style = typography.caption, color = colors.inkSecondary, textAlign = TextAlign.End)
        }
    }
}

/** 单行文本输入；白底 + 1px 描边，光标用正文色。 */
@Composable
fun InkTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        InkText(label, style = typography.caption, color = colors.inkMuted)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.rule)
                .background(colors.paper)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            textStyle = typography.ui.merge(TextStyle(color = colors.ink)),
            singleLine = singleLine,
            cursorBrush = SolidColor(colors.ink),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        InkText(placeholder, style = typography.ui, color = colors.inkMuted)
                    }
                    inner()
                }
            },
        )
    }
}

/** 底部四栏：当前项加粗并画一条 3dp 底线，不用填充色。 */
@Composable
fun InkTabBar(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth()) {
        InkDivider()
        Row(Modifier.fillMaxWidth().height(60.dp)) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelect(index) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    InkText(
                        label,
                        style = if (active) typography.uiStrong else typography.ui,
                        color = if (active) colors.ink else colors.inkMuted,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .background(if (active) colors.ink else colors.paper),
                    )
                }
            }
        }
    }
}

/** 整页确认（替代对话框）：白底、标题、正文、两个按钮。 */
@Composable
fun InkConfirmPage(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String? = null,
    onCancel: (() -> Unit)? = null,
) {
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize().padding(24.dp)) {
        InkText(title, style = typography.title)
        if (body.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            InkText(body, style = typography.body)
        }
        Spacer(Modifier.height(24.dp))
        InkButton(confirmLabel, onClick = onConfirm, primary = true)
        if (cancelLabel != null && onCancel != null) {
            Spacer(Modifier.height(10.dp))
            InkButton(cancelLabel, onClick = onCancel)
        }
    }
}

/**
 * 一行等分选项（T169）：字号、行距、页边距等档位。选中用 2dp 描边 + 加粗，
 * 不反色；每格最小 44dp，整行与上方标签一起读。
 */
@Composable
fun <T> InkOptionRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        InkText(label, style = typography.ui, color = colors.inkSecondary)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            options.forEach { (value, text) ->
                val isSelected = value == selected
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .defaultMinSize(minHeight = 44.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.ink else colors.rule,
                        )
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelect(value) },
                        )
                        .padding(horizontal = 2.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    InkText(
                        text,
                        style = if (isSelected) typography.uiStrong else typography.ui,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
