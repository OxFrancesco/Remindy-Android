package com.francescooddo.remindy.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.francescooddo.remindy.MainActivity
import com.francescooddo.remindy.R

object Notifications {

    const val CHANNEL_ALARMS = "place_alarms"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarms = NotificationChannel(
            CHANNEL_ALARMS,
            "Place reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when you arrive at or leave a linked place"
            enableVibration(true)
        }
        manager.createNotificationChannel(alarms)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun postPlaceAlarm(
        context: Context,
        title: String,
        note: String,
        placeName: String,
        arrived: Boolean
    ) {
        if (!canPost(context)) return
        val body = buildString {
            append(if (arrived) "Arriving at " else "Leaving ")
            append(placeName)
            append(".")
            if (note.isNotBlank()) {
                append(" ")
                append(note)
            }
        }
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_stat_checkmark)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() and 0xFFFF).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }
}
