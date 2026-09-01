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

    /**
     * Creates a user and returns the stored row, id included.
     *
     * Takes no id, because the server assigns it. That is also why this is not idempotent
     * the way [updateUser] is: calling it twice creates two people.
     */
    suspend fun createUser(name: String, email: String, role: String): User

    /** Removes a user. Throws [UserSyncException.UserNotFound] if there was none. */
    suspend fun deleteUser(userId: String)

    /**
     * The roles a user may hold, as published by the service.
     *
     * A port rather than an enum in the domain: which roles are legal is the backend's to
     * decide, and an app carrying its own copy would offer values the server rejects and
     * need a new release whenever one was added.
     */
    suspend fun getRoles(): List<String>
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
