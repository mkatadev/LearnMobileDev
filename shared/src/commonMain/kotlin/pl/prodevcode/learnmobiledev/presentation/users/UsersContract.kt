package pl.prodevcode.learnmobiledev.presentation.users

import pl.prodevcode.learnmobiledev.core.mvi.MviEffect
import pl.prodevcode.learnmobiledev.core.mvi.MviIntent
import pl.prodevcode.learnmobiledev.core.mvi.MviState
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.domain.model.User

/**
 * # Screen contract
 *
 * One file = the complete screen specification: what can be seen (State), what can be done
 * (Intent), and what happens once (Effect). A new person on the project can read this file
 * and know everything.
 */

data class UsersState(
    val query: String = "",
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,
    /** IDs whose favorite save is in progress: blocks double-clicks. */
    val savingFavorites: Set<String> = emptySet(),
    val simulateNetworkError: Boolean = false,
    /** The open editor, or `null` when nobody is being edited. */
    val editor: UserEditor? = null,
) : MviState {

    /**
     * **Derived state is computed here, not in Compose.**
     * This makes the "empty screen" condition testable without UI and impossible to forget
     * in one of the three places where you render the list.
     */
    val showEmptyState: Boolean
        get() = !isLoading && error == null && users.isEmpty()

    val showContent: Boolean
        get() = users.isNotEmpty()

    val favoritesCount: Int
        get() = users.count { it.isFavorite }
}

/**
 * The edit form as state: the id being edited, the draft values and whether a save is in
 * flight.
 *
 * The draft is in the store rather than in a `remember` inside the composable, which is
 * what makes an edit survive rotation, replay on the timeline, and render identically on
 * iOS. A text field is a view of state, not the place state lives.
 */
data class UserEditor(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val isSaving: Boolean = false,
) {
    /**
     * A cheap, local sanity check so the Save button can be disabled — **not** the rule
     * that decides what may be stored. The server owns that and answers `422`; this only
     * spares the user a round trip for an obviously empty form.
     */
    val canSave: Boolean
        get() = !isSaving && name.isNotBlank() && role.isNotBlank() && email.isNotBlank()

    fun matches(user: User): Boolean =
        name.trim() == user.name && email.trim() == user.email && role.trim() == user.role

    companion object {
        fun of(user: User): UserEditor =
            UserEditor(userId = user.id, name = user.name, email = user.email, role = user.role)
    }
}

sealed interface UsersIntent : MviIntent {

    /** Intents coming from the user. */
    sealed interface Ui : UsersIntent {
        data object ScreenOpened : Ui
        data class QueryChanged(val query: String) : Ui
        data object RefreshClicked : Ui
        data object RetryClicked : Ui
        data class FavoriteToggled(val userId: String) : Ui
        data class UserClicked(val userId: String) : Ui
        data class SimulateErrorChanged(val enabled: Boolean) : Ui
        data object ErrorDismissed : Ui
        data class EditClicked(val userId: String) : Ui
        data class EditNameChanged(val name: String) : Ui
        data class EditEmailChanged(val email: String) : Ui
        data class EditRoleChanged(val role: String) : Ui
        data object EditCancelled : Ui
        data object EditSubmitted : Ui
    }

    /**
     * Results of asynchronous operations. Named in the past tense because they describe a
     * **fact** that has already happened, unlike Ui intents, which describe a wish.
     */
    sealed interface Internal : UsersIntent {
        data object LoadStarted : Internal
        data object RefreshStarted : Internal
        data class LoadSucceeded(val users: List<User>) : Internal
        data class LoadFailed(val message: UiText) : Internal
        data class FavoriteSaveStarted(val userId: String) : Internal
        data class FavoriteSaveSucceeded(val userId: String) : Internal
        data class FavoriteSaveFailed(val userId: String, val message: UiText) : Internal
        data object EditSaveStarted : Internal

        /** Carries the user **as stored by the server**, which is the new truth. */
        data class EditSaveSucceeded(val user: User) : Internal
        data class EditSaveFailed(val message: UiText) : Internal
    }
}

sealed interface UsersEffect : MviEffect {
    data class ShowMessage(val text: UiText) : UsersEffect
    data class OpenUserDetails(val userId: String) : UsersEffect
}
