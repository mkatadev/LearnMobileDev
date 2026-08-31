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

    /**
     * Persists the editable fields and returns the user **as the server stored it**.
     *
     * The answer is the new truth, not the values that were sent: a backend is free to
     * trim, normalize or reject them, and a client that keeps its own copy instead ends
     * up showing something no server ever agreed to.
     */
    suspend fun updateUser(userId: String, name: String, email: String, role: String): User
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

    /** The backend understood the edit and refused the values (HTTP 422). */
    class InvalidUser(userId: String) :
        UserSyncException("Server rejected the edited fields for user=$userId")

    /** The user is gone — edited or favorited by someone after the list was loaded. */
    class UserNotFound(userId: String) :
        UserSyncException("Server does not know user=$userId")
}
