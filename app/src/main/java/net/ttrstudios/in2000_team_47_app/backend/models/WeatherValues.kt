package net.ttrstudios.in2000_team_47_app.backend.models

//Class for NowCast API call
//Class for deserialising the weather data
data class WeatherValues(
        val air_temperature: Number?,
        val precipitation_rate: Number?,
        val relative_humidity: Number?,
        val wind_from_direction: Number?,
        val wind_speed: Number?,
        val wind_speed_of_gust: Number?
)