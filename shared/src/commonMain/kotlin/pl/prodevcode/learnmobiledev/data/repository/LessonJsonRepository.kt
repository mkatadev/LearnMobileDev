package pl.prodevcode.learnmobiledev.data.repository

import pl.prodevcode.learnmobiledev.data.lesson.LessonsJsonParser
import pl.prodevcode.learnmobiledev.data.lesson.LessonsSource
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.domain.model.Lesson
import pl.prodevcode.learnmobiledev.domain.repository.LessonRepository
import pl.prodevcode.learnmobiledev.domain.repository.LessonsUnavailableException

/**
 * Adapter for [LessonRepository]: reads content from a source and parses it into the
 * domain model.
 *
 * Responsibilities are split deliberately:
 * - [LessonsSource] knows **where** the bytes come from (asset, file, network),
 * - [LessonsJsonParser] knows **how** to interpret them,
 * - this repository wires the two together, caches the result and translates technical
 *   failures into the domain failure [LessonsUnavailableException].
 *
 * Caching is justified here: the content never changes while the app is running, and
 * parsing ~45 kB of JSON on every visit would be wasteful.
 */
class LessonJsonRepository(
    private val source: LessonsSource,
    private val languageProvider: LanguageProvider,
    private val parser: LessonsJsonParser = LessonsJsonParser(),
) : LessonRepository {

    /**
     * The cache is keyed by language, so switching locale cannot serve stale content from
     * the previous one. A plain field would have been a subtle bug the moment the user
     * changed the language.
     */
    private var cached: Pair<String, List<Lesson>>? = null

    override suspend fun getLessons(): List<Lesson> {
        val language = languageProvider.language()
        cached?.let { (cachedLanguage, value) -> if (cachedLanguage == language) return value }

        val lessons = try {
            parser.parse(source.readContent())
        } catch (error: Exception) {
            throw LessonsUnavailableException("Failed to load course content", error)
        }

        cached = language to lessons
        return lessons
    }
}
