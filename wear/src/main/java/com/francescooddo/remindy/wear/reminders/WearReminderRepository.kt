package com.francescooddo.remindy.wear.reminders

import com.francescooddo.remindy.wear.protocol.ReminderSnapshot
import com.francescooddo.remindy.wear.protocol.WearReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface ReminderSnapshotStorage {
    val snapshot: ReminderSnapshot

    fun write(snapshot: ReminderSnapshot)
}

internal class WearReminderRepository(
    private val storage: ReminderSnapshotStorage,
) {
    private val mutableReminders = MutableStateFlow(storage.snapshot.reminders)

    val reminders: StateFlow<List<WearReminder>> = mutableReminders.asStateFlow()

    fun accept(path: String, payload: ByteArray): Boolean {
        if (path != ReminderSnapshot.PATH) return false
        val snapshot = ReminderSnapshot.decode(payload) ?: return false
        if (snapshot == storage.snapshot) return false
        if (snapshot.generatedAtMillis < storage.snapshot.generatedAtMillis) return false
        storage.write(snapshot)
        mutableReminders.value = snapshot.reminders
        return true
    }
}
