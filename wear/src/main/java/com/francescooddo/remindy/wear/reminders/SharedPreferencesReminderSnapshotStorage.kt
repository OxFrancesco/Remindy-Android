package com.francescooddo.remindy.wear.reminders

import android.content.Context
import android.util.Base64
import com.francescooddo.remindy.wear.protocol.ReminderSnapshot

internal class SharedPreferencesReminderSnapshotStorage(
    context: Context,
) : ReminderSnapshotStorage {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override val snapshot: ReminderSnapshot
        get() = preferences.getString(KEY_SNAPSHOT, null)
            ?.let { encoded -> runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull() }
            ?.let(ReminderSnapshot::decode)
            ?: EMPTY_SNAPSHOT

    override fun write(snapshot: ReminderSnapshot) {
        val encoded = Base64.encodeToString(snapshot.encode(), Base64.NO_WRAP)
        preferences.edit().putString(KEY_SNAPSHOT, encoded).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "wear_reminder_snapshot"
        const val KEY_SNAPSHOT = "snapshot"
        val EMPTY_SNAPSHOT = ReminderSnapshot(Long.MIN_VALUE, emptyList())
    }
}
