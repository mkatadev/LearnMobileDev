package pl.prodevcode.learnmobiledev.presentation.app

import pl.prodevcode.learnmobiledev.core.ui.localized
import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.prodevcode.learnmobiledev.presentation.theme.Spacing
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.presentation.users.RolePicker
import pl.prodevcode.learnmobiledev.presentation.users.UserEditor
import pl.prodevcode.learnmobiledev.presentation.users.UsersIntent
import pl.prodevcode.learnmobiledev.presentation.users.UsersViewModel

/**
 * The details screen reads from the **same** source of truth as the list.
 *
 * No duplicated data = no risk that the star in the list and details drifts out of sync.
 * Editing follows the same rule: the form is `UsersState.editor`, not a `remember` here,
 * so a rotation, a jump on the timeline or a switch to iOS all render the same draft.
 */
@Composable
fun UserDetailsScreen(
    viewModel: UsersViewModel,
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val user = state.users.firstOrNull { it.id == userId }
    val editor = state.editor?.takeIf { it.userId == userId }

    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onBack) { Text(localized(AppString.ActionBack)) }
        when {
            user == null -> Text(localized(AppString.UserDetailsNotFound, userId))

            editor == null -> UserDetails(
                user = user,
                onEdit = { viewModel.dispatch(UsersIntent.Ui.EditClicked(userId)) },
            )

            else -> UserEditorForm(
                editor = editor,
                roles = state.roles,
                onIntent = viewModel::dispatch,
            )
        }
    }
}

@Composable
private fun ColumnScope.UserDetails(user: User, onEdit: () -> Unit) {
    Text(
        text = user.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(user.role, style = MaterialTheme.typography.titleMedium)
    Text(user.email, style = MaterialTheme.typography.bodyMedium)
    Text(
        text = localized(
            if (user.isFavorite) {
                AppString.UserDetailsFavorite
            } else {
                AppString.UserDetailsNotFavorite
            },
        ),
        style = MaterialTheme.typography.bodyLarge,
    )
    OutlinedButton(onClick = onEdit) { Text(localized(AppString.UserDetailsEdit)) }
}

/**
 * A form whose every keystroke is an intent.
 *
 * There is no local `var text by remember`: the field renders `editor`, and typing sends
 * `EditNameChanged`. That is what puts the edit on the timeline and lets it be replayed —
 * the same reason the rest of the screen holds no state of its own.
 */
@Composable
private fun UserEditorForm(
    editor: UserEditor,
    roles: List<String>,
    onIntent: (UsersIntent) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = localized(AppString.UserEditTitle),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = editor.name,
                onValueChange = { onIntent(UsersIntent.Ui.EditNameChanged(it)) },
                label = { Text(localized(AppString.UserEditName)) },
                singleLine = true,
                enabled = !editor.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = editor.email,
                onValueChange = { onIntent(UsersIntent.Ui.EditEmailChanged(it)) },
                label = { Text(localized(AppString.UserEditEmail)) },
                singleLine = true,
                enabled = !editor.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            RolePicker(
                selected = editor.role,
                roles = roles,
                enabled = !editor.isSaving,
                onRoleSelected = { onIntent(UsersIntent.Ui.EditRoleChanged(it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onIntent(UsersIntent.Ui.EditCancelled) },
                    enabled = !editor.isSaving,
                ) {
                    Text(localized(AppString.ActionCancel))
                }
                Button(
                    onClick = { onIntent(UsersIntent.Ui.EditSubmitted) },
                    // Derived in the contract, not decided here: the same rule then holds
                    // for every caller and is covered by the reducer tests.
                    enabled = editor.canSave,
                ) {
                    Text(
                        localized(
                            if (editor.isSaving) AppString.UserEditSaving else AppString.ActionSave,
                        ),
                    )
                }
            }
        }
    }
}
