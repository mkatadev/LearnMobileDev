package pl.prodevcode.learnmobiledev.presentation.concurrency

import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.mvi.MviStore
import pl.prodevcode.learnmobiledev.core.ui.asUiText
import pl.prodevcode.learnmobiledev.domain.usecase.GetConcurrencyScenariosUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.RunConcurrencyScenarioUseCase

/**
 * Store for the concurrency lab.
 *
 * Note that scenarios run in **separate coroutines** and the intent loop never awaits
 * them. If `onIntentProcessed` were suspending and blocked the loop, one long-running
 * task would freeze the whole screen, including expanding a description.
 */
class ConcurrencyViewModel(
    private val getScenarios: GetConcurrencyScenariosUseCase,
    private val runScenario: RunConcurrencyScenarioUseCase,
) : MviStore<ConcurrencyState, ConcurrencyIntent, ConcurrencyEffect>(
    initialState = ConcurrencyState(),
    reducer = ConcurrencyReducer,
) {

    override fun onIntentProcessed(
        intent: ConcurrencyIntent,
        before: ConcurrencyState,
        after: ConcurrencyState,
    ) {
        when (intent) {
            is ConcurrencyIntent.Ui.ScreenOpened ->
                if (after.scenarios.isEmpty()) loadScenarios()

            is ConcurrencyIntent.Ui.RetryClicked -> loadScenarios()

            is ConcurrencyIntent.Ui.RunClicked ->
                execute(intent.scenarioId, after)

            is ConcurrencyIntent.Ui.RunAllClicked ->
                after.scenarios.forEach { execute(it.id, after) }

            is ConcurrencyIntent.Internal.RunFinished ->
                if (!intent.result.passed) {
                    emitEffect(
                        ConcurrencyEffect.ShowMessage(AppString.SyncFailureHint.asUiText()),
                    )
                }

            else -> Unit
        }
    }

    private fun loadScenarios() {
        dispatch(ConcurrencyIntent.Internal.LoadStarted)
        viewModelScope.launch {
            try {
                dispatch(ConcurrencyIntent.Internal.ScenariosLoaded(getScenarios()))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                dispatch(
                    ConcurrencyIntent.Internal.LoadFailed(
                        AppString.ErrorScenariosUnavailable.asUiText(),
                    ),
                )
            }
        }
    }

    private fun execute(scenarioId: String, state: ConcurrencyState) {
        if (state.isRunning(scenarioId)) return

        dispatch(ConcurrencyIntent.Internal.RunStarted(scenarioId))
        viewModelScope.launch {
            try {
                dispatch(ConcurrencyIntent.Internal.RunFinished(runScenario(scenarioId)))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                dispatch(
                    ConcurrencyIntent.Internal.RunFailed(
                        scenarioId = scenarioId,
                        message = AppString.ErrorScenarioFailed.asUiText(),
                    ),
                )
            }
        }
    }
}
