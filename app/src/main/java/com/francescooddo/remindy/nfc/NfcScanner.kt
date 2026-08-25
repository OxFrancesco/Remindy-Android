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
    private var mode: Mode = Mode.READ
    private var completion: ((ScanOutcome) -> Unit)? = null

    private val readerCallback = object : NfcAdapter.ReaderCallback {
        override fun onTagDiscovered(tag: Tag) {
            val uid = uidHex(tag.id)
            if (mode == Mode.READ) {
                mainHandler.post {
                    Haptics.success(activity)
                    finish(ScanOutcome(uid = uid, wroteLink = false, error = null))
                }
                return
            }
            writeLink(tag, uid)
        }
    }

    fun scan(mode: Mode, completion: (ScanOutcome) -> Unit) {
        this.mode = mode
        this.completion = completion
        if (!isAvailable) {
            Haptics.error(activity)
            finish(ScanOutcome(uid = null, wroteLink = false, error = "NFC isn't available on this device."))
            return
        }
        setScanning(true)
        adapter?.enableReaderMode(
            activity,
            readerCallback,
            readerFlags(mode),
            Bundle()
        )
    }

    val isAvailable: Boolean
        get() {
            val current = adapter
            return current != null && current.isEnabled
        }

    fun disable() {
        try {
            adapter?.disableReaderMode(activity)
        } catch (_: Exception) {
        }
    }

    private fun finish(outcome: ScanOutcome?) {
        disable()
        setScanning(false)
        val callback = completion
        completion = null
        callback?.invoke(outcome ?: ScanOutcome(uid = null, wroteLink = false, error = null))
    }

    private fun setScanning(value: Boolean) {
        isScanning = value
        onScanningChanged?.invoke(value)
    }

    private fun deliver(outcome: ScanOutcome) {
        mainHandler.post { finish(outcome) }
    }

    private fun writeLink(tag: Tag, uid: String) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            Haptics.error(activity)
            deliver(
                ScanOutcome(
                    uid = uid,
                    wroteLink = false,
                    error = "This tag can't store links, so background taps won't work. It's still linked for in-app scans."
                )
            )
            return
        }
        try {
            ndef.connect()
            if (!ndef.isWritable) {
                Haptics.error(activity)
                deliver(
                    ScanOutcome(
                        uid = uid,
                        wroteLink = false,
                        error = "This tag is read-only, so background taps won't work. It's still linked for in-app scans."
                    )
                )
            } else {
                val record = NdefRecord.createUri("remindy://t/$uid")
                ndef.writeNdefMessage(NdefMessage(record))
                Haptics.success(activity)
                deliver(ScanOutcome(uid = uid, wroteLink = true, error = null))
            }
            ndef.close()
        } catch (_: Exception) {
            Haptics.error(activity)
            deliver(ScanOutcome(uid = uid, wroteLink = false, error = "Writing to the tag failed."))
        }
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
