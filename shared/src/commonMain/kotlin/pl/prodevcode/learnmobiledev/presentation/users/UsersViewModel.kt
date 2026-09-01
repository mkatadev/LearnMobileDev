package pl.prodevcode.learnmobiledev.presentation.users

import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.mvi.LoggingMiddleware
import pl.prodevcode.learnmobiledev.core.mvi.Middleware
import pl.prodevcode.learnmobiledev.core.mvi.MviStore
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.core.ui.asUiText
import pl.prodevcode.learnmobiledev.domain.repository.NetworkFailureSwitch
import pl.prodevcode.learnmobiledev.domain.repository.UserSyncException
import pl.prodevcode.learnmobiledev.domain.usecase.CreateUserUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.DeleteUserUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetRolesUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SearchUsersUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetFavoriteUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.UpdateUserUseCase

/**
 * Store for the users screen — this is where all the messy parts live: I/O,
 * cancellation, debounce and retry.
 *
 * A rule worth remembering: *the reducer knows WHAT happened, the store knows WHAT TO DO
 * next*.
 *
 * It depends only on use cases (the domain layer). It knows nothing about Ktor, Room or
 * Compose, so the same code serves Android and iOS.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class UsersViewModel(
    private val searchUsers: SearchUsersUseCase,
    private val setFavorite: SetFavoriteUseCase,
    private val updateUser: UpdateUserUseCase,
    private val createUser: CreateUserUseCase,
    private val deleteUser: DeleteUserUseCase,
    private val getRoles: GetRolesUseCase,
    private val networkFailureSwitch: NetworkFailureSwitch? = null,
    middlewares: List<Middleware<UsersState, UsersIntent>> = listOf(LoggingMiddleware("Users")),
) : MviStore<UsersState, UsersIntent, UsersEffect>(
    initialState = UsersState(),
    reducer = UsersReducer,
    middlewares = middlewares,
) {

    private data class LoadRequest(val query: String, val isRefresh: Boolean)

    private val queryFlow = MutableStateFlow("")

    /**
     * Every load request travels through one channel consumed by `collectLatest`.
     * The effect: a new request **cancels** the previous one, so results of a stale query
     * can never overwrite newer ones (the classic search-field bug).
     *
     * A `Channel` rather than a `MutableSharedFlow` on purpose: a channel also buffers a
     * request issued before the consumer starts, so nothing is lost on screen entry.
     */
    private val loadRequests = Channel<LoadRequest>(capacity = Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            queryFlow
                .drop(1) // the first value is the initial state, not a user edit
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query -> loadRequests.trySend(LoadRequest(query, isRefresh = false)) }
        }
        viewModelScope.launch {
            loadRequests.receiveAsFlow().collectLatest { request -> performLoad(request) }
        }
    }

    override fun onIntentProcessed(
        intent: UsersIntent,
        before: UsersState,
        after: UsersState,
    ) {
        when (intent) {
            is UsersIntent.Ui.ScreenOpened -> {
                loadRequests.trySend(LoadRequest(after.query, isRefresh = false))
                loadRoles(after)
            }

            is UsersIntent.Ui.QueryChanged ->
                queryFlow.value = intent.query

            is UsersIntent.Ui.RetryClicked ->
                loadRequests.trySend(LoadRequest(after.query, isRefresh = false))

            is UsersIntent.Ui.RefreshClicked ->
                loadRequests.trySend(LoadRequest(after.query, isRefresh = true))

            is UsersIntent.Ui.SimulateErrorChanged ->
                networkFailureSwitch?.failNextLoad = intent.enabled

            is UsersIntent.Ui.UserClicked ->
                emitEffect(UsersEffect.OpenUserDetails(intent.userId))

            is UsersIntent.Ui.FavoriteToggled ->
                // The reducer ignored the click (a save is in flight), so the state did not
                // change and there is no reason to perform I/O.
                if (before != after) saveFavorite(intent.userId, after)

            is UsersIntent.Ui.EditSubmitted -> submitEdit(after)

            is UsersIntent.Ui.CreateSubmitted -> submitCreate(after)

            // `before` and not `after`: the reducer has already closed the dialog, so the
            // row it named is only still available on the state that went in.
            is UsersIntent.Ui.DeleteConfirmed -> before.pendingDeletion?.let { performDelete(it.id) }

            is UsersIntent.Internal.LoadFailed ->
                emitEffect(UsersEffect.ShowMessage(intent.message))

            is UsersIntent.Internal.EditSaveFailed ->
                emitEffect(UsersEffect.ShowMessage(intent.message))

            is UsersIntent.Internal.CreateSaveFailed ->
                emitEffect(UsersEffect.ShowMessage(intent.message))

            is UsersIntent.Internal.DeleteFailed ->
                emitEffect(UsersEffect.ShowMessage(intent.message))

            is UsersIntent.Internal.EditSaveSucceeded ->
                emitEffect(UsersEffect.ShowMessage(AppString.UserEditSaved.asUiText()))

            is UsersIntent.Internal.CreateSaveSucceeded ->
                emitEffect(UsersEffect.ShowMessage(AppString.UserCreated.asUiText()))

            is UsersIntent.Internal.DeleteSucceeded ->
                emitEffect(UsersEffect.ShowMessage(AppString.UserDeleted.asUiText()))

            is UsersIntent.Internal.FavoriteSaveFailed ->
                emitEffect(
                    UsersEffect.ShowMessage(
                        UiText.Resource(AppString.ErrorRollbackSuffix, listOf(intent.message)),
                    ),
                )

            else -> Unit
        }
    }

    private suspend fun performLoad(request: LoadRequest) {
        dispatch(
            if (request.isRefresh) {
                UsersIntent.Internal.RefreshStarted
            } else {
                UsersIntent.Internal.LoadStarted
            },
        )
        try {
            dispatch(UsersIntent.Internal.LoadSucceeded(searchUsers(request.query)))
        } catch (cancellation: CancellationException) {
            throw cancellation // cancellation is NOT a domain failure — always rethrow
        } catch (error: Exception) {
            dispatch(UsersIntent.Internal.LoadFailed(error.toUiText()))
        }
    }

    private fun saveFavorite(userId: String, stateAfterOptimisticUpdate: UsersState) {
        val desired = stateAfterOptimisticUpdate.users
            .firstOrNull { it.id == userId }
            ?.isFavorite
            ?: return

        dispatch(UsersIntent.Internal.FavoriteSaveStarted(userId))
        viewModelScope.launch {
            try {
                setFavorite(userId, desired)
                dispatch(UsersIntent.Internal.FavoriteSaveSucceeded(userId))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                dispatch(
                    UsersIntent.Internal.FavoriteSaveFailed(
                        userId = userId,
                        message = error.toUiText(),
                    ),
                )
            }
        }
    }

    /**
     * Saves the open editor.
     *
     * Two decisions the reducer must not make: refusing an incomplete or already running
     * form, and skipping the request when nothing was actually edited. Both are about
     * *what to do*, and both keep a needless round trip off the wire.
     */
    private fun submitEdit(state: UsersState) {
        val editor = state.editor?.takeIf { it.canSave } ?: return
        val current = state.users.firstOrNull { it.id == editor.userId }

        if (current != null && editor.matches(current)) {
            dispatch(UsersIntent.Internal.EditSaveSucceeded(current))
            return
        }

        dispatch(UsersIntent.Internal.EditSaveStarted)
        viewModelScope.launch {
            try {
                val saved = updateUser(
                    userId = editor.userId,
                    name = editor.name,
                    email = editor.email,
                    role = editor.role,
                )
                dispatch(UsersIntent.Internal.EditSaveSucceeded(saved))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                dispatch(UsersIntent.Internal.EditSaveFailed(error.toUiText()))
            }
        }
    }

    /**
     * Loads the role catalogue once per session.
     *
     * A failure is deliberately silent: the roles are needed to *offer* a choice, not to
     * show the list, and a snackbar about them would fire on a screen the user opened to
     * read names. The add button is disabled while the catalogue is empty, which says the
     * same thing without interrupting.
     */
    private fun loadRoles(state: UsersState) {
        if (state.roles.isNotEmpty()) return
        viewModelScope.launch {
            try {
                dispatch(UsersIntent.Internal.RolesLoaded(getRoles()))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Left empty on purpose: see above.
            }
        }
    }

    /**
     * Saves the create form.
     *
     * Unlike an edit there is nothing to compare against, so there is no "nothing changed"
     * shortcut — but the same two decisions are still the store's: refuse an incomplete or
     * already running form.
     */
    private fun submitCreate(state: UsersState) {
        val creator = state.creator?.takeIf { it.canSave } ?: return

        dispatch(UsersIntent.Internal.CreateSaveStarted)
        viewModelScope.launch {
            try {
                val created = createUser(
                    name = creator.name,
                    email = creator.email,
                    role = creator.role,
                )
                dispatch(UsersIntent.Internal.CreateSaveSucceeded(created))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                dispatch(UsersIntent.Internal.CreateSaveFailed(error.toUiText()))
            }
        }
    }

    /**
     * Deletes a user after the dialog has been confirmed.
     *
     * No optimistic removal: a favorite is a flag that flips back, but a row put back after
     * a failure has to return to the right place in a sorted list, and a list that shuffles
     * itself after an error is worse than one that waits.
     */
    private fun performDelete(userId: String) {
        dispatch(UsersIntent.Internal.DeleteStarted(userId))
        viewModelScope.launch {
            try {
                deleteUser(userId)
                dispatch(UsersIntent.Internal.DeleteSucceeded(userId))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                dispatch(
                    UsersIntent.Internal.DeleteFailed(
                        userId = userId,
                        message = error.toUiText(),
                    ),
                )
            }
        }
    }

    /**
     * Maps a domain failure to a localized message.
     *
     * The exception *type* decides what the user reads — never `error.message`, which is
     * technical, English and meant for logs.
     */
    private fun Exception.toUiText(): UiText = when (this) {
        is UserSyncException.NetworkUnavailable -> AppString.ErrorNetworkUnavailable.asUiText()
        is UserSyncException.FavoriteRejected -> AppString.ErrorFavoriteRejected.asUiText()
        is UserSyncException.InvalidUser -> AppString.ErrorUserInvalid.asUiText()
        is UserSyncException.UserNotFound -> AppString.ErrorUserNotFound.asUiText()
        else -> AppString.ErrorUnknown.asUiText()
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
