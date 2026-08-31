package pl.prodevcode.learnmobiledev.presentation.quiz

import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.mvi.MviStore
import pl.prodevcode.learnmobiledev.core.ui.asUiText
import pl.prodevcode.learnmobiledev.domain.usecase.GetQuizQuestionsUseCase

/**
 * Store for the quiz screen.
 *
 * Retrying mistakes does not touch the repository: the questions are already in the
 * state, so the new set is built locally. This is a legitimate case of the presentation
 * layer working on data it has already collected.
 */
class QuizViewModel(
    private val getQuestions: GetQuizQuestionsUseCase,
) : MviStore<QuizState, QuizIntent, QuizEffect>(
    initialState = QuizState(),
    reducer = QuizReducer,
) {

    override fun onIntentProcessed(
        intent: QuizIntent,
        before: QuizState,
        after: QuizState,
    ) {
        when (intent) {
            is QuizIntent.Ui.QuizStarted -> loadQuestions(after)

            is QuizIntent.Ui.RetryMistakesClicked -> {
                val mistakes = before.mistakes.map { it.question }
                if (mistakes.isEmpty()) {
                    emitEffect(QuizEffect.ShowMessage(AppString.QuizNoMistakes.asUiText()))
                } else {
                    dispatch(QuizIntent.Internal.QuestionsLoaded(mistakes))
                }
            }

            is QuizIntent.Internal.QuestionsLoaded ->
                if (intent.questions.isEmpty()) {
                    emitEffect(QuizEffect.ShowMessage(AppString.QuizNoQuestions.asUiText()))
                }

            is QuizIntent.Internal.LoadFailed ->
                emitEffect(QuizEffect.ShowMessage(intent.message))

            else -> Unit
        }
    }

    private fun loadQuestions(state: QuizState) {
        dispatch(QuizIntent.Internal.LoadStarted)
        viewModelScope.launch {
            try {
                dispatch(
                    QuizIntent.Internal.QuestionsLoaded(
                        getQuestions(categories = state.selectedCategories),
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                dispatch(
                    QuizIntent.Internal.LoadFailed(
                        AppString.ErrorQuestionsUnavailable.asUiText(),
                    ),
                )
            }
        }
    }
}
