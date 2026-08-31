package pl.prodevcode.learnmobiledev.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pl.prodevcode.learnmobiledev.core.ui.AppString
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.data.strings.StringsJsonParser

/**
 * Validates the shipped string catalogues.
 *
 * Strings are data, so they are validated like data. These checks are what allow the
 * runtime lookup to fall back to the raw key instead of throwing: a broken catalogue never
 * reaches a device in the first place.
 */
class StringCatalogContentTest {

    private val locales = File("src/commonMain/composeResources/files")
        .listFiles()
        ?.filter { it.isDirectory }
        ?.sortedBy { it.name }
        .orEmpty()

    private fun catalogOf(locale: File) =
        StringsJsonParser().parse(File(locale, "strings.json").readText())

    /**
     * Guards the promise in AGENTS.md that adding a language is one enum entry plus one
     * directory.
     *
     * The supported set used to be repeated in `LocalizedAsset`, which made that promise
     * false in a way nothing would notice: a language added to the enum but missing here
     * would appear in the switcher and silently serve English.
     */
    @Test
    fun `every language in the enum ships a content directory`() {
        val shipped = locales.map { it.name }.toSet()

        AppLanguage.entries.forEach { language ->
            assertTrue(
                language.tag in shipped,
                "no content directory for ${language.name} (expected files/${language.tag}/)",
            )
        }
    }

    @Test
    fun `every content directory belongs to a language in the enum`() {
        val known = AppLanguage.SUPPORTED_TAGS

        locales.forEach { locale ->
            assertTrue(locale.name in known, "orphaned content directory: files/${locale.name}/")
        }
    }

    @Test
    fun `every locale ships a catalogue`() {
        assertTrue(locales.isNotEmpty(), "no content locales found")

        locales.forEach { locale ->
            assertTrue(
                File(locale, "strings.json").exists(),
                "missing strings.json for ${locale.name}",
            )
        }
    }

    @Test
    fun `every catalogue defines every key the code can ask for`() {
        val required = AppString.entries.map { it.key }.toSet()

        locales.forEach { locale ->
            val missing = required - catalogOf(locale).keys

            assertTrue(missing.isEmpty(), "[${locale.name}] missing keys: $missing")
        }
    }

    @Test
    fun `no catalogue defines keys the code never uses`() {
        val known = AppString.entries.map { it.key }.toSet()

        locales.forEach { locale ->
            val extra = catalogOf(locale).keys - known

            assertTrue(extra.isEmpty(), "[${locale.name}] unused keys: $extra")
        }
    }

    @Test
    fun `no value is blank`() {
        locales.forEach { locale ->
            val blank = catalogOf(locale).filterValues { it.isBlank() }.keys

            assertTrue(blank.isEmpty(), "[${locale.name}] blank values: $blank")
        }
    }

    @Test
    fun `translations use the same placeholders as the default language`() {
        val placeholder = Regex("""%\d+\$[sd]""")
        val default = catalogOf(locales.first { it.name == "en" })

        locales.filter { it.name != "en" }.forEach { locale ->
            catalogOf(locale).forEach { (key, value) ->
                val expected = placeholder.findAll(default.getValue(key)).map { it.value }.toSet()
                val actual = placeholder.findAll(value).map { it.value }.toSet()

                // A translation that drops or invents a placeholder renders a broken
                // sentence — or silently swallows a number the user needs.
                assertEquals(expected, actual, "[${locale.name}] placeholder mismatch in $key")
            }
        }
    }
}
