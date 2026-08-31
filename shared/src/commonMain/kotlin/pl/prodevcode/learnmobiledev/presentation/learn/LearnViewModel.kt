package pl.prodevcode.learnmobiledev.presentation.learn

import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.mvi.MviStore
import pl.prodevcode.learnmobiledev.core.ui.asUiText
import pl.prodevcode.learnmobiledev.domain.usecase.GetLessonsUseCase

/**
 * Store for the learn screen. Same pattern as `UsersViewModel`, only simpler:
 * a single I/O entry point and no one-off effects.
 *
 * Compare the two files — the same base class serves a trivial screen and one with
 * debounce, cancellation and rollback. That is the value of a shared core.
 */
class LearnViewModel(
    private val getLessons: GetLessonsUseCase,
) : MviStore<LearnState, LearnIntent, LearnEffect>(
    initialState = LearnState(),
    reducer = LearnReducer,
) {

    override fun onIntentProcessed(
        intent: LearnIntent,
        before: LearnState,
        after: LearnState,
    ) {
        when (intent) {
            is LearnIntent.Ui.ScreenOpened -> if (after.lessons.isEmpty()) loadLessons()
            is LearnIntent.Ui.RetryClicked -> loadLessons()
            else -> Unit
        }
    }

    private fun loadLessons() {
        dispatch(LearnIntent.Internal.LoadStarted)
        viewModelScope.launch {
            try {
                dispatch(LearnIntent.Internal.LoadSucceeded(getLessons()))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // The exception message is technical and English; what the user sees is a
                // localized resource chosen here, in the presentation layer.
                dispatch(
                    LearnIntent.Internal.LoadFailed(
                        AppString.ErrorLessonsUnavailable.asUiText(),
                    ),
                )
            }
        }
    }
}
