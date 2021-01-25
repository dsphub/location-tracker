package com.dsp.androidsample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dsp.androidsample.log.Logger.d

open class SimpleNotification(val id: Int, val builder: NotificationCompat.Builder) {
    fun build(): Notification = builder.build()
}

class CustomNotificationManager(private val appContext: Context) {
    companion object {
        const val channelId = "test_channel"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel1",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Test Description"
            }
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun newNotification(id: Int, builder: NotificationCompat.Builder) =
        SimpleNotification(id, builder)

    fun show(notification: SimpleNotification) {
        show(notification.id, notification.builder)
    }

    fun show(id: Int, builder: NotificationCompat.Builder) {
        d { "TID=${Thread.currentThread().name} show notification id=$id" }
        with(NotificationManagerCompat.from(appContext)) {
            newNotification(id, builder).let {
                notify(id, it.build())
            }
        }
    }

    fun hide(notification: SimpleNotification) {
        hide(notification.id)
    }

    fun hide(id: Int) {
        with(NotificationManagerCompat.from(appContext)) {
            cancel(id)
        }
    }
}