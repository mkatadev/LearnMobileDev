package pl.prodevcode.learnmobiledev.presentation.app

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pl.prodevcode.learnmobiledev.core.mvi.MviStore
import pl.prodevcode.learnmobiledev.data.EffectiveLanguage
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.core.ui.StringCatalog
import pl.prodevcode.learnmobiledev.domain.usecase.GetAppLanguageUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetStringCatalogUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetThemeModeUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetAppLanguageUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetThemeModeUseCase

/**
 * Store for the app shell: tab, theme and content language.
 *
 * Persisting a preference is a side effect that runs **after** reduction, so the UI reacts
 * immediately while the disk write happens in the background. Waiting for the write before
 * updating the state would give both toggles a visible lag.
 *
 * A failed write is deliberately not surfaced: neither preference is critical enough to
 * interrupt the user — at worst it does not survive a restart.
 */
class AppShellViewModel(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val getAppLanguage: GetAppLanguageUseCase,
    private val setAppLanguage: SetAppLanguageUseCase,
    private val getStringCatalog: GetStringCatalogUseCase,
    private val effectiveLanguage: EffectiveLanguage,
) : MviStore<AppShellState, AppShellIntent, AppShellEffect>(
    initialState = AppShellState(),
    reducer = AppShellReducer,
) {

    init {
        viewModelScope.launch {
            // No stored choice means the device language decides, which EffectiveLanguage
            // already resolved synchronously. Restoring before the theme keeps the first
            // render on the right locale, so no screen has to reload.
            val stored = runCatching { getAppLanguage() }.getOrNull()
            val language = stored ?: effectiveLanguage.current()
            effectiveLanguage.apply(language)
            dispatch(
                AppShellIntent.Internal.LanguageApplied(language, contentChanged = false),
            )

            loadStrings()

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
            is AppShellIntent.Ui.LanguageToggled -> switchLanguage(before)
            else -> Unit
        }
    }

    /**
     * Applies the next language and reports whether the *effective* language changed.
     *
     * Switching between [AppLanguage.System] and an explicit choice that resolves to the
     * same tag changes nothing on screen, so content must not be reloaded for it.
     */
    private fun switchLanguage(state: AppShellState) {
        val next = state.language.next()
        val contentChanged = effectiveLanguage.apply(next)

        dispatch(AppShellIntent.Internal.LanguageApplied(next, contentChanged))

        // The catalogue is language-scoped, so a switch has to reload it as well.
        if (contentChanged) {
            viewModelScope.launch { loadStrings() }
        }

        viewModelScope.launch {
            try {
                setAppLanguage(next)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // A display preference — a failed write does not justify an error message.
            }
        }
    }

    /**
     * Loads the UI catalogue.
     *
     * A failure leaves [StringCatalog.EMPTY] in place, which renders raw keys — ugly but
     * still navigable, and far better than a blank screen. The content tests make sure a
     * broken catalogue never ships.
     */
    private suspend fun loadStrings() {
        val strings = runCatching { getStringCatalog() }.getOrNull().orEmpty()
        dispatch(AppShellIntent.Internal.StringsLoaded(StringCatalog(strings)))
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
