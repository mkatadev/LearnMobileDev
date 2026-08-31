package pl.prodevcode.learnmobiledev.domain.model

/**
 * Domain model — plain Kotlin, no framework annotations, no technical fields.
 *
 * In clean architecture this is the **innermost** layer: it knows nothing about the
 * network, the database or Compose. Every other layer depends on it, never the reverse.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val isFavorite: Boolean = false,
)
