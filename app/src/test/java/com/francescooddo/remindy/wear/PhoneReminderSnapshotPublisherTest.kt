package com.francescooddo.remindy.wear

import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.wear.protocol.ReminderSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class PhoneReminderSnapshotPublisherTest {
    @Test
    fun `phone publishes its current reminder snapshot to the persistent data path`() = runBlocking {
        val gateway = RecordingSnapshotGateway()
        val publisher = PhoneReminderSnapshotPublisher(gateway)

        publisher.publish(
            reminders = listOf(ReminderEntity(id = "one", title = "Buy milk")),
            nowMillis = 42L,
        )

        assertEquals(ReminderSnapshot.PATH, gateway.path)
        assertEquals(listOf("Buy milk"), ReminderSnapshot.decode(gateway.payload)?.reminders?.map { it.title })
    }

    private class RecordingSnapshotGateway : ReminderSnapshotGateway {
        var path: String? = null
        var payload = byteArrayOf()

        override suspend fun put(path: String, payload: ByteArray) {
            this.path = path
            this.payload = payload.copyOf()
        }
    }
}
