package com.francescooddo.remindy.wear

import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.domain.complete
import com.francescooddo.remindy.domain.isCurrentlyDone
import com.francescooddo.remindy.wear.protocol.TagCompletionRequest

internal interface TagLinkedReminderStore {
    suspend fun findByTag(uid: String): ReminderEntity?

    suspend fun update(reminder: ReminderEntity)
}

internal interface ProcessedWearOperations {
    fun claim(operationId: String): Boolean

    fun release(operationId: String)
}

internal sealed interface WearTagCompletionResult {
    data object Ignored : WearTagCompletionResult
    data object Invalid : WearTagCompletionResult
    data class Duplicate(val operationId: String) : WearTagCompletionResult
    data class UnknownTag(val uid: String) : WearTagCompletionResult
    data class AlreadyDone(val title: String) : WearTagCompletionResult
    data class Completed(
        val title: String,
        val logged: Boolean,
    ) : WearTagCompletionResult
}

internal class WearTagCompletionProcessor(
    private val reminderStore: TagLinkedReminderStore,
    private val operationStore: ProcessedWearOperations,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun process(path: String, payload: ByteArray): WearTagCompletionResult {
        if (path != TagCompletionRequest.PATH) return WearTagCompletionResult.Ignored
        val request = TagCompletionRequest.decode(payload) ?: return WearTagCompletionResult.Invalid
        if (!operationStore.claim(request.operationId)) {
            return WearTagCompletionResult.Duplicate(request.operationId)
        }

        return try {
            val reminder = reminderStore.findByTag(request.uid)
                ?: return WearTagCompletionResult.UnknownTag(request.uid)
            val now = nowMillis()
            if (reminder.isCurrentlyDone(now)) {
                WearTagCompletionResult.AlreadyDone(reminder.title)
            } else {
                reminder.complete(now)
                reminderStore.update(reminder)
                WearTagCompletionResult.Completed(
                    title = reminder.title,
                    logged = reminder.isLogger,
                )
            }
        } catch (failure: Exception) {
            operationStore.release(request.operationId)
            throw failure
        }
    }
}
