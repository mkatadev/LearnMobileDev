package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.ThemeMode
import pl.prodevcode.learnmobiledev.domain.repository.ThemePreferences

/**
 * Reads the stored theme preference.
 *
 * Returns null when none was ever chosen, so the caller can follow the system dark-mode
 * setting for the first launch instead of assuming a default.
 */
class GetThemeModeUseCase(private val preferences: ThemePreferences) {
    suspend operator fun invoke(): ThemeMode? = preferences.getThemeMode()
}

/** Stores the theme preference. */
class SetThemeModeUseCase(private val preferences: ThemePreferences) {
    suspend operator fun invoke(mode: ThemeMode) = preferences.setThemeMode(mode)
}
