package dev.batipy.rungo.data.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.batipy.rungo.MainActivity
import dev.batipy.rungo.R
import dev.batipy.rungo.RunGoApplication
import kotlinx.coroutines.launch

class RunGoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = application as RunGoApplication
        // Best-effort: if the user isn't logged in yet, this fails silently and
        // the token gets registered right after login instead (see LoginViewModel).
        app.applicationScope.launch {
            app.notificationRepository.registerDeviceToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: getString(R.string.app_name)
        val body = message.notification?.body ?: return
        val orderId = message.data["order_id"]?.toIntOrNull()
        val isChatMessage = message.data["type"] == "chat"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (orderId != null) {
                // Chat pushes jump straight into that order's chat instead of
                // landing on the order detail screen and making the user tap
                // "Написать" themselves.
                if (isChatMessage) putExtra(MainActivity.EXTRA_CHAT_ORDER_ID, orderId)
                else putExtra(MainActivity.EXTRA_ORDER_ID, orderId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            orderId ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ORDER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(orderId ?: System.currentTimeMillis().toInt(), notification)
    }
}
