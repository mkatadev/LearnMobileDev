package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.repository.UserRepository

/**
 * Deletes a user.
 *
 * A thin use case on purpose. It carries no rule today, but it keeps the store depending
 * on the domain rather than reaching for a repository directly — the day deleting means
 * more than one call (an audit entry, a cascade), there is a place for it that the screen
 * does not have to learn about.
 */
class DeleteUserUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userId: String) = repository.deleteUser(userId)
}
