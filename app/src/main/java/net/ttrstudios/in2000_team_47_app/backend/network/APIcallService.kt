package net.ttrstudios.in2000_team_47_app.backend.network

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitString
import net.ttrstudios.in2000_team_47_app.backend.models.OceanForecast
import net.ttrstudios.in2000_team_47_app.backend.models.WeatherValues
import net.ttrstudios.in2000_team_47_app.backend.utils.PointListService
import net.ttrstudios.in2000_team_47_app.backend.models.Weather
import org.json.JSONObject

class APIcallService(val context: Context) {

    // Given a set of coordinates, fetches data from Locationforecast for the coordinates. Gets
    // data for the timestamps saved in shared preferences.
    suspend fun getWeatherForecastsForCoordinates(
        latitude: Double,
        longitude: Double
    ): MutableList<Weather> {
        val weatherForecastsForRelevantTimes = mutableListOf<Weather>()
        try {
            val path =
                "https://in2000-apiproxy.ifi.uio.no/weatherapi/locationforecast/2.0/compact?lat=$latitude&lon=$longitude"
            val response = Fuel.get(path).awaitString()
            val properties = JSONObject(response).get("properties").toString()
            val forecasts = JSONObject(properties).getJSONArray("timeseries")

            for (i in 0 until forecasts.length()) {
                val time = getTimeForWeatherForecast(forecasts[i].toString())
                if (checkIfRelevantTimeAndDate(time)) {
                    val firstTimestamp = checkIfFirstTimeAndDate(time)
                    weatherForecastsForRelevantTimes
                        .add(
                            getWeatherForecastFromJSON(
                                forecasts[i].toString(), time,
                                firstTimestamp
                            )
                        )
                }
            }
        } catch (exception: Exception) {
            println(exception.message)
        }
        return weatherForecastsForRelevantTimes
    }

    // Given a set of coordinates, fetches data from Oceanforecast for the coordinates. Gets
    // data for the timestamps saved in shared preferences.
    suspend fun getOceanForecastsForCoordinates(
        latitude: Double,
        longitude: Double
    ): MutableList<OceanForecast> {
        val oceanForecastsForRelevantTimes = mutableListOf<OceanForecast>()
        try {
            val path =
                "https://in2000-apiproxy.ifi.uio.no/weatherapi/oceanforecast/2.0/complete?lat=$latitude&lon=$longitude"
            val response = Fuel.get(path).awaitString()
            val properties = JSONObject(response).get("properties").toString()
            val forecasts = JSONObject(properties).getJSONArray("timeseries")

            for (i in 0 until forecasts.length()) {
                val time = getTimeForOceanForecast(forecasts[i].toString())
                if (checkIfRelevantTimeAndDate(time)) {
                    oceanForecastsForRelevantTimes
                        .add(getOceanForecastFromJSON(forecasts[i].toString()))
                }
            }
        } catch (exception: Exception) {
            println(exception.message)
        }
        return oceanForecastsForRelevantTimes
    }

    // Fetches the timestamp for a forecast in Locationforecast.
    private fun getTimeForWeatherForecast(forecastJsonString: String): String {
        val forecastJSON = JSONObject(forecastJsonString)
        return forecastJSON.get("time").toString()
    }

    // Selects the relevant data for a forecast from Locationforecast, and creates a Weather object
    // from the selected data.
    private fun getWeatherForecastFromJSON(
        forecastJsonString: String, startTime: String,
        firstTimestamp: Boolean
    ): Weather {
        val forecastJSON = JSONObject(forecastJsonString)
        val data = JSONObject(forecastJSON.get("data").toString())
        val instant = JSONObject(data.get("instant").toString())
        val details = JSONObject(instant.get("details").toString())

        var precipitationRate: Double? = null
        var symbolCode: String? = null
        if (firstTimestamp) {
            try {
                val next1Hours = data.getJSONObject("next_1_hours")
                val details1Hours = next1Hours.getJSONObject("details")
                val summary1Hours = next1Hours.getJSONObject("summary")
                precipitationRate = details1Hours.getDouble("precipitation_amount")
                symbolCode = summary1Hours.getString("symbol_code")
            } catch (exception: Exception) {
                println("No precipitation rate available")
            }
        } else {
            try {
                val next6Hours = data.getJSONObject("next_6_hours")
                val details6Hours = next6Hours.getJSONObject("details")
                val summary6Hours = next6Hours.getJSONObject("summary")
                precipitationRate = details6Hours.getDouble("precipitation_amount")
                symbolCode = summary6Hours.getString("symbol_code")
            } catch (exception: Exception) {
                println("No precipitation rate available")
            }
        }

        var windSpeed: Double? = null
        try {
            windSpeed = details.getDouble("wind_speed")
        } catch (exception: Exception) { /*println("No wind speed available")*/
        }

        var temperature: Double? = null
        try {
            temperature = details.getDouble("air_temperature")
        } catch (exception: Exception) { /*println("No temperature available")*/
        }

        return Weather(
            WeatherValues(
                temperature, precipitationRate, null,
                null, windSpeed, null
            ), symbolCode, startTime
        )
    }

    // Fetches the timestamp for a forecast in Oceanforecast.
    private fun getTimeForOceanForecast(forecastJsonString: String): String {
        val forecastJSON = JSONObject(forecastJsonString)
        return forecastJSON.get("time").toString()
    }

    // Selects the relevant data for a forecast from Oceanforecast, and creates a OceanForecast
    // object from the selected data.
    private fun getOceanForecastFromJSON(forecastJsonString: String): OceanForecast {
        val forecastJSON = JSONObject(forecastJsonString)
        val data = JSONObject(forecastJSON.get("data").toString())
        val instant = JSONObject(data.get("instant").toString())
        val details = JSONObject(instant.get("details").toString())

        var seaSurfaceWaveHeight: Double? = null
        try {
            seaSurfaceWaveHeight = details.getDouble("sea_surface_wave_height")
        } catch (exception: Exception) {
            println("No wave height available")
        }

        return OceanForecast(
            seaSurfaceWaveHeight, null,
            null, null, null, null
        )
    }

    // Checks the timestamp for Locationforecast/Oceanforecast data against the timestamps saved
    // in shared preferences.
    private fun checkIfRelevantTimeAndDate(time: String): Boolean {
        val pointListService = PointListService(context)
        val relevantTimestamps = pointListService.loadTimeStampsAPI() ?: return false
        when (time) {
            in relevantTimestamps -> return true
        }
        return false
    }

    // Checks if the timestamp for Locationforecast/Oceanforecast data matches the first saved
    // timestamp in shared preferences.
    private fun checkIfFirstTimeAndDate(time: String): Boolean {
        val pointListService = PointListService(context)
        val relevantTimestamps = pointListService.loadTimeStampsAPI() ?: return false
        return time == relevantTimestamps[0]
    }
}
