package pl.prodevcode.learnmobiledev.presentation.theme

import androidx.compose.runtime.Composable

/**
 * Keeps the system bar icons readable against the app theme.
 *
 * Android draws status bar icons in a single tone chosen by the *window*, not by what is
 * painted beneath them. `enableEdgeToEdge()` picks that tone once, from the system theme,
 * so after an in-app switch to dark mode the icons stay dark and disappear against the
 * dark background.
 *
 * This is one of the few places where `expect/actual` beats the interface-plus-DI rule
 * from AGENTS.md: it is a pure UI side effect that needs the platform window handle, has
 * no logic worth testing and no meaningful alternative implementation.
 *
 * The Android side uses the platform `WindowInsetsController` (API 30+) rather than
 * `WindowCompat`, so the project avoids an androidx.core version that would demand a
 * newer compileSdk and AGP than this build targets. `minSdk` is 31, so the compat wrapper
 * would buy nothing anyway.
 */
@Composable
expect fun SystemBarsAppearance(isDarkTheme: Boolean)
