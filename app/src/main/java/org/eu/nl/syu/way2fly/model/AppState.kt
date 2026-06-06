package org.eu.nl.syu.way2fly.model

enum class UserRole {
    PASSENGER, STAFF
}

data class HelpRequest(
    val id: String,
    val urgency: Int, // 1-10
    val type: String,
    val location: String,
    val timestamp: Long,
    val details: String
)

data class GuidanceStep(
    val title: String,
    val description: String,
    val isMandatory: Boolean = true,
    val isCompleted: Boolean = false
)
