package pl.prodevcode.learnmobiledev.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import pl.prodevcode.learnmobiledev.data.user.FavoriteRequestDto
import pl.prodevcode.learnmobiledev.data.user.FavoriteResponseDto
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
        val response = client.get(USERS_PATH) {
            parameter("q", query)
            if (simulateFailure) header(FakeBackend.FAULT_HEADER, FakeBackend.FAULT_UNAVAILABLE)
        }
        val body = response.requireSuccess("GET $USERS_PATH")
        return json.decodeFromString<UsersResponseDto>(body).users.map { it.toDomain() }
    }

    /** Returns the value the server actually persisted, which need not be the one asked for. */
    suspend fun setFavorite(userId: String, favorite: Boolean): Boolean {
        val response = client.put("$USERS_PATH/$userId/favorite") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(FavoriteRequestDto(favorite)))
        }
        val body = response.requireSuccess("PUT $USERS_PATH/$userId/favorite")
        return json.decodeFromString<FavoriteResponseDto>(body).favorite
    }

    /**
     * Replaces the editable fields and returns the stored row.
     *
     * The response is the whole user rather than an acknowledgement, so the app renders
     * what the server has — including any normalization it applied on the way in.
     */
    suspend fun updateUser(userId: String, name: String, email: String, role: String): User {
        val response = client.put("$USERS_PATH/$userId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UserEditDto(name, email, role)))
        }
        val body = response.requireSuccess("PUT $USERS_PATH/$userId")
        return json.decodeFromString<UserDto>(body).toDomain()
    }

    private suspend fun HttpResponse.requireSuccess(call: String): String {
        if (!status.isSuccess()) {
            throw UsersApiException(status.value, "$call failed with $status")
        }
        return bodyAsText()
    }

    private companion object {
        const val USERS_PATH = "/api/v1/users"
    }
}
