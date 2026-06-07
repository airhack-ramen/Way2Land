package org.eu.nl.syu.way2fly.model

data class BoardingPassData(
    val passengerName: String,
    val pnr: String,
    val from: String,
    val to: String,
    val carrier: String,
    val flightNumber: String,
    val date: String, // Julian date (day of year)
    val seat: String,
    val phoneNumber: String? = null,
    val notifications: List<InboxMessage> = emptyList(),
    val guidanceSteps: List<GuidanceStep> = defaultSteps(),
    val threatScore: Int = (0..100).random(),
    val groupId: String? = null,
    val companionCount: Int = 0
)

fun defaultSteps() = listOf(
    GuidanceStep("Check-in", "Head to the check-in counters (Blue zone) to drop off luggage."),
    GuidanceStep("Security Control", "Prepare your liquids and electronics for screening (Orange zone)."),
    GuidanceStep("Duty Free", "Enjoy some shopping before your flight (Green zone).", isMandatory = false),
    GuidanceStep("Boarding", "Proceed to your designated gate B1-B10 for boarding.")
)

data class InboxMessage(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val actionLabel: String? = null,
    val stepToComplete: Int? = null
)
