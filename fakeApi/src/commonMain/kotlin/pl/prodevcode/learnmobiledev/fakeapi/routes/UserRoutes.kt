package pl.prodevcode.learnmobiledev.fakeapi.routes

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.prodevcode.learnmobiledev.fakeapi.UserDirectory
import pl.prodevcode.learnmobiledev.fakeapi.UserRecord
import pl.prodevcode.learnmobiledev.fakeapi.http.ApiResponse
import pl.prodevcode.learnmobiledev.fakeapi.http.RoutingBuilder

const val USERS_PATH = "/api/v1/users"

const val USER_PATH = "$USERS_PATH/{id}"

const val USER_FAVORITE_PATH = "$USERS_PATH/{id}/favorite"

/**
 * The one user the service always refuses to bookmark.
 *
 * The rollback lesson needs a write that fails *on the server*, after the UI has already
 * drawn the optimistic result. Deciding it here rather than in the app is what makes the
 * failure real: the client discovers it from a `409`, exactly as it would in production.
 */
const val REJECTED_FAVORITE_USER_ID = "u-4"

@Serializable
private data class UsersResponse(val users: List<UserRecord>)

@Serializable
private data class UserEditRequest(
    val name: String = "",
    val email: String = "",
    val role: String = "",
)

@Serializable
private data class FavoriteRequest(val favorite: Boolean)

@Serializable
private data class FavoriteResponse(val id: String, val favorite: Boolean)

/**
 * `encodeDefaults` so that `isFavorite` is always on the wire: a client should read the
 * state of a flag from the response, not infer it from a field's absence.
 */
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * The user service.
 *
 * - `GET /api/v1/users?q=` — the directory, filtered server-side.
 * - `PUT /api/v1/users/{id}` — replaces the editable fields, after validating them.
 * - `PUT /api/v1/users/{id}/favorite` — persists the flag, or refuses it.
 *
 * Searching is a query parameter and not something the client does to a full download:
 * a directory that fits in memory today will not tomorrow, and an app written against
 * "fetch everything and filter locally" has to be rewritten when that happens.
 */
internal fun RoutingBuilder.userRoutes(directory: UserDirectory) {
    get(USERS_PATH) { call ->
        val users = directory.search(call.request.query["q"].orEmpty())
        ApiResponse.ok(json.encodeToString(UsersResponse(users)))
    }

    put(USER_PATH) { call ->
        val id = call.pathParameters.getValue("id")
        val edit = runCatching { json.decodeFromString<UserEditRequest>(call.request.body) }
            .getOrNull()
            ?: return@put ApiResponse.badRequest("expected a JSON body with name, email and role")

        val name = edit.name.trim()
        val email = edit.email.trim()
        val role = edit.role.trim()

        // Validation is the server's job. A client is free to check the same rules for a
        // faster message, but it is not the one that gets to decide what may be stored.
        validate(name, email, role)?.let { return@put it }

        val updated = directory.update(id, name, email, role)
            ?: return@put ApiResponse.notFound("unknown user '$id'")

        ApiResponse.ok(json.encodeToString(updated))
    }

    put(USER_FAVORITE_PATH) { call ->
        val id = call.pathParameters.getValue("id")
        val desired = runCatching { json.decodeFromString<FavoriteRequest>(call.request.body) }
            .getOrNull()
            ?: return@put ApiResponse.badRequest("expected a JSON body with a 'favorite' flag")

        directory.find(id) ?: return@put ApiResponse.notFound("unknown user '$id'")

        if (id == REJECTED_FAVORITE_USER_ID) {
            return@put ApiResponse.conflict("favorites are locked for user '$id'")
        }

        val stored = directory.setFavorite(id, desired.favorite)
        ApiResponse.ok(json.encodeToString(FavoriteResponse(id, stored)))
    }
}

/**
 * `422` rather than `400`: the request was well-formed JSON, the server simply will not
 * accept these values. The distinction matters to the client, which shows a field error
 * for one and a generic failure for the other.
 */
private fun validate(name: String, email: String, role: String): ApiResponse? = when {
    name.isBlank() -> ApiResponse.unprocessable("'name' must not be blank")
    role.isBlank() -> ApiResponse.unprocessable("'role' must not be blank")
    !email.isValidEmail() -> ApiResponse.unprocessable("'$email' is not a valid email address")
    else -> null
}

private fun String.isValidEmail(): Boolean {
    val at = indexOf('@')
    val dot = lastIndexOf('.')
    return none { it.isWhitespace() } && at > 0 && dot > at + 1 && dot < length - 1
}
