package net.ttrstudios.in2000_team_47_app.backend.network

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitString

class MetAlerts {
    private val path = "https://in2000-apiproxy.ifi.uio.no/weatherapi/metalerts/1.1?"

    //Collects MetAlerts-response for given coordinates
    suspend fun getMetAlertsForCoordinates(latitude: Double, longitude: Double): String? {
        try {
            //Return path for coordinates
            return Fuel.get("${path}lat=${latitude}&lon=$longitude").awaitString()
        } catch (exception: FuelError) {
            println("A network request exception was thrown: ${exception.message}")
        }
        return null
    }
}