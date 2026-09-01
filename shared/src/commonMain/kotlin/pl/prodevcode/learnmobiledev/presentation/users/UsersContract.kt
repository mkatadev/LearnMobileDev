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
    /** The open create form, or `null` when nobody is being added. */
    val creator: UserEditor? = null,
    /**
     * The roles the server accepts, offered as a closed list.
     *
     * Empty until the service answers. The forms render a picker over exactly these, so
     * the app cannot offer a role the backend would refuse.
     */
    val roles: List<String> = emptyList(),
    /**
     * The user a delete is being confirmed for.
     *
     * In the state and not in a `remember`, because a dialog that vanishes on rotation and
     * leaves the row untouched is a bug the user has to notice to report.
     */
    val pendingDeletion: User? = null,
    /** IDs whose deletion is in flight: the row is disabled rather than removed twice. */
    val deleting: Set<String> = emptySet(),
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
 *
 * The same type backs the create form, where [userId] is empty because the server has not
 * assigned one yet. Two near-identical types would mean two sets of validation rules that
 * drift, and the form is the same form — only its destination differs.
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

        /**
         * A blank form for a user who does not exist yet.
         *
         * The role is pre-selected rather than left empty: the picker offers a closed list,
         * so there is no such thing as a valid blank role, and starting on one would only
         * let the user submit a form the server is certain to refuse.
         */
        fun empty(defaultRole: String = ""): UserEditor =
            UserEditor(userId = "", name = "", email = "", role = defaultRole)
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

        data object AddClicked : Ui
        data class CreateNameChanged(val name: String) : Ui
        data class CreateEmailChanged(val email: String) : Ui
        data class CreateRoleChanged(val role: String) : Ui
        data object CreateCancelled : Ui
        data object CreateSubmitted : Ui

        /** Asks for confirmation; it does not delete. Destructive actions get a second look. */
        data class DeleteClicked(val userId: String) : Ui
        data object DeleteConfirmed : Ui
        data object DeleteDismissed : Ui
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

        data object CreateSaveStarted : Internal

        /** The created row, id included — the client could not have known it. */
        data class CreateSaveSucceeded(val user: User) : Internal
        data class CreateSaveFailed(val message: UiText) : Internal

        data class DeleteStarted(val userId: String) : Internal
        data class DeleteSucceeded(val userId: String) : Internal
        data class DeleteFailed(val userId: String, val message: UiText) : Internal

        /** The roles the service publishes. A failure is silent: see the store. */
        data class RolesLoaded(val roles: List<String>) : Internal
    }
}

sealed interface UsersEffect : MviEffect {
    data class ShowMessage(val text: UiText) : UsersEffect
    data class OpenUserDetails(val userId: String) : UsersEffect
}
