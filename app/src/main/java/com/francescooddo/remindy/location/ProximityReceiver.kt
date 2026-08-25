package com.francescooddo.remindy.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import com.francescooddo.remindy.Graph
import com.francescooddo.remindy.domain.PlaceTrigger
import com.francescooddo.remindy.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProximityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val regionId = intent.getStringExtra(ProximityStore.EXTRA_REGION_ID) ?: return
        val entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)
        android.util.Log.d("ProximityReceiver", "fired region=$regionId entering=$entering")

        val database = Graph.databaseOrNull() ?: run {
            Graph.init(context.applicationContext)
            Graph.databaseOrNull()
        } ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = database.reminderDao().byRegion(regionId) ?: return@launch
                if (task.isArchived) return@launch
                val shouldNotify = when (task.placeTrigger) {
                    PlaceTrigger.ON_ENTRY -> entering
                    PlaceTrigger.ON_EXIT -> !entering
                }
                if (!shouldNotify) return@launch
                Notifications.postPlaceAlarm(
                    context = context,
                    title = task.title,
                    note = task.note,
                    placeName = task.placeName,
                    arrived = entering
                )
            } finally {
                pending.finish()
            }
        }
    }
}
