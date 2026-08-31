package pl.prodevcode.learnmobiledev.presentation.app

import pl.prodevcode.learnmobiledev.core.ui.localized
import pl.prodevcode.learnmobiledev.core.ui.AppString
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.prodevcode.learnmobiledev.presentation.theme.Spacing
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.presentation.users.UsersViewModel

/**
 * The details screen reads from the **same** source of truth as the list.
 *
 * No duplicated data = no risk that the star in the list and details drifts out of sync.
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

    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onBack) { Text(localized(AppString.ActionBack)) }
        if (user == null) {
            Text(localized(AppString.UserDetailsNotFound, userId))
        } else {
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
        }
    }
}
