package pl.prodevcode.learnmobiledev.domain.repository

import pl.prodevcode.learnmobiledev.domain.model.ThemeMode

/**
 * Port for persisting the theme preference.
 *
 * The domain states *what* is stored, not *where*. Android uses SharedPreferences,
 * iOS uses NSUserDefaults and tests use an in-memory implementation — none of which
 * changes this file.
 */
interface ThemePreferences {

    /** Null when the user has never chosen a theme explicitly. */
    suspend fun getThemeMode(): ThemeMode?

    suspend fun setThemeMode(mode: ThemeMode)
}
