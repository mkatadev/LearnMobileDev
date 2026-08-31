package pl.prodevcode.learnmobiledev

import pl.prodevcode.learnmobiledev.core.ui.AppString
import pl.prodevcode.learnmobiledev.core.ui.localized
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
import pl.prodevcode.learnmobiledev.presentation.concurrency.ConcurrencyScreen
import pl.prodevcode.learnmobiledev.presentation.concurrency.ConcurrencyViewModel
import pl.prodevcode.learnmobiledev.presentation.learn.LearnScreen
import pl.prodevcode.learnmobiledev.presentation.learn.LearnViewModel
import pl.prodevcode.learnmobiledev.presentation.quiz.QuizScreen
import pl.prodevcode.learnmobiledev.presentation.quiz.QuizViewModel
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.presentation.app.nameRes
import pl.prodevcode.learnmobiledev.presentation.theme.AppTheme
import pl.prodevcode.learnmobiledev.presentation.theme.Spacing
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
    if (!shellState.isThemeLoaded) {
        Box(modifier = Modifier.fillMaxSize().background(BrandColor))
        return
    }

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
                        onOpenLanguagePicker = {
                            shell.dispatch(AppShellIntent.Ui.LanguagePickerOpened)
                        },
                    )

                    Tab.Demo -> DemoTab()
                    Tab.Test -> TestTab()
                    Tab.Sync -> SyncTab()
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

        if (shellState.isLanguagePickerVisible) {
            LanguagePickerDialog(
                selected = shellState.selectedLanguage,
                pending = shellState.pendingLanguage,
                onSelect = { shell.dispatch(AppShellIntent.Ui.LanguageSelected(it)) },
                onDismiss = { shell.dispatch(AppShellIntent.Ui.LanguagePickerDismissed) },
            )
        }
    }
}

/**
 * Language picker.
 *
 * A list of the available languages rather than a toggle: a toggle only reads as an action
 * when there are exactly two options and the user already knows what the other one is, and
 * it cannot show that a *different* language is scheduled.
 *
 * The restart line appears only once a change is actually pending. Showing it permanently
 * would warn about something that has not happened, and users stop reading warnings that
 * are always on screen.
 *
 * There is no "close the app" button. Android could finish the activity, but no supported
 * API relaunches an iOS app — `exit(0)` is indistinguishable from a crash and is grounds
 * for App Store rejection. A button that worked on one platform and not the other would be
 * worse than none.
 */
@Composable
private fun LanguagePickerDialog(
    selected: AppLanguage,
    pending: AppLanguage?,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(AppString.LanguagePickerTitle)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    LanguageOption(
                        language = language,
                        isSelected = language == selected,
                        onClick = { onSelect(language) },
                    )
                }

                pending?.let {
                    Text(
                        text = localized(AppString.LanguageRestartHint, localized(it.nameRes())),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = Spacing.itemSpacing),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(localized(AppString.ActionDone))
            }
        },
    )
}

/** One row of the picker. The whole row is the target, not just the radio button. */
@Composable
private fun LanguageOption(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(Modifier.width(Spacing.small))
        Text(localized(language.nameRes()))
    }
}

/** Tab with the course material. Both switches live here, in the course header. */
@Composable
private fun LearnTab(
    themeLabel: AppString,
    languageLabel: AppString,
    onToggleTheme: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
) {
    val viewModel: LearnViewModel = koinViewModel()

    LearnScreen(
        viewModel = viewModel,
        themeLabel = themeLabel,
        languageLabel = languageLabel,
        onToggleTheme = onToggleTheme,
        onOpenLanguagePicker = onOpenLanguagePicker,
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
private fun TestTab() {
    val viewModel: QuizViewModel = koinViewModel()

    QuizScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
}

/** Tab with the concurrency lab. */
@Composable
private fun SyncTab() {
    val viewModel: ConcurrencyViewModel = koinViewModel()

    ConcurrencyScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
}
