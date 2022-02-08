package net.ttrstudios.in2000_team_47_app.backend.network

import android.location.Location
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*

const val DEFAULT_UPDATE_INTERVAL_SINGLE: Long = 1
const val FAST_UPDATE_INTERVAL_SINGLE: Long = 1
const val DEFAULT_UPDATE_INTERVAL_CONTINUOUS: Long = 2
const val FAST_UPDATE_INTERVAL_CONTINUOUS: Long = 2

class GPSservice(val context: Context) {
    // Google's API for location services. The majority of the app functions using this class.
    private var fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    val locationLiveData = MutableLiveData<Location>()

    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    fun singleLocationUpdate() {
        // Set all properties of LocationRequest
        locationRequest = LocationRequest.create().apply {
            // How often should the location check occur?
            interval = 1000 * DEFAULT_UPDATE_INTERVAL_SINGLE

            // How often CAN the location check occur. To prevent more frequent updates than the app can handle.
            fastestInterval = 1000 * FAST_UPDATE_INTERVAL_SINGLE

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Event that is triggered whenever the update interval is met.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                val location: Location = locationResult.lastLocation
                locationLiveData.postValue(location)

                //Since this is a single update, stop updates when the first update comes back.
                fusedLocationProviderClient.removeLocationUpdates(this)
            }
        }
        startLocationUpdates()
    }

    fun continuousLocationUpdates() {
        // Set all properties of LocationRequest
        locationRequest = LocationRequest.create().apply {
            // How often should the location check occur?
            interval = 1000 * DEFAULT_UPDATE_INTERVAL_CONTINUOUS

            // How often CAN the location check occur. To prevent more frequent updates than the app can handle.
            fastestInterval = 1000 * FAST_UPDATE_INTERVAL_CONTINUOUS

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Event that is triggered whenever the update interval is met.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                val location: Location = locationResult.lastLocation
                locationLiveData.postValue(location)
            }
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        if (!this::locationCallback.isInitialized) return
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    fun resumeLocationUpdates() {
        startLocationUpdates()
    }

}
