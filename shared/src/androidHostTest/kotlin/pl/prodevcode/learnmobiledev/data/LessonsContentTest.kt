package pl.prodevcode.learnmobiledev.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pl.prodevcode.learnmobiledev.data.lesson.LessonsJsonParser
import pl.prodevcode.learnmobiledev.domain.model.Block
import pl.prodevcode.learnmobiledev.domain.model.Lesson

/**
 * Validates the **real** course content shipped with the app.
 *
 * The test lives in the JVM source set (`androidHostTest`) because it reads a file from
 * disk, and common code has no filesystem access. That is the usual split: logic in
 * `commonTest`, platform-specific concerns in platform sources.
 *
 * Since the content is data, it is validated like data: a typo in the JSON breaks the
 * build rather than the user's screen.
 */
class LessonsContentTest {

    /**
     * Every shipped locale is validated, not just the default one. A typo in a translation
     * is exactly as damaging as a typo in the original.
     */
    private val locales = File(CONTENT_ROOT)
        .listFiles()
        ?.filter { it.isDirectory }
        ?.sortedBy { it.name }
        .orEmpty()

    private fun lessonsOf(locale: File) =
        LessonsJsonParser().parse(File(locale, "lessons.json").readText())

    private fun forEachLocale(block: (String, List<Lesson>) -> Unit) {
        assertTrue(locales.isNotEmpty(), "no content locales found")
        locales.forEach { locale -> block(locale.name, lessonsOf(locale)) }
    }

    @Test
    fun `the lessons json file parses`() = forEachLocale { locale, lessons ->
        assertTrue(lessons.isNotEmpty(), "[$locale] the lesson catalogue is empty")
    }

    @Test
    fun `lessons have unique identifiers`() = forEachLocale { locale, lessons ->
        val ids = lessons.map { it.id }

        assertEquals(ids.size, ids.toSet().size, "[$locale] duplicate lesson ids: $ids")
    }

    @Test
    fun `every lesson has a title, a summary and content`() = forEachLocale { locale, lessons ->
        lessons.forEach { lesson ->
            assertTrue(lesson.title.isNotBlank(), "[$locale] empty title: ${lesson.id}")
            assertTrue(lesson.summary.isNotBlank(), "[$locale] empty summary: ${lesson.id}")
            assertTrue(lesson.blocks.isNotEmpty(), "[$locale] no content: ${lesson.id}")
        }
    }

    @Test
    fun `no content block is empty`() = forEachLocale { locale, lessons ->
        lessons.forEach { lesson ->
            lesson.blocks.forEach { block ->
                val isEmpty = when (block) {
                    is Block.Paragraph -> block.text.isBlank()
                    is Block.Subheading -> block.text.isBlank()
                    is Block.Rule -> block.text.isBlank()
                    is Block.Code -> block.code.isBlank()
                    is Block.Bullets -> block.items.isEmpty()
                    is Block.Table -> block.headers.isEmpty() || block.rows.isEmpty()
                    is Block.Exercise -> block.text.isBlank()
                }
                assertTrue(!isEmpty, "[$locale] empty block in ${lesson.id}: $block")
            }
        }
    }

    @Test
    fun `every table row has as many columns as the header`() = forEachLocale { locale, lessons ->
        lessons.forEach { lesson ->
            lesson.blocks.filterIsInstance<Block.Table>().forEach { table ->
                table.rows.forEach { row ->
                    assertEquals(
                        table.headers.size,
                        row.size,
                        "[$locale] column mismatch in ${lesson.id}: $row",
                    )
                }
            }
        }
    }

    @Test
    fun `exercise numbers are unique across the course`() = forEachLocale { locale, lessons ->
        val numbers = lessons.flatMap { it.blocks }
            .filterIsInstance<Block.Exercise>()
            .map { it.number }

        assertEquals(numbers.size, numbers.toSet().size, "[$locale] duplicate exercise numbers: $numbers")
    }
}

/**
 * The content is the backend's data, so it lives in the `:fakeApi` module. The parsers that
 * interpret it live here, which is why the test does too: it is the contract between the
 * two, and a relative path across modules is the cheapest way to express it.
 */
private const val CONTENT_ROOT = "../fakeApi/src/commonMain/composeResources/files"
