package com.pagelence.uim3.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pagelence.core.assessment.AssessmentMode
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.model.AssessmentQuestionUi
import com.pagelence.uim3.model.AssessmentResultUi
import com.pagelence.uim3.model.AssessmentUiState

/** One continuous 5-character underline, fixed width regardless of the answer. */
private fun promptText(prompt: String): AnnotatedString = buildAnnotatedString {
    val parts = prompt.replace("___", "{{blank}}").split("{{blank}}")
    parts.forEachIndexed { index, part ->
        append(part)
        if (index < parts.lastIndex) {
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append("\u00A0".repeat(5))
            }
        }
    }
}

/**
 * Placement test: intro with the two modes, one question per page, then the
 * result page with accept/keep. No correctness feedback while answering, so the
 * score measures reading rather than trial and error.
 */
@Composable
fun AssessmentScreen(
    strings: Strings,
    state: AssessmentUiState,
    onBack: () -> Unit,
    onDismissExit: () -> Unit,
    onConfirmExit: () -> Unit,
    onStart: (AssessmentMode) -> Unit,
    onSelectOption: (Int) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onAccept: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = state.result
    val question = state.current
    Scaffold(
        modifier = modifier,
        topBar = {
            PageTopBar(
                title = strings.assessment,
                subtitle = if (state.active && question != null) {
                    strings.assessmentQuestion.format(state.index + 1, state.questions.size)
                } else {
                    null
                },
                onBack = onBack,
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when {
            result != null -> ResultPage(strings, result, onAccept, onKeep, Modifier.padding(padding))
            state.active && question != null -> QuestionPage(
                strings = strings,
                state = state,
                question = question,
                onSelectOption = onSelectOption,
                onSkip = onSkip,
                onNext = onNext,
                onPrevious = onPrevious,
                modifier = Modifier.padding(padding),
            )
            else -> IntroPage(strings, onStart, Modifier.padding(padding))
        }
    }

    if (state.exitConfirm) {
        AlertDialog(
            onDismissRequest = onDismissExit,
            title = { Text(strings.quizExitTitle) },
            text = { Text(strings.quizExitBody) },
            confirmButton = {
                TextButton(onClick = onConfirmExit) { Text(strings.quizExitConfirm) }
            },
            dismissButton = {
                TextButton(onClick = onDismissExit) { Text(strings.close) }
            },
        )
    }
}

@Composable
private fun IntroPage(
    strings: Strings,
    onStart: (AssessmentMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(strings.assessmentIntro, style = MaterialTheme.typography.bodyLarge)
        ModeCard(
            title = strings.assessmentQuick,
            meta = strings.assessmentQuickMeta,
            body = strings.assessmentQuickBody,
            startLabel = strings.assessmentStart,
            enabled = true,
            onStart = { onStart(AssessmentMode.QUICK) },
        )
        ModeCard(
            title = strings.assessmentFull,
            meta = strings.assessmentFullMeta,
            body = strings.assessmentFullBody,
            startLabel = strings.assessmentStart,
            enabled = true,
            onStart = { onStart(AssessmentMode.FULL) },
        )
        Text(
            strings.assessmentHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    meta: String,
    body: String,
    startLabel: String,
    enabled: Boolean,
    onStart: () -> Unit,
) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                MetaChip(meta)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onStart, enabled = enabled) { Text(startLabel) }
        }
    }
}

@Composable
private fun QuestionPage(
    strings: Strings,
    state: AssessmentUiState,
    question: AssessmentQuestionUi,
    onSelectOption: (Int) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val answered = question.selected != null || question.skipped
    val last = state.index == state.questions.lastIndex
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        LinearProgressIndicator(
            progress = { state.answeredCount.toFloat() / state.questions.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetaChip(question.level)
            Spacer(Modifier.width(8.dp))
            MetaChip(question.skillLabel)
        }
        question.passageText?.let { passage ->
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                ) {
                    question.passageTitle?.let {
                        Text(it, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(passage, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(promptText(question.prompt), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        question.options.forEachIndexed { optionIndex, option ->
            val selected = question.selected == optionIndex
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .selectable(selected = selected, onClick = { onSelectOption(optionIndex) }),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = { onSelectOption(optionIndex) })
                    Text(option, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (question.skipped) strings.assessmentSkipped else strings.assessmentSkipHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPrevious, enabled = state.index > 0) {
                Text(strings.previousPage)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip) { Text(strings.assessmentSkip) }
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onNext, enabled = answered, modifier = Modifier.fillMaxWidth()) {
            Text(if (last) strings.assessmentSeeResult else strings.assessmentNext)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ResultPage(
    strings: Strings,
    result: AssessmentResultUi,
    onAccept: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.assessmentResult, style = MaterialTheme.typography.headlineSmall)
        Text(strings.assessmentScore.format(result.correct, result.total), style = MaterialTheme.typography.bodyLarge)
        Text(
            "${result.scorePercent}%",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                MetaChip(strings.assessmentReference)
                Spacer(Modifier.height(8.dp))
                Text(result.band, style = MaterialTheme.typography.displaySmall)
                if (!result.singleLevel) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.assessmentSingle.format(result.suggested),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        result.skills.forEach { skill ->
            Column {
                Row {
                    Text(skill.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("${skill.percent}%", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { skill.percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            strings.assessmentHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) { Text(strings.assessmentAccept) }
        OutlinedCard(
            onClick = onKeep,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                strings.assessmentKeep,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(14.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}
