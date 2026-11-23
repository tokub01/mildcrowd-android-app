package de.velwebsolutions.mildcrowd.account.ui.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.account.api.DeviceOverview
import de.velwebsolutions.mildcrowd.account.api.ReportForm
import de.velwebsolutions.mildcrowd.account.api.SafetyAccount
import de.velwebsolutions.mildcrowd.account.api.SafetyList
import de.velwebsolutions.mildcrowd.account.ui.components.AccountPage
import de.velwebsolutions.mildcrowd.account.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.account.ui.components.EmptyLine
import de.velwebsolutions.mildcrowd.account.ui.components.FlowChoices
import de.velwebsolutions.mildcrowd.account.ui.components.GermanDate
import de.velwebsolutions.mildcrowd.account.ui.components.LinkText
import de.velwebsolutions.mildcrowd.account.ui.components.ListRow
import de.velwebsolutions.mildcrowd.account.ui.components.webUrl
import de.velwebsolutions.mildcrowd.account.ui.retry
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.AvatarView
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.Pill
import de.velwebsolutions.mildcrowd.design.components.PillTone
import de.velwebsolutions.mildcrowd.design.components.SectionCard
import de.velwebsolutions.mildcrowd.design.components.SupportContacts
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.ChatRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.navigation.WebRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import java.time.Instant
import kotlinx.coroutines.launch

/** Pfirsich (spring) at 30 % on paper, as a solid colour (the app does not draw see-through surfaces). */
private val ThisDeviceTint = Color(0xFFFFE5DA)

/** Pull to refresh around a page that reloads its view model. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Refreshable(onRefresh: suspend () -> Unit, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                onRefresh()
                refreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { content() }
}

/**
 * Geräte und Anmeldungen (/app/privatsphaere/geraete): this device, other app sign-ins and browser sessions to sign
 * out, the devices known for login alerts and the chat unlocks with the fingerprint.
 */
@Composable
internal fun DevicesScreen(viewModel: DevicesViewModel = hiltViewModel()) {
    val navigator = LocalNavigator.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmAll by remember { mutableStateOf(false) }
    var lastChatDevice by remember { mutableStateOf<DeviceOverview.ChatDevice?>(null) }
    MildcrowdScreen(
        title = "Geräte",
        message = ui.message,
        error = ui.error,
        onDismissMessage = viewModel::dismissMessage,
        onDismissError = viewModel::dismissError
    ) {
        Refreshable(viewModel::load) {
            AccountPage {
                PageHeader(
                    "Geräte und Anmeldungen",
                    eyebrow = "Privatsphäre",
                    lead = "Hier siehst du, wo du gerade angemeldet bist, und meldest fremde Geräte sofort ab."
                )
                LoadableView(ui.state, onRetry = viewModel.retry(viewModel::load)) { overview ->
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SignedIn(overview, viewModel) { confirmAll = true }
                        Known(overview, viewModel)
                        Chat(overview, viewModel, onLastDevice = { lastChatDevice = it }) { navigator.open(ChatRoute.Conversations) }
                        SectionCard(
                            "Konto zusätzlich schützen",
                            detail = "Mit der Zwei-Faktor-Anmeldung braucht jede neue Anmeldung zusätzlich einen Code von deinem Handy."
                        ) {
                            LinkText("Zwei-Faktor-Anmeldung", { navigator.open(AccountRoute.Settings) })
                            LinkText("Anmelde-Warnungen einstellen", { navigator.open(AccountRoute.Privacy) })
                        }
                    }
                }
            }
        }
        if (confirmAll) {
            ConfirmDialog(
                "Auf allen anderen Geräten abmelden?",
                "Überall sonst abmelden",
                onConfirm = { scope.launch { viewModel.signOutEverywhereElse() } },
                onDismiss = { confirmAll = false }
            )
        }
        lastChatDevice?.let { device ->
            ConfirmDialog(
                "Das ist dein einziger Zugang zum Chat. Ohne Reserve-Passphrase musst du die Chats danach neu einrichten und kannst bisherige Nachrichten nicht mehr lesen. Trotzdem entfernen?",
                "Entfernen",
                onConfirm = { scope.launch { viewModel.forgetChat(device) } },
                onDismiss = { lastChatDevice = null }
            )
        }
    }
}

@Composable
private fun RowText(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
        Text(detail, style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
    }
}

