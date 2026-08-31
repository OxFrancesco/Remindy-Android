package com.francescooddo.remindy.wear.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TagUidTest {
    @Test
    fun `tag bytes become uppercase hex without losing zeroes`() {
        val uid = TagUid.fromTagId(byteArrayOf(0x04, 0x00, 0xA1.toByte(), 0xFF.toByte()))

        assertEquals("0400A1FF", uid?.value)
    }

    @Test
    fun `empty tag ids are rejected`() {
        assertNull(TagUid.fromTagId(byteArrayOf()))
    }
}
