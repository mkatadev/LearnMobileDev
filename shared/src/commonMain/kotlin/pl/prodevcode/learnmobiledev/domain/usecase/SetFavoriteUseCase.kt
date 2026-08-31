package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.repository.UserRepository

/** Persists the favorite flag. Throws when the backend rejects the change. */
class SetFavoriteUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userId: String, favorite: Boolean): Boolean =
        repository.setFavorite(userId, favorite)
}
