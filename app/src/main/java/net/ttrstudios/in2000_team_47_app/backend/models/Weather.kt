package net.ttrstudios.in2000_team_47_app.backend.models

data class Weather(
    val weatherValues: WeatherValues,
    val symbolCode: String?,
    val startTime: String?
)