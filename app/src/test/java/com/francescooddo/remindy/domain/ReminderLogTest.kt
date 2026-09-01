package com.francescooddo.remindy.domain

import com.francescooddo.remindy.data.ReminderEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderLogTest {
    @Test
    fun `deleting a history entry removes only the selected occurrence`() {
        val reminder = ReminderEntity(log = listOf(100L, 200L, 200L, 300L))

        assertTrue(reminder.removeLogEntry(200L))

        assertEquals(listOf(100L, 200L, 300L), reminder.log)
    }

    @Test
    fun `deleting a missing history entry leaves the log unchanged`() {
        val reminder = ReminderEntity(log = listOf(100L, 200L))

        assertFalse(reminder.removeLogEntry(300L))

        assertEquals(listOf(100L, 200L), reminder.log)
    }

    @Test
    fun `deleting the current completion reopens the reminder immediately`() {
        val reminder = ReminderEntity(
            completedAt = 300L,
            recurrence = Recurrence.DAILY,
            log = listOf(100L, 200L, 300L)
        )

        assertTrue(reminder.removeLogEntry(300L))

        assertNull(reminder.completedAt)
        assertFalse(reminder.isCurrentlyDone(now = 301L))
    }

    @Test
    fun `deleting an older occurrence preserves the current completion`() {
        val reminder = ReminderEntity(
            completedAt = 300L,
            recurrence = Recurrence.DAILY,
            log = listOf(100L, 200L, 300L)
        )

        assertTrue(reminder.removeLogEntry(200L))

        assertEquals(300L, reminder.completedAt)
        assertTrue(reminder.isCurrentlyDone(now = 301L))
    }
}
