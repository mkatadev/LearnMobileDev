package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.data.lesson.LessonsSource
import pl.prodevcode.learnmobiledev.data.repository.LessonJsonRepository
import pl.prodevcode.learnmobiledev.domain.model.Block
import pl.prodevcode.learnmobiledev.domain.repository.LessonsUnavailableException

/**
 * Data-layer tests without Android and without files: the source is supplied as a lambda.
 *
 * They cover three things that are easy to break: mapping of polymorphic blocks,
 * translation of a technical failure into a domain one, and caching.
 */
class LessonJsonRepositoryTest {

    /** Content language is irrelevant here — the source is stubbed anyway. */
    private val testLanguage = LanguageProvider { "en" }

    private val sampleJson = """
        {
          "lessons": [
            {
              "id": "l1",
              "title": "Lesson 1",
              "summary": "Summary",
              "blocks": [
                { "type": "paragraph", "text": "Paragraph" },
                { "type": "code", "code": "val x = 1", "caption": "Example" },
                { "type": "bullets", "items": ["a", "b"] },
                { "type": "table", "headers": ["H"], "rows": [["R"]] },
                { "type": "exercise", "number": 1, "text": "Do it" }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses lessons and maps blocks onto the domain model`() = runTest {
        val repository = LessonJsonRepository(source = { sampleJson }, languageProvider = testLanguage)

        val lessons = repository.getLessons()

        assertEquals(1, lessons.size)
        assertEquals("Lesson 1", lessons.first().title)
        assertEquals(
            listOf(
                Block.Paragraph("Paragraph"),
                Block.Code(code = "val x = 1", caption = "Example"),
                Block.Bullets(listOf("a", "b")),
                Block.Table(headers = listOf("H"), rows = listOf(listOf("R"))),
                Block.Exercise(number = 1, text = "Do it"),
            ),
            lessons.first().blocks,
        )
    }

    @Test
    fun `a technical failure is translated into a domain failure`() = runTest {
        val repository = LessonJsonRepository(source = { "this is not JSON" }, languageProvider = testLanguage)

        assertFailsWith<LessonsUnavailableException> { repository.getLessons() }
    }

    @Test
    fun `unknown JSON fields do not break parsing`() = runTest {
        val withExtraField = sampleJson.replace(
            "\"id\": \"l1\"",
            "\"id\": \"l1\", \"nowePoleZPrzyszlejWersji\": 42",
        )
        val repository = LessonJsonRepository(source = { withExtraField }, languageProvider = testLanguage)

        assertEquals("l1", repository.getLessons().first().id)
    }

    @Test
    fun `the second call is served from cache without re-reading the source`() = runTest {
        var reads = 0
        val source = LessonsSource {
            reads++
            sampleJson
        }
        val repository = LessonJsonRepository(source = source, languageProvider = testLanguage)

        val first = repository.getLessons()
        val second = repository.getLessons()

        assertEquals(1, reads)
        assertTrue(first === second)
    }
}
