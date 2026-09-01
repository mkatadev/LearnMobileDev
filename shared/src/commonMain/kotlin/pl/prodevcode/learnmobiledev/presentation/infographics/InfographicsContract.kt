package pl.prodevcode.learnmobiledev.presentation.infographics

import pl.prodevcode.learnmobiledev.core.mvi.MviEffect
import pl.prodevcode.learnmobiledev.core.mvi.MviIntent
import pl.prodevcode.learnmobiledev.core.mvi.MviState
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.domain.model.Infographic

/**
 * # Screen contract
 *
 * One file = the complete screen specification: what can be seen (State), what can be done
 * (Intent), and what happens once (Effect).
 */

data class InfographicsState(
    val infographics: List<Infographic> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    /**
     * The infographic opened full-screen, or `null` when the list is showing.
     *
     * In the state rather than in a `remember`, because losing it would be a bug: a reader
     * who rotates the phone mid-zoom expects the picture to still be open, and that is
     * exactly the configuration change a `remember` does not survive.
     */
    val openedId: String? = null,
) : MviState {

    val opened: Infographic?
        get() = infographics.firstOrNull { it.id == openedId }

    val showEmptyState: Boolean
        get() = !isLoading && error == null && infographics.isEmpty()
}

sealed interface InfographicsIntent : MviIntent {

    sealed interface Ui : InfographicsIntent {
        data object ScreenOpened : Ui
        data object RetryClicked : Ui
        data class InfographicOpened(val id: String) : Ui
        data object ViewerDismissed : Ui
    }

    sealed interface Internal : InfographicsIntent {
        data object LoadStarted : Internal
        data class LoadSucceeded(val infographics: List<Infographic>) : Internal
        data class LoadFailed(val message: UiText) : Internal
    }
}

sealed interface InfographicsEffect : MviEffect {
    data class ShowMessage(val text: UiText) : InfographicsEffect
}
