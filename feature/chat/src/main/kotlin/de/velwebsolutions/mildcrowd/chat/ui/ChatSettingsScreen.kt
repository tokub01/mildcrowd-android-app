package de.velwebsolutions.mildcrowd.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.chat.api.ChatSettings
import de.velwebsolutions.mildcrowd.chat.api.DeviceUnlockRow
import de.velwebsolutions.mildcrowd.chat.core.formatDate
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.Pill
import de.velwebsolutions.mildcrowd.design.components.PillTone
import de.velwebsolutions.mildcrowd.design.components.SectionCard
import de.velwebsolutions.mildcrowd.design.components.SettingToggle
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.LiveRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import kotlinx.coroutines.launch

/**
 * The chat settings of the web's list screen, in the same order (iOS `ChatSettingsView`): "Wer darf dir schreiben?",
 * notifications (a pointer to Hinweise einstellen), biometrics and the member's devices, the passphrase, and opening the
 * chat on another device.
 */
@Composable
internal fun ChatSettingsScreen() {
    val model: ChatSettingsViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { model.load() }
    DisposableEffect(Unit) { onDispose { model.clearInputs() } }
    MildcrowdScreen(
        title = "Chat-Einstellungen",
        message = state.status,
        error = state.error,
        onDismissMessage = model::dismissStatus,
        onDismissError = model::dismissError
    ) {
        ChatSettingsContent(
            state = state,
            onAudience = { scope.launch { model.setAudience(it) } },
            onFriendRequests = { scope.launch { model.setFriendRequests(it) } },
            onHints = { navigator.open(LiveRoute.Settings) },
            onAccountDevices = { navigator.open(AccountRoute.Devices) },
            onAddDevice = model::addThisDevice,
            onRemoveDevice = { scope.launch { model.remove(it) } },
            onPassphrase = model::setPassphrase,
            onConfirmation = model::setConfirmation,
            onSaveBackup = model::saveBackup,
            onRemovePassphrase = { scope.launch { model.removePassphrase() } }
        )
    }
}

