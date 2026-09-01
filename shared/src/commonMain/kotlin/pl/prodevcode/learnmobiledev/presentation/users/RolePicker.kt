package pl.prodevcode.learnmobiledev.presentation.users

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import pl.prodevcode.learnmobiledev.core.ui.AppString
import pl.prodevcode.learnmobiledev.core.ui.localized

/**
 * The role as a closed list rather than a text field.
 *
 * The options come from the server (`GET /api/v1/roles`), so the app cannot offer a value
 * the backend would refuse with a `422`, and adding a role does not need a new release.
 * The field is read-only: typing would let a user enter something that is not a role at
 * all, which is the very thing a picker exists to prevent.
 *
 * Whether the menu is open is the one piece of state that stays in `remember`. Losing it
 * costs nothing — a reopened menu shows the same options, and nothing the user typed is
 * gone — which is exactly the test for what does *not* belong in the store.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePicker(
    selected: String,
    roles: List<String>,
    enabled: Boolean,
    onRoleSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(localized(AppString.UserRolePickerTitle)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            roles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role) },
                    onClick = {
                        onRoleSelected(role)
                        expanded = false
                    },
                )
            }
        }
    }
}
