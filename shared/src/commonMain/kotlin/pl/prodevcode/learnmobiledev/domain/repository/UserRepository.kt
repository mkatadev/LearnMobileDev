package pl.prodevcode.learnmobiledev.domain.repository

import pl.prodevcode.learnmobiledev.domain.model.User

/**
 * Port defined by the domain and implemented in the data layer.
 *
 * This is Dependency Inversion in practice: the inner layer declares the contract,
 * the outer layer fulfils it. Tests substitute their own implementation without
 * any mocking library.
 */
interface UserRepository {

    suspend fun getUsers(query: String): List<User>

    /** May throw [UserSyncException] — used to demonstrate optimistic-update rollback. */
    suspend fun setFavorite(userId: String, favorite: Boolean): Boolean
}

/**
 * Domain-level failure.
 *
 * The message is technical and English on purpose: it goes to logs and crash reporting,
 * never to the screen. What the user sees is decided by the presentation layer, which
 * maps the exception *type* to a localized string resource.
 */
sealed class UserSyncException(message: String) : Exception(message) {

    /** The backend could not be reached. */
    class NetworkUnavailable : UserSyncException("Users endpoint unavailable (HTTP 503)")

    /** The backend refused to persist the favorite flag. */
    class FavoriteRejected(userId: String) :
        UserSyncException("Server rejected favorite update for user=$userId")
}
