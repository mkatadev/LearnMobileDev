package pl.prodevcode.learnmobiledev.presentation.app

import pl.prodevcode.learnmobiledev.core.ui.AppString
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.model.ThemeMode

/**
 * Navigation target. Kept outside the screen's MVI state because navigation is the
 * **host's** responsibility, not the list screen's.
 */
sealed interface Route {
    data object UsersList : Route
    data class UserDetails(val userId: String) : Route
}

/**
 * Bottom navigation tabs.
 *
 * Both the label and the icon are catalogue references rather than literals. The glyph is
 * content too: a locale may want a different symbol, and hard-coding it would leave the
 * tab bar as the only untranslatable part of the UI.
 */
enum class Tab(val labelRes: AppString, val iconRes: AppString) {
    Learn(AppString.TabLearn, AppString.TabLearnIcon),
    Demo(AppString.TabDemo, AppString.TabDemoIcon),
    Test(AppString.TabTest, AppString.TabTestIcon),
    Sync(AppString.TabSync, AppString.TabSyncIcon),
}

/** Label for the current theme mode. */
fun ThemeMode.labelRes(): AppString = when (this) {
    ThemeMode.Light -> AppString.ThemeLight
    ThemeMode.Dark -> AppString.ThemeDark
}

/** Label of the current content language. */
fun AppLanguage.labelRes(): AppString = when (this) {
    AppLanguage.English -> AppString.LanguageEnglish
    AppLanguage.Polish -> AppString.LanguagePolish
}
