package com.dsp.ping.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dsp.ping.R
import com.dsp.ping.service.CloseActionReceiver
import com.dsp.ping.service.PingService
import com.dsp.ping.ui.MainActivity

/**
 * Канал и нотификация foreground-сервиса пинга.
 */
class PingNotificationManager(private val context: Context) {

    private val notificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.ping_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun build(host: String, availabilityPercent: Int?): Notification {
        val text = if (availabilityPercent != null) {
            context.getString(R.string.ping_availability_format, availabilityPercent)
        } else {
            context.getString(R.string.ping_availability_nd)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_globe_gray)
            .setContentTitle(host)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .addAction(0, context.getString(R.string.ping_action_close), closeIntent())
            .build()
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .putExtra(PingService.EXTRA_FROM_NOTIFICATION, true),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun closeIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context.getString(R.string.ping_action_close_intent))
            .setClass(context, CloseActionReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        const val CHANNEL_ID = "ping_status_channel"
    }
}
