package com.francescooddo.remindy.wear.reminders

import android.util.Log
import com.francescooddo.remindy.wear.protocol.ReminderSnapshot
import com.francescooddo.remindy.wear.surfaces.ReminderSurfaceUpdater
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

class WearReminderDataService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataEvents.forEach { event ->
            val item = event.dataItem
            val payload = item.data
            if (
                event.type == DataEvent.TYPE_CHANGED &&
                item.uri.path == ReminderSnapshot.PATH &&
                payload != null &&
                WearReminderGraph.repository.accept(item.uri.path.orEmpty(), payload)
            ) {
                ReminderSurfaceUpdater.request(this)
                Log.i(LOG_TAG, "reminder_snapshot_updated")
            }
        }
    }

    private companion object {
        const val LOG_TAG = "RemindyWearSync"
    }
}
