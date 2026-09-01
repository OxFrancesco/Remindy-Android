package com.francescooddo.remindy

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.francescooddo.remindy.nfc.FreshTagCompletions
import com.francescooddo.remindy.ui.AppViewModel
import com.francescooddo.remindy.ui.loading.RemindyLoadingGate
import com.francescooddo.remindy.ui.theme.RemindyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        setContent {
            RemindyTheme {
                RemindyLoadingGate(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme ?: return
        if (scheme != "remindy" || uri.host != "t") return
        val uid = uri.lastPathSegment
        if (
            uid != null &&
            intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED &&
            FreshTagCompletions.shouldIgnore(uid)
        ) {
            return
        }
        viewModel.completeByTag(uid)
    }
}
