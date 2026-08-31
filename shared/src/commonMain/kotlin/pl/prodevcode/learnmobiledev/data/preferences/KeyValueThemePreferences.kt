package pl.prodevcode.learnmobiledev.data.preferences

import pl.prodevcode.learnmobiledev.domain.model.ThemeMode
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.ThemePreferences

/**
 * Adapter storing the theme preference in a key-value store.
 *
 * [getThemeMode] returns null when the user has never chosen explicitly, which lets the
 * caller fall back to the system dark-mode setting for the very first launch.
 */
class KeyValueThemePreferences(
    private val store: KeyValueStore,
) : ThemePreferences {

    override suspend fun getThemeMode(): ThemeMode? {
        val saved = store.getString(KEY_THEME_MODE) ?: return null
        return ThemeMode.entries.firstOrNull { it.name == saved }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.putString(KEY_THEME_MODE, mode.name)
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
