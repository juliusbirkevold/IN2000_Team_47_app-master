package net.ttrstudios.in2000_team_47_app.backend.utils

import android.content.Context

//Class containing function returning the list of emergency numbers defined in strings.xml
class EmergencyNbrService(val context: Context) {

    //Returns list of strings
    fun getEmergencyNbrs(): List<String> {
        val fileString = context.assets.open("emergencyNumbers.txt")
            .bufferedReader().use { it.readText() }

        return fileString.split("\n")
    }
}