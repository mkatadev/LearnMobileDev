package pl.prodevcode.learnmobiledev.data.scenario

import kotlinx.serialization.json.Json
import pl.prodevcode.learnmobiledev.domain.model.ConcurrencyScenario

/** Pure parser for scenario descriptions: `String -> List<ConcurrencyScenario>`. */
class ScenariosJsonParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(content: String, workers: Int): List<ConcurrencyScenario> =
        json.decodeFromString<ScenariosFileDto>(content).scenarios.map { it.toDomain(workers) }
}
