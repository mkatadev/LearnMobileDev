package pl.prodevcode.learnmobiledev.presentation.app

import pl.prodevcode.learnmobiledev.core.mvi.Reducer
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.model.ThemeMode

val AppShellReducer = Reducer<AppShellState, AppShellIntent> { state, intent ->
    when (intent) {
        is AppShellIntent.Ui.TabSelected -> state.copy(tab = intent.tab)

        is AppShellIntent.Ui.ThemeToggled -> state.copy(themeMode = state.themeMode.next())

        is AppShellIntent.Ui.LanguagePickerOpened -> state.copy(isLanguagePickerVisible = true)

        is AppShellIntent.Ui.LanguagePickerDismissed ->
            state.copy(isLanguagePickerVisible = false)

        // The store performs the switch, so the reducer only records the outcome.
        is AppShellIntent.Ui.LanguageSelected -> state

        is AppShellIntent.Internal.LanguageRestored -> state.copy(language = intent.language)

        // The language on screen deliberately stays put: it cannot change until the app is
        // restarted, and pretending otherwise would be a lie the next frame exposes.
        is AppShellIntent.Internal.LanguageScheduled -> state.copy(
            pendingLanguage = intent.language.takeIf { it != state.language },
        )



        is AppShellIntent.Internal.ThemeRestored -> state.copy(
            themeMode = intent.mode,
            isThemeLoaded = true,
        )
    }
}

/** The other theme. With exactly two variants a toggle is simply the opposite one. */
private fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.Light -> ThemeMode.Dark
    ThemeMode.Dark -> ThemeMode.Light
}
