package pl.prodevcode.learnmobiledev.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pl.prodevcode.learnmobiledev.data.quiz.QuestionsJsonParser
import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory

/**
 * Validates the **real** question bank shipped with the app.
 *
 * The bank is data, so it is validated like data. A content defect (a repeated option,
 * a missing explanation, a wrong correct-answer index) breaks the build rather than
 * someone's study session.
 */
class QuestionsContentTest {

    /** Every shipped locale is validated, not just the default one. */
    private val locales = File(CONTENT_ROOT)
        .listFiles()
        ?.filter { it.isDirectory }
        ?.sortedBy { it.name }
        .orEmpty()

    private fun forEachLocale(block: (String, List<Question>) -> Unit) {
        assertTrue(locales.isNotEmpty(), "no content locales found")
        locales.forEach { locale ->
            block(locale.name, QuestionsJsonParser().parse(File(locale, "questions.json").readText()))
        }
    }

    @Test
    fun `the question bank is not empty`() = forEachLocale { locale, questions ->
        assertTrue(questions.size >= 50, "[$locale] expected 50+ questions, found ${questions.size}")
    }

    @Test
    fun `questions have unique identifiers`() = forEachLocale { locale, questions ->
        val ids = questions.map { it.id }

        assertEquals(ids.size, ids.toSet().size, "[$locale] duplicate question ids")
    }

    @Test
    fun `every question has text, options and an explanation`() = forEachLocale { locale, questions ->
        questions.forEach { question ->
            assertTrue(question.text.isNotBlank(), "[$locale] empty text: ${question.id}")
            assertTrue(question.options.size >= 2, "[$locale] too few options: ${question.id}")
            assertTrue(
                question.options.none { it.isBlank() },
                "[$locale] empty option: ${question.id}",
            )
            assertTrue(
                question.correctIndex in question.options.indices,
                "[$locale] invalid correctIndex: ${question.id}",
            )
        }
    }

    @Test
    fun `explanations are substantive rather than one-liners`() = forEachLocale { locale, questions ->
        questions.forEach { question ->
            assertTrue(
                question.explanation.length >= 80,
                "[$locale] explanation too terse in ${question.id} (${question.explanation.length} chars)",
            )
        }
    }

    @Test
    fun `answer options are not repeated within a question`() = forEachLocale { locale, questions ->
        questions.forEach { question ->
            assertEquals(
                question.options.size,
                question.options.toSet().size,
                "[$locale] duplicate options: ${question.id}",
            )
        }
    }

    @Test
    fun `every category has questions`() = forEachLocale { locale, questions ->
        val covered = questions.map { it.category }.toSet()

        QuizCategory.entries.forEach { category ->
            assertTrue(category in covered, "[$locale] no questions for category $category")
        }
    }

    @Test
    fun `correct answers are not always at the same position`() = forEachLocale { locale, questions ->
        val distribution = questions.groupingBy { it.correctIndex }.eachCount()

        // If every correct answer sat at the same index, the quiz could be passed
        // without reading a single question.
        assertTrue(
            distribution.size >= 2,
            "[$locale] all correct answers share one index: $distribution",
        )
    }
}

/**
 * The content is the backend's data, so it lives in the `:fakeApi` module. The parsers that
 * interpret it live here, which is why the test does too: it is the contract between the
 * two, and a relative path across modules is the cheapest way to express it.
 */
private const val CONTENT_ROOT = "../fakeApi/src/commonMain/composeResources/files"
