package com.francescooddo.remindy.ui.detail

import com.francescooddo.remindy.nfc.NfcScanner

internal data class TaskTagLinkState(
    val linkedTagId: String?,
    val error: String?
)

internal object TaskTagLinkWorkflow {
    fun applyWrite(
        currentTagId: String?,
        outcome: NfcScanner.ScanOutcome,
        persist: (String?) -> Unit
    ): TaskTagLinkState {
        val writtenUid = outcome.linkedUid
            ?: return TaskTagLinkState(currentTagId, outcome.error)

        persist(writtenUid)
        return TaskTagLinkState(writtenUid, null)
    }

    fun unlink(persist: (String?) -> Unit): TaskTagLinkState {
        persist(null)
        return TaskTagLinkState(linkedTagId = null, error = null)
    }
}
