package pl.prodevcode.learnmobiledev.presentation.app

import pl.prodevcode.learnmobiledev.core.mvi.MviEffect
import pl.prodevcode.learnmobiledev.core.mvi.MviIntent
import pl.prodevcode.learnmobiledev.core.mvi.MviState
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
     * The language chosen but not yet in effect.
     *
     * Resources follow the platform locale, which only changes at launch, so a switch
     * cannot be applied to the running app. Holding the pending choice in state is what
     * lets the UI explain that rather than appear to have ignored the tap.
     */
    val pendingLanguage: AppLanguage? = null,
    /** Whether the language picker is on screen. */
    val isLanguagePickerVisible: Boolean = false,
) : MviState {

    /**
     * What the picker should show as selected: the scheduled language if there is one,
     * otherwise the one in effect.
     */
    val selectedLanguage: AppLanguage get() = pendingLanguage ?: language
}

sealed interface AppShellIntent : MviIntent {

    sealed interface Ui : AppShellIntent {
        data class TabSelected(val tab: Tab) : Ui

        /** Switches between the light and dark palettes. */
        data object ThemeToggled : Ui

        /** Opens the language picker. */
        data object LanguagePickerOpened : Ui

        /** Picks a language. It applies on the next launch, not now. */
        data class LanguageSelected(val language: AppLanguage) : Ui

        /** Closes the picker. Any scheduled choice stays scheduled. */
        data object LanguagePickerDismissed : Ui
    }

    sealed interface Internal : AppShellIntent {
        /** Preference read from persistent storage at startup. */
        data class ThemeRestored(val mode: ThemeMode) : Internal

        /** The language in effect for this launch, read from the platform at startup. */
        data class LanguageRestored(val language: AppLanguage) : Internal

        /**
         * The choice was recorded and will apply on the next launch.
         *
         * Separate from [LanguageRestored] because the two mean opposite things: one is
         * what the app *is* showing, the other what it *will* show.
         */
        data class LanguageScheduled(val language: AppLanguage) : Internal

    }
}

/** The shell emits no one-off events. */
sealed interface AppShellEffect : MviEffect
