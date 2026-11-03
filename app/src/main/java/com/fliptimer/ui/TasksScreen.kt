package com.fliptimer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fliptimer.core.Task
import com.fliptimer.core.TaskLists
import com.fliptimer.core.TaskLists.DUE_SOON_WINDOW_MS
import com.fliptimer.core.formatDeadlineClock
import com.fliptimer.graph
import kotlin.math.abs

@Composable
fun TasksScreen() {
    val controller = LocalContext.current.graph.tasks
    val tasks by controller.tasks.collectAsState(initial = emptyList())
    val now = rememberNow(1000L)

    var editing by remember { mutableStateOf<Task?>(null) }
    var remindersFor by remember { mutableStateOf<Task?>(null) }
    var deleting by remember { mutableStateOf<Task?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var showCompleted by rememberSaveable { mutableStateOf(false) }

    val active = TaskLists.active(tasks)
    val completed = TaskLists.completed(tasks)

    val cardActions = TaskCardActions(
        onEdit = { editing = it },
        onReminders = { remindersFor = it },
        onToggleComplete = { t -> controller.setProgress(t, if (t.isComplete) 90 else 100) },
        onDelete = { deleting = it },
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tasks.isEmpty()) {
                item {
                    Text(
                        "No tasks yet. Tap + to add one.",
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(active, key = { it.id }) { TaskCard(it, now, cardActions) }

            if (completed.isNotEmpty()) {
                item(key = "completed_header") {
                    Row(
                        Modifier.fillMaxWidth().clickable { showCompleted = !showCompleted }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Completed (${completed.size})", style = MaterialTheme.typography.titleSmall)
                        Icon(
                            if (showCompleted) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (showCompleted) "Hide completed" else "Show completed",
                        )
                        Spacer(Modifier.weight(1f))
                        if (showCompleted) TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
                    }
                }
                if (showCompleted) items(completed, key = { it.id }) { TaskCard(it, now, cardActions) }
            }
        }
        FloatingActionButton(
            onClick = { editing = Task(createdAt = System.currentTimeMillis()) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Add task") }
    }

    editing?.let { t ->
        TaskEditorDialog(
            initial = t,
            onDismiss = { editing = null },
            onSave = { controller.save(it); editing = null },
        )
    }
    remindersFor?.let { t ->
        RemindersDialog(
            initial = t.reminders,
            onDismiss = { remindersFor = null },
            onSave = { controller.save(t.copy(reminders = it)); remindersFor = null },
        )
    }
    deleting?.let { t ->
        ConfirmDialog(
            title = "Delete \"${t.name}\"?",
            confirmLabel = "Delete",
            onDismiss = { deleting = null },
            onConfirm = { controller.delete(t); deleting = null },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Delete all ${completed.size} completed tasks?",
            confirmLabel = "Delete",
            onDismiss = { confirmClear = false },
            onConfirm = { controller.clearCompleted(); confirmClear = false },
        )
    }
}

private class TaskCardActions(
    val onEdit: (Task) -> Unit,
    val onReminders: (Task) -> Unit,
    val onToggleComplete: (Task) -> Unit,
    val onDelete: (Task) -> Unit,
)

@Composable
private fun TaskCard(task: Task, now: Long, actions: TaskCardActions) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(onClick = { actions.onEdit(task) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isComplete && !task.isRecurring) TextDecoration.LineThrough else null,
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; actions.onEdit(task) })
                        if (task.deadline != null && !task.isComplete) {
                            DropdownMenuItem(
                                text = { Text("Reminders…") },
                                onClick = { menuOpen = false; actions.onReminders(task) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (task.isComplete) "Reopen" else "Mark complete") },
                            onClick = { menuOpen = false; actions.onToggleComplete(task) },
                        )
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; actions.onDelete(task) })
                    }
                }
            }
            Column(Modifier.padding(end = 12.dp)) {
                DeadlineCountdown(task, now)
                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(progress = { task.progress / 100f }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Text("${task.progress}%", style = MaterialTheme.typography.labelMedium)
                }
                if (task.isComplete && !task.isRecurring) {
                    task.completedAt?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Completed ${formatDate(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** The live countdown: the main thing on a card with a deadline. */
@Composable
private fun DeadlineCountdown(task: Task, now: Long) {
    val deadline = task.deadline ?: return
    if (task.isComplete && !task.isRecurring) return
    val remaining = deadline - now

    val label = when {
        task.isComplete -> "Done · resets in"
        remaining < 0 -> "Overdue by"
        else -> "Due in"
    }
    val color = when {
        task.isComplete -> MaterialTheme.colorScheme.onSurfaceVariant
        remaining < 0 -> MaterialTheme.colorScheme.error
        remaining <= DUE_SOON_WINDOW_MS -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val details = listOfNotNull(formatDateTime(deadline), task.repeat?.label).joinToString(" · ")

    Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    Text(
        formatDeadlineClock(abs(remaining)),
        color = color,
        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
    )
    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
}

@Composable
fun ConfirmDialog(title: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
