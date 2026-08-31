package pl.prodevcode.learnmobiledev.data.scenario

import kotlinx.serialization.Serializable
import pl.prodevcode.learnmobiledev.domain.model.ConcurrencyScenario

/** Wire format for scenario descriptions. */
@Serializable
internal data class ScenariosFileDto(
    val version: Int = 1,
    val scenarios: List<ScenarioDto>,
)

@Serializable
internal data class ScenarioDto(
    val id: String,
    val title: String,
    val description: String,
    val expectation: String,
    val explanation: String,
    val demonstratesBug: Boolean = false,
)

/**
 * Maps the wire format to the domain model.
 *
 * `{workers}` is substituted here rather than in the UI: the number of coroutines is a
 * property of the lab, and the view should not have to know how a sentence is assembled.
 */
internal fun ScenarioDto.toDomain(workers: Int): ConcurrencyScenario = ConcurrencyScenario(
    id = id,
    title = title,
    description = description.withWorkers(workers),
    expectation = expectation.withWorkers(workers),
    explanation = explanation.withWorkers(workers),
    demonstratesBug = demonstratesBug,
)

private fun String.withWorkers(workers: Int): String = replace(WORKERS_PLACEHOLDER, "$workers")

private const val WORKERS_PLACEHOLDER = "{workers}"
