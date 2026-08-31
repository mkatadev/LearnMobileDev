package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.ConcurrencyScenario
import pl.prodevcode.learnmobiledev.domain.repository.ScenarioRepository

/** Lists the available concurrency scenarios. */
class GetConcurrencyScenariosUseCase(
    private val repository: ScenarioRepository,
) {
    suspend operator fun invoke(): List<ConcurrencyScenario> = repository.getScenarios()
}