@Composable
private fun SignedIn(overview: DeviceOverview, viewModel: DevicesViewModel, onSignOutOthers: () -> Unit) {
    SectionCard("Wo du angemeldet bist") {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ThisDeviceTint, RoundedCornerShape(MildcrowdRadius.TILE.dp))
                .padding(14.dp)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, tint = MildcrowdColors.Night)
                Text(overview.current.name ?: overview.current.device, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                Pill("Dieses Gerät", tone = PillTone.Night)
            }
            Text(
                "${overview.current.device} · Netz ${overview.current.ip} · aktiv jetzt",
                style = MildcrowdType.Small,
                color = MildcrowdColors.Ink2
            )
        }
        overview.apps.forEach { app ->
            ListRow(leading = {
                RowText(app.name, "Mildcrowd-App · zuletzt aktiv ${GermanDate.long(app.lastUsedAt ?: app.signedInAt ?: Instant.now())}")
            }, trailing = {
                AsyncMildcrowdButton(
                    "Abmelden",
                    { viewModel.signOutApp(app) },
                    modifier = Modifier.testTag("devices.app.${app.id}"),
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false
                )
            })
        }
        if (overview.sessionsListable) {
            overview.sessions.forEach { session ->
                ListRow(leading = {
                    RowText(session.device, "Netz ${session.ip} · zuletzt aktiv ${GermanDate.long(session.lastActiveAt)}")
                }, trailing = {
                    AsyncMildcrowdButton("Abmelden", {
                        viewModel.endSession(session)
                    }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
                })
            }
        } else {
            Text(
                "Andere Anmeldungen im Browser können auf diesem Server nicht angezeigt werden.",
                style = MildcrowdType.Small,
                color = MildcrowdColors.Muted
            )
        }
        if (overview.apps.isEmpty() && overview.sessions.isEmpty()) {
            EmptyLine("Du bist auf keinem anderen Gerät angemeldet.")
        } else {
            HorizontalDivider(color = MildcrowdColors.PaperLine)
            MildcrowdButton(
                "Überall sonst abmelden",
                onSignOutOthers,
                modifier = Modifier.testTag("devices.signOutOthers"),
                fullWidth = false
            )
            Text(
                "„Angemeldet bleiben“ wird dabei überall zurückgesetzt. Kommt dir ein Gerät fremd vor, ändere danach dein Passwort.",
                style = MildcrowdType.Caption,
                color = MildcrowdColors.Muted
            )
        }
    }
}

