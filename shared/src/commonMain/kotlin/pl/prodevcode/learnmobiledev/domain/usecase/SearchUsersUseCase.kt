package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository

/**
 * A use case is one business rule with one public method (`operator fun invoke`).
 *
 * Why not call the repository straight from the store? Because "no query returns
 * everyone, and favorites are sorted to the top" is a **business** rule, not a
 * presentation detail. Living inside a store it could not be reused by a widget or on
 * iOS without copying it.
 */
class SearchUsersUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(query: String): List<User> =
        repository.getUsers(query.trim())
            .sortedWith(compareByDescending<User> { it.isFavorite }.thenBy { it.name })
}
