package pl.prodevcode.learnmobiledev

import pl.prodevcode.learnmobiledev.core.ui.AppString
import pl.prodevcode.learnmobiledev.core.ui.LocalStrings
import pl.prodevcode.learnmobiledev.core.ui.localized
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.koin.core.module.Module
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import pl.prodevcode.learnmobiledev.di.appModules
import pl.prodevcode.learnmobiledev.di.previewPlatformModule
import pl.prodevcode.learnmobiledev.presentation.app.AppShellIntent
import pl.prodevcode.learnmobiledev.presentation.app.AppShellViewModel
import pl.prodevcode.learnmobiledev.presentation.app.Route
import pl.prodevcode.learnmobiledev.presentation.app.Tab
import pl.prodevcode.learnmobiledev.presentation.app.labelRes
import pl.prodevcode.learnmobiledev.presentation.app.UserDetailsScreen
import pl.prodevcode.learnmobiledev.presentation.concurrency.ConcurrencyIntent
import pl.prodevcode.learnmobiledev.presentation.concurrency.ConcurrencyScreen
import pl.prodevcode.learnmobiledev.presentation.concurrency.ConcurrencyViewModel
import pl.prodevcode.learnmobiledev.presentation.learn.LearnIntent
import pl.prodevcode.learnmobiledev.presentation.learn.LearnScreen
import pl.prodevcode.learnmobiledev.presentation.learn.LearnViewModel
import pl.prodevcode.learnmobiledev.presentation.quiz.QuizIntent
import pl.prodevcode.learnmobiledev.presentation.quiz.QuizScreen
import pl.prodevcode.learnmobiledev.presentation.quiz.QuizViewModel
import pl.prodevcode.learnmobiledev.presentation.theme.AppTheme
import pl.prodevcode.learnmobiledev.presentation.theme.BrandColor
import pl.prodevcode.learnmobiledev.presentation.users.UsersScreen
import pl.prodevcode.learnmobiledev.presentation.users.UsersViewModel

/**
 * Entry point shared by Android and iOS.
 *
 * The DI container starts here (`KoinApplication`), so both platforms get the same
 * dependency graph without duplicating configuration.
 */
/**
 * @param platformModule platform-specific dependencies (persistent preference storage).
 *        The default value lets the Compose preview run without configuration.
 */
@Composable
@Preview
fun App(platformModule: Module = previewPlatformModule()) {
    KoinApplication(application = { modules(appModules(platformModule)) }) {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val shell: AppShellViewModel = koinViewModel()
    val shellState by shell.state.collectAsState()

    // The theme preference loads from disk asynchronously. Rendering content before it is
    // read caused a light-theme flash for people who had selected dark mode.
    // Instead of guessing, we show a brand-colored screen for those few frames.
    // Both gates matter: without the catalogue the UI would flash raw keys, and without
    // the theme it would flash the wrong palette.
    if (!shellState.isThemeLoaded || !shellState.areStringsLoaded) {
        Box(modifier = Modifier.fillMaxSize().background(BrandColor))
        return
    }

    CompositionLocalProvider(LocalStrings provides shellState.strings) {
        AppTheme(themeMode = shellState.themeMode) {
            // Insets are applied surgically rather than through safeContentPadding() on the
            // whole layout: that also added side margins (cutouts, rounded corners) and
            // pushed the bottom navigation away from the screen edge.
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .statusBarsPadding()
                        .displayCutoutPadding(),
                ) {
                    when (shellState.tab) {
                        Tab.Learn -> LearnTab(
                            themeLabel = shellState.themeMode.labelRes(),
                            languageLabel = shellState.language.labelRes(),
                            onToggleTheme = { shell.dispatch(AppShellIntent.Ui.ThemeToggled) },
                            onToggleLanguage = {
                                shell.dispatch(AppShellIntent.Ui.LanguageToggled)
                            },
                            contentRevision = shellState.contentRevision,
                        )

                        Tab.Demo -> DemoTab()
                        Tab.Test -> TestTab(shellState.contentRevision)
                        Tab.Sync -> SyncTab(shellState.contentRevision)
                    }
                }

                // NavigationBar handles the bottom system inset itself, so it sticks to
                // the screen edge as it should.
                NavigationBar {
                    Tab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = shellState.tab == entry,
                            onClick = { shell.dispatch(AppShellIntent.Ui.TabSelected(entry)) },
                            icon = { Text(localized(entry.iconRes)) },
                            label = { Text(localized(entry.labelRes)) },
                        )
                    }
                }
            }
        }
    }
}

/** Tab with the course material. Both switches live here, in the course header. */
@Composable
private fun LearnTab(
    themeLabel: AppString,
    languageLabel: AppString,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    contentRevision: Int,
) {
    val viewModel: LearnViewModel = koinViewModel()

    // The store outlives a language switch, so the screen has to tell it that its cached
    // content now belongs to the wrong locale. Keying on the revision makes that explicit.
    ReloadOnLanguageChange(contentRevision) {
        viewModel.dispatch(LearnIntent.Ui.ContentInvalidated)
    }

    LearnScreen(
        viewModel = viewModel,
        themeLabel = themeLabel,
        languageLabel = languageLabel,
        onToggleTheme = onToggleTheme,
        onToggleLanguage = onToggleLanguage,
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * Tab with the working example.
 *
 * Navigating to details is a reaction to an **effect** (`OpenUserDetails`), not to screen
 * state, so returning from details does not open them again after recomposition.
 */
@Composable
private fun DemoTab() {
    val viewModel: UsersViewModel = koinViewModel()
    var route by remember { mutableStateOf<Route>(Route.UsersList) }

    when (val current = route) {
        is Route.UsersList -> UsersScreen(
            viewModel = viewModel,
            onOpenDetails = { route = Route.UserDetails(it) },
            modifier = Modifier.fillMaxSize(),
        )

        is Route.UserDetails -> UserDetailsScreen(
            viewModel = viewModel,
            userId = current.userId,
            onBack = { route = Route.UsersList },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Tab with the knowledge test. */
@Composable
private fun TestTab(contentRevision: Int) {
    val viewModel: QuizViewModel = koinViewModel()

    ReloadOnLanguageChange(contentRevision) {
        viewModel.dispatch(QuizIntent.Ui.ContentInvalidated)
    }

    QuizScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
}

/** Tab with the concurrency lab. */
@Composable
private fun SyncTab(contentRevision: Int) {
    val viewModel: ConcurrencyViewModel = koinViewModel()

    ReloadOnLanguageChange(contentRevision) {
        viewModel.dispatch(ConcurrencyIntent.Ui.ContentInvalidated)
    }

    ConcurrencyScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
}

/**
 * Runs [onChange] whenever the content revision changes, skipping the initial composition.
 *
 * The first render already uses the restored language, so invalidating there would trigger
 * a pointless reload on every tab switch.
 */
@Composable
private fun ReloadOnLanguageChange(contentRevision: Int, onChange: () -> Unit) {
    var lastRevision by rememberSaveable { mutableStateOf(contentRevision) }

    LaunchedEffect(contentRevision) {
        if (contentRevision != lastRevision) {
            lastRevision = contentRevision
            onChange()
        }
    }
}
