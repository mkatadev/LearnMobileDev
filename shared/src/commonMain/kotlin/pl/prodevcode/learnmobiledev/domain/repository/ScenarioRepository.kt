package pl.prodevcode.learnmobiledev.domain.repository

import pl.prodevcode.learnmobiledev.domain.model.ConcurrencyScenario

/** Port that supplies the descriptions of concurrency scenarios. */
interface ScenarioRepository {
    suspend fun getScenarios(): List<ConcurrencyScenario>
}

/** Signals that scenario descriptions could not be loaded. Message is technical, for logs. */
class ScenariosUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
