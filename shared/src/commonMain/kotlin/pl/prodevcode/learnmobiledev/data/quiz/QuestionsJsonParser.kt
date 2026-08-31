package pl.prodevcode.learnmobiledev.data.quiz

import kotlinx.serialization.json.Json
import pl.prodevcode.learnmobiledev.domain.model.Question

/** Pure parser for the question bank. Testable without Android. */
class QuestionsJsonParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(content: String): List<Question> =
        json.decodeFromString<QuestionsFileDto>(content).questions.map { it.toDomain() }
}
