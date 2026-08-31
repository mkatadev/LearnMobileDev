package pl.prodevcode.learnmobiledev.presentation.theme

import androidx.compose.runtime.Composable

/**
 * No-op on iOS.
 *
 * The status bar style there is owned by the hosting `UIViewController`, and
 * `ComposeUIViewController` already derives it from the rendered content, so the icons
 * stay readable without any extra work.
 */
@Composable
actual fun SystemBarsAppearance(isDarkTheme: Boolean) = Unit
