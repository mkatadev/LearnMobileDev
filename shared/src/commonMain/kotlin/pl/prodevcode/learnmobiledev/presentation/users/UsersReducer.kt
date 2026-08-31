package pl.prodevcode.learnmobiledev.presentation.users

import pl.prodevcode.learnmobiledev.core.mvi.Reducer
import pl.prodevcode.learnmobiledev.domain.model.User

/**
 * Pure reducer for the users screen.
 *
 * Notice: **zero** `suspend`, zero repository, zero `Dispatchers`. That lets ordinary unit
 * tests cover the entire screen logic in milliseconds.
 *
 * Purely "command-like" intents (for example [UsersIntent.Ui.RetryClicked]) do not change
 * state here: their effect is asynchronous work triggered in [UsersViewModel], which will
 * come back as `Internal`.
 */
val UsersReducer = Reducer<UsersState, UsersIntent> { state, intent ->
    when (intent) {
        is UsersIntent.Ui.ScreenOpened -> state
        is UsersIntent.Ui.RetryClicked -> state
        is UsersIntent.Ui.RefreshClicked -> state
        is UsersIntent.Ui.UserClicked -> state

        is UsersIntent.Ui.QueryChanged -> state.copy(query = intent.query)

        is UsersIntent.Ui.SimulateErrorChanged ->
            state.copy(simulateNetworkError = intent.enabled)

        is UsersIntent.Ui.ErrorDismissed -> state.copy(error = null)

        // Optimistic update: change the UI immediately, before the server confirms.
        // When a previous save is still running, ignore the click: double-tap protection
        // implemented in pure logic, not through `enabled = false` in Compose.
        is UsersIntent.Ui.FavoriteToggled ->
            if (intent.userId in state.savingFavorites) {
                state
            } else {
                state.copy(users = state.users.toggleFavorite(intent.userId))
            }

        is UsersIntent.Internal.LoadStarted -> state.copy(
            isLoading = true,
            error = null,
        )

        is UsersIntent.Internal.RefreshStarted -> state.copy(
            isRefreshing = true,
            error = null,
        )

        is UsersIntent.Internal.LoadSucceeded -> state.copy(
            isLoading = false,
            isRefreshing = false,
            error = null,
            // Keep local "favorite" values for items whose save is in flight.
            users = intent.users.map { fresh ->
                val pending = state.savingFavorites.contains(fresh.id)
                if (pending) {
                    fresh.copy(
                        isFavorite = state.users.firstOrNull { it.id == fresh.id }?.isFavorite
                            ?: fresh.isFavorite,
                    )
                } else {
                    fresh
                }
            },
        )

        is UsersIntent.Internal.LoadFailed -> state.copy(
            isLoading = false,
            isRefreshing = false,
            error = intent.message,
        )

        is UsersIntent.Internal.FavoriteSaveStarted -> state.copy(
            savingFavorites = state.savingFavorites + intent.userId,
        )

        is UsersIntent.Internal.FavoriteSaveSucceeded -> state.copy(
            savingFavorites = state.savingFavorites - intent.userId,
        )

        // Rollback: the server rejected the change, so we undo the optimistic update.
        is UsersIntent.Internal.FavoriteSaveFailed -> state.copy(
            savingFavorites = state.savingFavorites - intent.userId,
            users = state.users.toggleFavorite(intent.userId),
        )

        // Opening the editor copies the row into a draft. Editing the list in place would
        // mean a cancelled edit had already changed what other screens read.
        is UsersIntent.Ui.EditClicked -> state.users
            .firstOrNull { it.id == intent.userId }
            ?.let { state.copy(editor = UserEditor.of(it)) }
            ?: state

        is UsersIntent.Ui.EditNameChanged ->
            state.mapEditor { it.copy(name = intent.name) }

        is UsersIntent.Ui.EditEmailChanged ->
            state.mapEditor { it.copy(email = intent.email) }

        is UsersIntent.Ui.EditRoleChanged ->
            state.mapEditor { it.copy(role = intent.role) }

        is UsersIntent.Ui.EditCancelled -> state.copy(editor = null)

        // Command-like: whether to call the server is the store's decision, not the
        // reducer's, so the draft is left exactly as the user typed it.
        is UsersIntent.Ui.EditSubmitted -> state

        is UsersIntent.Internal.EditSaveStarted -> state.mapEditor { it.copy(isSaving = true) }

        // No optimistic update here, unlike the favorite: an edit is several fields that
        // the server may normalize or reject, so the list adopts what came back and
        // nothing before that.
        is UsersIntent.Internal.EditSaveSucceeded -> state.copy(
            editor = null,
            users = state.users.map { if (it.id == intent.user.id) intent.user else it },
        )

        // The editor stays open with the rejected values: the user needs to see and fix
        // them, and a form that empties itself on failure is a form people retype.
        is UsersIntent.Internal.EditSaveFailed -> state.mapEditor { it.copy(isSaving = false) }
    }
}

private fun UsersState.mapEditor(transform: (UserEditor) -> UserEditor): UsersState =
    editor?.let { copy(editor = transform(it)) } ?: this

private fun List<User>.toggleFavorite(userId: String) =
    map { if (it.id == userId) it.copy(isFavorite = !it.isFavorite) else it }
