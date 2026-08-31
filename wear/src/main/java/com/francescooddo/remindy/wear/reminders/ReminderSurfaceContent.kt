package com.francescooddo.remindy.wear.reminders

import com.francescooddo.remindy.wear.protocol.WearReminder

internal data class ReminderSurfaceContent(
    val appReminders: List<WearReminder>,
    val tileTitles: List<String>,
    val complicationText: String,
) {
    companion object {
        fun from(reminders: List<WearReminder>): ReminderSurfaceContent = ReminderSurfaceContent(
            appReminders = reminders,
            tileTitles = reminders.take(MAX_TILE_REMINDERS).map(WearReminder::title),
            complicationText = reminders.size.toString(),
        )

        private const val MAX_TILE_REMINDERS = 2
    }
}
