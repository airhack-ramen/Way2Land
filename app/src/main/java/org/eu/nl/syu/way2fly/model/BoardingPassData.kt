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
    val phoneNumber: String? = null
)
