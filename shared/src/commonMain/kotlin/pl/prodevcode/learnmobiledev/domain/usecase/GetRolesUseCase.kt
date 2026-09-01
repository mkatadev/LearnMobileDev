package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.repository.UserRepository

/**
 * The roles a user may hold.
 *
 * Sorting is a rule about how the choice is *offered*, not about how it is stored, but it
 * belongs here rather than in the UI: a picker on iOS or in a widget should list them in
 * the same order, and an order decided in one screen is an order the next one gets wrong.
 */
class GetRolesUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(): List<String> = repository.getRoles().sorted()
}
