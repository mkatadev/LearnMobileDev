package pl.prodevcode.learnmobiledev.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pl.prodevcode.learnmobiledev.core.ui.AppString

/**
 * Validates the UI translations shipped with the app.
 *
 * [AppString] resolves keys through a generated map, so a key with no `<string>` behind it
 * compiles happily and throws the first time that screen is opened. This test is what turns
 * that into a build failure, and it is the reason the map is acceptable in the first place.
 *
 * Every locale is checked, not just the default: a missing Polish translation silently
 * falls back to English, which is a defect that ships quietly.
 */
class StringResourcesTest {

    private val locales = File("src/commonMain/composeResources")
        .listFiles()
        ?.filter { it.isDirectory && it.name.startsWith("values") }
        ?.sortedBy { it.name }
        .orEmpty()

    /** Parsed with a regex rather than an XML library: the format is fixed and generated. */
    private fun keysOf(locale: File): List<String> =
        Regex("""<string name="([^"]+)">""")
            .findAll(File(locale, "strings.xml").readText())
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `every locale defines every key the code refers to`() {
        assertTrue(locales.isNotEmpty(), "no string resources found")

        val required = AppString.entries.map { it.key }.toSet()
        locales.forEach { locale ->
            val missing = required - keysOf(locale).toSet()

            assertTrue(missing.isEmpty(), "[${locale.name}] missing translations: $missing")
        }
    }

    /** An entry no code path can reach is dead weight, and usually a renamed key. */
    @Test
    fun `no locale defines a key the code does not use`() {
        val used = AppString.entries.map { it.key }.toSet()
        locales.forEach { locale ->
            val unused = keysOf(locale).toSet() - used

            assertTrue(unused.isEmpty(), "[${locale.name}] unused translations: $unused")
        }
    }

    @Test
    fun `keys are unique within a locale`() {
        locales.forEach { locale ->
            val keys = keysOf(locale)

            assertEquals(
                keys.size,
                keys.toSet().size,
                "[${locale.name}] duplicate keys: ${keys.groupBy { it }.filterValues { it.size > 1 }.keys}",
            )
        }
    }

    @Test
    fun `no translation is blank`() {
        locales.forEach { locale ->
            val blank = Regex("""<string name="([^"]+)"></string>""")
                .findAll(File(locale, "strings.xml").readText())
                .map { it.groupValues[1] }
                .toList()

            assertTrue(blank.isEmpty(), "[${locale.name}] blank translations: $blank")
        }
    }

    /**
     * A placeholder mismatch between locales is a crash waiting for a language switch:
     * the call site passes what the default expects, and the translation reads past it.
     */
    @Test
    fun `placeholders match across locales`() {
        val byLocale = locales.associate { locale ->
            val text = File(locale, "strings.xml").readText()
            locale.name to Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(text)
                .associate { match ->
                    val placeholders = Regex("""%(\d+)\$""")
                        .findAll(match.groupValues[2])
                        .map { it.groupValues[1] }
                        .toSet()
                    match.groupValues[1] to placeholders
                }
        }

        val reference = byLocale.getValue("values")
        byLocale.filterKeys { it != "values" }.forEach { (name, translations) ->
            translations.forEach { (key, placeholders) ->
                assertEquals(
                    reference[key],
                    placeholders,
                    "[$name] placeholder mismatch for '$key'",
                )
            }
        }
    }
}
