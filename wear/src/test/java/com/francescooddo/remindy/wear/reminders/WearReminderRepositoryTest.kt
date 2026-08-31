package com.francescooddo.remindy.wear.reminders

import com.francescooddo.remindy.wear.protocol.ReminderSnapshot
import com.francescooddo.remindy.wear.protocol.WearReminder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WearReminderRepositoryTest {
    @Test
    fun `watch replaces its list with exactly the latest phone snapshot`() {
        val old = ReminderSnapshot(1L, listOf(WearReminder("old", "Old reminder")))
        val storage = InMemoryReminderSnapshotStorage(old)
        val repository = WearReminderRepository(storage)
        val latest = ReminderSnapshot(
            2L,
            listOf(
                WearReminder("first", "Buy milk"),
                WearReminder("second", "Call mum"),
            ),
        )

        val accepted = repository.accept(ReminderSnapshot.PATH, latest.encode())

        assertTrue(accepted)
        assertEquals(listOf("Buy milk", "Call mum"), repository.reminders.value.map { it.title })
        assertEquals(latest, storage.snapshot)
    }

    @Test
    fun `identical cached snapshot does not trigger surface refresh work`() {
        val snapshot = ReminderSnapshot(2L, listOf(WearReminder("one", "Buy milk")))
        val repository = WearReminderRepository(InMemoryReminderSnapshotStorage(snapshot))

        assertFalse(repository.accept(ReminderSnapshot.PATH, snapshot.encode()))
    }

    private class InMemoryReminderSnapshotStorage(
        override var snapshot: ReminderSnapshot,
    ) : ReminderSnapshotStorage {
        override fun write(snapshot: ReminderSnapshot) {
            this.snapshot = snapshot
        }
    }
}
