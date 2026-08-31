package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.ScenarioResult
import pl.prodevcode.learnmobiledev.domain.repository.ConcurrencyLab

/** Runs a single concurrency scenario. */
class RunConcurrencyScenarioUseCase(
    private val lab: ConcurrencyLab,
) {
    suspend operator fun invoke(scenarioId: String): ScenarioResult = lab.run(scenarioId)
}
