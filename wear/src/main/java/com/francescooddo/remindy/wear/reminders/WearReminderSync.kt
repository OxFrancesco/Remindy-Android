package com.francescooddo.remindy.wear.reminders

import android.content.Context
import android.util.Log
import com.francescooddo.remindy.wear.protocol.ReminderSnapshot
import com.francescooddo.remindy.wear.surfaces.ReminderSurfaceUpdater
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Wearable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

internal object WearReminderSync {
    fun refresh(context: Context, scope: CoroutineScope) {
        val appContext = context.applicationContext
        scope.launch {
            val items = runCatching {
                Wearable.getDataClient(appContext).dataItems.awaitResult()
            }.getOrElse { failure ->
                Log.w(LOG_TAG, "reminder_snapshot_refresh_failed", failure)
                return@launch
            }
            try {
                items.forEach { item ->
                    val payload = item.data ?: return@forEach
                    if (item.uri.path == ReminderSnapshot.PATH) {
                        if (WearReminderGraph.repository.accept(item.uri.path.orEmpty(), payload)) {
                            ReminderSurfaceUpdater.request(appContext)
                        }
                    }
                }
            } finally {
                items.release()
            }
        }
    }

    private const val LOG_TAG = "RemindyWearSync"
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener(continuation::resume)
    addOnFailureListener(continuation::resumeWithException)
    addOnCanceledListener(continuation::cancel)
}
