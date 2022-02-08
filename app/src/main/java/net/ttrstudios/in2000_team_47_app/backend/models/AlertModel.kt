package net.ttrstudios.in2000_team_47_app.backend.models

//Dataclass to alert
data class AlertModel(
    val id: String?, val sender: String?, val sent: String?, val status: String?,
    val msgType: String?, val scope: String?, var info: AlertInfo?
)