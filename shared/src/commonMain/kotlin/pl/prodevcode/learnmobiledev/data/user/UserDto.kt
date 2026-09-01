package pl.prodevcode.learnmobiledev.data.user

import kotlinx.serialization.Serializable
import pl.prodevcode.learnmobiledev.domain.model.User

/**
 * DTO — the **wire representation** of a user, kept apart from the domain model for the
 * same reasons as the content DTOs: the domain owes nothing to kotlinx.serialization, and
 * a rename on the server changes one mapper instead of the whole app.
 */
@Serializable
internal data class UsersResponseDto(val users: List<UserDto>)

@Serializable
internal data class RolesResponseDto(val roles: List<String>)

@Serializable
internal data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val isFavorite: Boolean = false,
)

@Serializable
internal data class FavoriteRequestDto(val favorite: Boolean)

@Serializable
internal data class FavoriteResponseDto(val id: String, val favorite: Boolean)

/**
 * The body of both a create and an edit. They carry the same fields — the difference is
 * the method and who assigns the id, not the payload.
 */
@Serializable
internal data class UserEditDto(val name: String, val email: String, val role: String)

internal fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    role = role,
    isFavorite = isFavorite,
)
