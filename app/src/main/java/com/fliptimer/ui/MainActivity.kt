package com.fliptimer.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.fliptimer.R
import com.fliptimer.graph
import com.fliptimer.system.Notifications

enum class Tab(val label: String) { Timer("Timer"), Tasks("Tasks"), History("History") }

class MainActivity : ComponentActivity() {
    private var tab by mutableStateOf(Tab.Timer)
    private var notifAllowed by mutableStateOf(true)

    private val requestNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            notifAllowed = Notifications.canPost(this)
            graph.launchSyncAll()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notifAllowed = Notifications.canPost(this)
        if (!notifAllowed && savedInstanceState == null) askForNotifications()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            FlipTimerTheme {
                AppScaffold(
                    tab = tab,
                    onTab = { tab = it },
                    notifAllowed = notifAllowed,
                    onRequestNotif = ::askForNotifications,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        notifAllowed = Notifications.canPost(this)
        graph.launchSyncAll() // also restores the timer notification if it was swiped away
    }

    private fun handleIntent(intent: Intent?) {
        val name = intent?.getStringExtra(EXTRA_TAB) ?: return
        Tab.entries.firstOrNull { it.name == name }?.let { tab = it }
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33) requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val EXTRA_TAB = "tab"
    }
}

@Composable
private fun AppScaffold(tab: Tab, onTab: (Tab) -> Unit, notifAllowed: Boolean, onRequestNotif: () -> Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { onTab(t) },
                        icon = {
                            when (t) {
                                Tab.Timer -> Icon(painterResource(R.drawable.ic_timer), contentDescription = null)
                                Tab.Tasks -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                Tab.History -> Icon(Icons.Filled.DateRange, contentDescription = null)
                            }
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Timer -> TimerScreen(notifAllowed, onRequestNotif, onOpenTasks = { onTab(Tab.Tasks) })
                Tab.Tasks -> TasksScreen()
                Tab.History -> HistoryScreen()
            }
        }
    }
}
