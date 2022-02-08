package net.ttrstudios.in2000_team_47_app.backend.utils

import kotlin.math.abs

// Convert coordinates from decimal degrees format to degrees, minutes, seconds format.
fun convertDDcoordsToDMS(latitude: Double, longitude: Double): List<String> {

    val northsouth = if (latitude >= 0) "N" else "S"
    val eastwest = if (longitude >= 0) "E" else "W"

    val absLat = abs(latitude)
    val absLon = abs(longitude)

    val degreesLat: Int = absLat.toInt()
    val minutesLat: Int = (60 * (absLat - degreesLat)).toInt()
    val secondsLat: Double = 60 * ((60 * (absLat - degreesLat)) - minutesLat)

    val degreesLon: Int = absLon.toInt()
    val minutesLon: Int = (60 * (absLon - degreesLon)).toInt()
    val secondsLon: Double = 60 * ((60 * (absLon - degreesLon)) - minutesLon)

    return listOf(
        "$degreesLat°$minutesLat\'${String.format("%.1f", secondsLat)}\"$northsouth",
        "$degreesLon°$minutesLon\'${String.format("%.1f", secondsLon)}\"$eastwest"
    )
}