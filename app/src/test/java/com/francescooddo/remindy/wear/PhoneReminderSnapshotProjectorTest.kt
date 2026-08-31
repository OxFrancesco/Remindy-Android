package com.francescooddo.remindy.wear

import com.francescooddo.remindy.data.ReminderEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneReminderSnapshotProjectorTest {
    @Test
    fun `phone publishes only active top level reminders in app order`() {
        val reminders = listOf(
            ReminderEntity(id = "later", title = "Later", createdAt = 30L),
            ReminderEntity(id = "due", title = "Due first", createdAt = 40L, dueDate = 900L),
            ReminderEntity(id = "earlier", title = "Earlier", createdAt = 10L),
            ReminderEntity(id = "done", title = "Done", completedAt = 800L),
            ReminderEntity(id = "archived", title = "Archived", isArchived = true),
            ReminderEntity(id = "child", title = "Subtask", parentId = "earlier"),
        )

        val snapshot = PhoneReminderSnapshotProjector.project(
            reminders = reminders,
            nowMillis = 1_000L,
        )

        assertEquals(listOf("Due first", "Earlier", "Later"), snapshot.reminders.map { it.title })
    }
}
