package com.francescooddo.remindy.nfc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreshTagCompletionGuardTest {
    @Test
    fun `newly written tag cannot immediately complete the reminder being created`() {
        var now = 1_000L
        val guard = FreshTagCompletionGuard(nowMillis = { now })

        guard.recordLinked("04A1B2C3")

        assertTrue(guard.shouldIgnore("04A1B2C3"))
        assertFalse(guard.shouldIgnore("04A1B2C3"), "Only the write echo should be ignored")
    }

    @Test
    fun `different and later tag taps still complete reminders`() {
        var now = 1_000L
        val guard = FreshTagCompletionGuard(nowMillis = { now })

        guard.recordLinked("04A1B2C3")

        assertFalse(guard.shouldIgnore("DIFFERENT"))
        now += 5_001L
        assertFalse(guard.shouldIgnore("04A1B2C3"))
    }
}
