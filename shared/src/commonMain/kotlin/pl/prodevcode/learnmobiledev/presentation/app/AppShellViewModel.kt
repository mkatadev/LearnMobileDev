package pl.prodevcode.learnmobiledev.presentation.app

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pl.prodevcode.learnmobiledev.core.mvi.MviStore
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.PlatformLocale
import pl.prodevcode.learnmobiledev.domain.usecase.GetThemeModeUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetAppLanguageUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetThemeModeUseCase

/**
 * Store for the app shell: tab, theme and content language.
 *
 * Persisting a preference is a side effect that runs **after** reduction, so the UI reacts
 * immediately while the disk write happens in the background. Waiting for the write before
 * updating the state would give the theme toggle a visible lag.
 *
 * The language is the exception: it cannot take effect until the app is restarted, because
 * resources follow the platform locale. See [PlatformLocale].
 *
 * A failed write is deliberately not surfaced: neither preference is critical enough to
 * interrupt the user — at worst it does not survive a restart.
 */
class AppShellViewModel(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setAppLanguage: SetAppLanguageUseCase,
    private val platformLocale: PlatformLocale,
) : MviStore<AppShellState, AppShellIntent, AppShellEffect>(
    initialState = AppShellState(),
    reducer = AppShellReducer,
) {

    init {
        // The platform locale is the single source of truth for what is on screen: the
        // resources have already been resolved against it. Reading the stored preference
        // instead could claim a language the UI is demonstrably not rendering in — which
        // is exactly what happens between the switch and the restart.
        dispatch(
            AppShellIntent.Internal.LanguageRestored(
                AppLanguage.fromTag(platformLocale.current()) ?: AppLanguage.DEFAULT,
            ),
        )

        viewModelScope.launch {
            val theme = runCatching { getThemeMode() }.getOrNull()
            dispatch(
                AppShellIntent.Internal.ThemeRestored(theme ?: AppShellState().themeMode),
            )
        }
    }

    override fun onIntentProcessed(
        intent: AppShellIntent,
        before: AppShellState,
        after: AppShellState,
    ) {
        when (intent) {
            is AppShellIntent.Ui.ThemeToggled -> persistTheme(after)
            is AppShellIntent.Ui.LanguageSelected -> selectLanguage(intent.language)
            else -> Unit
        }
    }

    /**
     * Records the chosen language, to be applied when the app is started again.
     *
     * Nothing on screen changes: resources were resolved against the platform locale at
     * launch and cannot be re-resolved in place. The state carries the pending choice so
     * the UI can say so, instead of leaving the user with a picker that appears to have
     * done nothing.
     *
     * The platform write happens first and synchronously. It is the one that actually
     * decides the next launch; the stored preference merely mirrors it for the app's own
     * use, so losing that write is survivable while losing this one is not.
     */
    private fun selectLanguage(language: AppLanguage) {
        platformLocale.apply(language.tag)
        dispatch(AppShellIntent.Internal.LanguageScheduled(language))

        viewModelScope.launch {
            try {
                setAppLanguage(language)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // A display preference — a failed write does not justify an error message.
            }
        }
    }

    private fun persistTheme(state: AppShellState) {
        viewModelScope.launch {
            try {
                setThemeMode(state.themeMode)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Same reasoning as above.
            }
        }
    }
}
