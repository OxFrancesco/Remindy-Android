package com.francescooddo.remindy.wear.reminders

import com.francescooddo.remindy.wear.protocol.WearReminder
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderSurfaceContentTest {
    @Test
    fun `app tile and complication derive only from synced reminders`() {
        val reminders = listOf(
            WearReminder("one", "Buy milk"),
            WearReminder("two", "Call mum"),
            WearReminder("three", "Water plants"),
        )

        val content = ReminderSurfaceContent.from(reminders)

        assertEquals(listOf("Buy milk", "Call mum", "Water plants"), content.appReminders.map { it.title })
        assertEquals(listOf("Buy milk", "Call mum"), content.tileTitles)
        assertEquals("3", content.complicationText)
    }
}
