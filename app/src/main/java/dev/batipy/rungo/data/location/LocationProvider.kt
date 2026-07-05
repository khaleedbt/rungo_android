package dev.batipy.rungo.data.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class LocationProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Caller must have already obtained ACCESS_FINE_LOCATION before calling this. */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        val location = client.getCurrentLocation(request, null).await() ?: return null
        return location.latitude to location.longitude
    }
}
