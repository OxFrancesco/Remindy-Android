package com.francescooddo.remindy.wear.bridge

import com.francescooddo.remindy.wear.protocol.TagCompletionRequest

internal interface TagMessageGateway {
    suspend fun connectedNodeIds(): List<String>

    suspend fun sendMessage(nodeId: String, path: String, payload: ByteArray): Boolean
}

internal data class TagDeliverySummary(
    val attempted: Int,
    val delivered: Int,
)

internal class WearTagCompletionSender(
    private val gateway: TagMessageGateway,
) {
    suspend fun send(operationId: String, uid: String): TagDeliverySummary {
        val request = TagCompletionRequest(operationId = operationId, uid = uid)
        val nodes = gateway.connectedNodeIds().distinct()
        val delivered = nodes.count { nodeId ->
            gateway.sendMessage(
                nodeId = nodeId,
                path = TagCompletionRequest.PATH,
                payload = request.encode(),
            )
        }
        return TagDeliverySummary(attempted = nodes.size, delivered = delivered)
    }
}
