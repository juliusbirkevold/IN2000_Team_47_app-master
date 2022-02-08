package net.ttrstudios.in2000_team_47_app.backend.network

import android.content.Context
import android.location.Address
import android.location.Geocoder

class GeoCoding(context: Context) {
    private var address: Address? = null
    private val geocoder: Geocoder = Geocoder(context)

    fun getAddressFromLocationName(locationName: String): Address? {
        try {
            val addresses: List<Address> = geocoder.getFromLocationName(locationName, 1)
            address = addresses[0]
        } catch (e: Exception) {
            println(e)
            println("Unable to get street address")
        }
        return address
    }

    fun getAddressFromCoordinates(latitude: Double, longitude: Double): Address? {
        try {
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)
            address = addresses[0]
        } catch (e: Exception) {
            println(e)
            println("Unable to get street address")
        }
        return address
    }
}