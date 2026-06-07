package org.eu.nl.syu.way2fly.model

data class BoardingPassData(
    val passengerName: String,
    val pnr: String,
    val from: String,
    val to: String,
    val carrier: String,
    val flightNumber: String,
    val date: String,
    val seat: String,
    val phoneNumber: String? = null,
    val notifications: List<InboxMessage> = emptyList(),
    val guidanceSteps: List<GuidanceStep> = defaultSteps(),
    val activeGroups: List<FriendGroup> = emptyList(),
    val threatScore: Int = 0,
    val passengerToken: String? = null,
    val groupId: String? = null,
    val companionCount: Int = 0
)

fun defaultSteps() = listOf(
    GuidanceStep("Security Check", "Complete the main security screening", isCompleted = false),
    GuidanceStep("Check-in", "Confirm your attendance at the counter", isCompleted = false),
    GuidanceStep("Passport Control", "Verify your travel documents", isCompleted = false),
    GuidanceStep("Boarding", "Proceed to the assigned gate", isCompleted = false)
)

data class InboxMessage(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val actionLabel: String? = null,
    val stepToComplete: Int? = null
)

data class FriendGroup(
    val id: String,
    val name: String,
    val memberCount: Int,
    val distance: String
)
