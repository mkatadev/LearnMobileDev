package pl.prodevcode.learnmobiledev

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Enforces the language policy from AGENTS.md: **no Polish anywhere in Kotlin sources**.
 *
 * Comments, KDoc, test names and exception messages are all English. User-facing text
 * belongs to `composeResources`, course content to the JSON assets.
 *
 * A rule that is only written down gets broken. This test makes it fail the build instead.
 */
class CodeLanguagePolicyTest {

    /**
     * Built from escape sequences rather than literal characters, so that this file does
     * not trip the very rule it enforces.
     */
    private val polishCharacters = Regex(
        "[\u0105\u0107\u0119\u0142\u0144\u00F3\u015B\u017A\u017C" +
            "\u0104\u0106\u0118\u0141\u0143\u00D3\u015A\u0179\u017B]",
    )

    /**
     * Fictional personal data in demo fixtures is data, not code, so Polish surnames are
     * allowed there. This is the single, deliberate exception.
     */
    private val allowedFiles = setOf("InMemoryUserRepository.kt")

    private val sourceRoots = listOf(
        File("src"),
        File("../androidApp/src"),
    )

    @Test
    fun `kotlin sources contain no Polish text`() {
        val offenders = sourceRoots
            .filter { it.exists() }
            .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" } }
            .filterNot { it.name in allowedFiles }
            .flatMap { file ->
                file.readLines()
                    .withIndex()
                    .filter { (_, line) -> polishCharacters.containsMatchIn(line) }
                    .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
            }

        assertTrue(
            offenders.isEmpty(),
            "Polish found in Kotlin sources (see AGENTS.md \u00a71):\n" +
                offenders.joinToString("\n"),
        )
    }

    /**
     * Catches user-facing text hard-coded in a composable.
     *
     * Anything rendered to the user — including a bullet, a checkmark or a counter format
     * such as `"3/15"` — belongs to the catalogue. Symbols look harmless, but a locale may
     * want a different glyph, and a format assembled with string interpolation cannot be
     * reordered by a translator.
     */
    @Test
    fun `composables render no hard-coded text`() {
        val literalText = Regex("""(Text\(|text = )"[^"]""")

        val offenders = File("src/commonMain/kotlin")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines()
                    .withIndex()
                    .filter { (_, line) -> literalText.containsMatchIn(line) }
                    .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
            }
            .toList()

        assertTrue(
            offenders.isEmpty(),
            "hard-coded UI text (see AGENTS.md \u00a71):\n" + offenders.joinToString("\n"),
        )
    }

    @Test
    fun `the language policy exception list stays minimal`() {
        // Growing this list silently would defeat the purpose of the rule above.
        assertTrue(
            allowedFiles.size <= 1,
            "more files exempt from the language policy than expected: $allowedFiles",
        )
    }
}
