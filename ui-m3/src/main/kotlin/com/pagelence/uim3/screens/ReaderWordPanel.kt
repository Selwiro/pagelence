package com.pagelence.uim3.screens

// ReaderWordPanel：从 ReaderScreen.kt 拆出；只做展示，不含业务规则。

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.SectionLabel
import com.pagelence.uim3.model.SenseUi
import com.pagelence.uim3.model.WordPanelUi

@Composable
internal fun Cardish(title: String, content: @Composable () -> Unit) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
internal fun WordPanel(
    strings: Strings,
    panel: WordPanelUi,
    definitionMode: DefinitionMode,
    onToggleCard: () -> Unit,
    onClose: () -> Unit,
) {
    val zhBase = panel.senses.filter { it.language == "zh" && !it.fromSurface }.take(6)
    val zhExtra = panel.senses.filter { it.language == "zh" && it.fromSurface }.take(4)
    val enBase = panel.senses.filter { it.language == "en" && !it.fromSurface }.take(6)
    val enExtra = panel.senses.filter { it.language == "en" && it.fromSurface }.take(4)
    val showZh = definitionMode != DefinitionMode.EN
    val showEn = definitionMode != DefinitionMode.ZH
    val hasZh = zhBase.isNotEmpty() || zhExtra.isNotEmpty()
    val hasEn = enBase.isNotEmpty() || enExtra.isNotEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(panel.term, style = MaterialTheme.typography.headlineMedium)
            panel.ipa?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(10.dp))
                Text(
                    "/$it/",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (panel.lemma.isNotBlank() && !panel.lemma.equals(panel.term, ignoreCase = true)) {
            Text(
                "${strings.lemmaBase} ${panel.lemma}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        panel.note?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            panel.pos?.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
            panel.level?.let { MetaChip(if (panel.levelApprox) "≈$it" else it) }
        }
        // The word panel shows only the CEFR level and book occurrences; the
        // wordfreq band (高频/少见 …) is deliberately left out.
        val meta = buildList {
            if (panel.levelApprox) panel.levelSource?.let { add(levelSourceLabel(strings, it)) }
            panel.occurrences?.let { add("${strings.occurrences} $it") }
        }
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                meta.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        panel.gloss?.takeIf { it.isNotBlank() }?.let { gloss ->
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        strings.sentenceMeaning,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        gloss,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        // A word can have a Chinese sense but no English one (or the reverse).
        // If the selected meaning language has nothing, fall back to the other
        // language instead of rendering an apparently empty panel.
        val showZhSenses = showZh && hasZh
        val showEnSenses = showEn && hasEn
        if (showZhSenses) {
            SectionLabel(strings.definitionsZh)
            ZhSenseList(zhBase)
            if (zhExtra.isNotEmpty()) {
                OtherFormLabel(strings.otherFormSenses)
                ZhSenseList(zhExtra, muted = true)
            }
        }
        if (showEnSenses) {
            SectionLabel(strings.definitionsEn)
            EnSenseList(enBase)
            if (enExtra.isNotEmpty()) {
                OtherFormLabel(strings.otherFormSenses)
                EnSenseList(enExtra)
            }
        }
        if (!showZhSenses && !showEnSenses && panel.senses.isNotEmpty()) {
            val fallbackZh = hasZh
            SectionLabel(if (fallbackZh) strings.definitionsZh else strings.definitionsEn)
            Text(
                strings.definitionLanguageFallback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            if (fallbackZh) {
                ZhSenseList(zhBase)
                if (zhExtra.isNotEmpty()) {
                    OtherFormLabel(strings.otherFormSenses)
                    ZhSenseList(zhExtra, muted = true)
                }
            } else {
                EnSenseList(enBase)
                if (enExtra.isNotEmpty()) {
                    OtherFormLabel(strings.otherFormSenses)
                    EnSenseList(enExtra)
                }
            }
        }
        if (panel.senses.isEmpty()) {
            Text(
                panel.emptyLabel ?: strings.noDefinition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        panel.exampleEn?.let { example ->
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        strings.example,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(example, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                    panel.exampleZh?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (panel.gloss.isNullOrBlank() && panel.senses.isEmpty() && !panel.sentenceZh.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            SectionLabel(strings.sentenceMeaning)
            Text(
                panel.sentenceZh.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onToggleCard, modifier = Modifier.weight(1f)) {
                Text(if (panel.collected) strings.removeCard else strings.collectCard)
            }
            TextButton(onClick = onClose) { Text(strings.close) }
        }
    }
}

@Composable
internal fun ZhSenseList(senses: List<SenseUi>, muted: Boolean = false) {
    senses.forEach { sense ->
        Row(Modifier.padding(bottom = 4.dp)) {
            sense.pos?.takeIf { it.isNotBlank() }?.let {
                // Reserve a column wide enough for `conj.` / `abbr.` and let it
                // grow with the font scale instead of wrapping or clipping.
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (muted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.widthIn(min = 36.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                sense.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = if (muted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun EnSenseList(senses: List<SenseUi>) {
    senses.forEach { sense ->
        Text(
            listOfNotNull(sense.pos?.takeIf { it.isNotBlank() }, sense.definition)
                .joinToString(" "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
internal fun OtherFormLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}
