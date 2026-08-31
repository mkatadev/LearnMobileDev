package pl.prodevcode.learnmobiledev.domain.model

/**
 * Theme preference.
 *
 * There is deliberately no "system" variant: the device setting only decides the
 * **initial** value, and from then on the choice is explicit. Mirrors [AppLanguage].
 */
enum class ThemeMode {
    Light,
    Dark,
    ;

    companion object {
        fun fromDarkFlag(isDark: Boolean): ThemeMode = if (isDark) Dark else Light
    }
}
