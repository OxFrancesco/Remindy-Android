package com.francescooddo.remindy.wear

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
}
