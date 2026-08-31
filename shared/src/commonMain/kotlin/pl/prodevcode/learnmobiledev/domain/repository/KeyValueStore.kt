package pl.prodevcode.learnmobiledev.domain.repository

/**
 * Minimal port for a persistent key-value store.
 *
 * Deliberately narrow (ISP): storing a preference needs neither the full
 * SharedPreferences nor the full NSUserDefaults API. As a result the test implementation
 * is a few lines rather than a stub of twenty methods.
 */
interface KeyValueStore {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
}

/** Default implementation used by previews and tests. */
class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()

    override suspend fun getString(key: String): String? = values[key]

    override suspend fun putString(key: String, value: String) {
        values[key] = value
    }
}
