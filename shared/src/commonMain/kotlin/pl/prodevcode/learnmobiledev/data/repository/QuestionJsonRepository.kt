package pl.prodevcode.learnmobiledev.data.repository

import pl.prodevcode.learnmobiledev.data.quiz.QuestionsJsonParser
import pl.prodevcode.learnmobiledev.data.quiz.QuestionsSource
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.repository.QuestionRepository
import pl.prodevcode.learnmobiledev.domain.repository.QuestionsUnavailableException

/**
 * Adapter for [QuestionRepository]. Same shape as the lesson repository:
 * source, parser, cache and translation of technical failures into a domain failure.
 */
class QuestionJsonRepository(
    private val source: QuestionsSource,
    private val languageProvider: LanguageProvider,
    private val parser: QuestionsJsonParser = QuestionsJsonParser(),
) : QuestionRepository {

    /**
     * The cache is keyed by language, so switching locale cannot serve stale content from
     * the previous one. A plain field would have been a subtle bug the moment the user
     * changed the language.
     */
    private var cached: Pair<String, List<Question>>? = null

    override suspend fun getQuestions(): List<Question> {
        val language = languageProvider.language()
        cached?.let { (cachedLanguage, value) -> if (cachedLanguage == language) return value }

        val questions = try {
            parser.parse(source.readContent())
        } catch (error: Exception) {
            throw QuestionsUnavailableException("Failed to load question bank", error)
        }

        cached = language to questions
        return questions
    }
}
