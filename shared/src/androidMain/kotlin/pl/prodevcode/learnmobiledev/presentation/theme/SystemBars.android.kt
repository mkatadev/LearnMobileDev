package pl.prodevcode.learnmobiledev.presentation.theme

import android.app.Activity
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

@Composable
actual fun SystemBarsAppearance(isDarkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    // SideEffect rather than LaunchedEffect: this must land after every successful
    // composition, including the first frame, and it is a cheap synchronous call.
    SideEffect {
        val controller = (view.context as? Activity)?.window?.insetsController ?: return@SideEffect

        // "Light bars" means dark icons, so the flag is the inverse of the theme.
        val appearance = if (isDarkTheme) {
            0
        } else {
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        }

        controller.setSystemBarsAppearance(
            appearance,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        )
    }
}
