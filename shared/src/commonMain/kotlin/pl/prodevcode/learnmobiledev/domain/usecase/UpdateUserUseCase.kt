package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository

/**
 * Saves an edited user.
 *
 * The trimming lives here rather than in the store or the text field: "a name is what is
 * left after the whitespace" is a rule about users, and it must hold whether the edit
 * arrives from this screen, from a deep link or from iOS.
 *
 * What it deliberately does **not** do is decide whether the values are acceptable. That
 * is the server's answer, and duplicating it in the app would produce two rules that drift.
 */
class UpdateUserUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(
        userId: String,
        name: String,
        email: String,
        role: String,
    ): User = repository.updateUser(
        userId = userId,
        name = name.trim(),
        email = email.trim(),
        role = role.trim(),
    )
}
