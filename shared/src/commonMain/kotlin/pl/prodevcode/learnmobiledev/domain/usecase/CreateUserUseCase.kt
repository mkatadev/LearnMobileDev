package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository

/**
 * Creates a user.
 *
 * Trimming lives here for the same reason it does in [UpdateUserUseCase]: "a name is what
 * is left after the whitespace" is a rule about users, and it must hold however the user
 * arrives. Whether the values are *acceptable* is still the server's answer — duplicating
 * that here would produce two rules that drift apart.
 */
class CreateUserUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(name: String, email: String, role: String): User =
        repository.createUser(
            name = name.trim(),
            email = email.trim(),
            role = role.trim(),
        )
}
