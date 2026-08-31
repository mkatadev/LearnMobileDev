package pl.prodevcode.learnmobiledev.presentation.quiz

import pl.prodevcode.learnmobiledev.core.ui.AppString
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.mvi.Reducer
import pl.prodevcode.learnmobiledev.core.ui.asUiText

/**
 * Quiz reducer — the entire session mechanics in one pure function.
 *
 * Two places are worth noting:
 * - [QuizIntent.Ui.AnswerSelected] ignores another selection after the answer is revealed,
 *   so the result cannot be "fixed" after seeing the explanation,
 * - [QuizIntent.Ui.NextClicked] records the answer in history only when moving forward,
 *   so the `answers` list always matches the questions that were actually completed.
 */
val QuizReducer = Reducer<QuizState, QuizIntent> { state, intent ->
    when (intent) {
        is QuizIntent.Ui.ScreenOpened -> state
        is QuizIntent.Ui.QuizStarted -> state

        is QuizIntent.Ui.RetryMistakesClicked -> state

        is QuizIntent.Ui.CategoryToggled -> state.copy(
            selectedCategories = if (intent.category in state.selectedCategories) {
                state.selectedCategories - intent.category
            } else {
                state.selectedCategories + intent.category
            },
        )

        is QuizIntent.Ui.AllCategoriesSelected -> state.copy(selectedCategories = emptySet())

        // After the answer is revealed, selection is locked — otherwise the quiz would not measure knowledge.
        is QuizIntent.Ui.AnswerSelected ->
            if (state.isAnswerRevealed) state else state.copy(selectedAnswerIndex = intent.index)

        is QuizIntent.Ui.NextClicked -> {
            val question = state.currentQuestion
            val selected = state.selectedAnswerIndex

            if (question == null || selected == null) {
                state
            } else {
                val recorded = state.answers + AnsweredQuestion(question, selected)
                if (state.isLastQuestion) {
                    state.copy(
                        answers = recorded,
                        phase = QuizPhase.Finished,
                        selectedAnswerIndex = null,
                    )
                } else {
                    state.copy(
                        answers = recorded,
                        currentIndex = state.currentIndex + 1,
                        selectedAnswerIndex = null,
                    )
                }
            }
        }

        // Exiting with no progress needs no confirmation — ask only when there is something to lose.
        is QuizIntent.Ui.ExitRequested ->
            if (state.hasProgress) state.copy(isExitDialogVisible = true) else state.toSetup()

        is QuizIntent.Ui.ExitConfirmed -> state.toSetup()

        is QuizIntent.Ui.ExitDismissed -> state.copy(isExitDialogVisible = false)

        is QuizIntent.Ui.RestartClicked -> state.copy(
            phase = QuizPhase.Setup,
            questions = emptyList(),
            answers = emptyList(),
            currentIndex = 0,
            selectedAnswerIndex = null,
            error = null,
            isExitDialogVisible = false,
        )

        is QuizIntent.Internal.LoadStarted -> state.copy(isLoading = true, error = null)

        is QuizIntent.Internal.QuestionsLoaded ->
            if (intent.questions.isEmpty()) {
                state.copy(
                    isLoading = false,
                    error = AppString.QuizNoQuestions.asUiText(),
                )
            } else {
                state.copy(
                    isLoading = false,
                    error = null,
                    phase = QuizPhase.InProgress,
                    questions = intent.questions,
                    currentIndex = 0,
                    selectedAnswerIndex = null,
                    answers = emptyList(),
                    isExitDialogVisible = false,
                )
            }

        is QuizIntent.Internal.LoadFailed -> state.copy(
            isLoading = false,
            error = intent.message,
        )
    }
}

/** Return to the category selection screen with the session cleared. */
private fun QuizState.toSetup(): QuizState = copy(
    phase = QuizPhase.Setup,
    questions = emptyList(),
    answers = emptyList(),
    currentIndex = 0,
    selectedAnswerIndex = null,
    isExitDialogVisible = false,
    error = null,
)
