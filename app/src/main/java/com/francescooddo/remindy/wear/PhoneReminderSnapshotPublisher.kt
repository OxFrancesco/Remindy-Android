package com.francescooddo.remindy.wear

import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.wear.protocol.ReminderSnapshot

internal fun interface ReminderSnapshotGateway {
    suspend fun put(path: String, payload: ByteArray)
}

internal class PhoneReminderSnapshotPublisher(
    private val gateway: ReminderSnapshotGateway,
) {
    suspend fun publish(
        reminders: List<ReminderEntity>,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val snapshot = PhoneReminderSnapshotProjector.project(reminders, nowMillis)
        gateway.put(ReminderSnapshot.PATH, snapshot.encode())
    }
}
