package pl.prodevcode.learnmobiledev.presentation.infographics

import pl.prodevcode.learnmobiledev.core.mvi.Reducer

/**
 * Pure reducer for the infographics screen.
 *
 * Command-like intents ([InfographicsIntent.Ui.ScreenOpened], `RetryClicked`) do not change
 * state here: what they trigger is asynchronous work started by the store, which comes back
 * as `Internal`.
 */
val InfographicsReducer = Reducer<InfographicsState, InfographicsIntent> { state, intent ->
    when (intent) {
        is InfographicsIntent.Ui.ScreenOpened -> state
        is InfographicsIntent.Ui.RetryClicked -> state

        // Guarded in the reducer rather than by hiding the tap target: opening an id that
        // is not in the list would leave the viewer showing nothing, and a programmatic
        // dispatch could reach it even if the UI could not.
        is InfographicsIntent.Ui.InfographicOpened ->
            if (state.infographics.any { it.id == intent.id }) {
                state.copy(openedId = intent.id)
            } else {
                state
            }

        is InfographicsIntent.Ui.ViewerDismissed -> state.copy(openedId = null)

        is InfographicsIntent.Internal.LoadStarted -> state.copy(
            isLoading = true,
            error = null,
        )

        is InfographicsIntent.Internal.LoadSucceeded -> state.copy(
            isLoading = false,
            error = null,
            infographics = intent.infographics,
            // A reload may no longer publish what is on screen. Closing the viewer beats
            // leaving it open on a picture the service has stopped serving.
            openedId = state.openedId?.takeIf { id -> intent.infographics.any { it.id == id } },
        )

        is InfographicsIntent.Internal.LoadFailed -> state.copy(
            isLoading = false,
            error = intent.message,
        )
    }
}
