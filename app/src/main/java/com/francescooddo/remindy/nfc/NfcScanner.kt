package com.francescooddo.remindy.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper

internal class NfcScanGate {
    private data class ActiveScan(val id: Long, var claimedTag: Boolean = false)

    private var nextScanId = 0L
    private var activeScan: ActiveScan? = null

    @Synchronized
    fun arm(): Long? {
        if (activeScan != null) return null
        nextScanId += 1
        activeScan = ActiveScan(nextScanId)
        return nextScanId
    }

    @Synchronized
    fun claimTag(scanId: Long): Boolean {
        val scan = activeScan ?: return false
        if (scan.id != scanId || scan.claimedTag) return false
        scan.claimedTag = true
        return true
    }

    @Synchronized
    fun finish(scanId: Long): Boolean {
        if (activeScan?.id != scanId) return false
        activeScan = null
        return true
    }

    @Synchronized
    fun cancel() {
        activeScan = null
    }
}

class NfcScanner(private val activity: Activity) {
    data class ScanOutcome(
        val uid: String?,
        val error: String?
    )

    var isScanning: Boolean = false
        private set

    var onScanningChanged: ((Boolean) -> Unit)? = null

    private val adapter: NfcAdapter? get() = NfcAdapter.getDefaultAdapter(activity)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var completion: ((ScanOutcome) -> Unit)? = null
    private val scanGate = NfcScanGate()

    private fun readerCallback(scanId: Long) =
        NfcAdapter.ReaderCallback { tag ->
            if (!scanGate.claimTag(scanId)) return@ReaderCallback
            val uid = uidHex(tag.id)
            mainHandler.post {
                Haptics.success(activity)
                finish(ScanOutcome(uid = uid, error = null), scanId)
            }
        }

    fun scan(completion: (ScanOutcome) -> Unit) {
        if (!isAvailable) {
            Haptics.error(activity)
            completion(ScanOutcome(uid = null, error = "NFC isn't available on this device."))
            return
        }
        val scanId = scanGate.arm() ?: return
        this.completion = completion
        setScanning(true)
        try {
            adapter?.enableReaderMode(
                activity,
                readerCallback(scanId),
                readerFlags(),
                Bundle()
            )
        } catch (_: Exception) {
            Haptics.error(activity)
            finish(
                ScanOutcome(uid = null, error = "Couldn't start NFC scanning."),
                scanId
            )
        }
    }

    val isAvailable: Boolean
        get() {
            val current = adapter
            return current != null && current.isEnabled
        }

    fun disable() {
        disableReaderMode()
        scanGate.cancel()
        completion = null
        setScanning(false)
    }

    private fun disableReaderMode() {
        try {
            adapter?.disableReaderMode(activity)
        } catch (_: Exception) {
        }
    }

    private fun finish(outcome: ScanOutcome?, scanId: Long) {
        if (!scanGate.finish(scanId)) return
        val callback = completion
        completion = null
        disableReaderMode()
        setScanning(false)
        callback?.invoke(outcome ?: ScanOutcome(uid = null, error = null))
    }

    private fun setScanning(value: Boolean) {
        isScanning = value
        onScanningChanged?.invoke(value)
    }

    companion object {
        internal fun readerFlags(): Int {
            val pollingFlags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V

            return pollingFlags or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        }

        fun uidHex(data: ByteArray): String =
            data.joinToString("") { "%02X".format(it) }

        fun uidFromUrl(uriString: String): String? {
            val prefix = "remindy://t/"
            if (!uriString.startsWith(prefix)) return null
            val uid = uriString.removePrefix(prefix)
            return uid.ifEmpty { null }
        }
    }
}
