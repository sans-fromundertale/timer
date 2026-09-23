package com.fliptimer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fliptimer.core.TimerState
import com.fliptimer.core.formatMillis
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var notifAllowed by mutableStateOf(true)

    private val requestNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            notifAllowed = Notifications.canPost(this)
            TimerRepository.syncSideEffects(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TimerRepository.init(this)
        Notifications.createChannels(this)
        notifAllowed = Notifications.canPost(this)
        if (!notifAllowed) askForNotifications()

        setContent {
            val ctx = LocalContext.current
            val dark = isSystemInDarkTheme()
            val scheme = when {
                Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(ctx)
                Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(ctx)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = scheme) {
                Surface(Modifier.fillMaxSize()) {
                    TimerScreen(notifAllowed, ::askForNotifications)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notifAllowed = Notifications.canPost(this)
        TimerRepository.syncSideEffects(this) // restores the notification if it was swiped away
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33) requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
fun TimerScreen(notifAllowed: Boolean, onRequestNotif: () -> Unit) {
    val ctx = LocalContext.current
    val s by TimerRepository.state.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(s.running, s.anchorMillis) {
        while (true) {
            now = System.currentTimeMillis()
            if (!s.running) break
            delay(50)
        }
    }

    fun update(f: (TimerState) -> TimerState) = TimerRepository.update(ctx, f)

    val value = s.valueAt(now)
    val text = formatMillis(value)
    val timeColor =
        if (value < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            if (s.direction == TimerState.DOWN) "▼ Counting down" else "▲ Counting up",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            color = timeColor,
            maxLines = 1,
            style = TextStyle(
                fontSize = if (text.length > 6) 56.sp else 80.sp,
                fontWeight = FontWeight.Light,
                fontFeatureSettings = "tnum",
            ),
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val t = System.currentTimeMillis()
                update { if (it.running) it.pause(t) else it.start(t) }
            }) { Text(if (s.running) "Pause" else "Start") }
            FilledTonalButton(onClick = {
                val t = System.currentTimeMillis()
                update { it.flip(t) }
            }) { Text("Flip") }
            OutlinedButton(onClick = { update { it.reset() } }) { Text("Reset") }
        }

        if (!s.running) {
            Spacer(Modifier.height(40.dp))
            StartTimeEditor(s.initialMillis) { ms -> update { it.setInitial(ms) } }
        }

        if (!notifAllowed) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Notifications are off, so you won't get alerts at zero or see the timer while the app is closed.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRequestNotif) { Text("Turn on notifications") }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun StartTimeEditor(initialMillis: Long, onSet: (Long) -> Unit) {
    val total = initialMillis / 1000
    var h by remember(initialMillis) { mutableStateOf((total / 3600).toString()) }
    var m by remember(initialMillis) { mutableStateOf(((total % 3600) / 60).toString()) }
    var sec by remember(initialMillis) { mutableStateOf((total % 60).toString()) }

    Text("Start time", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField(h, { h = it }, "h")
        NumField(m, { m = it }, "min")
        NumField(sec, { sec = it }, "sec")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = {
        val secs = (h.toLongOrNull() ?: 0) * 3600 + (m.toLongOrNull() ?: 0) * 60 + (sec.toLongOrNull() ?: 0)
        onSet(secs * 1000)
    }) { Text("Set and reset") }
}

@Composable
private fun NumField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(3)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(88.dp),
    )
}