@Composable
private fun Known(overview: DeviceOverview, viewModel: DevicesViewModel) {
    val alerts = if (overview.loginAlerts) {
        "Meldet sich jemand mit einem anderen Browser oder System an, bekommst du sofort eine E-Mail."
    } else {
        "Anmelde-Warnungen per E-Mail sind ausgeschaltet."
    }
    SectionCard(
        "Bekannte Geräte",
        detail = "$alerts Wir speichern nur Browser und System, keine IP-Adresse. Nach einem Jahr ohne Anmeldung vergessen wir ein Gerät."
    ) {
        if (overview.known.isEmpty()) EmptyLine("Noch keine Geräte gespeichert.")
        overview.known.forEach { device ->
            ListRow(leading = { RowText(device.label, "zuletzt angemeldet ${GermanDate.long(device.lastSeenAt)}") }, trailing = {
                AsyncMildcrowdButton("Vergessen", { viewModel.forget(device) }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            })
        }
    }
}

@Composable
private fun Chat(
    overview: DeviceOverview,
    viewModel: DevicesViewModel,
    onLastDevice: (DeviceOverview.ChatDevice) -> Unit,
    onSetUp: () -> Unit
) {
    val passphrase = if (overview.chatPassphrase) {
        "Zusätzlich hast du eine Chat-Passphrase."
    } else {
        "Eine Chat-Passphrase hast du nicht festgelegt."
    }
    SectionCard(
        "Chat mit Fingerabdruck oder Gesichtserkennung",
        detail =
            "Diese Geräte öffnen deine verschlüsselten Chats. Der Zugang liegt dafür im Sicherheitschip des Geräts – wir können ihn nicht lesen. $passphrase Ein neues Gerät richtest du im Chat ein."
    ) {
        if (overview.chatDevices.isEmpty()) EmptyLine("Noch kein Gerät eingerichtet.")
        overview.chatDevices.forEach { device ->
            ListRow(leading = { RowText(device.label, "eingerichtet am ${GermanDate.long(device.createdAt)}") }, trailing = {
                AsyncMildcrowdButton(
                    "Entfernen",
                    {
                        if (!overview.chatPassphrase &&
                            overview.chatDevices.size == 1
                        ) {
                            onLastDevice(device)
                        } else {
                            viewModel.forgetChat(device)
                        }
                    },
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false
                )
            })
        }
        LinkText("Im Chat ein Gerät einrichten", onSetUp)
    }
}

/** Sicherheit / Blockiert & stumm (/app/sicherheit). */
@Composable
internal fun SafetyScreen(viewModel: SafetyViewModel = hiltViewModel()) {
    val navigator = LocalNavigator.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    MildcrowdScreen(
        title = "Sicherheit",
        message = ui.message,
        error = ui.error,
        onDismissMessage = viewModel::dismissMessage,
        onDismissError = viewModel::dismissError
    ) {
        Refreshable(viewModel::load) {
            AccountPage {
                PageHeader(
                    "Sicherheit",
                    eyebrow = "Privatsphäre",
                    lead =
                        "Hier verwaltest du Personen, die du blockiert, stummgeschaltet oder eingeschränkt hast. Alles wirkt leise – niemand wird benachrichtigt."
                )
                LoadableView(ui.state, onRetry = viewModel.retry(viewModel::load)) { lists ->
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SafetyListCard(
                            "Blockierte Personen",
                            "Ihr seht gegenseitig keine Posts, Antworten oder Profile mehr und könnt nicht interagieren.",
                            lists.blocked,
                            "Du hast aktuell niemanden blockiert.",
                            "Blockade aufheben",
                            SafetyList.Blocks,
                            onProfile = null,
                            lift = viewModel::lift
                        )
                        SafetyListCard(
                            "Stummgeschaltete Personen",
                            "Posts dieser Personen erscheinen nicht mehr auf deiner Startseite, in Entdecken oder in Antworten. Ihr Profil bleibt für dich aufrufbar.",
                            lists.muted,
                            "Du hast aktuell niemanden stummgeschaltet.",
                            "Nicht mehr stummschalten",
                            SafetyList.Mutes,
                            onProfile = { navigator.open(NetworkRoute.Profile(it.username)) },
                            lift = viewModel::lift
                        )
                        SafetyListCard(
                            "Eingeschränkte Personen",
                            "Ihre Antworten unter deinen Posts sieht nur sie selbst, und du bekommst keine Hinweise von ihr. Sie kann dir weiter folgen und deine Posts lesen.",
                            lists.restricted,
                            "Du hast aktuell niemanden eingeschränkt.",
                            "Nicht mehr einschränken",
                            SafetyList.Restrictions,
                            onProfile = { navigator.open(NetworkRoute.Profile(it.username)) },
                            lift = viewModel::lift
                        )
                    }
                }
                SectionCard("Wie Schutz funktioniert") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Explained(
                            "Blockieren:",
                            " Wenn du jemanden blockierst, existiert ihr füreinander nicht mehr. Posts und Profile sind wie nicht existent (404). Bestehende Follows werden beidseitig gelöscht."
                        )
                        Explained(
                            "Stummschalten:",
                            " Nur für dich sichtbar. Du siehst keine Beiträge der Person mehr im Feed, kannst ihr Profil aber weiter aufrufen. Die Person merkt nichts."
                        )
                        Explained(
                            "Einschränken:",
                            " Leiser als Blockieren. Die Person kann weiter lesen und antworten, aber ihre Antworten unter deinen Posts sieht nur sie selbst. Sie merkt nichts."
                        )
                    }
                }
                SectionCard("Deine Merkliste", detail = "Posts kannst du dir privat für später merken.") {
                    MildcrowdButton("Zur Merkliste", {
                        navigator.open(NetworkRoute.Bookmarks)
                    }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
                }
            }
        }
    }
}

@Composable
private fun Explained(term: String, text: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(term) }
            append(text)
        },
        style = MildcrowdType.Small,
        color = MildcrowdColors.Ink2
    )
}

