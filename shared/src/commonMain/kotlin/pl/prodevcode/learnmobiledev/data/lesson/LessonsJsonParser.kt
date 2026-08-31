package pl.prodevcode.learnmobiledev.data.lesson

import kotlinx.serialization.json.Json
import pl.prodevcode.learnmobiledev.domain.model.Lesson

/**
 * Pure content parser: `String -> List<Lesson>`.
 *
 * Deliberately separated from the data source, so that parsing and DTO-to-domain mapping
 * can be tested without Android, without a Context and without files.
 */
class LessonsJsonParser(
    private val json: Json = DefaultJson,
) {
    fun parse(content: String): List<Lesson> =
        json.decodeFromString<LessonsFileDto>(content).lessons.map { it.toDomain() }

    companion object {
        /**
         * `classDiscriminator = "type"` matches the `"type"` field in the JSON, which is
         * how polymorphic blocks announce what they are.
         */
        val DefaultJson: Json = Json {
            classDiscriminator = "type"
            ignoreUnknownKeys = true
        }
    }
}
