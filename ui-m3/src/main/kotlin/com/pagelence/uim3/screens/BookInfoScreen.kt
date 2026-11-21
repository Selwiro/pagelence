package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.BookCover
import com.pagelence.uim3.components.SectionLabel
import com.pagelence.uim3.components.rememberCoverPainter
import com.pagelence.uim3.model.BookDetailUi

/**
 * The info page behind "关于此书": blurb first, then the package facts (author,
 * word count, level, creator, publication and licences). Modelled on the
 * reference "详细信息" page, without the family-library row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoScreen(
    strings: Strings,
    state: BookDetailUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BookCover(
                            title = state.coverTitle,
                            author = state.author,
                            seed = state.bookId,
                            monogram = state.monogram,
                            cover = rememberCoverPainter(state.coverPath),
                            modifier = Modifier.width(36.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                state.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                strings.bookInfoTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        val words = wordsText(state.wordCount)
        val publication = listOfNotNull(
            state.publisher?.takeIf { it.isNotBlank() },
            state.edition?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        val licence = listOfNotNull(
            state.sourceLicense?.let { "${strings.bookLicenseSource}：$it" },
            state.creatorLicense?.let { "${strings.bookLicenseCreator}：$it" },
            state.sourceLicenseNote?.takeIf { it.isNotBlank() },
        ).joinToString("\n")
        val sources = when {
            state.sourceNames.isNotEmpty() -> state.sourceNames.joinToString("、")
            state.builtIn -> strings.bookSourceBuiltIn
            else -> null
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "blurb") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(strings.bookIntro)
                    Text(
                        state.description?.takeIf { it.isNotBlank() } ?: strings.bookInfoEmpty,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            item(key = "divider") { HorizontalDivider() }

            item(key = "facts-header") {
                SectionLabel(strings.bookInfo)
            }

            item(key = "facts") {
                Column {
                    state.author?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(strings.bookAuthor, it)
                    }
                    if (state.wordCount > 0) InfoRow(strings.bookWordCount, words)
                    state.level?.let { InfoRow(strings.bookLevelLabel, it) }
                    state.category?.let { InfoRow(strings.bookCategory, it) }
                    state.creator?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(strings.bookCreator, it)
                    }
                    if (publication.isNotBlank()) InfoRow(strings.bookPublish, publication)
                    if (licence.isNotBlank()) InfoRow(strings.bookLicense, licence)
                    InfoRow(strings.bookLanguage, strings.bookLanguageEn)
                    if (state.chapters.isNotEmpty()) {
                        InfoRow(strings.bookChaptersCount, state.chapters.size.toString())
                        InfoRow(strings.bookUnitsCount, state.unitCount.toString())
                    }
                    sources?.let { InfoRow(strings.bookSources, it) }
                }
            }

            if (state.tags.isNotEmpty()) {
                item(key = "info-tags") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(strings.bookTags)
                        Text(
                            state.tags.joinToString("、"),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    // Short values sit on the right like the reference; long or multi-line
    // values (licences, publication) read better stacked under the label.
    if (value.length > 28 || value.contains('\n')) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/** `2.5 万词` for long books, `15 词` for short ones. */
private fun wordsText(words: Int): String = when {
    words >= 10_000 -> {
        val wan = Math.round(words / 1_000.0).toDouble() / 10.0
        val text = if (wan == Math.floor(wan)) wan.toInt().toString() else String.format("%.1f", wan)
        "$text 万词"
    }
    words > 0 -> "$words 词"
    else -> ""
}
