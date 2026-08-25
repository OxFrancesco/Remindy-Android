package com.francescooddo.remindy.nfc

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
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

    enum class Mode { READ, WRITE }

    data class ScanOutcome(
        val uid: String?,
        val wroteLink: Boolean,
        val error: String?
    )

    var isScanning: Boolean = false
        private set

    var onScanningChanged: ((Boolean) -> Unit)? = null

    private val adapter: NfcAdapter? get() = NfcAdapter.getDefaultAdapter(activity)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var completion: ((ScanOutcome) -> Unit)? = null
    private val scanGate = NfcScanGate()

    private fun readerCallback(mode: Mode, scanId: Long) =
        NfcAdapter.ReaderCallback { tag ->
            if (!scanGate.claimTag(scanId)) return@ReaderCallback
            val uid = uidHex(tag.id)
            if (mode == Mode.READ) {
                mainHandler.post {
                    Haptics.success(activity)
                    finish(ScanOutcome(uid = uid, wroteLink = false, error = null), scanId)
                }
                return@ReaderCallback
            }
            writeLink(tag, uid, scanId)
        }

    fun scan(mode: Mode, completion: (ScanOutcome) -> Unit) {
        if (!isAvailable) {
            Haptics.error(activity)
            completion(ScanOutcome(uid = null, wroteLink = false, error = "NFC isn't available on this device."))
            return
        }
        val scanId = scanGate.arm() ?: return
        this.completion = completion
        setScanning(true)
        try {
            adapter?.enableReaderMode(
                activity,
                readerCallback(mode, scanId),
                readerFlags(mode),
                Bundle()
            )
        } catch (_: Exception) {
            Haptics.error(activity)
            finish(
                ScanOutcome(uid = null, wroteLink = false, error = "Couldn't start NFC scanning."),
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
        callback?.invoke(outcome ?: ScanOutcome(uid = null, wroteLink = false, error = null))
    }

    private fun setScanning(value: Boolean) {
        isScanning = value
        onScanningChanged?.invoke(value)
    }

    private fun deliver(outcome: ScanOutcome, scanId: Long) {
        mainHandler.post { finish(outcome, scanId) }
    }

    private fun writeLink(tag: Tag, uid: String, scanId: Long) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            Haptics.error(activity)
            deliver(
                ScanOutcome(
                    uid = uid,
                    wroteLink = false,
                    error = "This tag can't store links, so background taps won't work. It's still linked for in-app scans."
                ),
                scanId
            )
            return
        }
        val outcome = try {
            ndef.use { connection ->
                connection.connect()
                if (!connection.isWritable) {
                    ScanOutcome(
                        uid = uid,
                        wroteLink = false,
                        error = "This tag is read-only, so background taps won't work. It's still linked for in-app scans."
                    )
                } else {
                    val record = NdefRecord.createUri("remindy://t/$uid")
                    connection.writeNdefMessage(NdefMessage(record))
                    ScanOutcome(uid = uid, wroteLink = true, error = null)
                }
            }
        } catch (_: Exception) {
            ScanOutcome(uid = uid, wroteLink = false, error = "Writing to the tag failed.")
        }
        if (outcome.error == null) {
            Haptics.success(activity)
        } else {
            Haptics.error(activity)
        }
        deliver(outcome, scanId)
    }

    companion object {
        internal fun readerFlags(mode: Mode): Int {
            val pollingFlags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V

            return if (mode == Mode.READ) {
                pollingFlags or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            } else {
                pollingFlags
            }
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
