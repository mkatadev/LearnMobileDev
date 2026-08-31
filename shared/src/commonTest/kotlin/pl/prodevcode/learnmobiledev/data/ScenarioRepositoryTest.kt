package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.data.repository.ScenarioJsonRepository
import pl.prodevcode.learnmobiledev.domain.repository.ScenariosUnavailableException

/** Scenario descriptions are data, so they are validated like data. */
class ScenarioRepositoryTest {

    /** Content language is irrelevant here — the source is stubbed anyway. */
    private val testLanguage = LanguageProvider { "en" }

    private val sampleJson = """
        {
          "scenarios": [
            {
              "id": "lost-update",
              "title": "Lost update",
              "description": "{workers} coroutines increment a shared field.",
              "expectation": "We expect {workers}, but will see fewer.",
              "explanation": "There is a suspension point between read and write.",
              "demonstratesBug": true
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `substitutes the worker count into every text`() = runTest {
        val repository =
            ScenarioJsonRepository(100, { sampleJson }, testLanguage)

        val scenario = repository.getScenarios().single()

        assertTrue(scenario.description.contains("100"), scenario.description)
        assertTrue(scenario.expectation.contains("100"), scenario.expectation)
        assertFalse(scenario.description.contains("{workers}"))
    }

    @Test
    fun `maps the bug flag`() = runTest {
        val repository =
            ScenarioJsonRepository(10, { sampleJson }, testLanguage)

        assertTrue(repository.getScenarios().single().demonstratesBug)
    }

    @Test
    fun `a technical failure is translated into a domain failure`() = runTest {
        val repository =
            ScenarioJsonRepository(10, { "not json at all" }, testLanguage)

        assertFailsWith<ScenariosUnavailableException> { repository.getScenarios() }
    }

    @Test
    fun `the second call is served from cache`() = runTest {
        var reads = 0
        val repository = ScenarioJsonRepository(
            workers = 10,
            source = {
                reads++
                sampleJson
            },
            languageProvider = testLanguage,
        )

        repository.getScenarios()
        repository.getScenarios()

        assertEquals(1, reads)
    }
}
