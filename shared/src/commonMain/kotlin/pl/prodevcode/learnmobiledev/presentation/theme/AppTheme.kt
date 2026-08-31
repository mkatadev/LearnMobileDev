package pl.prodevcode.learnmobiledev.presentation.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import pl.prodevcode.learnmobiledev.domain.model.ThemeMode

/**
 * Application theme.
 *
 * Palettes are defined explicitly instead of relying on Material 3 defaults, because the
 * quiz and lab use colors semantically: `primaryContainer` means a correct answer,
 * `errorContainer` an incorrect one. Those meanings must stay legible in both themes.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E5AAC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E3FF),
    onPrimaryContainer = Color(0xFF00174A),
    secondary = Color(0xFF565E71),
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131B2C),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1B1F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFACC7FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF11447F),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFBEC6DC),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF1A1B1F),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF2E3038),
    onSurfaceVariant = Color(0xFFC4C6D0),
    background = Color(0xFF121317),
    onBackground = Color(0xFFE3E2E6),
)

/** Whether the dark palette should be used for the given preference. */
fun ThemeMode.isDark(): Boolean = this == ThemeMode.Dark

/**
 * Brand color: the same one used as the launcher icon background.
 *
 * Used on the transitional screen before we know the saved theme preference.
 * This makes startup look like a continuation of the splash screen, not a flash of the
 * wrong theme.
 */
val BrandColor = Color(0xFF2E5AAC)

@Composable
fun AppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val isDark = themeMode.isDark()
    SystemBarsAppearance(isDarkTheme = isDark)

    MaterialTheme(colorScheme = if (isDark) DarkColors else LightColors) {
        // Surface paints a theme-aware background under the whole app; without it, areas
        // outside components (for example behind the system bar) stay white in dark mode.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
