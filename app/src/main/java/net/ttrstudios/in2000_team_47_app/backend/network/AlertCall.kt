package net.ttrstudios.in2000_team_47_app.backend.network

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitString

class AlertCall {
    // Collects the Alert-response
    suspend fun getAlertsFromMetAlerts(url: String): String? {
        try {
            return Fuel.get(url).awaitString()
        } catch (exception: FuelError) {
            println("A network request exception was thrown: ${exception.message}")
        }
        return null
    }
}

