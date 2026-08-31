package com.francescooddo.remindy.wear.surfaces

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.layout.column
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.tile
import com.francescooddo.remindy.wear.MainActivity
import com.francescooddo.remindy.wear.reminders.ReminderSurfaceContent
import com.francescooddo.remindy.wear.reminders.WearReminderGraph

class RemindyTileService : Material3TileService() {
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val content = ReminderSurfaceContent.from(WearReminderGraph.repository.reminders.value)
        return tile(Timeline.fromLayoutElement(reminderLayout(content)))
    }

    private fun MaterialScope.reminderLayout(content: ReminderSurfaceContent): LayoutElement {
        val rows = if (content.tileTitles.isEmpty()) listOf("No reminders") else content.tileTitles
        val rowElements = buildList {
            rows.forEachIndexed { index, title ->
                if (index > 0) add(Spacer.Builder().setHeight(dp(9f)).build())
                add(text(title.layoutString))
            }
        }
        val openApp = protoLayoutScope.clickable(
            pendingIntent = PendingIntent.getActivity(
                this@RemindyTileService,
                0,
                Intent(this@RemindyTileService, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
            id = "open-remindy",
            fallbackAction = launchAction(ComponentName(this@RemindyTileService, MainActivity::class.java)),
        )
        return primaryLayout(
            titleSlot = { text("Remindy".layoutString) },
            mainSlot = {
                column(
                    *rowElements.toTypedArray(),
                    width = expand(),
                    height = expand(),
                )
            },
            onClick = openApp,
        )
    }
}
