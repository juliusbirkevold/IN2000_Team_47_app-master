package net.ttrstudios.in2000_team_47_app.frontend.MainActivity

import android.app.Application
import android.location.Address
import android.location.Location
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.*
import net.ttrstudios.in2000_team_47_app.backend.models.*
import net.ttrstudios.in2000_team_47_app.backend.network.*
import net.ttrstudios.in2000_team_47_app.backend.utils.EmergencyNbrService
import net.ttrstudios.in2000_team_47_app.backend.utils.PointListService
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val locationLiveData = MutableLiveData<Location>()
    val forecastLiveData = MutableLiveData<MutableList<MutableList<WeatherForecast>>>()
    val limitsLiveData = MutableLiveData<ForecastLimits>()
    val addressLiveData = MutableLiveData<Address>()

    private val observer = Observer<Location> { location ->
        locationLiveData.postValue(location)
    }
    private lateinit var gps: GPSservice
    var startup = true
    var gpsUpdatesOn = false
    var gpsUpdatesPaused = false

    // Uses Google Play Services Location to get the users current location.
    fun singleLocationUpdate() {

        val context = getApplication<Application>().applicationContext
        val singleUpdateGps = GPSservice(context)
        singleUpdateGps.singleLocationUpdate()
        singleUpdateGps.locationLiveData.observeForever(observer)
    }

    // Uses Google Play Services Location to get continuous updates of the users location.
    fun startContinuousLocationUpdates() {
        gpsUpdatesOn = true
        val context = getApplication<Application>().applicationContext
        gps = GPSservice(context)
        viewModelScope.launch(Dispatchers.IO) {
            gps.continuousLocationUpdates()
        }
        gps.locationLiveData.observeForever(observer)
    }

    fun stopContinuousLocationUpdates() {
        if (!this::gps.isInitialized) return
        gps.stopLocationUpdates()
        gps.locationLiveData.removeObserver(observer)
    }

    fun resumeContinuousLocationUpdates() {
        if (!this::gps.isInitialized) return
        gps.resumeLocationUpdates()
        gps.locationLiveData.observeForever(observer)
    }

    // Fetches forecast+alert data from APIs for a new place, creates WeatherForecast objects from
    // the data, and adds the new WeatherForecast objects to the forecastLiveData.
    fun getForecastAndAlertsForNewPlace(place: Poi, isGpsLocation: Boolean) {
        val context = getApplication<Application>().applicationContext
        val apiCallService = APIcallService(context)
        val metAlerts = MetAlerts()
        val alertCall = AlertCall()
        val allForecasts = forecastLiveData.value ?: return
        val forecastsForPlaceAllTimes = mutableListOf<WeatherForecast>()
        viewModelScope.launch(Dispatchers.IO) {
            // Get API data
            val futureWeather = apiCallService
                .getWeatherForecastsForCoordinates(place.latitude, place.longitude)

            val oceanForecasts = apiCallService
                .getOceanForecastsForCoordinates(place.latitude, place.longitude)

            val responseMetAlerts = metAlerts
                .getMetAlertsForCoordinates(place.latitude, place.longitude)
                ?: return@launch
            val inputStreamMetAlerts: InputStream =
                responseMetAlerts.byteInputStream() // read xml byte for byte
            val alertOverviews = MetAlertsParser().parse(inputStreamMetAlerts)

            val alerts = mutableListOf<AlertInfo>()
            for (alertOverview in alertOverviews) {
                val responseAlerts =
                    alertOverview.link?.let { alertCall.getAlertsFromMetAlerts(it) }
                responseAlerts?.let {
                    val inputStreamAlerts: InputStream =
                        responseAlerts.byteInputStream() // read xml byte for byte
                    val alert = AlertsParser().parse(inputStreamAlerts).info
                    alert?.let { alerts.add(alert) }
                }
            }
            // Extract the information we want to use from the alerts
            val processedAlerts = processAlerts(alerts)

            // Build WeatherForecast objects for each time period
            for (i in 0 until futureWeather.size) {
                var oceanForecast: OceanForecast? = null
                try {
                    oceanForecast = oceanForecasts[i]
                } catch (exception: IndexOutOfBoundsException) {
                }
                val forecast =
                    buildForecast(place, futureWeather[i], oceanForecast, processedAlerts)
                forecastsForPlaceAllTimes.add(forecast)
            }
            // If forecastLiveData has no forecasts, set up forecastLiveData with the forecasts
            // for the new place. Otherwise add the new forecast for each time period to the
            // correct time period sublist in forecastLiveData.
            if (allForecasts.isEmpty()) {
                for (forecastForTime in forecastsForPlaceAllTimes) allForecasts
                    .add(mutableListOf(forecastForTime))
            } else {
                var j = forecastsForPlaceAllTimes.lastIndex
                // If the new forecasts are for gps location, it is added first for each time
                // period. If a time period has expired in the APIs, then a empty dummy forecast
                // is used for that time period.
                for (forecastsForTime in allForecasts.reversed()) {
                    if (j >= 0) {
                        if (isGpsLocation) forecastsForTime.add(0, forecastsForPlaceAllTimes[j])
                        else forecastsForTime.add(forecastsForPlaceAllTimes[j])
                    } else {
                        val dummyForecast = WeatherForecast(
                            place.name, null, null,
                            null, null, null, processedAlerts,
                            place.markerColor, LatLng(place.latitude, place.longitude),
                            "Regular", mutableListOf()
                        )

                        if (isGpsLocation) forecastsForTime.add(0, dummyForecast)
                        else forecastsForTime.add(dummyForecast)
                    }
                    j--
                }
            }
            forecastLiveData.postValue(allForecasts)
        }
    }

    // Fetches forecast+alert data from APIs for the saved places, creates WeatherForecast objects
    // from the data, and adds the new WeatherForecast objects to the forecastLiveData.
    fun getForecastAndAlertsForSavedPlaces(places: MutableList<Poi>) {
        val context = getApplication<Application>().applicationContext
        val apiCallService = APIcallService(context)
        val metAlerts = MetAlerts()
        val alertCall = AlertCall()
        val allForecasts = mutableListOf<MutableList<WeatherForecast>>()
        viewModelScope.launch(Dispatchers.IO) {
            places.forEachIndexed { index, place ->
                // Get API data for place
                val futureWeather = apiCallService
                    .getWeatherForecastsForCoordinates(place.latitude, place.longitude)

                val oceanForecasts = apiCallService
                    .getOceanForecastsForCoordinates(place.latitude, place.longitude)

                val responseMetAlerts = metAlerts
                    .getMetAlertsForCoordinates(place.latitude, place.longitude)
                    ?: return@forEachIndexed
                val inputStreamMetAlerts: InputStream =
                    responseMetAlerts.byteInputStream() // read xml byte for byte
                val alertOverviews = MetAlertsParser().parse(inputStreamMetAlerts)

                val alerts = mutableListOf<AlertInfo>()
                for (alertOverview in alertOverviews) {
                    val responseAlerts =
                        alertOverview.link?.let { alertCall.getAlertsFromMetAlerts(it) }
                    responseAlerts?.let {
                        val inputStreamAlerts: InputStream =
                            responseAlerts.byteInputStream() // read xml byte for byte
                        val alert = AlertsParser().parse(inputStreamAlerts).info
                        alert?.let { alerts.add(alert) }
                    }
                }
                // Extract the information we want to use from the alerts
                val processedAlerts = processAlerts(alerts)

                // Build WeatherForecast objects for each time period and add them to the correct
                // time period sublist in forecastLiveData.
                for (i in 0 until futureWeather.size) {
                    var oceanForecast: OceanForecast? = null
                    try {
                        oceanForecast = oceanForecasts[i]
                    } catch (exception: IndexOutOfBoundsException) {
                    }
                    val forecast =
                        buildForecast(place, futureWeather[i], oceanForecast, processedAlerts)
                    if (index == 0) allForecasts.add(mutableListOf(forecast))
                    else allForecasts[i].add(forecast)
                }
            }
            forecastLiveData.postValue(allForecasts)
        }
    }

    // Build a WeatherForecast object from weather data obtained from the APIs
    private fun buildForecast(
        place: Poi, weather: Weather, oceanForecast: OceanForecast?,
        processedAlerts: MutableList<ProcessedAlert>
    ): WeatherForecast {
        val precipitation: Double? = weather.weatherValues.precipitation_rate?.toDouble()
        val windSpeed: Int? = weather.weatherValues.wind_speed?.toDouble()?.roundToInt()
        val temperature: Int? = weather.weatherValues.air_temperature?.toDouble()?.roundToInt()
        val waveHeight = oceanForecast?.seaSurfaceWaveHeight
        val symbolCode: String? = weather.symbolCode

        val warnings = checkLimits(precipitation, waveHeight, windSpeed, temperature)

        val checkedAlerts = checkAlertsTime(processedAlerts, weather.startTime)

        var type = "Regular"
        when {
            //(alerts.isNotEmpty() and warnings.isNotEmpty()) -> type = "AlertsAndWarnings"
            (checkedAlerts.isNotEmpty()) -> type = "Alerts"
            (warnings.isNotEmpty()) -> type = "Warnings"
        }

        return WeatherForecast(
            place.name,
            waveHeight,
            windSpeed,
            temperature,
            precipitation,
            symbolCode,
            checkedAlerts,
            place.markerColor,
            LatLng(place.latitude, place.longitude),
            type,
            warnings
        )
    }

    // Check if the weather data from the APIs is outside the values set in preferences. If so
    // add warnings for the types of weather data outside the preferences.
    private fun checkLimits(
        precipitation: Double?, waveHeight: Double?, windSpeed: Int?,
        temperature: Int?
    ): MutableList<String> {
        val context = getApplication<Application>().applicationContext
        val pointListService = PointListService(context)

        val limits = pointListService.loadLimits()
        val warnings = mutableListOf<String>()
        limits?.let {
            precipitation?.let {
                if ((it < limits.precipitationLow) || (it > limits.precipitationHigh))
                    warnings.add("Precipitation")
            }
            waveHeight?.let {
                if ((it < limits.waveHeightLow) || (it > limits.waveHeightHigh))
                    warnings.add("Waves")
            }
            windSpeed?.let {
                if ((it < limits.windSpeedLow) || (it > limits.windSpeedHigh))
                    warnings.add("Wind")
            }
            temperature?.let {
                if ((it < limits.temperatureLow) || (it > limits.temperatureHigh))
                    warnings.add("Temperature")
            }
        }
        return warnings
    }

    // Check if the alert is active at the forecast time.
    private fun checkAlertsTime(
        alerts: MutableList<ProcessedAlert>,
        startTime: String?
    ): MutableList<ProcessedAlert> {
        val checkedAlerts = mutableListOf<ProcessedAlert>()
        for (alert in alerts) {
            if ((alert.onset == null) || (alert.expires == null) || (startTime == null)) {
                // If one of the times used to check relevancy is missing, we play it safe and add the alert.
                checkedAlerts.add(alert)
            } else {
                // If one of the date parsings fail we play it safe and add the alert.
                // Otherwise we only add the alert if the forecast time is within the alert duration.
                val onset = alert.onset.toDate("yyyy-MM-dd'T'HH:'00':'00+00:00'")
                val expires = alert.expires.toDate("yyyy-MM-dd'T'HH:'00':'00+00:00'")
                val time = startTime.toDate("yyyy-MM-dd'T'HH:'00':'00Z'")
                if ((onset == null) || (expires == null) || (time == null)) checkedAlerts.add(alert)
                else if ((time >= onset) && (time < expires)) checkedAlerts.add(alert)
            }
        }
        return checkedAlerts
    }

    // Extract the information we want to use from the alerts. Everything from the headline
    // except for the time period in UTC. The time period built from onset and expires, formatted
    // to use the system default timezone, and to be on a Norwegian format. The instruction
    // and description as is.
    private fun processAlerts(alerts: MutableList<AlertInfo>): MutableList<ProcessedAlert> {
        val processedAlerts = mutableListOf<ProcessedAlert>()
        for (alert in alerts) {
            val headlineParts = alert.headline?.split(", ")
            var headline: String? = null
            headlineParts?.let {
                headline = ""
                for (i in 0 until (headlineParts.size - 1)) {
                    headline += "${headlineParts[i]}, "
                }
                headline = headline!!.trim(',', ' ')
            }
            var fromTime = alert.onset ?: "n/a"
            if (fromTime != "n/a") {
                fromTime = fromTime.replace("T", " ").substring(0..18)
                fromTime = fromTime.toDate()?.formatTo("EE dd MMM HH:mm") ?: "n/a"
            }
            var toTime = alert.expires ?: "n/a"
            if (toTime != "n/a") {
                toTime = toTime.replace("T", " ").substring(0..18)
                toTime = toTime.toDate()?.formatTo("EE dd MMM HH:mm") ?: "n/a"
            }
            var timePeriod: String? = null
            if ((fromTime != "n/a") && (toTime != "n/a")) {
                timePeriod = "${fromTime.capitalize(Locale.getDefault())} - " +
                        toTime.capitalize(Locale.getDefault())
            }
            processedAlerts.add(
                ProcessedAlert(
                    headline, timePeriod, alert.instruction,
                    alert.description, alert.onset, alert.expires
                )
            )
        }
        return processedAlerts
    }

    private fun String.toDate(
        dateFormat: String = "yyyy-MM-dd HH:mm:ss",
        timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    ): Date? {
        val simpleDateFormat = SimpleDateFormat(dateFormat, Locale("no", "NO"))
        simpleDateFormat.timeZone = timeZone
        return simpleDateFormat.parse(this)
    }

    private fun Date.formatTo(
        dateFormat: String,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val simpleDateFormat = SimpleDateFormat(dateFormat, Locale("no", "NO"))
        simpleDateFormat.timeZone = timeZone
        return simpleDateFormat.format(this)
    }

    // Update the warnings in the forecasts when the preferred limits change.
    fun updateForecastWarnings() {
        val forecasts = forecastLiveData.value ?: return
        for (forecastsForTime in forecasts) {
            for (forecast in forecastsForTime) {
                forecast.warnings = checkLimits(
                    forecast.precipitation, forecast.waveHeight,
                    forecast.wind, forecast.temperature
                )
            }
        }
    }

    // GeoCode an address object from a location name
    fun getAddressFromLocationName(locationName: String) {
        val context = getApplication<Application>().applicationContext
        val geoCoding = GeoCoding(context)
        viewModelScope.launch(Dispatchers.IO) {
            val address = geoCoding.getAddressFromLocationName(locationName)
                ?: Address(Locale(""))
            // If GeoCoding doesn't return an address for the given location name, then
            // use an address object with address line "Fant ingen adresse"
            if (address.getAddressLine(0) == null) {
                address.setAddressLine(0, "Fant ingen adresse")
            }
            addressLiveData.postValue(address)
        }
    }

    // GeoCode an address object from latitude + longitude
    fun getAddressFromCoordinates(latitude: Double, longitude: Double) {
        val context = getApplication<Application>().applicationContext
        val geoCoding = GeoCoding(context)
        viewModelScope.launch(Dispatchers.IO) {
            val address = geoCoding.getAddressFromCoordinates(latitude, longitude)
                ?: Address(Locale(""))
            // If GeoCoding doesn't return an address for the given coordinates, then
            // use an address object with address line "Ingen adresse"
            if (address.getAddressLine(0) == null) {
                address.setAddressLine(0, "Ingen adresse")
            }
            addressLiveData.postValue(address)
        }
    }

    // Remove observer from GPSservice livedata when the viewmodel is destroyed
    // to avoid leaking memory.
    override fun onCleared() {
        if (this::gps.isInitialized) gps.locationLiveData.removeObserver(observer)
        super.onCleared()
    }

    // Set up timestamps used for selecting weather data from the APIs, and timestamps used
    // for the time selection in the overview. Both types of timestamps represent the same
    // times, but for the APIs they are in UTC and on the format used in the APIs, while
    // for the time selection they are in the system default timezone and on a presentable format.
    fun setTimeStampsForSeekBarAndApi() {
        val context = getApplication<Application>().applicationContext
        val pointListService = PointListService(context)

        val sdfNow = SimpleDateFormat("H", Locale("no", "NO"))
        sdfNow.timeZone = TimeZone.getTimeZone("UTC")
        // When current hour in UTC
        val nextTimeRange = when (sdfNow.format(Date().time).toInt()) {
            in 0..5 -> 6 //6..11
            in 6..11 -> 12 //12..17
            in 12..17 -> 18 //18..23
            in 18..23 -> 24 //00..05
            else -> {
                println("currentHour is not a valid hour number")
                -1
            }
        }

        val numberOfTimestamps =
            21 // Number of 6 hour periods to get forecast for + 1 for current time
        var numberOfTimestampsLeft = numberOfTimestamps

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(Calendar.MINUTE, 0)
        val sdfTimePicker = SimpleDateFormat("EEEE dd MMMM HH:'00'", Locale("no", "NO"))
        val sdfApiTimestamps = SimpleDateFormat("yyyy-MM-dd'T'HH:'00':'00Z'", Locale("no", "NO"))
        sdfApiTimestamps.timeZone = TimeZone.getTimeZone("UTC")

        // Set up the timestamp lists and add the "current" time (most recent whole hour)
        val timestamps = mutableListOf<String>()
        val timestampsAPI = mutableListOf<String>()
        var time = calendar.time
        timestamps.add(sdfTimePicker.format(time))
        timestampsAPI.add(sdfApiTimestamps.format(time))
        numberOfTimestampsLeft--

        // Move to the next time range and add the start of the period to the timestamp lists
        if (nextTimeRange == 24) {
            calendar.add(Calendar.DATE, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
        } else calendar.set(Calendar.HOUR_OF_DAY, nextTimeRange)
        time = calendar.time
        timestamps.add(sdfTimePicker.format(time))
        timestampsAPI.add(sdfApiTimestamps.format(time))
        numberOfTimestampsLeft--

        // Continuously add the start of 6 hour time periods until numberOfTimestampsLeft is zero.
        val sdfHour = SimpleDateFormat("HH:mm", Locale("no", "NO"))
        while (numberOfTimestampsLeft > 0) {
            calendar.set(Calendar.MINUTE, 0)
            calendar.add(Calendar.HOUR_OF_DAY, 6)
            time = calendar.time
            // Add the end time of the time period to the time selection timestamps.
            timestamps[timestamps.size - 1] =
                "${timestamps[timestamps.size - 1]} - ${sdfHour.format(time)}"
            timestamps.add(sdfTimePicker.format(time))
            timestampsAPI.add(sdfApiTimestamps.format(time))
            numberOfTimestampsLeft--
        }
        // Add the end time of the last time period to the last time selection timestamp.
        calendar.set(Calendar.MINUTE, 0)
        calendar.add(Calendar.HOUR_OF_DAY, 6)
        timestamps[timestamps.size - 1] =
            "${timestamps[timestamps.size - 1]} - ${sdfHour.format(calendar.time)}"

        pointListService.saveTimestampsForSeekBar(timestamps)
        pointListService.saveTimestampsForAPI(timestampsAPI)
    }

    fun addFirstLimits() {
        val poiService = PointListService(getApplication<Application>().applicationContext)
        poiService.saveLimits(ForecastLimits(0, 30, 0, 10, 0, 30, -20, 40))
    }

    fun saveLimits(limits: ForecastLimits) {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        pointListService.saveLimits(limits)
    }

    fun getLimits() {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        val result = pointListService.loadLimits()
        limitsLiveData.postValue(result!!)
    }

    fun saveData(list: MutableList<Poi>) {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        pointListService.saveData(list)
    }

    fun getData(): MutableList<Poi> {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        return pointListService.loadData()
    }

    fun saveGpsStatus(gpsStatus: Boolean) {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        pointListService.saveGpsStatus(gpsStatus)
    }

    fun getGpsStatus(): Boolean {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        return pointListService.loadGpsStatus()
    }

    fun saveCoordinates(coordinates: LatLng) {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        pointListService.saveCoordinates(coordinates)
    }

    fun getCoordinates(): LatLng? {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        return pointListService.loadCoordinates()
    }

    fun saveFirstStart(firstStart: Boolean) {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        pointListService.saveFirstStart(firstStart)
    }

    fun getFirstStart(): Boolean {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        return pointListService.loadFirstStart()
    }

    fun saveGpsMessageDismissed(gpsMessageDismissed: Boolean) {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        pointListService.saveGpsMessageDismissed(gpsMessageDismissed)
    }

    fun getGpsMessageDismissed(): Boolean {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        return pointListService.loadGpsMessageDismissed()
    }

    fun getTimeStampsForSeekBar(): List<String>? {
        val pointListService = PointListService(getApplication<Application>().applicationContext)
        return pointListService.loadTimeStampsForSeekBar()
    }

    fun getEmt(): List<String> {
        val emt = EmergencyNbrService(getApplication<Application>().applicationContext)
        return emt.getEmergencyNbrs()
    }
}