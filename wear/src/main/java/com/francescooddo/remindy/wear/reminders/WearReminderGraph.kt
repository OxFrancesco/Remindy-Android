package com.francescooddo.remindy.wear.reminders

import android.content.Context

internal object WearReminderGraph {
    @Volatile
    private var repositoryInstance: WearReminderRepository? = null

    fun init(context: Context) {
        if (repositoryInstance == null) {
            synchronized(this) {
                if (repositoryInstance == null) {
                    repositoryInstance = WearReminderRepository(
                        SharedPreferencesReminderSnapshotStorage(context.applicationContext),
                    )
                }
            }
        }
    }

    val repository: WearReminderRepository
        get() = requireNotNull(repositoryInstance) { "WearReminderGraph is not initialized" }
}
