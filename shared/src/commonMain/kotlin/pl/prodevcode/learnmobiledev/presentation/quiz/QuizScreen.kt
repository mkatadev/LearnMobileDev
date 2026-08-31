package pl.prodevcode.learnmobiledev.presentation.quiz

import pl.prodevcode.learnmobiledev.core.ui.localized
import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.prodevcode.learnmobiledev.presentation.theme.Spacing
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.core.ui.resolve
import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory

/**
 * Quiz screen.
 *
 * Teaching rule: **after every answer we show the explanation**, and for an incorrect
 * answer we also show the correct one. "Wrong" by itself teaches nothing.
 */
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.dispatch(QuizIntent.Ui.ScreenOpened)
    }

    var pendingMessage by remember { mutableStateOf<UiText?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QuizEffect.ShowMessage -> pendingMessage = effect.text
            }
        }
    }

    pendingMessage?.let { message ->
        val text = message.resolve()
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(text)
            pendingMessage = null
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.phase == QuizPhase.Setup -> SetupSection(
                    state = state,
                    onIntent = viewModel::dispatch,
                )

                state.phase == QuizPhase.InProgress -> QuestionSection(
                    state = state,
                    onIntent = viewModel::dispatch,
                )

                else -> SummarySection(state = state, onIntent = viewModel::dispatch)
            }

            if (state.isExitDialogVisible) {
                ExitConfirmationDialog(
                    answered = state.answers.size,
                    onConfirm = { viewModel.dispatch(QuizIntent.Ui.ExitConfirmed) },
                    onDismiss = { viewModel.dispatch(QuizIntent.Ui.ExitDismissed) },
                )
            }
        }
    }
}

/**
 * Confirmation for interrupting the test.
 *
 * Dialog visibility comes from state, not from a local `remember` — that lets it survive
 * screen rotation and remain visible on the timeline.
 */
@Composable
private fun ExitConfirmationDialog(
    answered: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(AppString.QuizExitTitle)) },
        text = {
            Text(
                if (answered > 0) {
                    localized(AppString.QuizExitMessageProgress, answered)
                } else {
                    localized(AppString.QuizExitMessageSingle)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(localized(AppString.QuizExitConfirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(localized(AppString.QuizExitDismiss)) }
        },
    )
}

@Composable
private fun SetupSection(state: QuizState, onIntent: (QuizIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = localized(AppString.QuizTitle),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = localized(AppString.QuizSubtitle),
            style = MaterialTheme.typography.bodyMedium,
        )

        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error.resolve(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = localized(AppString.QuizPickCategories),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = if (state.selectedCategories.isEmpty()) {
                localized(AppString.QuizAllCategories)
            } else {
                localized(AppString.QuizSelectedCount, state.selectedCategories.size)
            },
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(8.dp))
        CategoryChips(
            categories = state.availableCategories,
            selected = state.selectedCategories,
            onToggle = { onIntent(QuizIntent.Ui.CategoryToggled(it)) },
        )

        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = { onIntent(QuizIntent.Ui.QuizStarted) }) {
                Text(localized(AppString.QuizStart))
            }
            if (state.selectedCategories.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onIntent(QuizIntent.Ui.AllCategoriesSelected) }) {
                    Text(localized(AppString.QuizClearFilter))
                }
            }
        }
    }
}

/**
 * Wrapping layout: chips arrange themselves according to the available width.
 *
 * Rigidly splitting rows into N items created uneven spacing and broke longer labels —
 * FlowRow adapts to the screen size and text length.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<QuizCategory>,
    selected: Set<QuizCategory>,
    onToggle: (QuizCategory) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category in selected,
                onClick = { onToggle(category) },
                label = {
                    Text(
                        text = localized(category.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun QuestionSection(state: QuizState, onIntent: (QuizIntent) -> Unit) {
    val question = state.currentQuestion ?: return

    LazyColumn(contentPadding = PaddingValues(top = Spacing.screenTop, bottom = Spacing.listBottom)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onIntent(QuizIntent.Ui.ExitRequested) }) {
                    Text(localized(AppString.QuizBackToList))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = localized(
                        AppString.QuizProgress,
                        state.questionNumber,
                        state.questions.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = { (state.currentIndex + 1f) / state.questions.size },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = localized(AppString.QuizCorrectCount, state.correctCount),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = localized(
                    AppString.QuizMeta,
                    localized(question.category.labelRes),
                    localized(question.difficulty.labelRes),
                ),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(12.dp))
            Text(question.text, style = MaterialTheme.typography.titleMedium)

            question.code?.let { code ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        softWrap = false,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        itemsIndexedOptions(question, state, onIntent)

        if (state.isAnswerRevealed) {
            item {
                Spacer(Modifier.height(12.dp))
                ExplanationCard(question = question, isCorrect = state.isCurrentAnswerCorrect)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onIntent(QuizIntent.Ui.NextClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        localized(
                            if (state.isLastQuestion) AppString.QuizFinish else AppString.QuizNext,
                        ),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Extracted so `QuestionSection` stays readable. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedOptions(
    question: Question,
    state: QuizState,
    onIntent: (QuizIntent) -> Unit,
) {
    items(question.options.withIndex().toList()) { (index, option) ->
        val isSelected = state.selectedAnswerIndex == index
        val isCorrect = index == question.correctIndex
        val revealed = state.isAnswerRevealed

        val container = when {
            revealed && isCorrect -> MaterialTheme.colorScheme.primaryContainer
            revealed && isSelected -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surface
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(enabled = !revealed) {
                    onIntent(QuizIntent.Ui.AnswerSelected(index))
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        revealed && isCorrect -> localized(AppString.MarkerCorrect)
                        revealed && isSelected -> localized(AppString.MarkerIncorrect)
                        else -> localized(AppString.MarkerOption, 'A' + index)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(option, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ExplanationCard(question: Question, isCorrect: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Text(
                text = localized(
                    if (isCorrect) AppString.QuizCorrect else AppString.QuizWrong,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!isCorrect) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = localized(AppString.QuizCorrectAnswer, question.correctAnswer),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SummarySection(state: QuizState, onIntent: (QuizIntent) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(top = Spacing.screenTop, bottom = Spacing.listBottom)) {
        item {
            TextButton(onClick = { onIntent(QuizIntent.Ui.ExitConfirmed) }) {
                Text(localized(AppString.QuizBackToCategories))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = localized(
                    AppString.QuizScore,
                    state.correctCount,
                    state.answers.size,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = localized(
                    AppString.QuizScorePercent,
                    state.scorePercent,
                    localized(verdict(state.scorePercent)),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = { onIntent(QuizIntent.Ui.RestartClicked) }) {
                    Text(localized(AppString.QuizNewTest))
                }
                if (state.mistakes.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onIntent(QuizIntent.Ui.RetryMistakesClicked) }) {
                        Text(localized(AppString.QuizRetryMistakes, state.mistakes.size))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            if (state.mistakes.isNotEmpty()) {
                Text(
                    text = localized(AppString.QuizMistakesTitle),
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider()
            }
        }

        items(state.mistakes) { answered ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(
                    text = localized(answered.question.category.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(answered.question.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = localized(
                        AppString.QuizCorrectLabel,
                        answered.question.correctAnswer,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = answered.question.explanation,
                    style = MaterialTheme.typography.bodySmall,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun verdict(percent: Int): AppString = when {
    percent >= 90 -> AppString.QuizVerdictExcellent
    percent >= 75 -> AppString.QuizVerdictGood
    percent >= 50 -> AppString.QuizVerdictAverage
    else -> AppString.QuizVerdictWeak
}
