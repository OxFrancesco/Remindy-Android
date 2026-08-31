package com.francescooddo.remindy.wear

import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.wear.protocol.TagCompletionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class WearTagCompletionProcessorTest {
    @Test
    fun `duplicate watch delivery completes a matching reminder exactly once`() = runBlocking {
        val reminder = ReminderEntity(
            title = "Water the plant",
            tagId = "04A1B2C3D4E5F6",
        )
        val reminderStore = InMemoryReminderStore(reminder)
        val operationStore = InMemoryOperationStore()
        val processor = WearTagCompletionProcessor(
            reminderStore = reminderStore,
            operationStore = operationStore,
            nowMillis = { 1_234L },
        )
        val request = TagCompletionRequest(
            operationId = "scan-123",
            uid = "04A1B2C3D4E5F6",
        )

        val first = processor.process(TagCompletionRequest.PATH, request.encode())
        val duplicate = processor.process(TagCompletionRequest.PATH, request.encode())

        assertIs<WearTagCompletionResult.Completed>(first)
        assertIs<WearTagCompletionResult.Duplicate>(duplicate)
        assertEquals(1, reminderStore.updateCount)
        assertEquals(1_234L, reminder.completedAt)
    }

    private class InMemoryReminderStore(
        private val reminder: ReminderEntity,
    ) : TagLinkedReminderStore {
        var updateCount = 0

        override suspend fun findByTag(uid: String): ReminderEntity? =
            reminder.takeIf { it.tagId.equals(uid, ignoreCase = true) }

        override suspend fun update(reminder: ReminderEntity) {
            updateCount += 1
        }
    }

    private class InMemoryOperationStore : ProcessedWearOperations {
        private val claimed = mutableSetOf<String>()

        override fun claim(operationId: String): Boolean = claimed.add(operationId)

        override fun release(operationId: String) {
            claimed.remove(operationId)
        }
    }
}
