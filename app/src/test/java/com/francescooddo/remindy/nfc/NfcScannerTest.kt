package com.francescooddo.remindy.nfc

import android.nfc.NfcAdapter
import kotlin.test.Test
import kotlin.test.assertEquals

class NfcScannerTest {
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
