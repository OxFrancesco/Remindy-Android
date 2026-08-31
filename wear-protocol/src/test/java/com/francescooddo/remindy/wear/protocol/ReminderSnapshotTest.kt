package com.francescooddo.remindy.wear.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderSnapshotTest {
    @Test
    fun `phone snapshot round trips titles without inventing rows`() {
        val snapshot = ReminderSnapshot(
            generatedAtMillis = 42L,
            reminders = listOf(
                WearReminder(id = "first", title = "Pay rent"),
                WearReminder(id = "second", title = "Call mum\nthis evening"),
            ),
        )

        val decoded = ReminderSnapshot.decode(snapshot.encode())

        assertEquals(snapshot, decoded)
    }

    @Test
    fun `watch rejects malformed reminder snapshots`() {
        assertNull(ReminderSnapshot.decode(byteArrayOf()))
        assertNull(ReminderSnapshot.decode("not-a-snapshot".encodeToByteArray()))
    }
}
