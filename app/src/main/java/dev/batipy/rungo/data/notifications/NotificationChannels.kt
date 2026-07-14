package dev.batipy.rungo.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dev.batipy.rungo.R

const val ORDER_NOTIFICATION_CHANNEL_ID = "orders"
const val TRACKING_NOTIFICATION_CHANNEL_ID = "tracking"

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

/** Low importance — this is just the required "tracking is active" status
 * notification for the courier's foreground location service, not something
 * that should alert or make noise. */
fun createTrackingNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        TRACKING_NOTIFICATION_CHANNEL_ID,
        context.getString(R.string.notification_channel_tracking),
        NotificationManager.IMPORTANCE_LOW
    )
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
}
