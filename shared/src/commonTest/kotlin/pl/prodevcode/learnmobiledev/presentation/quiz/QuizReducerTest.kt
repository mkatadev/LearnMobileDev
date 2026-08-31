package pl.prodevcode.learnmobiledev.presentation.quiz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pl.prodevcode.learnmobiledev.domain.model.Difficulty
import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory

/** Quiz mechanics as a pure function: tests without UI and without coroutines. */
class QuizReducerTest {

    private fun question(id: String, correct: Int = 0) = Question(
        id = id,
        category = QuizCategory.Mvi,
        difficulty = Difficulty.Senior,
        text = "Question $id",
        options = listOf("A", "B", "C"),
        correctIndex = correct,
        explanation = "Explanation $id",
    )

    private val questions = listOf(question("q1", 0), question("q2", 1))

    private val inProgress = QuizState(
        phase = QuizPhase.InProgress,
        questions = questions,
    )

    private fun reduce(state: QuizState, intent: QuizIntent) = QuizReducer.reduce(state, intent)

    @Test
    fun `selecting an answer reveals the explanation`() {
        val result = reduce(inProgress, QuizIntent.Ui.AnswerSelected(0))

        assertTrue(result.isAnswerRevealed)
        assertTrue(result.isCurrentAnswerCorrect)
    }

    @Test
    fun `an answer cannot be changed once revealed`() {
        val answered = reduce(inProgress, QuizIntent.Ui.AnswerSelected(2)) // wrong

        val cheat = reduce(answered, QuizIntent.Ui.AnswerSelected(0)) // correct

        assertEquals(2, cheat.selectedAnswerIndex)
        assertFalse(cheat.isCurrentAnswerCorrect)
    }

    @Test
    fun `moving on records the answer and clears the selection`() {
        val answered = reduce(inProgress, QuizIntent.Ui.AnswerSelected(0))

        val next = reduce(answered, QuizIntent.Ui.NextClicked)

        assertEquals(1, next.answers.size)
        assertTrue(next.answers.first().isCorrect)
        assertEquals(1, next.currentIndex)
        assertNull(next.selectedAnswerIndex)
    }

    @Test
    fun `Next without an answer leaves the state unchanged`() {
        val result = reduce(inProgress, QuizIntent.Ui.NextClicked)

        assertEquals(inProgress, result)
    }

    @Test
    fun `after the last question the quiz moves to the summary phase`() {
        var state = inProgress
        state = reduce(state, QuizIntent.Ui.AnswerSelected(0))
        state = reduce(state, QuizIntent.Ui.NextClicked)
        state = reduce(state, QuizIntent.Ui.AnswerSelected(1))
        state = reduce(state, QuizIntent.Ui.NextClicked)

        assertEquals(QuizPhase.Finished, state.phase)
        assertEquals(2, state.answers.size)
        assertEquals(2, state.correctCount)
        assertEquals(100, state.scorePercent)
    }

    @Test
    fun `wrong answers land on the revision list`() {
        var state = inProgress
        state = reduce(state, QuizIntent.Ui.AnswerSelected(2)) // wrong
        state = reduce(state, QuizIntent.Ui.NextClicked)
        state = reduce(state, QuizIntent.Ui.AnswerSelected(1)) // correct
        state = reduce(state, QuizIntent.Ui.NextClicked)

        assertEquals(1, state.mistakes.size)
        assertEquals("q1", state.mistakes.first().question.id)
        assertEquals(50, state.scorePercent)
    }

    @Test
    fun `toggling a category works both ways`() {
        val added = reduce(QuizState(), QuizIntent.Ui.CategoryToggled(QuizCategory.Rx))
        assertEquals(setOf(QuizCategory.Rx), added.selectedCategories)

        val removed = reduce(added, QuizIntent.Ui.CategoryToggled(QuizCategory.Rx))
        assertTrue(removed.selectedCategories.isEmpty())
    }

    @Test
    fun `an empty load result yields an error rather than an empty quiz`() {
        val result = reduce(
            QuizState(isLoading = true),
            QuizIntent.Internal.QuestionsLoaded(emptyList()),
        )

        assertEquals(QuizPhase.Setup, result.phase)
        kotlin.test.assertNotNull(result.error)
    }

    @Test
    fun `restart clears answers and returns to setup`() {
        val finished = QuizState(
            phase = QuizPhase.Finished,
            questions = questions,
            answers = listOf(AnsweredQuestion(questions[0], 0)),
        )

        val result = reduce(finished, QuizIntent.Ui.RestartClicked)

        assertEquals(QuizPhase.Setup, result.phase)
        assertTrue(result.answers.isEmpty())
        assertEquals(0, result.currentIndex)
    }

    @Test
    fun `the score percentage does not divide by zero`() {
        assertEquals(0, QuizState().scorePercent)
    }

    @Test
    fun `exiting without progress returns straight to the category list`() {
        val result = reduce(inProgress, QuizIntent.Ui.ExitRequested)

        assertEquals(QuizPhase.Setup, result.phase)
        assertFalse(result.isExitDialogVisible)
    }

    @Test
    fun `exiting with progress asks for confirmation first`() {
        val answered = reduce(inProgress, QuizIntent.Ui.AnswerSelected(0))

        val result = reduce(answered, QuizIntent.Ui.ExitRequested)

        assertTrue(result.isExitDialogVisible)
        assertEquals(QuizPhase.InProgress, result.phase, "the quiz must not disappear before the user decides")
    }

    @Test
    fun `dismissing the dialog leaves the session untouched`() {
        var state = reduce(inProgress, QuizIntent.Ui.AnswerSelected(0))
        state = reduce(state, QuizIntent.Ui.ExitRequested)

        val result = reduce(state, QuizIntent.Ui.ExitDismissed)

        assertFalse(result.isExitDialogVisible)
        assertEquals(QuizPhase.InProgress, result.phase)
        assertEquals(0, result.selectedAnswerIndex)
    }

    @Test
    fun `confirming the exit clears the whole session`() {
        var state = reduce(inProgress, QuizIntent.Ui.AnswerSelected(0))
        state = reduce(state, QuizIntent.Ui.NextClicked)
        state = reduce(state, QuizIntent.Ui.ExitRequested)

        val result = reduce(state, QuizIntent.Ui.ExitConfirmed)

        assertEquals(QuizPhase.Setup, result.phase)
        assertTrue(result.answers.isEmpty())
        assertTrue(result.questions.isEmpty())
        assertEquals(0, result.currentIndex)
        assertFalse(result.isExitDialogVisible)
    }

    @Test
    fun `selected categories survive returning to the list`() {
        val withFilter = inProgress.copy(selectedCategories = setOf(QuizCategory.Rx))

        val result = reduce(withFilter, QuizIntent.Ui.ExitConfirmed)

        assertEquals(setOf(QuizCategory.Rx), result.selectedCategories)
    }
}
