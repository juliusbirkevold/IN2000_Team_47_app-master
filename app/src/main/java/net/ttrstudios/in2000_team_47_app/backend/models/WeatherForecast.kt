package net.ttrstudios.in2000_team_47_app.backend.models

import com.mapbox.mapboxsdk.geometry.LatLng

data class WeatherForecast(
    var locationName: String, val waveHeight: Double?, val wind: Int?,
    val temperature: Int?, val precipitation: Double?, val symbolCode: String?,
    val alerts: MutableList<ProcessedAlert>, var markerColor: String,
    val coordinates: LatLng, var type: String,
    var warnings: MutableList<String>, var alertShownNumber: Int = 0
)
