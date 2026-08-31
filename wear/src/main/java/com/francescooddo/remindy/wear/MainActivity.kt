package com.francescooddo.remindy.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import com.francescooddo.remindy.wear.nfc.WearNfcProbe
import com.francescooddo.remindy.wear.ui.NfcProbeScreen

class MainActivity : ComponentActivity() {
    private lateinit var probe: WearNfcProbe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        probe = WearNfcProbe.attach(this)
        handleTagIntent(intent)

        setContent {
            val state by probe.state.collectAsStateWithLifecycle()
            MaterialTheme {
                AppScaffold {
                    NfcProbeScreen(
                        state = state,
                        onScanAgain = probe::requestScan,
                    )
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