@Composable
private fun SafetyListCard(
    title: String,
    detail: String,
    accounts: List<SafetyAccount>,
    empty: String,
    action: String,
    list: SafetyList,
    onProfile: ((SafetyAccount) -> Unit)?,
    lift: suspend (SafetyList, SafetyAccount) -> Unit
) {
    SectionCard(title, detail = detail, count = accounts.size) {
        if (accounts.isEmpty()) EmptyLine(empty)
        accounts.forEach { account ->
            ListRow(leading = {
                Row(
                    (if (onProfile != null) Modifier.pressable({ onProfile(account) }) else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarView(account.initials, account.avatarColor, photoUrl = account.photoUrl)
                    Column {
                        Text(account.displayName, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                        Text("@${account.username}", style = MildcrowdType.Small, color = MildcrowdColors.Muted)
                    }
                }
            }, trailing = {
                AsyncMildcrowdButton(
                    action,
                    { lift(list, account) },
                    modifier = Modifier.testTag("safety.${list.path}.${account.username}"),
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false
                )
            })
        }
    }
}

/** Melden (/app/melden): what is wrong, an optional comment; afterwards thanks and where to find help. */
@Composable
internal fun ReportScreen(url: String?) {
    val viewModel = hiltViewModel<ReportViewModel, ReportViewModel.Factory>(creationCallback = { it.create(url) })
    val navigator = LocalNavigator.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val privacyUrl = webUrl("datenschutz#meldungen")
    MildcrowdScreen(title = "Melden", error = ui.error, onDismissError = viewModel::dismissError) {
        AccountPage(maxWidth = 640) {
            ReportContent(
                ui,
                viewModel::choose,
                viewModel::setComment,
                viewModel::send,
                viewModel.retry(viewModel::load),
                onPrivacy = { navigator.open(WebRoute(privacyUrl)) }
            ) {
                if (navigator.sheet != null) navigator.dismissSheet() else navigator.handle(NetworkRoute.Source)
            }
        }
    }
}

@Composable
internal fun ReportContent(
    ui: ReportUiState,
    onCategory: (String) -> Unit,
    onComment: (String) -> Unit,
    onSend: suspend () -> Unit,
    onRetry: suspend () -> Unit,
    onPrivacy: () -> Unit = {},
    onDone: () -> Unit
) {
    LoadableView(ui.state, onRetry = onRetry) { form ->
        if (ui.sent) {
            Thanks(form, onDone)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PageHeader(
                    "Was stimmt hier nicht?",
                    eyebrow = "Melden",
                    lead =
                        "Melden ist ausdrücklich erwünscht – auch wenn du dir nicht sicher bist. Lieber einmal zu viel als zu wenig. Die gemeldete Person erfährt nicht, wer gemeldet hat."
                )
                SectionCard("Was ist das Problem?") {
                    Text("Gemeldet wird: ${form.url}", style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
                    FlowChoices(form.categories, ui.category, tag = "report.category", choose = onCategory)
                    ui.fieldErrors["category"]?.let { FieldError(it) }
                    MildcrowdTextField(
                        "Möchtest du noch etwas dazu sagen? (freiwillig)",
                        ui.comment,
                        onComment,
                        prompt = "Zum Beispiel: Das geht schon länger so, auch in anderen Posts.",
                        error = ui.fieldErrors["comment"],
                        lines = 3..10
                    )
                    Text(
                        "Mit dem Melden bestätigst du, dass du es ehrlich meinst. Deine E-Mail-Adresse nutzen wir nur, um dir das Ergebnis zu schicken.",
                        style = MildcrowdType.Caption,
                        color = MildcrowdColors.Muted
                    )
                    AsyncMildcrowdButton("Melden", onSend, modifier = Modifier.testTag("report.send"), fullWidth = false)
                    Text(
                        "Wir speichern deine Meldung als Nachweis; Einzelheiten in der Datenschutzerklärung.",
                        style = MildcrowdType.Caption,
                        color = MildcrowdColors.Muted
                    )
                    LinkText("Datenschutzerklärung", onPrivacy, Modifier.testTag("report.privacy"), bold = false)
                }
                SupportContacts()
            }
        }
    }
}

@Composable
private fun Thanks(form: ReportForm, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.M.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Danke, dass du das meldest.",
            style = MildcrowdType.display(30),
            color = MildcrowdColors.Night,
            modifier = Modifier.testTag("report.thanks")
        )
        Text(
            "Du hast richtig gehandelt. Unser Moderationsteam schaut sich das an, in der Regel innerhalb von ${form.reviewHours} Stunden, und schreibt dir per E-Mail, was dabei herauskommt.",
            style = MildcrowdType.Body,
            color = MildcrowdColors.Ink2
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Die gemeldete Person erfährt nicht, wer gemeldet hat.") }
                append(" Wenn du sie nicht mehr sehen willst, kannst du sie auf ihrem Profil blockieren oder stumm schalten.")
            },
            style = MildcrowdType.Body,
            color = MildcrowdColors.Ink2
        )
        SupportContacts()
        MildcrowdButton("Zurück zur Startseite", onDone, fullWidth = false)
    }
}
