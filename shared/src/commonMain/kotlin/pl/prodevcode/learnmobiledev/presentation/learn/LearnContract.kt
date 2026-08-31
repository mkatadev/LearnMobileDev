package pl.prodevcode.learnmobiledev.presentation.learn

import pl.prodevcode.learnmobiledev.core.mvi.MviEffect
import pl.prodevcode.learnmobiledev.core.mvi.MviIntent
import pl.prodevcode.learnmobiledev.core.mvi.MviState
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.domain.model.Lesson

/**
 * The learn screen — the second MVI example in this app.
 *
 * Since content is loaded from JSON, the screen has a full data lifecycle
 * (loading -> success / failure), so it follows the same shape as the users screen,
 * only without debounce and optimistic updates.
 */
data class LearnState(
    val lessons: List<Lesson> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val openLessonId: String? = null,
    val completedLessonIds: Set<String> = emptySet(),
) : MviState {

    val openLesson: Lesson?
        get() = lessons.firstOrNull { it.id == openLessonId }

    val progressPercent: Int
        get() = if (lessons.isEmpty()) 0 else completedLessonIds.size * 100 / lessons.size

    fun isCompleted(lessonId: String): Boolean = lessonId in completedLessonIds
}

sealed interface LearnIntent : MviIntent {

    sealed interface Ui : LearnIntent {
        data object ScreenOpened : Ui
        data object RetryClicked : Ui
        data class LessonClicked(val lessonId: String) : Ui
        data class LessonCompletionToggled(val lessonId: String) : Ui
        data object ProgressReset : Ui
    }

    sealed interface Internal : LearnIntent {
        data object LoadStarted : Internal
        data class LoadSucceeded(val lessons: List<Lesson>) : Internal
        data class LoadFailed(val message: UiText) : Internal
    }
}

/** The learn screen has no one-off events, so the effect type is deliberately empty. */
sealed interface LearnEffect : MviEffect
