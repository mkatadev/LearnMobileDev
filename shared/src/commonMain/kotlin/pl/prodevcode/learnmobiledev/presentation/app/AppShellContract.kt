package pl.prodevcode.learnmobiledev.presentation.app

import pl.prodevcode.learnmobiledev.core.mvi.MviEffect
import pl.prodevcode.learnmobiledev.core.mvi.MviIntent
import pl.prodevcode.learnmobiledev.core.mvi.MviState
import pl.prodevcode.learnmobiledev.core.ui.StringCatalog
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.model.ThemeMode

/**
 * Application shell state: selected tab and theme preference.
 *
 * The selected tab could be `var by remember` in Compose, but then it would not survive
 * configuration changes and could not be tested. Keeping it in the store is consistent
 * with the rest of the project and costs only a dozen or so lines.
 */
data class AppShellState(
    val tab: Tab = Tab.Learn,
    val themeMode: ThemeMode = ThemeMode.Light,
    /**
     * Until the preference has been read from disk, we do not yet know which theme the
     * user chose. The flag avoids a flash of the light theme at startup.
     */
    val isThemeLoaded: Boolean = false,
    val language: AppLanguage = AppLanguage.DEFAULT,
    /**
     * Bumped whenever the effective content language changes. Screens use it as a key to
     * reload their content — the cached JSON belongs to the previous locale.
     */
    val contentRevision: Int = 0,
    /** UI strings for the active language; empty until the catalogue is loaded. */
    val strings: StringCatalog = StringCatalog.EMPTY,
    /** Gates the first render, so no screen flashes untranslated keys. */
    val areStringsLoaded: Boolean = false,
) : MviState

sealed interface AppShellIntent : MviIntent {

    sealed interface Ui : AppShellIntent {
        data class TabSelected(val tab: Tab) : Ui

        /** Switches between the light and dark palettes. */
        data object ThemeToggled : Ui

        /** Switches between English and Polish. */
        data object LanguageToggled : Ui
    }

    sealed interface Internal : AppShellIntent {
        /** Preference read from persistent storage at startup. */
        data class ThemeRestored(val mode: ThemeMode) : Internal

        /**
         * The content language was applied. [contentChanged] is false when the effective
         * language stayed the same, so screens do not reload for nothing.
         */
        data class LanguageApplied(
            val language: AppLanguage,
            val contentChanged: Boolean,
        ) : Internal

        /** The string catalogue finished loading for the active language. */
        data class StringsLoaded(val strings: StringCatalog) : Internal
    }
}

/** The shell emits no one-off events. */
sealed interface AppShellEffect : MviEffect