@Composable
internal fun ChatSettingsContent(
    state: ChatSettingsState,
    onAudience: (String) -> Unit,
    onFriendRequests: (Boolean) -> Unit,
    onHints: () -> Unit,
    onAccountDevices: () -> Unit,
    onAddDevice: suspend () -> Unit,
    onRemoveDevice: (DeviceUnlockRow) -> Unit,
    onPassphrase: (String) -> Unit,
    onConfirmation: (String) -> Unit,
    onSaveBackup: suspend () -> Unit,
    onRemovePassphrase: () -> Unit
) {
    var pendingRemoval by remember { mutableStateOf<DeviceUnlockRow?>(null) }
    var confirmRemovePassphrase by remember { mutableStateOf(false) }
    ChatPage {
        PageHeader(title = "Chat-Einstellungen")
        state.settings?.let { AudienceSection(it, state.savingSetting, onAudience, onFriendRequests) }
        SectionCard(
            "Benachrichtigungen",
            detail =
                "E-Mail (ohne Namen) und Push (mit dem Namen, nie mit dem Text) bei neuen Chat-Nachrichten stellst du unter „Hinweise einstellen“ ein."
        ) {
            MildcrowdButton(
                "Hinweise einstellen",
                onHints,
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                modifier = Modifier.testTag("chat.settings.hints")
            )
        }
        SectionCard(
            "Mit Fingerabdruck oder Gesichtserkennung entsperren",
            detail =
                "Statt die Passphrase zu tippen, reicht dann dein Finger oder ein Blick. Mildcrowd speichert nur eine verschlüsselte Kopie deines Chat-Zugangs, die sich ausschließlich mit dem Sicherheitschip dieses Geräts öffnen lässt – nicht von uns. Deine Passphrase gilt weiter."
        ) {
            state.devices.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("chat.devices.row")) {
                    Text(
                        "${row.label} · seit ${formatDate(row.createdAt)}",
                        style = MildcrowdType.Small,
                        color = MildcrowdColors.Ink,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (row.id == state.ownDeviceId) {
                        Spacer(Modifier.width(8.dp))
                        Pill("Dieses Gerät", tone = PillTone.Spring)
                    }
                    Spacer(Modifier.weight(1f))
                    SmallAction(
                        "Entfernen",
                        { if (state.removalNeedsConfirmation) pendingRemoval = row else onRemoveDevice(row) },
                        Modifier.semantics { contentDescription = "„${row.label}“ entfernen" }
                    )
                }
            }
            if (!state.deviceAvailable) {
                Text(
                    "Auf diesem Gerät ist kein Fingerabdruck und keine Gesichtserkennung verfügbar.",
                    style = MildcrowdType.Small,
                    color = MildcrowdColors.Muted
                )
            } else if (!state.thisDeviceIsSetUp) {
                AsyncMildcrowdButton(
                    "Dieses Gerät einrichten",
                    onAddDevice,
                    fullWidth = false,
                    modifier = Modifier.testTag("chat.devices.add")
                )
            }
            SmallAction("Alle Geräte und Anmeldungen: Privatsphäre → Geräte", onAccountDevices, color = MildcrowdColors.SpringText)
        }
        SectionCard(
            state.backupTitle,
            detail =
                "Mit der Passphrase öffnest du den Chat auch auf Geräten ohne Fingerabdruck oder Gesichtserkennung. Sie verlässt nie dein Gerät; wir speichern nur deinen damit verschlüsselten Chat-Zugang."
        ) {
            MildcrowdTextField(
                "Neue Passphrase",
                state.passphrase,
                onPassphrase,
                isSecure = true,
                modifier = Modifier.testTag("chat.backup.passphrase")
            )
            MildcrowdTextField(
                "Wiederholen",
                state.confirmation,
                onConfirmation,
                error = state.backupError,
                isSecure = true,
                modifier = Modifier.testTag("chat.backup.confirmation")
            )
            AsyncMildcrowdButton(
                "Passphrase speichern",
                onSaveBackup,
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                enabled = !state.busy,
                modifier = Modifier.testTag("chat.backup.save")
            )
            AnimatedVisibility(state.busy) { Text("Wird verschlüsselt …", style = MildcrowdType.Small, color = MildcrowdColors.Ink2) }
            if (state.canRemovePassphrase) {
                HorizontalDivider(color = MildcrowdColors.PaperLine)
                Text(
                    "Du kannst die Passphrase auch ganz abschalten. Deine Chats und Nachrichten bleiben, du entsperrst dann nur noch mit Fingerabdruck oder Gesichtserkennung – auf Geräten ohne kommst du nicht mehr in den Chat.",
                    style = MildcrowdType.Small,
                    color = MildcrowdColors.Ink2
                )
                MildcrowdButton("Passphrase abschalten", {
                    confirmRemovePassphrase = true
                }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            }
        }
        SectionCard(
            "Mit anderem Gerät freigeben",
            detail =
                "Möchtest du den Chat auf einem anderen Gerät öffnen – im Browser oder in der App –, tippe dort auf „Freigabe anfragen“. Die Anfrage erscheint dann oben in deiner Chat-Liste: Vergleiche die Prüfzahl und gib frei."
        ) {}
    }
    pendingRemoval?.let { row ->
        ConfirmDialog(
            "Das ist dein einziger Zugang zum Chat. Ohne Reserve-Passphrase musst du die Chats danach neu einrichten und kannst bisherige Nachrichten nicht mehr lesen. Trotzdem entfernen?",
            "Entfernen",
            { onRemoveDevice(row) }
        ) { pendingRemoval = null }
    }
    if (confirmRemovePassphrase) {
        ConfirmDialog(
            "Passphrase abschalten? Danach öffnest du den Chat nur noch mit Fingerabdruck oder Gesichtserkennung auf deinen eingerichteten Geräten.",
            "Abschalten",
            onRemovePassphrase
        ) { confirmRemovePassphrase = false }
    }
}

@Composable
private fun AudienceSection(settings: ChatSettings, saving: Boolean, onAudience: (String) -> Unit, onFriendRequests: (Boolean) -> Unit) {
    SectionCard("Wer darf dir schreiben?") {
        Text("Chats beginnen dürfen", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
        MenuPicker(
            label = "Chats beginnen dürfen",
            options = settings.choices,
            selected = settings.chatInvitesAudience,
            onSelect = onAudience,
            enabled = !saving,
            modifier = Modifier.testTag("chat.settings.audience")
        )
        Text(
            "Bei „Alle Mitglieder“ landen Menschen, denen du nicht folgst und mit denen du nicht befreundet bist, als Anfrage.",
            style = MildcrowdType.Small,
            color = MildcrowdColors.Muted
        )
        SettingToggle(
            "Freundschaftsanfragen erhalten",
            settings.friendRequests,
            onFriendRequests,
            enabled = !saving,
            modifier = Modifier.testTag("chat.settings.friendRequests")
        )
    }
}
