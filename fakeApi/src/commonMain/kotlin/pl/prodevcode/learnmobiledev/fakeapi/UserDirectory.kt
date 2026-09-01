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
 * The service's user database: the seed table as it has been changed since.
 *
 * Filtering and persistence live here rather than in the app, which is the whole point of
 * the move — the client sends a query and gets rows back, and has no way to reach the data
 * except through HTTP.
 *
 * The seed is read once and then *materialized* into a mutable list that every write
 * changes. An earlier version kept the seed pristine and layered edits on top, which reads
 * well right up until rows can also be created and deleted: the overlay then has to encode
 * absence and ordering too, which is a table with extra steps. A table is simply its
 * current rows, so this is one.
 *
 * A [Mutex] guards it: the app fires searches and writes concurrently, and a real backend
 * would serialize them too.
 */
class UserDirectory(
    private val storage: UserStorage = BundledUserStorage(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val lock = Mutex()
    private var rows: MutableList<UserRecord>? = null
    private val favorites = mutableSetOf<String>()

    /** Ids are assigned by the service; a client never gets to name a row it creates. */
    private var nextId = 1

    /** Rows matching [query] across name, role and email; a blank query matches everyone. */
    suspend fun search(query: String): List<UserRecord> = lock.withLock {
        table().map { it.withFavorite() }.filter { it.matches(query) }
    }

    suspend fun find(id: String): UserRecord? = lock.withLock {
        table().firstOrNull { it.id == id }?.withFavorite()
    }

    /** Stores the flag and answers with what is now persisted. */
    suspend fun setFavorite(id: String, favorite: Boolean): Boolean = lock.withLock {
        if (favorite) favorites += id else favorites -= id
        favorite
    }

    /**
     * Appends a row and answers with it, id included. The client sends no id and cannot
     * choose one — that is the server's to assign, and the reason creating is a `POST`.
     */
    suspend fun create(name: String, email: String, role: String): UserRecord = lock.withLock {
        val table = table()
        val created = UserRecord(id = freeId(table), name = name, email = email, role = role)
        table += created
        created
    }

    /**
     * Replaces the editable fields of a row and answers with the stored result, or `null`
     * when there is no such user. The favorite flag is not editable through here: it has
     * its own endpoint, and letting two routes write the same field is how they start to
     * disagree.
     */
    suspend fun update(id: String, name: String, email: String, role: String): UserRecord? =
        lock.withLock {
            val table = table()
            val index = table.indexOfFirst { it.id == id }
            if (index < 0) return@withLock null

            val updated = table[index].copy(name = name, email = email, role = role)
            table[index] = updated
            updated.withFavorite()
        }

    /**
     * Removes a row, reporting whether there was one. The favorite goes with it, so an id
     * handed out later cannot inherit a flag from a user who no longer exists.
     */
    suspend fun delete(id: String): Boolean = lock.withLock {
        val removed = table().removeAll { it.id == id }
        if (removed) favorites -= id
        removed
    }

    private suspend fun table(): MutableList<UserRecord> = rows ?: load().also { rows = it }

    private suspend fun load(): MutableList<UserRecord> {
        val document = storage.read() ?: return mutableListOf()
        return json.decodeFromString<UserTable>(document).users.toMutableList()
    }

    /** Skips ids the table already uses, so a new row cannot collide with the seed. */
    private fun freeId(rows: List<UserRecord>): String {
        val taken = rows.mapTo(mutableSetOf()) { it.id }
        while ("$ID_PREFIX$nextId" in taken) nextId++
        return "$ID_PREFIX${nextId++}"
    }

    private fun UserRecord.withFavorite(): UserRecord = copy(isFavorite = id in favorites)

    private fun UserRecord.matches(query: String): Boolean =
        query.isBlank() ||
            name.contains(query, ignoreCase = true) ||
            role.contains(query, ignoreCase = true) ||
            email.contains(query, ignoreCase = true)

    private companion object {
        const val ID_PREFIX = "u-"
    }
}
