package com.francescooddo.remindy

import android.app.Application
import com.francescooddo.remindy.notifications.Notifications
import com.francescooddo.remindy.wear.PhoneReminderSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RemindyApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Notifications.ensureChannels(this)
        PhoneReminderSync.start(
            context = this,
            reminderDao = Graph.db.reminderDao(),
            scope = applicationScope,
        )
    }
}
