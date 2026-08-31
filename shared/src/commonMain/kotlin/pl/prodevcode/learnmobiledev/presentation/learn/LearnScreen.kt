package pl.prodevcode.learnmobiledev.presentation.learn

import pl.prodevcode.learnmobiledev.core.ui.localized
import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import pl.prodevcode.learnmobiledev.presentation.theme.Spacing
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.ui.resolve
import pl.prodevcode.learnmobiledev.domain.model.Block
import pl.prodevcode.learnmobiledev.domain.model.Lesson

/**
 * Course screen. The UI has only two jobs: render `LearnState` and send intents.
 *
 * Rendering content is a pure `Block -> Composable` mapping, so adding a new block type
 * does not require touching screen logic.
 */
@Composable
fun LearnScreen(
    viewModel: LearnViewModel,
    themeLabel: AppString,
    languageLabel: AppString,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.dispatch(LearnIntent.Ui.ScreenOpened)
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = Spacing.screenHorizontal)) {
        ProgressHeader(
            state = state,
            themeLabel = themeLabel,
            languageLabel = languageLabel,
            onToggleTheme = onToggleTheme,
            onToggleLanguage = onToggleLanguage,
            onReset = { viewModel.dispatch(LearnIntent.Ui.ProgressReset) },
        )

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.error!!.resolve(), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { viewModel.dispatch(LearnIntent.Ui.RetryClicked) }) {
                    Text(localized(AppString.ActionRetry))
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = Spacing.listBottom),
                verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
            ) {
                items(items = state.lessons, key = { it.id }) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        isOpen = state.openLessonId == lesson.id,
                        isCompleted = state.isCompleted(lesson.id),
                        onClick = { viewModel.dispatch(LearnIntent.Ui.LessonClicked(lesson.id)) },
                        onToggleCompleted = {
                            viewModel.dispatch(LearnIntent.Ui.LessonCompletionToggled(lesson.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressHeader(
    state: LearnState,
    themeLabel: AppString,
    languageLabel: AppString,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.screenTop, bottom = Spacing.itemSpacing)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localized(AppString.LearnTitle),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = localized(AppString.LearnSubtitle),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Spacing.small))
            val languageDescription = localized(AppString.LanguageToggleDescription)
            TextButton(
                onClick = onToggleLanguage,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.semantics { contentDescription = languageDescription },
            ) {
                Text(
                    text = localized(languageLabel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            // Use only the symbol instead of a label — the full mode name took enough
            // space that the screen title was truncated with an ellipsis.
            val themeDescription = localized(AppString.ThemeToggleDescription)
            TextButton(
                onClick = onToggleTheme,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                // The symbol itself tells a screen reader nothing — a description is required.
                modifier = Modifier.semantics { contentDescription = themeDescription },
            ) {
                Text(
                    text = localized(themeLabel),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Spacer(Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { state.progressPercent / 100f },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = localized(
                    AppString.LearnProgress,
                    state.completedLessonIds.size,
                    state.lessons.size,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            if (state.completedLessonIds.isNotEmpty()) {
                TextButton(onClick = onReset) { Text(localized(AppString.ActionReset)) }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    isOpen: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(lesson.summary, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = if (isOpen) "▾" else "▸",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (isOpen) {
                Spacer(Modifier.padding(top = 10.dp))
                HorizontalDivider()
                Spacer(Modifier.padding(top = 10.dp))
                lesson.blocks.forEach { block ->
                    BlockView(block)
                    Spacer(Modifier.padding(top = 6.dp))
                }
                TextButton(onClick = onToggleCompleted) {
                    Text(
                        localized(
                            if (isCompleted) AppString.LearnMarkedDone else AppString.LearnMarkDone,
                        ),
                    )
                }
            } else if (isCompleted) {
                Text(
                    text = localized(AppString.LearnDoneBadge),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Mapping the content model to UI. One `when` — zero business logic. */
@Composable
private fun BlockView(block: Block) {
    when (block) {
        is Block.Paragraph -> Text(
            text = block.text.withBoldMarkers(),
            style = MaterialTheme.typography.bodyMedium,
        )

        is Block.Subheading -> Text(
            text = block.text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp),
        )

        is Block.Bullets -> Column {
            block.items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(localized(AppString.MarkerBullet), style = MaterialTheme.typography.bodyMedium)
                    Text(item.withBoldMarkers(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is Block.Code -> Column {
            block.caption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = block.code,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp),
                )
            }
        }

        is Block.Rule -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(10.dp),
        ) {
            Text(localized(AppString.MarkerRule), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = block.text.withBoldMarkers(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        is Block.Table -> Column(modifier = Modifier.fillMaxWidth()) {
            TableRow(cells = block.headers, isHeader = true)
            HorizontalDivider()
            block.rows.forEach { row ->
                TableRow(cells = row, isHeader = false)
                HorizontalDivider()
            }
        }

        is Block.Exercise -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(10.dp),
        ) {
            Text(
                text = localized(
                    AppString.QuizExerciseLabel,
                    localized(AppString.LearnExercisePrefix),
                    block.number,
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, isHeader: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        cells.forEach { cell ->
            Text(
                text = cell,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f).padding(end = 6.dp),
            )
        }
    }
}

/**
 * Minimal "markdown": fragments in `**asterisks**` are rendered in bold.
 * Intentionally a few lines instead of a library — in a course, dependency readability matters.
 */
@Composable
private fun String.withBoldMarkers() = buildAnnotatedString {
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    var rest = this@withBoldMarkers
    while (true) {
        val open = rest.indexOf("**")
        val close = if (open == -1) -1 else rest.indexOf("**", startIndex = open + 2)
        if (open == -1 || close == -1) {
            append(rest)
            return@buildAnnotatedString
        }
        append(rest.substring(0, open))
        withStyle(bold) { append(rest.substring(open + 2, close)) }
        rest = rest.substring(close + 2)
    }
}
