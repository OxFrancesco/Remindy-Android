package com.francescooddo.remindy.wear

import android.content.Context
import android.util.Log
import com.francescooddo.remindy.Graph
import com.francescooddo.remindy.data.ReminderDao
import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.notifications.Notifications
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

class WearTagCompletionService : WearableListenerService() {
    private val processor by lazy {
        WearTagCompletionProcessor(
            reminderStore = RoomTagLinkedReminderStore(Graph.db.reminderDao()),
            operationStore = SharedPreferencesProcessedWearOperations(this),
        )
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        val result = runCatching {
            runBlocking {
                processor.process(messageEvent.path, messageEvent.data)
            }
        }.getOrElse { failure ->
            Log.e(LOG_TAG, "tag_completion_failed", failure)
            return
        }

        when (result) {
            is WearTagCompletionResult.Completed -> {
                val message = if (result.logged) "Logged from your watch" else "Completed from your watch"
                Notifications.postTagCompletion(this, result.title, message)
                Log.i(LOG_TAG, "tag_completion_completed")
            }
            is WearTagCompletionResult.AlreadyDone ->
                Log.i(LOG_TAG, "tag_completion_already_done")
            is WearTagCompletionResult.UnknownTag -> {
                Notifications.postTagCompletion(this, "Unknown NFC tag", "Link this tag in Remindy first")
                Log.w(LOG_TAG, "tag_completion_unknown")
            }
            is WearTagCompletionResult.Duplicate ->
                Log.d(LOG_TAG, "tag_completion_duplicate operationId=${result.operationId}")
            WearTagCompletionResult.Invalid -> Log.w(LOG_TAG, "tag_completion_invalid")
            WearTagCompletionResult.Ignored -> Unit
        }
    }

    private companion object {
        const val LOG_TAG = "RemindyWearBridge"
    }
}

private class RoomTagLinkedReminderStore(
    private val dao: ReminderDao,
) : TagLinkedReminderStore {
    override suspend fun findByTag(uid: String): ReminderEntity? = dao.byTag(uid)

    override suspend fun update(reminder: ReminderEntity) {
        dao.update(reminder)
    }
}

private class SharedPreferencesProcessedWearOperations(
    context: Context,
) : ProcessedWearOperations {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun claim(operationId: String): Boolean {
        val operations = readOperations()
        if (!operations.add(operationId)) return false
        while (operations.size > MAX_OPERATIONS) {
            operations.remove(operations.first())
        }
        return preferences.edit().putString(KEY_OPERATIONS, operations.joinToString("\n")).commit()
    }

    @Synchronized
    override fun release(operationId: String) {
        val operations = readOperations()
        if (operations.remove(operationId)) {
            preferences.edit().putString(KEY_OPERATIONS, operations.joinToString("\n")).apply()
        }
    }

    private fun readOperations(): LinkedHashSet<String> =
        preferences.getString(KEY_OPERATIONS, null)
            ?.lineSequence()
            ?.filter(String::isNotBlank)
            ?.toCollection(LinkedHashSet())
            ?: LinkedHashSet()

    private companion object {
        const val PREFERENCES_NAME = "wear_tag_completions"
        const val KEY_OPERATIONS = "processed_operation_ids"
        const val MAX_OPERATIONS = 128
    }
}
