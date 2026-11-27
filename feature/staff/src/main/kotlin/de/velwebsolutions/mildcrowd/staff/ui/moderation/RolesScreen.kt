package de.velwebsolutions.mildcrowd.staff.ui.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.CrowdCard
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.staff.api.RoleUser
import de.velwebsolutions.mildcrowd.staff.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.staff.ui.components.LoadMoreButton
import de.velwebsolutions.mildcrowd.staff.ui.components.OptionPicker
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffLoadable
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffPage
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffTitle
import de.velwebsolutions.mildcrowd.staff.ui.retry
import kotlinx.coroutines.launch

/** Role management for admins (web: livewire.moderation.roles). Only the operator account appoints admins. */
@Composable
fun RolesScreen(viewModel: RolesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MildcrowdScreen(
        "Rollen",
        message = state.message,
        error = state.error,
        onDismissMessage = viewModel::dismissMessage,
        onDismissError = viewModel::dismissError
    ) {
        StaffLoadable(state.users, viewModel.retry(viewModel::load)) { users ->
            StaffPage(viewModel::load, spacing = 12) {
                StaffTitle("Rollen")
                Text(
                    "Moderation und Admin brauchen eine eingeschaltete Zwei-Faktor-Anmeldung, bevor sie den Moderationsbereich öffnen " +
                        "können. Admins ernennen und ändern kann nur das Betreiberkonto.",
                    style = MildcrowdType.Small,
                    color = MildcrowdColors.Ink2
                )
                if (users.any(RoleUser::accessManageable)) {
                    Text(
                        "Als Betreiberkonto kannst du E-Mail-Adressen bestätigen und Passwörter zurücksetzen. " +
                            "Schick möglichst den Link – dann kennt nur das Mitglied das neue Passwort. " +
                            "Ein direkt gesetztes Passwort meldet das Konto überall ab, und das Mitglied bekommt sofort eine E-Mail.",
                        style = MildcrowdType.Small,
                        color = MildcrowdColors.Ink2
                    )
                }
                users.forEach { user -> RoleRow(user, state, viewModel) }
                if (state.hasMore) LoadMoreButton("Weitere Konten laden", viewModel::loadMore, Modifier.testTag("roles.more"))
            }
        }
    }
}

@Composable
private fun RoleRow(user: RoleUser, state: RolesUiState, viewModel: RolesViewModel) {
    CrowdCard(padding = 14) {
        Text(user.displayName, style = MildcrowdType.BodyBold, color = MildcrowdColors.Ink)
        Text(user.email, style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            OptionPicker(
                "Rolle von ${user.displayName}",
                state.roleNames,
                state.chosen[user.id] ?: user.role.value,
                { viewModel.choose(user.id, it) },
                Modifier.weight(1f).testTag("roles.picker.${user.id}"),
                enabled = user.editable,
                isEnabled = { it.value in user.choosableRoles }
            )
            val note = user.note
            if (note != null) {
                Text(note, style = MildcrowdType.Caption, color = MildcrowdColors.Muted, modifier = Modifier.padding(bottom = 12.dp))
            } else {
                AsyncMildcrowdButton("Speichern", {
                    viewModel.save(user)
                }, Modifier.testTag("roles.save.${user.id}"), kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            }
        }
        if (user.accessManageable) AccessSection(user, state.passwords[user.id].orEmpty(), viewModel)
        state.errors[user.id]?.let { FieldError(it, Modifier.padding(top = 8.dp)) }
    }
}

/**
 * Operator account only: confirm the address, send the password link or set a password. Confirming the address and
 * setting a password are asked first, with the web's questions (moderation/roles.blade.php).
 */
@Composable
private fun AccessSection(user: RoleUser, password: String, viewModel: RolesViewModel) {
    val scope = rememberCoroutineScope()
    var confirmingEmail by remember { mutableStateOf(false) }
    var settingPassword by remember { mutableStateOf(false) }
    if (confirmingEmail) {
        ConfirmDialog(
            "Die E-Mail-Adresse ${user.email} als bestätigt markieren?",
            "E-Mail bestätigen",
            { scope.launch { viewModel.confirmEmail(user) } },
            { confirmingEmail = false },
            confirmTag = "roles.confirmEmail.confirm"
        )
    }
    if (settingPassword) {
        ConfirmDialog(
            "Passwort von ${user.displayName} setzen? Das Konto wird überall abgemeldet.",
            "Passwort setzen",
            { scope.launch { viewModel.setPassword(user) } },
            { settingPassword = false },
            destructive = true,
            confirmTag = "roles.setPassword.confirm"
        )
    }
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MildcrowdColors.PaperLine)
        if (user.emailVerified) {
            Text("E-Mail bestätigt", style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
        } else {
            MildcrowdButton(
                "E-Mail bestätigen",
                { confirmingEmail = true },
                Modifier.testTag("roles.confirmEmail.${user.id}"),
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false
            )
        }
        AsyncMildcrowdButton(
            "Link für neues Passwort senden",
            { viewModel.sendPasswordLink(user) },
            Modifier.testTag("roles.passwordLink.${user.id}"),
            kind = MildcrowdButtonKind.Secondary,
            fullWidth = false
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            MildcrowdTextField(
                "Neues Passwort",
                password,
                { viewModel.typePassword(user.id, it) },
                Modifier.weight(1f).testTag("roles.password.${user.id}"),
                isSecure = true
            )
            MildcrowdButton(
                "Setzen",
                { settingPassword = true },
                Modifier.testTag("roles.setPassword.${user.id}"),
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                enabled = password.isNotEmpty()
            )
        }
    }
}
