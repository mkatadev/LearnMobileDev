package pl.prodevcode.learnmobiledev.presentation.concurrency

import pl.prodevcode.learnmobiledev.core.mvi.Reducer
import pl.prodevcode.learnmobiledev.domain.model.ScenarioResult

/**
 * Reducer for the lab.
 *
 * The key detail: `running` is a **set**, not a Boolean. That lets several scenarios run
 * at once while the UI still knows exactly which one is in flight. A single `isLoading`
 * flag would break the moment a second scenario started.
 */
val ConcurrencyReducer = Reducer<ConcurrencyState, ConcurrencyIntent> { state, intent ->
    when (intent) {
        is ConcurrencyIntent.Ui.ScreenOpened -> state
        is ConcurrencyIntent.Ui.RetryClicked -> state

        is ConcurrencyIntent.Ui.RunAllClicked -> state

        is ConcurrencyIntent.Ui.ScenarioClicked -> state.copy(
            expandedScenarioId =
                if (state.expandedScenarioId == intent.scenarioId) null else intent.scenarioId,
        )

        // Clicking a scenario that is already running does nothing — double-tap protection
        // expressed in pure logic rather than through `enabled = false` in Compose.
        is ConcurrencyIntent.Ui.RunClicked -> state

        is ConcurrencyIntent.Ui.ResultsCleared -> state.copy(results = emptyMap())

        is ConcurrencyIntent.Internal.LoadStarted -> state.copy(isLoading = true, error = null)

        is ConcurrencyIntent.Internal.ScenariosLoaded -> state.copy(
            scenarios = intent.scenarios,
            isLoading = false,
            error = null,
        )

        is ConcurrencyIntent.Internal.LoadFailed -> state.copy(
            isLoading = false,
            error = intent.message,
        )

        is ConcurrencyIntent.Internal.RunStarted -> state.copy(
            running = state.running + intent.scenarioId,
            results = state.results - intent.scenarioId,
        )

        is ConcurrencyIntent.Internal.RunFinished -> state.copy(
            running = state.running - intent.result.scenarioId,
            results = state.results + (intent.result.scenarioId to intent.result),
        )

        is ConcurrencyIntent.Internal.RunFailed -> state.copy(
            running = state.running - intent.scenarioId,
            results = state.results + (
                intent.scenarioId to ScenarioResult(
                    scenarioId = intent.scenarioId,
                    expected = "-",
                    actual = "-",
                    passed = false,
                )
                ),
        )
    }
}
