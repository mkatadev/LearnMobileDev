package pl.prodevcode.learnmobiledev.presentation.app

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.model.ThemeMode

/** The app shell is a pure function too: tab and theme are testable without any UI. */
class AppShellReducerTest {

    private fun reduce(state: AppShellState, intent: AppShellIntent) =
        AppShellReducer.reduce(state, intent)

    @Test
    fun `it opens on the learn tab`() {
        val state = AppShellState()

        assertEquals(Tab.Learn, state.tab)
    }

    @Test
    fun `selecting a tab changes only the tab`() {
        val result = reduce(
            AppShellState(themeMode = ThemeMode.Dark),
            AppShellIntent.Ui.TabSelected(Tab.Test),
        )

        assertEquals(Tab.Test, result.tab)
        assertEquals(ThemeMode.Dark, result.themeMode, "the theme must not change")
    }

    @Test
    fun `the theme toggle alternates between the two palettes`() {
        var state = AppShellState(themeMode = ThemeMode.Light)

        state = reduce(state, AppShellIntent.Ui.ThemeToggled)
        assertEquals(ThemeMode.Dark, state.themeMode)

        state = reduce(state, AppShellIntent.Ui.ThemeToggled)
        assertEquals(ThemeMode.Light, state.themeMode, "toggling twice returns to the start")
    }

    @Test
    fun `changing the theme does not reset the selected tab`() {
        val result = reduce(
            AppShellState(tab = Tab.Sync),
            AppShellIntent.Ui.ThemeToggled,
        )

        assertEquals(Tab.Sync, result.tab)
    }

    @Test
    fun `restoring a stored theme marks the state as loaded`() {
        val result = reduce(
            AppShellState(),
            AppShellIntent.Internal.ThemeRestored(ThemeMode.Dark),
        )

        assertEquals(ThemeMode.Dark, result.themeMode)
        kotlin.test.assertTrue(result.isThemeLoaded)
    }

    @Test
    fun `the initial state is not marked as loaded`() {
        // The splash gate relies on this: until the preference comes back from disk we
        // render nothing, to avoid a flash of the wrong theme.
        kotlin.test.assertFalse(AppShellState().isThemeLoaded)
    }

    @Test
    fun `the restored language is the one on screen`() {
        val result = reduce(
            AppShellState(),
            AppShellIntent.Internal.LanguageRestored(AppLanguage.Polish),
        )

        assertEquals(AppLanguage.Polish, result.language)
        kotlin.test.assertNull(result.pendingLanguage)
    }

    @Test
    fun `a scheduled language is pending and does not change the current one`() {
        // Resources were resolved at launch, so the UI is still rendering the old
        // language. Claiming otherwise would contradict what is on screen.
        val result = reduce(
            AppShellState(language = AppLanguage.English),
            AppShellIntent.Internal.LanguageScheduled(AppLanguage.Polish),
        )

        assertEquals(AppLanguage.English, result.language)
        assertEquals(AppLanguage.Polish, result.pendingLanguage)
    }

    @Test
    fun `scheduling the language already in effect clears the notice`() {
        // Toggling twice lands back where it started: nothing will change on restart, so
        // asking the user to restart would be a lie.
        val result = reduce(
            AppShellState(language = AppLanguage.English, pendingLanguage = AppLanguage.Polish),
            AppShellIntent.Internal.LanguageScheduled(AppLanguage.English),
        )

        kotlin.test.assertNull(result.pendingLanguage)
    }

    @Test
    fun `closing the picker keeps the scheduled language`() {
        // Only the dialog goes away; the choice is already stored on the platform and
        // still applies on the next launch.
        val result = reduce(
            AppShellState(pendingLanguage = AppLanguage.Polish, isLanguagePickerVisible = true),
            AppShellIntent.Ui.LanguagePickerDismissed,
        )

        kotlin.test.assertFalse(result.isLanguagePickerVisible)
        assertEquals(AppLanguage.Polish, result.pendingLanguage)
    }

    @Test
    fun `the picker preselects the scheduled language rather than the active one`() {
        // Reopening the picker must show what will happen, otherwise the choice the user
        // just made looks like it was discarded.
        val state = AppShellState(
            language = AppLanguage.English,
            pendingLanguage = AppLanguage.Polish,
        )

        assertEquals(AppLanguage.Polish, state.selectedLanguage)
    }

    @Test
    fun `the picker preselects the active language when nothing is scheduled`() {
        assertEquals(
            AppLanguage.English,
            AppShellState(language = AppLanguage.English).selectedLanguage,
        )
    }

    @Test
    fun `every tab points at distinct catalogue entries`() {
        // A copy-paste slip in the enum would show the same label or glyph on two tabs.
        assertEquals(Tab.entries.size, Tab.entries.map { it.labelRes }.toSet().size)
        assertEquals(Tab.entries.size, Tab.entries.map { it.iconRes }.toSet().size)
    }
}
