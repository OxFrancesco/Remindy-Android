package com.francescooddo.remindy.wear.nfc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SingleTagScanGateTest {
    @Test
    fun `one operation accepts one callback`() {
        val gate = SingleTagScanGate()
        val operation = ScanOperationId("scan-1")

        assertTrue(gate.arm(operation))
        assertFalse(gate.arm(ScanOperationId("scan-2")))
        assertTrue(gate.claim(operation))
        assertFalse(gate.claim(operation))
        assertTrue(gate.finish(operation))
    }

    @Test
    fun `cancel rejects a callback claimed before delivery`() {
        val gate = SingleTagScanGate()
        val operation = ScanOperationId("scan-1")

        assertTrue(gate.arm(operation))
        assertTrue(gate.claim(operation))
        gate.cancel()

        assertFalse(gate.finish(operation))
        assertFalse(gate.claim(operation))
        assertTrue(gate.arm(ScanOperationId("scan-2")))
    }
}
