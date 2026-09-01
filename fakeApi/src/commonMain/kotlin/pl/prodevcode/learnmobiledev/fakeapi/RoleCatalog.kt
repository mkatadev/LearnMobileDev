package pl.prodevcode.learnmobiledev.fakeapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The service's list of roles a user may hold.
 *
 * A `fun interface` over the raw document, like [UserStorage], so routing can be tested on
 * a fixture rather than on the shipped file.
 */
fun interface RoleStorage {
    suspend fun read(): String?
}

/** Reads the role list bundled with this module. */
class BundledRoleStorage : RoleStorage {

    private val content = BundledContentStorage()

    override suspend fun read(): String? = content.readFile("files/roles.json")
}

@Serializable
private data class RoleTable(val version: Int = 1, val roles: List<String>)

/**
 * The roles the service will accept, served so the app can offer them as a closed list.
 *
 * This is deliberately *not* an app resource and not an enum. A role is a value stored on
 * a user row, so the set of legal values is the server's to define — an app that shipped
 * its own copy would offer options the backend might reject, and adding a role would mean
 * releasing a new build. It is also not localized, for the same reason the user table is
 * not: the string is data that gets written to a row, so translating it would make the
 * stored value depend on the language the author happened to be using.
 *
 * Read once and cached, like the user table.
 */
class RoleCatalog(
    private val storage: RoleStorage = BundledRoleStorage(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private var roles: List<String>? = null

    suspend fun all(): List<String> = roles ?: load().also { roles = it }

    /** Validation is the server's: a row may only hold a role the service published. */
    suspend fun contains(role: String): Boolean = all().contains(role)

    private suspend fun load(): List<String> {
        val document = storage.read() ?: return emptyList()
        return json.decodeFromString<RoleTable>(document).roles
    }
}
