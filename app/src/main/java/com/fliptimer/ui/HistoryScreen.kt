package com.fliptimer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fliptimer.core.daysEndedNonNegative
import com.fliptimer.core.displaySeconds
import com.fliptimer.core.formatMillis
import com.fliptimer.graph
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

@Composable
fun HistoryScreen() {
    val days by LocalContext.current.graph.dayLog.days.collectAsState(initial = emptyList())

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        if (days.isEmpty()) {
            item {
                Text(
                    "Each day's ending timer value will show up here after midnight.",
                    modifier = Modifier.padding(top = 48.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@LazyColumn
        }
        item {
            val recent = minOf(7, days.size)
            Text(
                "Last $recent days: ${daysEndedNonNegative(days, recent)} ended at 0:00 or above",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        items(days, key = { it.date.toEpochDay() }) { d ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dayFormat.format(d.date), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    formatMillis(d.valueMillis),
                    color = if (displaySeconds(d.valueMillis) < 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    style = TextStyle(fontSize = 20.sp, fontFeatureSettings = "tnum"),
                )
            }
            HorizontalDivider()
        }
    }
}
