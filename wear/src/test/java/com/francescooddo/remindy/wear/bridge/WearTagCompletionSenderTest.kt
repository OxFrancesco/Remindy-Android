package com.francescooddo.remindy.wear.bridge

import com.francescooddo.remindy.wear.protocol.TagCompletionRequest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class WearTagCompletionSenderTest {
    @Test
    fun `one stable tag message is sent to each connected phone`() = runBlocking {
        val gateway = RecordingTagMessageGateway(
            nodeIds = listOf("phone-b", "phone-a", "phone-a"),
        )
        val sender = WearTagCompletionSender(gateway)

        val result = sender.send(
            operationId = "scan-123",
            uid = "04A1B2C3D4E5F6",
        )

        assertEquals(TagDeliverySummary(attempted = 2, delivered = 2), result)
        assertEquals(listOf("phone-b", "phone-a"), gateway.messages.map { it.nodeId })
        gateway.messages.forEach { message ->
            assertEquals(TagCompletionRequest.PATH, message.path)
            assertContentEquals("1\nscan-123\n04A1B2C3D4E5F6".encodeToByteArray(), message.payload)
        }
    }

    private class RecordingTagMessageGateway(
        private val nodeIds: List<String>,
    ) : TagMessageGateway {
        val messages = mutableListOf<Message>()

        override suspend fun connectedNodeIds(): List<String> = nodeIds

        override suspend fun sendMessage(nodeId: String, path: String, payload: ByteArray): Boolean {
            messages += Message(nodeId, path, payload.copyOf())
            return true
        }
    }

    private data class Message(
        val nodeId: String,
        val path: String,
        val payload: ByteArray,
    )
}
