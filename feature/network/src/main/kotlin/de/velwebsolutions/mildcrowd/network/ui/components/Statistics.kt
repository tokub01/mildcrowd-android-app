package de.velwebsolutions.mildcrowd.network.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

/** One number with a label (`x-network.stat`). */
@Composable
fun StatTile(value: Int, label: String, modifier: Modifier = Modifier) {
    val formatted = NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)
    Column(
        modifier
            .defaultMinSize(minHeight = 96.dp)
            .background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.S.dp))
            .padding(18.dp)
            .clearAndSetSemantics { contentDescription = "$formatted $label" },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(formatted, style = MildcrowdType.figure(), color = MildcrowdColors.Night)
        Text(label, style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
    }
}

/** A grid of number tiles, two per row on phones. */
@Composable
fun StatGrid(tiles: List<Pair<Int, String>>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (value, label) -> StatTile(value, label, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** One week of a [WeekChart]. */
data class WeekRow(val week: Instant, val values: List<Int>)

private val SeriesColors get() = listOf(MildcrowdColors.Spring, MildcrowdColors.Sun, MildcrowdColors.Night)

/** The accessible weekly chart (`x-network.week-chart`): one row per week with a bar and the number per series. */
@Composable
fun WeekChart(caption: String, series: List<String>, rows: List<WeekRow>, modifier: Modifier = Modifier) {
    val maximum = maxOf(1, rows.flatMap { it.values }.maxOrNull() ?: 1)
    Column(
        modifier.fillMaxWidth().background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.M.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(caption, style = MildcrowdType.heading(), color = MildcrowdColors.Night, modifier = Modifier.semantics { heading() })
        Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            series.forEachIndexed { index, label ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(SeriesColors[index % 3], CircleShape))
                    Text(label, style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
                }
            }
        }
        rows.forEach { row ->
            val label = "Woche ab ${week(row.week)}: " + series.zip(row.values).joinToString(", ") { (name, value) -> "$name $value" }
            Row(
                Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = label },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(week(row.week), style = MildcrowdType.Small, color = MildcrowdColors.Ink, modifier = Modifier.width(60.dp))
                row.values.forEachIndexed { index, value ->
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val width = if (value == 0) 0f else maxOf(4f, 56f * value / maximum)
                        Box(Modifier.width(width.dp).height(8.dp).background(SeriesColors[index % 3], CircleShape))
                        Text("$value", style = MildcrowdType.Caption, color = MildcrowdColors.Ink)
                    }
                }
            }
            HorizontalDivider(color = MildcrowdColors.PaperLine)
        }
    }
}

/** "3. Aug." – the start of a week. */
fun week(instant: Instant): String = GermanTime.format(instant, "d. MMM")
