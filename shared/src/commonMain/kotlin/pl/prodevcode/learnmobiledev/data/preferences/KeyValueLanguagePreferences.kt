package pl.prodevcode.learnmobiledev.data.preferences

import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.LanguagePreferences

/**
 * Adapter storing the language preference in a key-value store.
 *
 * An unrecognised value — for example after a variant is removed from the enum in a later
 * release — is treated as "no choice", so startup falls back to the device language
 * instead of failing.
 */
class KeyValueLanguagePreferences(
    private val store: KeyValueStore,
) : LanguagePreferences {

    override suspend fun getLanguage(): AppLanguage? {
        val saved = store.getString(KEY_LANGUAGE) ?: return null
        return AppLanguage.entries.firstOrNull { it.name == saved }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        store.putString(KEY_LANGUAGE, language.name)
    }

    private companion object {
        const val KEY_LANGUAGE = "app_language"
    }
}
