package pl.prodevcode.learnmobiledev.presentation.learn

import pl.prodevcode.learnmobiledev.core.mvi.Reducer

/**
 * Learn screen reducer.
 *
 * Note `LessonClicked`: clicking an open lesson collapses it. In MVVM that rule often
 * lands in Compose as `var expanded by remember`; here it is part of state, so it can be
 * tested and replayed on the timeline.
 */
val LearnReducer = Reducer<LearnState, LearnIntent> { state, intent ->
    when (intent) {
        is LearnIntent.Ui.ScreenOpened -> state
        is LearnIntent.Ui.RetryClicked -> state

        // Clearing the lessons matters: without it the screen would keep rendering the
        // previous locale until the new content arrives.

        is LearnIntent.Ui.LessonClicked -> state.copy(
            openLessonId = if (state.openLessonId == intent.lessonId) null else intent.lessonId,
        )

        is LearnIntent.Ui.LessonCompletionToggled -> state.copy(
            completedLessonIds = if (intent.lessonId in state.completedLessonIds) {
                state.completedLessonIds - intent.lessonId
            } else {
                state.completedLessonIds + intent.lessonId
            },
        )

        is LearnIntent.Ui.ProgressReset -> state.copy(completedLessonIds = emptySet())

        is LearnIntent.Internal.LoadStarted -> state.copy(isLoading = true, error = null)

        is LearnIntent.Internal.LoadSucceeded -> state.copy(
            isLoading = false,
            error = null,
            lessons = intent.lessons,
            // The first lesson opens itself, but only if the user has not chosen anything yet.
            openLessonId = state.openLessonId ?: intent.lessons.firstOrNull()?.id,
        )

        is LearnIntent.Internal.LoadFailed -> state.copy(
            isLoading = false,
            error = intent.message,
        )
    }
}
