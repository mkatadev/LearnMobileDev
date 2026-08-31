package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.LanguagePreferences

/**
 * Reads the stored language preference.
 *
 * Returns null when the user has never chosen one, so the caller can fall back to the
 * device language instead of assuming a default.
 */
class GetAppLanguageUseCase(private val preferences: LanguagePreferences) {
    suspend operator fun invoke(): AppLanguage? = preferences.getLanguage()
}

/** Stores the language preference. */
class SetAppLanguageUseCase(private val preferences: LanguagePreferences) {
    suspend operator fun invoke(language: AppLanguage) = preferences.setLanguage(language)
}
