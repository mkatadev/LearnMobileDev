package pl.prodevcode.learnmobiledev.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.domain.model.Difficulty
import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory
import pl.prodevcode.learnmobiledev.domain.repository.QuestionRepository

/**
 * The question bank is authored by hand and its correct answer drifts towards one
 * position. These tests pin down the rule that neutralises that: a session must present
 * the options in a random order, with the correct answer still pointed at.
 */
class GetQuizQuestionsUseCaseTest {

    private fun question(id: String, category: QuizCategory = QuizCategory.Mvi) = Question(
        id = id,
        category = category,
        difficulty = Difficulty.Senior,
        text = "text-$id",
        options = listOf("a-$id", "b-$id", "c-$id", "d-$id"),
        correctIndex = 1,
        explanation = "explanation-$id",
    )

    private class FakeRepository(private val questions: List<Question>) : QuestionRepository {
        override suspend fun getQuestions(): List<Question> = questions
    }

    private fun useCase(questions: List<Question>) =
        GetQuizQuestionsUseCase(FakeRepository(questions))

    @Test
    fun `correct answers do not all sit at the authored position`() = runTest {
        val bank = List(60) { question("q$it") }

        val result = useCase(bank)(seed = 7, limit = 60)

        assertTrue(
            result.map { it.correctIndex }.toSet().size > 1,
            "all correct answers share one index: ${result.map { it.correctIndex }}",
        )
    }

    @Test
    fun `shuffling options keeps correctIndex on the correct answer`() = runTest {
        val bank = List(30) { question("q$it") }

        useCase(bank)(seed = 3, limit = 30).forEach { shuffled ->
            val original = bank.first { it.id == shuffled.id }
            assertEquals(
                original.correctAnswer,
                shuffled.correctAnswer,
                "wrong answer for ${shuffled.id}",
            )
            assertEquals(
                original.options.sorted(),
                shuffled.options.sorted(),
                "options altered in ${shuffled.id}",
            )
        }
    }

    @Test
    fun `the same seed produces the same session`() = runTest {
        val bank = List(30) { question("q$it") }

        assertEquals(useCase(bank)(seed = 11), useCase(bank)(seed = 11))
    }

    @Test
    fun `only the selected categories are served and the limit is respected`() = runTest {
        val bank = List(10) { question("mvi$it") } +
            List(10) { question("kmp$it", QuizCategory.Kmp) }

        val result = useCase(bank)(categories = setOf(QuizCategory.Kmp), limit = 5, seed = 1)

        assertEquals(5, result.size)
        assertTrue(result.all { it.category == QuizCategory.Kmp }, "unselected category served")
    }
}
