package pl.prodevcode.learnmobiledev.presentation.infographics

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.mvi.MviStore
import pl.prodevcode.learnmobiledev.core.ui.AppString
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.core.ui.asUiText
import pl.prodevcode.learnmobiledev.domain.repository.InfographicsUnavailableException
import pl.prodevcode.learnmobiledev.domain.usecase.GetInfographicsUseCase

/**
 * Store for the infographics screen.
 *
 * Loading is skipped when the pictures are already in the state: each one is about a
 * megabyte, and re-fetching them every time the tab is re-entered would waste the round
 * trip for content that cannot have changed.
 */
class InfographicsViewModel(
    private val getInfographics: GetInfographicsUseCase,
) : MviStore<InfographicsState, InfographicsIntent, InfographicsEffect>(
    initialState = InfographicsState(),
    reducer = InfographicsReducer,
) {

    override fun onIntentProcessed(
        intent: InfographicsIntent,
        before: InfographicsState,
        after: InfographicsState,
    ) {
        when (intent) {
            is InfographicsIntent.Ui.ScreenOpened ->
                if (after.infographics.isEmpty()) load()

            is InfographicsIntent.Ui.RetryClicked -> load()

            is InfographicsIntent.Internal.LoadFailed ->
                emitEffect(InfographicsEffect.ShowMessage(intent.message))

            else -> Unit
        }
    }

    private fun load() {
        dispatch(InfographicsIntent.Internal.LoadStarted)
        viewModelScope.launch {
            try {
                dispatch(InfographicsIntent.Internal.LoadSucceeded(getInfographics()))
            } catch (cancellation: CancellationException) {
                throw cancellation // cancellation is NOT a domain failure — always rethrow
            } catch (error: Exception) {
                dispatch(InfographicsIntent.Internal.LoadFailed(error.toUiText()))
            }
        }
    }

    /** The exception *type* decides what the user reads, never the technical message. */
    private fun Exception.toUiText(): UiText = when (this) {
        is InfographicsUnavailableException -> AppString.ErrorInfographicsUnavailable.asUiText()
        else -> AppString.ErrorUnknown.asUiText()
    }
}
