package com.francescooddo.remindy.wear

import android.app.Application
import com.francescooddo.remindy.wear.reminders.WearReminderGraph

class RemindyWearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WearReminderGraph.init(this)
    }
}
