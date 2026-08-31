package com.francescooddo.remindy.wear.nfc

internal class SingleTagScanGate {
    private var activeOperation: ScanOperationId? = null
    private var claimed = false

    @Synchronized
    fun arm(operationId: ScanOperationId): Boolean {
        if (activeOperation != null) return false
        activeOperation = operationId
        claimed = false
        return true
    }

    @Synchronized
    fun claim(operationId: ScanOperationId): Boolean {
        if (activeOperation != operationId || claimed) return false
        claimed = true
        return true
    }

    @Synchronized
    fun finish(operationId: ScanOperationId): Boolean {
        if (activeOperation != operationId || !claimed) return false
        activeOperation = null
        claimed = false
        return true
    }

    @Synchronized
    fun cancel() {
        activeOperation = null
        claimed = false
    }
}
