package pl.prodevcode.learnmobiledev.data.preferences

import platform.Foundation.NSUserDefaults
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore

/**
 * Implementation based on NSUserDefaults.
 *
 * Unlike Android, it needs neither a Context nor thread switching — NSUserDefaults
 * buffers values in memory and writes them asynchronously.
 */
class NsUserDefaultsKeyValueStore : KeyValueStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun getString(key: String): String? = defaults.stringForKey(key)

    override suspend fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
