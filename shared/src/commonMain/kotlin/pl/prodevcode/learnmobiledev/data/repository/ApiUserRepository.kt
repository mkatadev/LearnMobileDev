package pl.prodevcode.learnmobiledev.data.repository

import io.ktor.http.HttpStatusCode
import pl.prodevcode.learnmobiledev.data.remote.UsersApi
import pl.prodevcode.learnmobiledev.data.remote.UsersApiException
import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.domain.repository.NetworkFailureSwitch
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository
import pl.prodevcode.learnmobiledev.domain.repository.UserSyncException

/**
 * The users port, fulfilled over HTTP.
 *
 * There is no local list any more: the directory, the search and the favorites all live in
 * the user service, and this class does the one thing a data-layer implementation should —
 * translate between a transport failure and a domain one. The store and the reducer above
 * it did not change by a line, which is the point of having a port in the first place.
 *
 * [failNextLoad] no longer fakes an error inside the app; it asks the backend to fail, so
 * the demo exercises a real `503` travelling through a real client.
 */
class ApiUserRepository(
    private val api: UsersApi,
) : UserRepository, NetworkFailureSwitch {

    override var failNextLoad: Boolean = false

    override suspend fun getUsers(query: String): List<User> = try {
        api.getUsers(query, simulateFailure = failNextLoad)
    } catch (failure: UsersApiException) {
        throw failure.toDomain(userId = null)
    }

    override suspend fun setFavorite(userId: String, favorite: Boolean): Boolean = try {
        api.setFavorite(userId, favorite)
    } catch (failure: UsersApiException) {
        throw failure.toDomain(userId)
    }

    override suspend fun updateUser(
        userId: String,
        name: String,
        email: String,
        role: String,
    ): User = try {
        api.updateUser(userId, name, email, role)
    } catch (failure: UsersApiException) {
        throw failure.toDomain(userId)
    }

    /**
     * The status code decides the domain meaning: `409` is the server refusing this
     * particular write, `422` is it refusing these values, `404` is a row that is no longer
     * there, and anything else is the endpoint being unusable. Reading the message instead
     * would tie the app to English prose the backend is free to change.
     */
    private fun UsersApiException.toDomain(userId: String?): UserSyncException = when {
        userId == null -> UserSyncException.NetworkUnavailable()
        status == HttpStatusCode.Conflict.value -> UserSyncException.FavoriteRejected(userId)
        status == HttpStatusCode.UnprocessableEntity.value -> UserSyncException.InvalidUser(userId)
        status == HttpStatusCode.NotFound.value -> UserSyncException.UserNotFound(userId)
        else -> UserSyncException.NetworkUnavailable()
    }
}
