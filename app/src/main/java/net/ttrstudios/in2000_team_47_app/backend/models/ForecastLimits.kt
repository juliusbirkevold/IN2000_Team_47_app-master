package net.ttrstudios.in2000_team_47_app.backend.models

data class ForecastLimits(
    val precipitationLow: Int, val precipitationHigh: Int,
    val waveHeightLow: Int, val waveHeightHigh: Int,
    val windSpeedLow: Int, val windSpeedHigh: Int,
    val temperatureLow: Int, val temperatureHigh: Int
)
