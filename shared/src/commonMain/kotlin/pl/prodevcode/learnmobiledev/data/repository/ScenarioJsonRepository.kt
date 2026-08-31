package pl.prodevcode.learnmobiledev.data.repository

import pl.prodevcode.learnmobiledev.data.scenario.ScenariosJsonParser
import pl.prodevcode.learnmobiledev.data.scenario.ScenariosSource
import pl.prodevcode.learnmobiledev.domain.model.ConcurrencyScenario
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.domain.repository.ScenarioRepository
import pl.prodevcode.learnmobiledev.domain.repository.ScenariosUnavailableException

/**
 * Adapter for [ScenarioRepository]: reads descriptions from a JSON asset.
 *
 * Same shape as the lesson and question repositories — source, parser, cache and
 * translation of technical failures into a domain failure.
 */
class ScenarioJsonRepository(
    private val workers: Int,
    private val source: ScenariosSource,
    private val languageProvider: LanguageProvider,
    private val parser: ScenariosJsonParser = ScenariosJsonParser(),
) : ScenarioRepository {

    /** Keyed by language, so a locale switch cannot serve stale content. */
    private var cached: Pair<String, List<ConcurrencyScenario>>? = null

    override suspend fun getScenarios(): List<ConcurrencyScenario> {
        val language = languageProvider.language()
        cached?.let { (cachedLanguage, value) -> if (cachedLanguage == language) return value }

        val scenarios = try {
            parser.parse(source.readContent(), workers)
        } catch (error: Exception) {
            throw ScenariosUnavailableException("Failed to load concurrency scenarios", error)
        }

        cached = language to scenarios
        return scenarios
    }
}
