package com.fliptimer.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fliptimer.core.ReminderPreset
import com.fliptimer.core.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/** Default for a newly added deadline: tomorrow at 11:59 PM. */
private fun defaultDeadline(zone: ZoneId): Long =
    LocalDate.now(zone).plusDays(1).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()

@Composable
fun TaskEditorDialog(initial: Task, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var progress by remember { mutableFloatStateOf(initial.progress.toFloat()) }
    var deadline by remember { mutableStateOf(initial.deadline) }

    fun zoned(ms: Long): ZonedDateTime = Instant.ofEpochMilli(ms).atZone(zone)

    fun pickDate() {
        val current = zoned(deadline ?: defaultDeadline(zone))
        DatePickerDialog(context, { _, y, m, d ->
            deadline = LocalDate.of(y, m + 1, d).atTime(current.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }

    fun pickTime() {
        val current = zoned(deadline ?: defaultDeadline(zone))
        TimePickerDialog(context, { _, h, min ->
            deadline = current.toLocalDate().atTime(LocalTime.of(h, min)).atZone(zone).toInstant().toEpochMilli()
        }, current.hour, current.minute, DateFormat.is24HourFormat(context)).show()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text(if (initial.isNew) "New task" else "Edit task", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Text("Progress: ${progress.roundToInt()}%", style = MaterialTheme.typography.titleSmall)
                Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f, steps = 19)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Deadline", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(
                        checked = deadline != null,
                        onCheckedChange = { on -> deadline = if (on) defaultDeadline(zone) else null },
                    )
                }
                deadline?.let { d ->
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = ::pickDate) { Text(formatDate(d)) }
                        OutlinedButton(onClick = ::pickTime) { Text(formatTime(d)) }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.padding(4.dp))
                    Button(
                        enabled = name.isNotBlank(),
                        onClick = {
                            onSave(
                                initial.copy(name = name.trim(), description = description.trim(), deadline = deadline)
                                    .withProgress(progress.roundToInt(), System.currentTimeMillis())
                            )
                        },
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun RemindersDialog(initial: Set<ReminderPreset>, onDismiss: () -> Unit, onSave: (Set<ReminderPreset>) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminders") },
        text = {
            Column {
                ReminderPreset.entries.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = if (p in selected) selected - p else selected + p },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = p in selected, onCheckedChange = null)
                        Text(p.menuLabel, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                TextButton(onClick = { selected = ReminderPreset.DEFAULT }) { Text("Restore defaults") }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
