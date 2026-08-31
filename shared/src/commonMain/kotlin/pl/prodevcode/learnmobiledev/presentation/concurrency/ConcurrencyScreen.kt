package pl.prodevcode.learnmobiledev.presentation.concurrency

import pl.prodevcode.learnmobiledev.core.ui.localized
import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import pl.prodevcode.learnmobiledev.presentation.theme.Spacing
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.core.ui.resolve
import pl.prodevcode.learnmobiledev.domain.model.ConcurrencyScenario
import pl.prodevcode.learnmobiledev.domain.model.ScenarioResult

/**
 * "Sync" screen — runs concurrency scenarios and shows the PASS/FAIL result live.
 *
 * Scenarios marked as bug demonstrations are **supposed** to fail. A red result is
 * evidence here, not a crash.
 */
@Composable
fun ConcurrencyScreen(
    viewModel: ConcurrencyViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.dispatch(ConcurrencyIntent.Ui.ScreenOpened)
    }

    var pendingMessage by remember { mutableStateOf<UiText?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ConcurrencyEffect.ShowMessage -> pendingMessage = effect.text
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            Header(
                state = state,
                onRunAll = { viewModel.dispatch(ConcurrencyIntent.Ui.RunAllClicked) },
                onClear = { viewModel.dispatch(ConcurrencyIntent.Ui.ResultsCleared) },
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                state.error != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.listBottom),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error!!.resolve(), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = { viewModel.dispatch(ConcurrencyIntent.Ui.RetryClicked) },
                    ) {
                        Text(localized(AppString.ActionRetry))
                    }
                }

                else -> ScenarioList(state = state, onIntent = viewModel::dispatch)
            }
        }
    }
}

@Composable
private fun ScenarioList(
    state: ConcurrencyState,
    onIntent: (ConcurrencyIntent) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.listBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
    ) {
        items(items = state.scenarios, key = { it.id }) { scenario ->
            ScenarioCard(
                scenario = scenario,
                result = state.resultOf(scenario.id),
                isRunning = state.isRunning(scenario.id),
                isExpanded = state.expandedScenarioId == scenario.id,
                onClick = { onIntent(ConcurrencyIntent.Ui.ScenarioClicked(scenario.id)) },
                onRun = { onIntent(ConcurrencyIntent.Ui.RunClicked(scenario.id)) },
            )
        }
    }
}

@Composable
private fun Header(
    state: ConcurrencyState,
    onRunAll: () -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.screenTop, bottom = Spacing.itemSpacing)) {
        Text(
            text = localized(AppString.SyncTitle),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = localized(AppString.SyncSubtitle),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onRunAll, enabled = !state.isBusy) {
                Text(
                    localized(
                        if (state.isBusy) AppString.SyncRunning else AppString.SyncRunAll,
                    ),
                )
            }
            Spacer(Modifier.width(8.dp))
            if (state.finishedCount > 0) {
                Text(
                    text = localized(
                        AppString.SyncPassedSummary,
                        state.passedCount,
                        state.finishedCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
                TextButton(onClick = onClear) { Text(localized(AppString.ActionClear)) }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: ConcurrencyScenario,
    result: ScenarioResult?,
    isRunning: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onRun: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (scenario.demonstratesBug) {
                            Text(localized(AppString.MarkerBug), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = scenario.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(scenario.description, style = MaterialTheme.typography.bodySmall)
                }
                StatusBadge(result = result, isRunning = isRunning)
            }

            Spacer(Modifier.padding(top = 8.dp))
            Text(
                text = localized(AppString.SyncExpectation, scenario.expectation),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )

            result?.let { ResultPanel(it) }

            if (isExpanded) {
                Spacer(Modifier.padding(top = 8.dp))
                HorizontalDivider()
                Spacer(Modifier.padding(top = 8.dp))
                Text(scenario.explanation, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onRun, enabled = !isRunning) {
                    Text(
                        localized(
                            if (isRunning) AppString.SyncRunInProgress else AppString.SyncRun,
                        ),
                    )
                }
                TextButton(onClick = onClick) {
                    Text(
                        localized(
                            if (isExpanded) AppString.SyncHideExplanation else AppString.SyncExplain,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(result: ScenarioResult?, isRunning: Boolean) {
    when {
        isRunning -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
        result == null -> Text(
            text = localized(AppString.MarkerNone),
            style = MaterialTheme.typography.titleMedium,
        )
        result.passed -> Text(
            text = localized(AppString.SyncStatusPass),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        else -> Text(
            text = localized(AppString.SyncStatusFail),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ResultPanel(result: ScenarioResult) {
    val container = if (result.passed) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val onContainer = if (result.passed) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
        ) {
            Text(
                text = localized(AppString.SyncExpected, result.expected),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = onContainer,
            )
            Text(
                text = localized(AppString.SyncActual, result.actual),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = onContainer,
            )
            result.log.forEach { line ->
                Text(
                    text = localized(AppString.MarkerLogLine, line),
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer,
                )
            }
        }
    }
}
