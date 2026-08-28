package com.francescooddo.remindy.ui.detail

import com.francescooddo.remindy.nfc.NfcScanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskTagLinkWorkflowTest {
    @Test
    fun `successful relink immediately persists the new tag`() {
        val persistedTags = mutableListOf<String?>()

        val result = TaskTagLinkWorkflow.applyWrite(
            currentTagId = "OLD_TAG",
            outcome = NfcScanner.ScanOutcome(
                uid = "NEW_TAG",
                wroteLink = true,
                error = null
            ),
            persist = { tagId: String? -> persistedTags.add(tagId) }
        )

        assertEquals("NEW_TAG", result.linkedTagId)
        assertNull(result.error)
        assertEquals(listOf<String?>("NEW_TAG"), persistedTags)
    }

    @Test
    fun `failed relink keeps the previous tag and does not persist`() {
        val persistedTags = mutableListOf<String?>()

        val result = TaskTagLinkWorkflow.applyWrite(
            currentTagId = "OLD_TAG",
            outcome = NfcScanner.ScanOutcome(
                uid = "NEW_TAG",
                wroteLink = false,
                error = "Writing failed"
            ),
            persist = { tagId: String? -> persistedTags.add(tagId) }
        )

        assertEquals("OLD_TAG", result.linkedTagId)
        assertEquals("Writing failed", result.error)
        assertEquals(emptyList(), persistedTags)
    }

    @Test
    fun `unlink immediately clears the persisted tag`() {
        val persistedTags = mutableListOf<String?>()

        val result = TaskTagLinkWorkflow.unlink { tagId: String? -> persistedTags.add(tagId) }

        assertNull(result.linkedTagId)
        assertNull(result.error)
        assertEquals(listOf<String?>(null), persistedTags)
    }
}
