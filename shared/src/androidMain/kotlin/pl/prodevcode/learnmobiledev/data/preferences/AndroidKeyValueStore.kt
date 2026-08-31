package pl.prodevcode.learnmobiledev.data.preferences

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore

/**
 * Implementation based on SharedPreferences.
 *
 * Reads and writes run on [Dispatchers.IO] because the first call loads the XML file
 * from disk. A `suspend` function must be safe to call from the main thread — that is
 * this class's responsibility, not its caller's.
 */
class AndroidKeyValueStore(context: Context) : KeyValueStore {

    private val preferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    override suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        preferences.getString(key, null)
    }

    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        preferences.edit().putString(key, value).apply()
    }

    private companion object {
        const val NAME = "learn_mobile_dev_prefs"
    }
}
