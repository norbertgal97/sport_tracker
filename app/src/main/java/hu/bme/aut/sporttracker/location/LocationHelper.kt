package hu.bme.aut.sporttracker.location

import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

class LocationHelper(private val context: Context, private val callback: LocationCallback) {

    fun startLocationMonitoring() {
        val request = LocationRequest().apply {
            interval = 5000L
            fastestInterval = 1000L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        LocationServices.getFusedLocationProviderClient(context)
            .requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    fun stopLocationMonitoring() {
        LocationServices.getFusedLocationProviderClient(context)
            .removeLocationUpdates(callback)
    }
}