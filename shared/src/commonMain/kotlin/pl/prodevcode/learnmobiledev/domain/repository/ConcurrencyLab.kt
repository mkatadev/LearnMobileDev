package pl.prodevcode.learnmobiledev.domain.repository

import pl.prodevcode.learnmobiledev.domain.model.ScenarioResult

/**
 * Port that **executes** concurrency scenarios.
 *
 * Deliberately separated from [ScenarioRepository], which supplies their descriptions:
 * running code and describing it are two different responsibilities, and only one of
 * them needs translating.
 */
interface ConcurrencyLab {
    suspend fun run(scenarioId: String): ScenarioResult
}
