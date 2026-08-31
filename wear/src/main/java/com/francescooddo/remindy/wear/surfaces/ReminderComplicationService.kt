package com.francescooddo.remindy.wear.surfaces

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.francescooddo.remindy.wear.MainActivity
import com.francescooddo.remindy.wear.R
import com.francescooddo.remindy.wear.reminders.ReminderSurfaceContent
import com.francescooddo.remindy.wear.reminders.WearReminderGraph

class ReminderComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return NoDataComplicationData()
        val content = ReminderSurfaceContent.from(WearReminderGraph.repository.reminders.value)
        return complicationData(content.complicationText, openAppIntent())
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        if (type == ComplicationType.SHORT_TEXT) complicationData("3", null) else NoDataComplicationData()

    private fun complicationData(text: String, tapAction: PendingIntent?): ComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("$text active reminders").build(),
        )
            .setTitle(PlainComplicationText.Builder("Remindy").build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_launcher_foreground),
                ).build(),
            )
            .setTapAction(tapAction)
            .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
