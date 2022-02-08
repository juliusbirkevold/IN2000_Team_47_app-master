package net.ttrstudios.in2000_team_47_app.backend.models

data class ProcessedAlert(
    val headline: String?, val timePeriod: String?,
    val instruction: String?, val description: String?,
    val onset: String?, val expires: String?
)
