package pl.prodevcode.learnmobiledev.presentation.users

import pl.prodevcode.learnmobiledev.core.ui.localized
import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.prodevcode.learnmobiledev.presentation.theme.Spacing
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.core.ui.resolve
import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.core.mvi.TimelineEntry

/**
 * A View in MVI has exactly two jobs:
 * 1. **render** state (the `State -> UI` function),
 * 2. **send** intents.
 *
 * There is no `if (loading) load()`, no `var` with business logic, and no repository calls
 * here. That is why the same screen works on Android and iOS without changes.
 */
@Composable
fun UsersScreen(
    viewModel: UsersViewModel,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val timeline by viewModel.timeline.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // The "screen appeared" intent: the start of the screen lifecycle in MVI.
    LaunchedEffect(viewModel) {
        viewModel.dispatch(UsersIntent.Ui.ScreenOpened)
    }

    // Consumption of one-shot effects. Each effect is handled EXACTLY once.
    // Effects are resolved to text only here, in the @Composable context,
    // where the current language is known.
    var pendingMessage by remember { mutableStateOf<UiText?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UsersEffect.ShowMessage -> pendingMessage = effect.text
                is UsersEffect.OpenUserDetails -> onOpenDetails(effect.userId)
            }
        }
    }

    pendingMessage?.let { message ->
        val text = message.resolve()
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(text)
            pendingMessage = null
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            Header(state = state, onIntent = viewModel::dispatch)

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> LoadingIndicator()
                    state.error != null -> ErrorPanel(
                        message = state.error!!.resolve(),
                        onRetry = { viewModel.dispatch(UsersIntent.Ui.RetryClicked) },
                        onDismiss = { viewModel.dispatch(UsersIntent.Ui.ErrorDismissed) },
                    )
                    state.showEmptyState -> EmptyPanel(query = state.query)
                    else -> UsersList(state = state, onIntent = viewModel::dispatch)
                }
            }

            TimelinePanel(
                timeline = timeline,
                onJumpTo = viewModel::jumpTo,
            )
        }
    }
}

@Composable
private fun Header(state: UsersState, onIntent: (UsersIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = localized(AppString.UsersTitle),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = Spacing.screenTop),
        )
        Text(
            text = localized(
                AppString.UsersFavoritesSummary,
                state.favoritesCount,
                state.savingFavorites.size,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = { onIntent(UsersIntent.Ui.QueryChanged(it)) },
            label = {
                Text(
                    localized(
                        AppString.UsersSearchLabel,
                        UsersViewModel.SEARCH_DEBOUNCE_MS,
                    ),
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Switch(
                checked = state.simulateNetworkError,
                onCheckedChange = { onIntent(UsersIntent.Ui.SimulateErrorChanged(it)) },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = localized(AppString.UsersSimulateError),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { onIntent(UsersIntent.Ui.RefreshClicked) }) {
                Text(
                    localized(
                        if (state.isRefreshing) AppString.UsersRefreshing else AppString.UsersRefresh,
                    ),
                )
            }
        }
        if (state.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
private fun UsersList(state: UsersState, onIntent: (UsersIntent) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(top = Spacing.small, bottom = Spacing.listBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
    ) {
        items(items = state.users, key = { it.id }) { user ->
            UserRow(
                user = user,
                isSaving = user.id in state.savingFavorites,
                onClick = { onIntent(UsersIntent.Ui.UserClicked(user.id)) },
                onFavoriteClick = { onIntent(UsersIntent.Ui.FavoriteToggled(user.id)) },
            )
        }
    }
}

@Composable
private fun UserRow(
    user: User,
    isSaving: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Text(user.role, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onFavoriteClick) {
                Text(
                    text = localized(
                        when {
                            isSaving -> AppString.UsersFavoriteSaving
                            user.isFavorite -> AppString.UsersFavoriteYes
                            else -> AppString.UsersFavoriteNo
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyPanel(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (query.isBlank()) {
                localized(AppString.UsersEmpty)
            } else {
                localized(AppString.UsersEmptyQuery, query)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ElevatedCard {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = localized(AppString.ErrorGeneric),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text(localized(AppString.ActionHide)) }
                    OutlinedButton(onClick = onRetry) { Text(localized(AppString.ActionRetry)) }
                }
            }
        }
    }
}

/**
 * The "time travel" panel: MVI's best selling point.
 *
 * Since every state change is `reduce(state, intent)`, the history of (intent, state) pairs
 * fully describes the user's session. Clicking an entry restores exactly that UI state.
 */
@Composable
private fun TimelinePanel(
    timeline: List<TimelineEntry<UsersState, UsersIntent>>,
    onJumpTo: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = localized(AppString.UsersTimelineTitle, timeline.size),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    localized(
                        if (expanded) AppString.UsersTimelineCollapse else AppString.UsersTimelineExpand,
                    ),
                )
            }
        }
        if (expanded) {
            LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                items(items = timeline.asReversed(), key = { it.index }) { entry ->
                    Text(
                        text = localized(
                            AppString.UsersTimelineEntry,
                            entry.index,
                            entry.intent?.shortName()
                                ?: localized(AppString.UsersTimelineInitial),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJumpTo(entry.index) }
                            .padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun UsersIntent.shortName(): String =
    toString().substringBefore('@').substringAfterLast('.')
