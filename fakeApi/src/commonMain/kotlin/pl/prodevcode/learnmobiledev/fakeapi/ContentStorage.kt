package pl.prodevcode.learnmobiledev.fakeapi

/**
 * The backend's storage — its "database".
 *
 * A real service would query Postgres here; this one asks the host application for the
 * bytes of a bundled JSON file. Either way the routing layer above is unaware: it asks for
 * a document by language and name, and gets it or does not.
 *
 * `null` means "not stored", which the routes translate into `404`. Throwing would blur
 * the line between an absent document and a broken storage.
 */
fun interface ContentStorage {
    suspend fun read(language: String, resource: String): String?
}

/**
 * Which languages the backend serves, and which one it falls back to.
 *
 * Language negotiation belongs on the server: the client states a preference, the server
 * answers with what it actually has. The app therefore never needs to know which
 * translations exist — it reads `Content-Language` off the response if it cares.
 *
 * The set is supplied by the caller rather than hardcoded, so that adding a language to
 * the app cannot silently leave the backend serving English.
 */
class LanguageCatalog(
    supported: Set<String>,
    private val default: String,
) {
    private val supported: Set<String> = supported.map { it.lowercase() }.toSet()

    init {
        require(default.lowercase() in this.supported) {
            "default language '$default' is not in the supported set $supported"
        }
    }

    fun resolve(requested: String?): String {
        val tag = requested?.lowercase()?.substringBefore('-') ?: return default
        return if (tag in supported) tag else default
    }
}
