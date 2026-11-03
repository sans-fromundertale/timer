package com.fliptimer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fliptimer.core.Task
import com.fliptimer.core.TaskLists
import com.fliptimer.core.TimerState
import com.fliptimer.core.formatCountdown
import com.fliptimer.core.formatMillis
import com.fliptimer.graph
import kotlin.math.abs

private const val MAX_DUE_SOON = 3

@Composable
fun TimerScreen(notifAllowed: Boolean, onRequestNotif: () -> Unit, onOpenTasks: () -> Unit) {
    val g = LocalContext.current.graph
    val s by g.timer.state.collectAsState()
    val tasks by g.tasks.tasks.collectAsState(initial = emptyList())
    val now = rememberNow(if (s.running) 50L else 1000L)
    var editingStart by remember { mutableStateOf(false) }

    val value = s.valueAt(now)
    val text = formatMillis(value)

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (s.direction == TimerState.DOWN) "▼ Counting down" else "▲ Counting up",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            color = if (value < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            style = TextStyle(
                fontSize = if (text.length > 6) 56.sp else 80.sp,
                fontWeight = FontWeight.Light,
                fontFeatureSettings = "tnum",
            ),
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { g.timer.dispatch { st, t -> if (st.running) st.pause(t) else st.start(t) } }) {
                Text(if (s.running) "Pause" else "Start")
            }
            FilledTonalButton(onClick = { g.timer.dispatch { st, t -> st.flip(t) } }) { Text("Flip") }
            OutlinedButton(onClick = { g.timer.dispatch { st, _ -> st.reset() } }) { Text("Reset") }
        }
        TextButton(onClick = { editingStart = true }) { Text("Start time: ${formatMillis(s.initialMillis)}") }

        val due = TaskLists.dueSoon(tasks, now)
        if (due.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            DueSoon(due, now, onOpenTasks)
        }

        if (!notifAllowed) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Notifications are off, so you won't get alerts or see the timer while the app is closed.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRequestNotif) { Text("Turn on notifications") }
        }
    }

    if (editingStart) {
        StartTimeDialog(
            initialMillis = s.initialMillis,
            onDismiss = { editingStart = false },
            onConfirm = { ms ->
                g.timer.dispatch { st, _ -> st.setInitial(ms) }
                editingStart = false
            },
        )
    }
}

@Composable
private fun DueSoon(due: List<Task>, now: Long, onOpenTasks: () -> Unit) {
    Column(Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
        Text(
            "Due within 24 hours",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        due.take(MAX_DUE_SOON).forEach { t ->
            val remaining = t.remaining(now)!!
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenTasks).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    t.name,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatCountdown(remaining),
                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }
        if (due.size > MAX_DUE_SOON) {
            Text(
                "+${due.size - MAX_DUE_SOON} more",
                modifier = Modifier.clickable(onClick = onOpenTasks).padding(vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StartTimeDialog(initialMillis: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val total = abs(initialMillis) / 1000
    var negative by remember { mutableStateOf(initialMillis < 0) }
    var h by remember { mutableStateOf((total / 3600).toString()) }
    var m by remember { mutableStateOf(((total % 3600) / 60).toString()) }
    var sec by remember { mutableStateOf((total % 60).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start time") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !negative, onClick = { negative = false }, label = { Text("+ Positive") })
                    FilterChip(selected = negative, onClick = { negative = true }, label = { Text("− Negative") })
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(h, { h = it }, "h", Modifier.weight(1f))
                    NumberField(m, { m = it }, "min", Modifier.weight(1f))
                    NumberField(sec, { sec = it }, "sec", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Setting this resets the timer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val secs = (h.toLongOrNull() ?: 0) * 3600 + (m.toLongOrNull() ?: 0) * 60 + (sec.toLongOrNull() ?: 0)
                onConfirm(secs * 1000 * (if (negative) -1 else 1))
            }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
