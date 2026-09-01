package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.Infographic
import pl.prodevcode.learnmobiledev.domain.repository.InfographicRepository

/**
 * The published infographics.
 *
 * Thin today, and still a use case rather than a repository call from the store: the rule
 * about *which* infographics a reader should see belongs to the domain, and the day it
 * stops being "all of them" there is a place for it that no screen has to learn about.
 */
class GetInfographicsUseCase(
    private val repository: InfographicRepository,
) {
    suspend operator fun invoke(): List<Infographic> = repository.getInfographics()
}
