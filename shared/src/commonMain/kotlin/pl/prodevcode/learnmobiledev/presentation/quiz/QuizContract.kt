package pl.prodevcode.learnmobiledev.presentation.quiz

import pl.prodevcode.learnmobiledev.core.mvi.MviEffect
import pl.prodevcode.learnmobiledev.core.mvi.MviIntent
import pl.prodevcode.learnmobiledev.core.mvi.MviState
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory

/** Quiz session phase. Modeled explicitly so it is not inferred from a mix of flags. */
enum class QuizPhase { Setup, InProgress, Finished }

/** Answer given to a single question. */
data class AnsweredQuestion(
    val question: Question,
    val selectedIndex: Int,
) {
    val isCorrect: Boolean
        get() = selectedIndex == question.correctIndex
}

/**
 * Quiz screen state.
 *
 * Note [selectedAnswerIndex]: after answering, we do NOT move on immediately; instead we
 * show the explanation. The goal is learning, so feedback matters more than speed.
 */
data class QuizState(
    val phase: QuizPhase = QuizPhase.Setup,
    val availableCategories: List<QuizCategory> = QuizCategory.entries,
    val selectedCategories: Set<QuizCategory> = emptySet(),
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswerIndex: Int? = null,
    val answers: List<AnsweredQuestion> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    /**
     * Visibility of the exit confirmation dialog.
     *
     * The dialog is **part of state**, not `var showDialog by remember` in Compose.
     * That makes it testable without UI and replayable on the timeline — and after a
     * screen rotation it does not disappear halfway through the user's decision.
     */
    val isExitDialogVisible: Boolean = false,
) : MviState {

    val currentQuestion: Question?
        get() = questions.getOrNull(currentIndex)

    /** Answer confirmed — only then do we show the explanation. */
    val isAnswerRevealed: Boolean
        get() = selectedAnswerIndex != null

    val isCurrentAnswerCorrect: Boolean
        get() = selectedAnswerIndex != null && selectedAnswerIndex == currentQuestion?.correctIndex

    val isLastQuestion: Boolean
        get() = currentIndex == questions.lastIndex

    val correctCount: Int
        get() = answers.count { it.isCorrect }

    val scorePercent: Int
        get() = if (answers.isEmpty()) 0 else correctCount * 100 / answers.size

    /**
     * Position of the current question, 1-based.
     *
     * Deliberately a number rather than a formatted `"3/15"` string: state holds data, and
     * how it reads belongs to the catalogue. A string here would also be untranslatable —
     * some locales separate the two numbers differently.
     */
    val questionNumber: Int
        get() = currentIndex + 1

    /** Whether there is progress to lose on exit — decides whether to ask for confirmation. */
    val hasProgress: Boolean
        get() = answers.isNotEmpty() || selectedAnswerIndex != null

    /** Questions answered incorrectly — material for review. */
    val mistakes: List<AnsweredQuestion>
        get() = answers.filterNot { it.isCorrect }
}

sealed interface QuizIntent : MviIntent {

    sealed interface Ui : QuizIntent {
        data object ScreenOpened : Ui
        data class CategoryToggled(val category: QuizCategory) : Ui
        data object AllCategoriesSelected : Ui
        data object QuizStarted : Ui

        /** The content language changed, so anything cached is now in the wrong locale. */
        data object ContentInvalidated : Ui
        data class AnswerSelected(val index: Int) : Ui
        data object NextClicked : Ui
        data object RestartClicked : Ui
        data object ExitRequested : Ui
        data object ExitConfirmed : Ui
        data object ExitDismissed : Ui
        data object RetryMistakesClicked : Ui
    }

    sealed interface Internal : QuizIntent {
        data object LoadStarted : Internal
        data class QuestionsLoaded(val questions: List<Question>) : Internal
        data class LoadFailed(val message: UiText) : Internal
    }
}

sealed interface QuizEffect : MviEffect {
    data class ShowMessage(val text: UiText) : QuizEffect
}
