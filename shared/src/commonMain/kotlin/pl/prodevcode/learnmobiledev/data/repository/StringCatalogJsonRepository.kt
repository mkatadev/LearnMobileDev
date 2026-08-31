package pl.prodevcode.learnmobiledev.data.repository

import pl.prodevcode.learnmobiledev.data.strings.StringsJsonParser
import pl.prodevcode.learnmobiledev.data.strings.StringsSource
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.domain.repository.StringCatalogRepository
import pl.prodevcode.learnmobiledev.domain.repository.StringsUnavailableException

/**
 * Adapter for [StringCatalogRepository]. Same shape as the content repositories: source,
 * parser, language-keyed cache and translation of technical failures into a domain one.
 */
class StringCatalogJsonRepository(
    private val source: StringsSource,
    private val languageProvider: LanguageProvider,
    private val parser: StringsJsonParser = StringsJsonParser(),
) : StringCatalogRepository {

    private var cached: Pair<String, Map<String, String>>? = null

    override suspend fun getStrings(): Map<String, String> {
        val language = languageProvider.language()
        cached?.let { (cachedLanguage, value) -> if (cachedLanguage == language) return value }

        val strings = try {
            parser.parse(source.readContent())
        } catch (error: Exception) {
            throw StringsUnavailableException("Failed to load string catalogue", error)
        }

        cached = language to strings
        return strings
    }
}
