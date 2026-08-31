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
import pl.prodevcode.learnmobiledev.domain.usecase.SearchUsersUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetFavoriteUseCase

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
            is UsersIntent.Ui.ScreenOpened ->
                loadRequests.trySend(LoadRequest(after.query, isRefresh = false))

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

            is UsersIntent.Internal.LoadFailed ->
                emitEffect(UsersEffect.ShowMessage(intent.message))

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
     * Maps a domain failure to a localized message.
     *
     * The exception *type* decides what the user reads — never `error.message`, which is
     * technical, English and meant for logs.
     */
    private fun Exception.toUiText(): UiText = when (this) {
        is UserSyncException.NetworkUnavailable -> AppString.ErrorNetworkUnavailable.asUiText()
        is UserSyncException.FavoriteRejected -> AppString.ErrorFavoriteRejected.asUiText()
        else -> AppString.ErrorUnknown.asUiText()
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
