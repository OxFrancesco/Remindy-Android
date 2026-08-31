package com.francescooddo.remindy.wear

import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.domain.isCurrentlyDone
import com.francescooddo.remindy.wear.protocol.ReminderSnapshot
import com.francescooddo.remindy.wear.protocol.WearReminder

internal object PhoneReminderSnapshotProjector {
    fun project(
        reminders: List<ReminderEntity>,
        nowMillis: Long,
    ): ReminderSnapshot {
        val active = reminders
            .asSequence()
            .filter { reminder ->
                reminder.parentId == null &&
                    !reminder.isArchived &&
                    !reminder.isCurrentlyDone(nowMillis) &&
                    reminder.title.isNotBlank()
            }
            .sortedWith(
                compareBy<ReminderEntity> { it.dueDate == null }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    .thenBy(ReminderEntity::createdAt),
            )
            .map { reminder ->
                WearReminder(
                    id = reminder.id,
                    title = reminder.title.trim().take(MAX_WATCH_TITLE_LENGTH),
                )
            }
            .toList()
        return ReminderSnapshot(
            generatedAtMillis = nowMillis,
            reminders = active,
        )
    }

    private const val MAX_WATCH_TITLE_LENGTH = 240
}
