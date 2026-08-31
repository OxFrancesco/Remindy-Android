package com.francescooddo.remindy.wear.surfaces

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

internal object ReminderSurfaceUpdater {
    fun request(context: Context) {
        TileService.getUpdater(context).requestUpdate(RemindyTileService::class.java)
        ComplicationDataSourceUpdateRequester.create(
            context = context,
            complicationDataSourceComponent = ComponentName(context, ReminderComplicationService::class.java),
        ).requestUpdateAll()
    }
}
