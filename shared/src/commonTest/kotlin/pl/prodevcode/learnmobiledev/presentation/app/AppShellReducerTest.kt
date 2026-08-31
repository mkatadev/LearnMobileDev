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
    fun `applying a language records it and bumps the content revision`() {
        val result = reduce(
            AppShellState(),
            AppShellIntent.Internal.LanguageApplied(AppLanguage.Polish, contentChanged = true),
        )

        assertEquals(AppLanguage.Polish, result.language)
        assertEquals(1, result.contentRevision)
    }

    @Test
    fun `an unchanged language leaves the content revision alone`() {
        // Screens key their reload on the revision, so bumping it here would cause a
        // pointless reload of identical content.
        val result = reduce(
            AppShellState(contentRevision = 3),
            AppShellIntent.Internal.LanguageApplied(AppLanguage.English, contentChanged = false),
        )

        assertEquals(3, result.contentRevision)
    }

    @Test
    fun `the language toggle alternates between the two languages`() {
        assertEquals(AppLanguage.Polish, AppLanguage.English.next())
        assertEquals(AppLanguage.English, AppLanguage.Polish.next())
    }

    @Test
    fun `every tab points at distinct catalogue entries`() {
        // A copy-paste slip in the enum would show the same label or glyph on two tabs.
        assertEquals(Tab.entries.size, Tab.entries.map { it.labelRes }.toSet().size)
        assertEquals(Tab.entries.size, Tab.entries.map { it.iconRes }.toSet().size)
    }
}
