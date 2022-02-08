package net.ttrstudios.in2000_team_47_app.backend.models

//Dataclass for information connected to an alert
data class AlertInfo(
    val event: String?, val severity: String?, val urgency: String?,
    val certainty: String?, val effective: String?, val onset: String?,
    val expires: String?, val headline: String?, val description: String?,
    val instruction: String?
)