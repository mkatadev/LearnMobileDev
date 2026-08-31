package pl.prodevcode.learnmobiledev.presentation.app

import pl.prodevcode.learnmobiledev.core.mvi.Reducer
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.model.ThemeMode

val AppShellReducer = Reducer<AppShellState, AppShellIntent> { state, intent ->
    when (intent) {
        is AppShellIntent.Ui.TabSelected -> state.copy(tab = intent.tab)

        is AppShellIntent.Ui.ThemeToggled -> state.copy(themeMode = state.themeMode.next())

        // The store performs the switch, so the reducer only records the outcome.
        is AppShellIntent.Ui.LanguageToggled -> state

        is AppShellIntent.Internal.LanguageApplied -> state.copy(
            language = intent.language,
            contentRevision =
                if (intent.contentChanged) state.contentRevision + 1 else state.contentRevision,
        )

        is AppShellIntent.Internal.StringsLoaded -> state.copy(
            strings = intent.strings,
            areStringsLoaded = true,
        )

        is AppShellIntent.Internal.ThemeRestored -> state.copy(
            themeMode = intent.mode,
            isThemeLoaded = true,
        )
    }
}

/** The other language. With exactly two variants a toggle is simply the opposite one. */
fun AppLanguage.next(): AppLanguage = when (this) {
    AppLanguage.English -> AppLanguage.Polish
    AppLanguage.Polish -> AppLanguage.English
}

/** The other theme. With exactly two variants a toggle is simply the opposite one. */
private fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.Light -> ThemeMode.Dark
    ThemeMode.Dark -> ThemeMode.Light
}
