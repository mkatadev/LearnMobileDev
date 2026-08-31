package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.data.repository.QuestionJsonRepository
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory
import pl.prodevcode.learnmobiledev.domain.repository.QuestionsUnavailableException

/** Validates mapping and failure handling in the quiz data layer. */
class QuestionJsonRepositoryTest {

    /** Content language is irrelevant here — the source is stubbed anyway. */
    private val testLanguage = LanguageProvider { "en" }

    private fun json(body: String) = """{ "questions": [ $body ] }"""

    private val valid = """
        {
          "id": "x1",
          "category": "rx",
          "difficulty": "senior",
          "text": "Question",
          "options": ["A", "B"],
          "correctIndex": 1,
          "explanation": "Because"
        }
    """.trimIndent()

    @Test
    fun `maps a question onto the domain model`() = runTest {
        val repository = QuestionJsonRepository(source = { json(valid) }, languageProvider = testLanguage)

        val question = repository.getQuestions().single()

        assertEquals(QuizCategory.Rx, question.category)
        assertEquals("B", question.correctAnswer)
    }

    @Test
    fun `an out of range correctIndex is a data defect`() = runTest {
        val broken = valid.replace("\"correctIndex\": 1", "\"correctIndex\": 7")
        val repository = QuestionJsonRepository(source = { json(broken) }, languageProvider = testLanguage)

        assertFailsWith<QuestionsUnavailableException> { repository.getQuestions() }
    }

    @Test
    fun `an unknown category is a data defect`() = runTest {
        val broken = valid.replace("\"category\": \"rx\"", "\"category\": \"nieistniejaca\"")
        val repository = QuestionJsonRepository(source = { json(broken) }, languageProvider = testLanguage)

        assertFailsWith<QuestionsUnavailableException> { repository.getQuestions() }
    }

    @Test
    fun `a question with a single option is rejected`() = runTest {
        val broken = valid.replace("[\"A\", \"B\"]", "[\"A\"]")
            .replace("\"correctIndex\": 1", "\"correctIndex\": 0")
        val repository = QuestionJsonRepository(source = { json(broken) }, languageProvider = testLanguage)

        assertFailsWith<QuestionsUnavailableException> { repository.getQuestions() }
    }
}
