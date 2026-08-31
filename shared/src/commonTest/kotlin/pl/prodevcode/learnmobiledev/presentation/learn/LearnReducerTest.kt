package pl.prodevcode.learnmobiledev.presentation.learn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.domain.model.Block
import pl.prodevcode.learnmobiledev.domain.model.Lesson

/**
 * The learn screen is tested exactly like the users screen, because both are pure
 * functions. That repeatability is what a shared MVI core buys you.
 */
class LearnReducerTest {

    private val lessons = listOf(
        lesson("a"),
        lesson("b"),
        lesson("c"),
        lesson("d"),
    )
    private val loaded = LearnState(lessons = lessons, openLessonId = "a")

    private fun lesson(id: String) = Lesson(
        id = id,
        title = "Lesson $id",
        summary = "Summary $id",
        blocks = listOf(Block.Paragraph("content")),
    )

    private fun reduce(state: LearnState, intent: LearnIntent) =
        LearnReducer.reduce(state, intent)

    @Test
    fun `LoadStarted turns on the loader and clears the error`() {
        val result = reduce(LearnState(error = UiText.Raw("stale")), LearnIntent.Internal.LoadStarted)

        assertTrue(result.isLoading)
        assertNull(result.error)
    }

    @Test
    fun `LoadSucceeded fills in lessons and opens the first one`() {
        val result = reduce(
            LearnState(isLoading = true),
            LearnIntent.Internal.LoadSucceeded(lessons),
        )

        assertEquals(lessons, result.lessons)
        assertEquals("a", result.openLessonId)
        assertFalse(result.isLoading)
    }

    @Test
    fun `LoadSucceeded does not override the lesson chosen by the user`() {
        val result = reduce(
            LearnState(openLessonId = "c"),
            LearnIntent.Internal.LoadSucceeded(lessons),
        )

        assertEquals("c", result.openLessonId)
    }

    @Test
    fun `LoadFailed sets the error and ends loading`() {
        val result = reduce(
            LearnState(isLoading = true),
            LearnIntent.Internal.LoadFailed(UiText.Raw("missing file")),
        )

        assertEquals(UiText.Raw("missing file"), result.error)
        assertFalse(result.isLoading)
    }

    @Test
    fun `clicking another lesson opens it`() {
        val result = reduce(loaded, LearnIntent.Ui.LessonClicked("b"))

        assertEquals("b", result.openLessonId)
    }

    @Test
    fun `clicking the open lesson collapses it`() {
        val result = reduce(loaded, LearnIntent.Ui.LessonClicked("a"))

        assertNull(result.openLessonId)
    }

    @Test
    fun `marking a lesson toggles both ways`() {
        val done = reduce(loaded, LearnIntent.Ui.LessonCompletionToggled("a"))
        assertTrue(done.isCompleted("a"))

        val undone = reduce(done, LearnIntent.Ui.LessonCompletionToggled("a"))
        assertFalse(undone.isCompleted("a"))
    }

    @Test
    fun `progress is derived from the state`() {
        val state = loaded.copy(completedLessonIds = setOf("a", "b"))

        assertEquals(50, state.progressPercent)
    }

    @Test
    fun `progress of an empty list does not divide by zero`() {
        assertEquals(0, LearnState().progressPercent)
    }

    @Test
    fun `reset clears progress but keeps the lesson open`() {
        val state = loaded.copy(openLessonId = "b", completedLessonIds = setOf("a"))

        val result = reduce(state, LearnIntent.Ui.ProgressReset)

        assertTrue(result.completedLessonIds.isEmpty())
        assertEquals("b", result.openLessonId)
    }
}
