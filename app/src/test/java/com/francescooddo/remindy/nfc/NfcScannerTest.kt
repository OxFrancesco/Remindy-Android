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
    fun `write scans allow Android to enumerate NDEF`() {
        val flags = NfcScanner.readerFlags(NfcScanner.Mode.WRITE)

        assertEquals(0, flags and NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK)
    }

    @Test
    fun `read scans skip NDEF enumeration`() {
        val flags = NfcScanner.readerFlags(NfcScanner.Mode.READ)

        assertEquals(
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            flags and NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        )
    }
}
