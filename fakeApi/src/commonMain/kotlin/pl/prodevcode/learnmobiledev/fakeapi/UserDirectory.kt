package pl.prodevcode.learnmobiledev.fakeapi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The seed data of the user service — its `users` table before anybody touched it.
 *
 * A `fun interface` over the raw document, exactly like [ContentStorage]: the routing layer
 * must be testable on a fixture, without an Android runtime and without the shipped file.
 */
fun interface UserStorage {
    suspend fun read(): String?
}

/** Reads the user table bundled with this module. */
class BundledUserStorage : UserStorage {

    private val content = BundledContentStorage()

    override suspend fun read(): String? = content.readFile("files/users.json")
}

/**
 * A user as the service publishes it.
 *
 * `isFavorite` is not part of the seed document: it is state the service owns, and it is
 * computed per response from [UserDirectory]'s favorites.
 */
@Serializable
data class UserRecord(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val isFavorite: Boolean = false,
)

@Serializable
private data class UserTable(val version: Int = 1, val users: List<UserRecord>)

/**
 * The service's user database: the seed table plus the favorites it has accepted so far.
 *
 * Filtering and persistence live here rather than in the app, which is the whole point of
 * the move — the client sends a query and gets rows back, and has no way to reach the data
 * except through HTTP.
 *
 * The seed is read once and cached, because a database does not re-read its disk on every
 * request. A [Mutex] guards the mutable parts: the app fires searches and favorite writes
 * concurrently, and a real backend would serialize them too.
 */
class UserDirectory(
    private val storage: UserStorage = BundledUserStorage(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val lock = Mutex()
    private var seed: List<UserRecord>? = null
    private val favorites = mutableSetOf<String>()

    /**
     * Accepted edits, keyed by id. Kept apart from the seed so the shipped table stays the
     * table the service was installed with — a row is the seed with its edit applied, the
     * same way a migration does not rewrite history.
     */
    private val edits = mutableMapOf<String, UserRecord>()

    /** Rows matching [query] across name, role and email; a blank query matches everyone. */
    suspend fun search(query: String): List<UserRecord> = lock.withLock {
        table().map { it.current() }.filter { it.matches(query) }
    }

    suspend fun find(id: String): UserRecord? = lock.withLock {
        table().firstOrNull { it.id == id }?.current()
    }

    /** Stores the flag and answers with what is now persisted. */
    suspend fun setFavorite(id: String, favorite: Boolean): Boolean = lock.withLock {
        if (favorite) favorites += id else favorites -= id
        favorite
    }

    /**
     * Replaces the editable fields of a row and answers with the stored result, or `null`
     * when there is no such user. The favorite flag is not editable through here: it has
     * its own endpoint, and letting two routes write the same field is how they start to
     * disagree.
     */
    suspend fun update(id: String, name: String, email: String, role: String): UserRecord? =
        lock.withLock {
            val existing = table().firstOrNull { it.id == id } ?: return@withLock null
            val updated = existing.copy(name = name, email = email, role = role)
            edits[id] = updated
            updated.current()
        }

    private suspend fun table(): List<UserRecord> = seed ?: load().also { seed = it }

    private suspend fun load(): List<UserRecord> {
        val document = storage.read() ?: return emptyList()
        return json.decodeFromString<UserTable>(document).users
    }

    /** The row as the service would answer it today: the edit, if any, plus the flag. */
    private fun UserRecord.current(): UserRecord =
        (edits[id] ?: this).copy(isFavorite = id in favorites)

    private fun UserRecord.matches(query: String): Boolean =
        query.isBlank() ||
            name.contains(query, ignoreCase = true) ||
            role.contains(query, ignoreCase = true) ||
            email.contains(query, ignoreCase = true)
}
