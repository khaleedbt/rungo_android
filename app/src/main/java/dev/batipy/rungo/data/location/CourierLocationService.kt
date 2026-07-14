package dev.batipy.rungo.data.location

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dev.batipy.rungo.R
import dev.batipy.rungo.RunGoApplication
import dev.batipy.rungo.data.notifications.TRACKING_NOTIFICATION_CHANNEL_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "CourierLocationService"
private const val EXTRA_ORDER_ID = "order_id"
private const val NOTIFICATION_ID = 8420
private const val UPDATE_INTERVAL_MS = 12_000L

/**
 * Foreground service, one instance per app process, started/stopped by
 * CourierOrderDetailViewModel while the courier's own order is
 * in_progress/in_delivery. Posts a GPS ping to the order every ~12s via
 * OrdersRepository.postCourierLocation — the backend persists it (for
 * tracked_distance_km) and pushes it live to whoever's watching the order's
 * map (see OrderLocationConsumer).
 *
 * Runs as a foreground service (not a background location request) so it
 * keeps working while the courier's screen shows something else entirely —
 * that's the whole point, the client needs updates regardless of what the
 * courier is looking at.
 */
class CourierLocationService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var callback: LocationCallback? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var orderId: Int = -1

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_ORDER_ID, -1) ?: -1
        if (id == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Must check this BEFORE calling startForeground() — this service
        // declares foregroundServiceType="location" in the manifest, and on
        // Android 14+ calling startForeground() for a location-typed service
        // without the permission already granted throws a SecurityException
        // that crashes the whole app, not just this service.
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            // Best-effort — the courier just hasn't granted location yet.
            // Nothing to crash over, the client simply won't see a live
            // position until it's granted and this service restarts.
            Log.w(TAG, "ACCESS_FINE_LOCATION not granted, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        orderId = id
        startForeground(NOTIFICATION_ID, buildNotification(id))
        startLocationUpdates()
        return START_REDELIVER_INTENT
    }

    private fun startLocationUpdates() {
        stopLocationUpdates()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS).build()
        val currentOrderId = orderId
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val app = application as RunGoApplication
                scope.launch {
                    app.ordersRepository.postCourierLocation(
                        currentOrderId, location.latitude, location.longitude, location.accuracy
                    )
                }
            }
        }
        callback = cb
        try {
            fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.w(TAG, "requestLocationUpdates denied", e)
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        callback = null
    }

    private fun buildNotification(orderId: Int): Notification =
        NotificationCompat.Builder(this, TRACKING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.tracking_notification_title, orderId))
            .setContentText(getString(R.string.tracking_notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /** Idempotent — safe to call repeatedly for the same order (e.g. on
         * every screen refresh while it's in_progress/in_delivery). */
        fun start(context: Context, orderId: Int) {
            val intent = Intent(context, CourierLocationService::class.java)
                .putExtra(EXTRA_ORDER_ID, orderId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CourierLocationService::class.java))
        }
    }
}
