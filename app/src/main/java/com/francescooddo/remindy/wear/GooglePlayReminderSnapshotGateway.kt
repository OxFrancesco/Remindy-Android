package com.francescooddo.remindy.wear

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal class GooglePlayReminderSnapshotGateway(
    context: Context,
) : ReminderSnapshotGateway {
    private val dataClient = Wearable.getDataClient(context)

    override suspend fun put(path: String, payload: ByteArray) {
        val request = PutDataRequest.create(path)
            .setData(payload)
            .setUrgent()
        dataClient.putDataItem(request).awaitResult()
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener(continuation::resume)
    addOnFailureListener(continuation::resumeWithException)
    addOnCanceledListener(continuation::cancel)
}
