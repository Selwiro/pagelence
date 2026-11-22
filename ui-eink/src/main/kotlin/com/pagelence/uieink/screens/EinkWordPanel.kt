package com.pagelence.uieink.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.SenseUi
import com.pagelence.uim3.model.WordPanelUi
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 墨水屏词条页：整页显示，替代普通版的底部面板（墨屏不做浮层）。
 *
 * 结构照普通版：词面 + 音标 → 原形/缩合说明 → 词性/等级/本书次数 → 本句释义 →
 * 中文/英文词典释义 → 例句 → 收藏。关闭（返回键或「关闭」）回到原页，位置不变。
 */
@Composable
fun EinkWordPanelScreen(
    strings: Strings,
    panel: WordPanelUi,
    definitionMode: DefinitionMode,
    onToggleCard: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val showZh = definitionMode != DefinitionMode.EN
    val showEn = definitionMode != DefinitionMode.ZH
    val zhSenses = panel.senses.filter { it.language == "zh" }
    val enSenses = panel.senses.filter { it.language == "en" }
    val meta = buildList {
        panel.pos?.takeIf { it.isNotBlank() }?.let { add(it) }
        panel.level?.let { add(if (panel.levelApprox) "≈$it" else it) }
        panel.occurrences?.let { add("${strings.occurrences} $it") }
    }

    Column(modifier.fillMaxSize()) {
        InkTopBar(
            title = panel.term,
            subtitle = listOfNotNull(
                panel.ipa?.takeIf { it.isNotBlank() }?.let { "/$it/" },
                panel.lemma.takeIf { it.isNotBlank() && !it.equals(panel.term, ignoreCase = true) }
                    ?.let { "${strings.lemmaBase} $it" },
            ).joinToString("  ").ifBlank { null },
            backLabel = strings.close,
            onBack = onClose,
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                if (meta.isNotEmpty()) {
                    InkText(meta.joinToString(" · "), style = typography.caption, color = colors.inkSecondary)
                    Spacer(Modifier.height(8.dp))
                }
                panel.note?.takeIf { it.isNotBlank() }?.let {
                    InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
                    Spacer(Modifier.height(8.dp))
                }
                val gloss = panel.gloss?.takeIf { it.isNotBlank() }
                if (gloss != null) {
                    InkText(strings.sentenceMeaning, style = typography.caption, color = colors.inkMuted)
                    Spacer(Modifier.height(4.dp))
                    InkText(gloss, style = typography.body)
                    Spacer(Modifier.height(12.dp))
                }
                val emptyLabel = panel.emptyLabel
                if (emptyLabel != null && panel.senses.isEmpty()) {
                    InkText(emptyLabel, style = typography.body, color = colors.inkSecondary)
                }
                if (showZh && zhSenses.isNotEmpty()) {
                    InkDivider()
                    Spacer(Modifier.height(10.dp))
                    InkText(strings.definitionsZh, style = typography.uiStrong)
                    zhSenses.forEach { SenseRow(it) }
                    Spacer(Modifier.height(10.dp))
                }
                if (showEn && enSenses.isNotEmpty()) {
                    InkDivider()
                    Spacer(Modifier.height(10.dp))
                    InkText(strings.definitionsEn, style = typography.uiStrong)
                    enSenses.forEach { SenseRow(it) }
                    Spacer(Modifier.height(10.dp))
                }
                val exampleEn = panel.exampleEn?.takeIf { it.isNotBlank() }
                if (exampleEn != null) {
                    InkDivider()
                    Spacer(Modifier.height(10.dp))
                    InkText(exampleEn, style = typography.bodySmall)
                    panel.exampleZh?.takeIf { it.isNotBlank() }?.let {
                        InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
                    }
                    Spacer(Modifier.height(10.dp))
                } else {
                    val sentenceZh = panel.sentenceZh
                    if (sentenceZh != null && sentenceZh.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        InkText(sentenceZh, style = typography.bodySmall, color = colors.inkSecondary)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                InkButton(
                    label = if (panel.collected) strings.removeCard else strings.collectCard,
                    onClick = onToggleCard,
                    primary = !panel.collected,
                )
                Spacer(Modifier.height(10.dp))
                InkButton(strings.close, onClick = onClose)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SenseRow(sense: SenseUi) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Spacer(Modifier.height(6.dp))
    InkText(
        listOfNotNull(sense.pos?.takeIf { it.isNotBlank() }, sense.definition).joinToString("  "),
        style = typography.bodySmall,
        color = colors.inkSecondary,
    )
}
