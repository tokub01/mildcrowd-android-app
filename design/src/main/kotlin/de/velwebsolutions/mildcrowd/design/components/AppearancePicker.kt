package de.velwebsolutions.mildcrowd.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.design.theme.Appearance
import de.velwebsolutions.mildcrowd.design.theme.AppearanceChoice
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType

/**
 * „Darstellung“ (the web's `x-theme-switch`): Automatisch, Hell or Dunkel for this device, applied and remembered at
 * once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.testTag("appearance"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Darstellung", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink2)
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppearanceChoice.entries.forEach { choice ->
                val chosen = Appearance.choice == choice
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(CircleShape)
                        .background(if (chosen) MildcrowdColors.Night else MildcrowdColors.Paper, CircleShape)
                        .border(1.5.dp, if (chosen) MildcrowdColors.Night else MildcrowdColors.DuneLine, CircleShape)
                        .selectable(chosen, role = Role.RadioButton) { Appearance.choose(choice, Appearance.preferences(context)) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("appearance.${choice.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        choice.label,
                        style = MildcrowdType.SmallBold,
                        color = if (chosen) MildcrowdColors.OnNight else MildcrowdColors.Night
                    )
                }
            }
        }
    }
}
