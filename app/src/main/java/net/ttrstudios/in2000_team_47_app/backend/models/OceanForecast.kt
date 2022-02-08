package net.ttrstudios.in2000_team_47_app.backend.models

data class OceanForecast(
        val seaSurfaceWaveHeight: Double?,
        val seaSurfaceWaveFromDirection: Double?,
        val seaWaterSpeed: Double?,
        val seaWaterToDirection: Double?,
        val seaWaterTemperature: Double?,
        val time: String?
)
