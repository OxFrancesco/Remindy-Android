package com.francescooddo.remindy.nfc

import android.nfc.NfcAdapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NfcScannerTest {
    @Test
    fun `one scan accepts only one tag and rejects duplicate starts`() {
        val gate = NfcScanGate()

        val firstScan = gate.arm()
        requireNotNull(firstScan)
        assertEquals(null, gate.arm(), "A second scan must not replace the active scan")
        assertTrue(gate.claimTag(firstScan))
        assertFalse(gate.claimTag(firstScan), "A bouncing tag must not be processed twice")

        assertTrue(gate.finish(firstScan))
        assertTrue(gate.arm() != null, "The scanner must re-arm after finishing")
    }

    @Test
    fun `callbacks from a finished scan cannot consume the next scan`() {
        val gate = NfcScanGate()
        val firstScan = requireNotNull(gate.arm())
        assertTrue(gate.claimTag(firstScan))
        assertTrue(gate.finish(firstScan))

        val secondScan = requireNotNull(gate.arm())
        assertFalse(gate.claimTag(firstScan))
        assertFalse(gate.finish(firstScan))
        assertTrue(gate.claimTag(secondScan))
    }

    @Test
    fun `read scans consume the tag without enumerating NDEF`() {
        val flags = NfcScanner.readerFlags(NfcScanner.Mode.READ)

        assertEquals(
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            flags and NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        )
    }

    @Test
    fun `link scans enumerate NDEF so the tag can be overwritten`() {
        val flags = NfcScanner.readerFlags(NfcScanner.Mode.WRITE)

        assertEquals(
            0,
            flags and NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        )
    }

    @Test
    fun `written tag opens the linked reminder`() {
        assertEquals("remindy://t/04A1B2C3", NfcScanner.linkUri("04A1B2C3"))
    }

    @Test
    fun `tag is only shown as linked after a successful write`() {
        val failedWrite = NfcScanner.ScanOutcome(
            uid = "04A1B2C3",
            wroteLink = false,
            error = "Writing failed"
        )
        val successfulWrite = NfcScanner.ScanOutcome(
            uid = "04A1B2C3",
            wroteLink = true,
            error = null
        )

        assertEquals(null, failedWrite.linkedUid)
        assertEquals("04A1B2C3", successfulWrite.linkedUid)
    }
}
