package dev.batipy.rungo.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dev.batipy.rungo.R

const val ORDER_NOTIFICATION_CHANNEL_ID = "orders"

fun createOrderNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        ORDER_NOTIFICATION_CHANNEL_ID,
        context.getString(R.string.notification_channel_orders),
        NotificationManager.IMPORTANCE_HIGH
    )
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
}
