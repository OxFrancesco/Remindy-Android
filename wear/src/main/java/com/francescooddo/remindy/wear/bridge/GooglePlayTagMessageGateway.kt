package com.francescooddo.remindy.wear.bridge

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Wearable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal class GooglePlayTagMessageGateway(
    context: Context,
) : TagMessageGateway {
    private val nodeClient = Wearable.getNodeClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    override suspend fun connectedNodeIds(): List<String> =
        nodeClient.connectedNodes.awaitResult().map { node -> node.id }

    override suspend fun sendMessage(nodeId: String, path: String, payload: ByteArray): Boolean =
        runCatching {
            messageClient.sendMessage(nodeId, path, payload).awaitResult()
            true
        }.getOrDefault(false)
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener(continuation::resume)
    addOnFailureListener(continuation::resumeWithException)
    addOnCanceledListener(continuation::cancel)
}
