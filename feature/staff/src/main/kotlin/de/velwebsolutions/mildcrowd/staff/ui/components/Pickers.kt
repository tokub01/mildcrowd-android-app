package de.velwebsolutions.mildcrowd.staff.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.staff.api.Labelled
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** A labelled choice from a menu (the web's `<select>`); options outside [enabled] are shown but not choosable. */
@Composable
fun OptionPicker(
    label: String,
    options: List<Labelled>,
    selection: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isEnabled: (Labelled) -> Boolean = { true }
) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(MildcrowdRadius.XS.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .border(1.dp, MildcrowdColors.FieldLine, shape)
                    .pressable({ open = true }, enabled = enabled)
                    .padding(horizontal = 14.dp)
                    .semantics { contentDescription = label },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    options.firstOrNull { it.value == selection }?.label ?: selection,
                    style = MildcrowdType.Body,
                    color = if (enabled) MildcrowdColors.Ink else MildcrowdColors.Muted,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = MildcrowdColors.Night)
            }
            DropdownMenu(open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, style = MildcrowdType.Body) },
                        onClick = {
                            open = false
                            onSelect(option.value)
                        },
                        enabled = isEnabled(option)
                    )
                }
            }
        }
    }
}

/**
 * A date and time in German time, chosen with the Material date and time pickers (iOS `DatePicker`). Typed in by keyboard
 * (the pencil of the date picker), an empty or incomplete date selects nothing, so "Weiter" waits for a valid one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    value: Instant,
    onChange: (Instant) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    latest: Instant? = null
) {
    var pickingDate by remember { mutableStateOf(false) }
    var chosenDay by remember { mutableStateOf<LocalDate?>(null) }
    val local = value.atZone(GermanTime.zone)
    val shape = RoundedCornerShape(MildcrowdRadius.XS.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .border(if (error == null) 1.dp else 2.dp, if (error == null) MildcrowdColors.FieldLine else MildcrowdColors.Danger, shape)
                .pressable({ pickingDate = true })
                .padding(horizontal = 14.dp)
                .semantics { contentDescription = "$label: ${GermanTime.dayAndTime(value)}" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(GermanTime.dayAndTime(value), style = MildcrowdType.Body, color = MildcrowdColors.Ink, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.Event, contentDescription = null, tint = MildcrowdColors.Night)
        }
        if (error != null) FieldError(error)
    }
    if (pickingDate) {
        val dateState =
            rememberDatePickerState(initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                MildcrowdTextButton(
                    "Weiter",
                    {
                        dateState.selectedDateMillis?.let { selected ->
                            chosenDay = Instant.ofEpochMilli(selected).atZone(ZoneOffset.UTC).toLocalDate()
                            pickingDate = false
                        }
                    },
                    enabled = dateState.selectedDateMillis != null
                )
            },
            dismissButton = { MildcrowdTextButton("Abbrechen", { pickingDate = false }) }
        ) { DatePicker(dateState) }
    }
    val day = chosenDay
    if (day != null) {
        val timeState = rememberTimePickerState(local.hour, local.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { chosenDay = null },
            confirmButton = {
                MildcrowdTextButton("Fertig", {
                    val chosen = day.atTime(LocalTime.of(timeState.hour, timeState.minute)).atZone(GermanTime.zone).toInstant()
                    onChange(if (latest != null && chosen.isAfter(latest)) latest else chosen)
                    chosenDay = null
                })
            },
            dismissButton = { MildcrowdTextButton("Abbrechen", { chosenDay = null }) },
            text = { TimePicker(timeState) }
        )
    }
}

/** A confirmation before an action that cannot be undone or goes out to people (iOS `confirmationDialog`). */
@Composable
fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    destructive: Boolean = false,
    confirmTag: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night) },
        text = message?.let { { Text(it, style = MildcrowdType.Small, color = MildcrowdColors.Ink2) } },
        confirmButton = {
            MildcrowdTextButton(
                confirmLabel,
                {
                    onDismiss()
                    onConfirm()
                },
                modifier = if (confirmTag != null) Modifier.testTag(confirmTag) else Modifier,
                color = if (destructive) MildcrowdColors.Danger else MildcrowdColors.Night
            )
        },
        dismissButton = { MildcrowdTextButton("Abbrechen", onDismiss) }
    )
}
