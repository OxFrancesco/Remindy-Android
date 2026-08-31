package com.francescooddo.remindy.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import com.francescooddo.remindy.wear.nfc.WearNfcProbe
import com.francescooddo.remindy.wear.reminders.ReminderSurfaceContent
import com.francescooddo.remindy.wear.reminders.WearReminderGraph
import com.francescooddo.remindy.wear.reminders.WearReminderSync
import com.francescooddo.remindy.wear.ui.ReminderListScreen

class MainActivity : ComponentActivity() {
    private lateinit var probe: WearNfcProbe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        probe = WearNfcProbe.attach(this)
        handleTagIntent(intent)
        WearReminderSync.refresh(this, lifecycleScope)

        setContent {
            val reminders by WearReminderGraph.repository.reminders.collectAsStateWithLifecycle()
            MaterialTheme {
                AppScaffold {
                    ReminderListScreen(ReminderSurfaceContent.from(reminders).appReminders)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTagIntent(intent)
    }

    private fun handleTagIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "remindy" || uri.host != "t") return
        uri.lastPathSegment?.let(probe::acceptDiscoveredUid)
    }
}
