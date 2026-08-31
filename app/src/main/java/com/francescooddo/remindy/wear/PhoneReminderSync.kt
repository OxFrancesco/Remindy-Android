package com.francescooddo.remindy.wear

import android.content.Context
import android.util.Log
import com.francescooddo.remindy.data.ReminderDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal object PhoneReminderSync {
    fun start(
        context: Context,
        reminderDao: ReminderDao,
        scope: CoroutineScope,
    ) {
        val publisher = PhoneReminderSnapshotPublisher(
            GooglePlayReminderSnapshotGateway(context.applicationContext),
        )
        scope.launch {
            reminderDao.observeAll().collectLatest { reminders ->
                runCatching { publisher.publish(reminders) }
                    .onFailure { failure -> Log.w(LOG_TAG, "reminder_snapshot_publish_failed", failure) }
            }
        }
    }

    private const val LOG_TAG = "RemindyWearSync"
}
