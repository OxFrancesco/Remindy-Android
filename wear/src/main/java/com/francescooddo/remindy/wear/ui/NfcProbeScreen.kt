package com.francescooddo.remindy.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.francescooddo.remindy.wear.nfc.AdapterAvailability
import com.francescooddo.remindy.wear.nfc.NfcCapability
import com.francescooddo.remindy.wear.nfc.NfcProbeState
import com.francescooddo.remindy.wear.nfc.ReaderModeVerdict
import com.francescooddo.remindy.wear.nfc.ScanBlocker
import com.francescooddo.remindy.wear.nfc.ScanPhase

@Composable
internal fun NfcProbeScreen(
    state: NfcProbeState,
    onScanAgain: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val scanning = state.scan is ScanPhase.Starting || state.scan is ScanPhase.Listening

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
        ) {
            item {
                ListHeader {
                    Text("Remindy NFC")
                }
            }
            item {
                Text(
                    text = scanLabel(state.scan),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Text(
                    text = state.lastObservation?.uid?.value ?: "No tag read yet",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    text = capabilityLabel(state.capability),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            if (!scanning) {
                item {
                    Button(
                        label = { Text("Scan again") },
                        onClick = onScanAgain,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun scanLabel(scan: ScanPhase): String = when (scan) {
    ScanPhase.Paused -> "Open to scan"
    is ScanPhase.Starting -> "Starting reader…"
    is ScanPhase.Listening -> "Ready — hold near tag"
    is ScanPhase.Read -> "Tag read"
    is ScanPhase.Failed -> scan.message
    is ScanPhase.Blocked -> when (scan.reason) {
        ScanBlocker.AdapterMissing -> "NFC adapter unavailable"
        ScanBlocker.NfcDisabled -> "NFC is off"
        ScanBlocker.ReaderModeUnsupported -> "Reader mode unsupported"
    }
}

private fun capabilityLabel(capability: NfcCapability): String = when (capability) {
    NfcCapability.Checking -> "Checking NFC…"
    is NfcCapability.Inspected -> {
        val adapter = when (capability.adapter) {
            AdapterAvailability.Missing -> "adapter missing"
            AdapterAvailability.Disabled -> "adapter disabled"
            AdapterAvailability.Enabled -> "adapter enabled"
        }
        val reader = when (capability.readerMode) {
            ReaderModeVerdict.NotTried -> "reader not tried"
            ReaderModeVerdict.Accepted -> "reader accepted"
            is ReaderModeVerdict.Unsupported -> "reader unsupported"
            is ReaderModeVerdict.Failed -> "reader failed"
        }
        val feature = if (capability.featureNfcAdvertised) "feature yes" else "feature no"
        "$adapter · $reader · $feature"
    }
}
