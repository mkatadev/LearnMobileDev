package pl.prodevcode.learnmobiledev.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import pl.prodevcode.learnmobiledev.data.user.FavoriteRequestDto
import pl.prodevcode.learnmobiledev.data.user.FavoriteResponseDto
import pl.prodevcode.learnmobiledev.data.user.RolesResponseDto
import pl.prodevcode.learnmobiledev.data.user.UserDto
import pl.prodevcode.learnmobiledev.data.user.UserEditDto
import pl.prodevcode.learnmobiledev.data.user.UsersResponseDto
import pl.prodevcode.learnmobiledev.data.user.toDomain
import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackend

/** Any answer that is not a `2xx`. The status is kept, because the caller maps it. */
class UsersApiException(
    val status: Int,
    message: String,
) : Exception(message)

/**
 * The app's client for the user service.
 *
 * The directory itself lives behind the API (see the `:fakeApi` module) — the app holds no
 * user list, filters nothing locally and cannot toggle a favorite without the server
 * agreeing. Searching is delegated with `?q=`, which is the only version of this screen
 * that keeps working once the directory outgrows a phone.
 *
 * @see FakeBackend.FAULT_HEADER for how the demo asks the backend to fail on purpose.
 */
class UsersApi(
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    suspend fun getUsers(query: String, simulateFailure: Boolean = false): List<User> {
        val response = client.get(ApiRoutes.V1.Users(q = query)) {
            if (simulateFailure) header(FakeBackend.FAULT_HEADER, FakeBackend.FAULT_UNAVAILABLE)
        }
        val body = response.requireSuccess("GET users")
        return json.decodeFromString<UsersResponseDto>(body).users.map { it.toDomain() }
    }

    /** Returns the value the server actually persisted, which need not be the one asked for. */
    suspend fun setFavorite(userId: String, favorite: Boolean): Boolean {
        val response = client.put(userId.favoriteRoute()) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(FavoriteRequestDto(favorite)))
        }
        val body = response.requireSuccess("PUT favorite")
        return json.decodeFromString<FavoriteResponseDto>(body).favorite
    }

    /** The roles the service will accept, so the app can offer exactly those and no others. */
    suspend fun getRoles(): List<String> {
        val body = client.get(ApiRoutes.V1.Roles()).requireSuccess("GET roles")
        return json.decodeFromString<RolesResponseDto>(body).roles
    }

    /**
     * Creates a user and returns the stored row.
     *
     * `POST` rather than `PUT`, because the server assigns the id: the client cannot name
     * the resource it is asking for, and repeating the call creates a second row. That is
     * exactly why a create must not be retried blindly the way a favorite may be.
     */
    suspend fun createUser(name: String, email: String, role: String): User {
        val response = client.post(ApiRoutes.V1.Users()) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UserEditDto(name, email, role)))
        }
        val body = response.requireSuccess("POST users")
        return json.decodeFromString<UserDto>(body).toDomain()
    }

    /** Answers `204` with no body, so there is nothing to decode — only a status to check. */
    suspend fun deleteUser(userId: String) {
        client.delete(userId.userRoute()).requireSuccess("DELETE user")
    }

    /**
     * Replaces the editable fields and returns the stored row.
     *
     * The response is the whole user rather than an acknowledgement, so the app renders
     * what the server has — including any normalization it applied on the way in.
     */
    suspend fun updateUser(userId: String, name: String, email: String, role: String): User {
        val response = client.put(userId.userRoute()) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UserEditDto(name, email, role)))
        }
        val body = response.requireSuccess("PUT user")
        return json.decodeFromString<UserDto>(body).toDomain()
    }

    private fun String.userRoute() = ApiRoutes.V1.Users.Detail(id = this)

    private fun String.favoriteRoute() = ApiRoutes.V1.Users.Detail.Favorite(userRoute())

    private suspend fun HttpResponse.requireSuccess(call: String): String {
        if (!status.isSuccess()) {
            throw UsersApiException(status.value, "$call failed with $status")
        }
        return bodyAsText()
    }

}
