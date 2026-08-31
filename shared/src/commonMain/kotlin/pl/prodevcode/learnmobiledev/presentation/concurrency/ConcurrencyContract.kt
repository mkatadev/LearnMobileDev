package pl.prodevcode.learnmobiledev.presentation.concurrency

import pl.prodevcode.learnmobiledev.core.mvi.MviEffect
import pl.prodevcode.learnmobiledev.core.mvi.MviIntent
import pl.prodevcode.learnmobiledev.core.mvi.MviState
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.domain.model.ConcurrencyScenario
import pl.prodevcode.learnmobiledev.domain.model.ScenarioResult

/**
 * The concurrency lab screen.
 *
 * The third MVI example in the project. It shows the pattern where **several independent
 * operations** run at once and the state has to track each of them separately.
 */
data class ConcurrencyState(
    val scenarios: List<ConcurrencyScenario> = emptyList(),
    val results: Map<String, ScenarioResult> = emptyMap(),
    val running: Set<String> = emptySet(),
    val expandedScenarioId: String? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
) : MviState {

    val isBusy: Boolean
        get() = running.isNotEmpty()

    val passedCount: Int
        get() = results.values.count { it.passed }

    val finishedCount: Int
        get() = results.size

    fun isRunning(scenarioId: String): Boolean = scenarioId in running

    fun resultOf(scenarioId: String): ScenarioResult? = results[scenarioId]
}

sealed interface ConcurrencyIntent : MviIntent {

    sealed interface Ui : ConcurrencyIntent {
        data object ScreenOpened : Ui
        data object RetryClicked : Ui

        /** The content language changed, so anything cached is now in the wrong locale. */
        data object ContentInvalidated : Ui
        data class ScenarioClicked(val scenarioId: String) : Ui
        data class RunClicked(val scenarioId: String) : Ui
        data object RunAllClicked : Ui
        data object ResultsCleared : Ui
    }

    sealed interface Internal : ConcurrencyIntent {
        data object LoadStarted : Internal
        data class ScenariosLoaded(val scenarios: List<ConcurrencyScenario>) : Internal
        data class LoadFailed(val message: UiText) : Internal
        data class RunStarted(val scenarioId: String) : Internal
        data class RunFinished(val result: ScenarioResult) : Internal
        data class RunFailed(val scenarioId: String, val message: UiText) : Internal
    }
}

sealed interface ConcurrencyEffect : MviEffect {
    data class ShowMessage(val text: UiText) : ConcurrencyEffect
}
