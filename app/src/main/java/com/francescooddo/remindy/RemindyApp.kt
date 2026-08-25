package com.francescooddo.remindy

import android.app.Application
import com.francescooddo.remindy.notifications.Notifications

class RemindyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Notifications.ensureChannels(this)
    }
}
