package pl.prodevcode.learnmobiledev.domain.repository

import pl.prodevcode.learnmobiledev.domain.model.AppLanguage

/**
 * Port for persisting the language preference.
 *
 * [getLanguage] returns null when the user has never chosen explicitly, which lets the
 * caller distinguish "no choice yet, follow the device" from a deliberate selection.
 */
interface LanguagePreferences {
    suspend fun getLanguage(): AppLanguage?
    suspend fun setLanguage(language: AppLanguage)
}
