package org.eu.nl.syu.way2fly.util

import org.eu.nl.syu.way2fly.model.BoardingPassData

object BCBPParser {
    /**
     * Parses IATA BCBP (Bar Coded Boarding Pass) standard string.
     * Basic format: M1SURNAME/NAME          EABCDEF JFKLAX DL 0123 123Y001A0001 1
     */
    fun parse(raw: String): BoardingPassData? {
        if (raw.length < 57) return null
        if (raw[0] != 'M') return null

        try {
            val passengerName = raw.substring(2, 22).trim()
            val pnr = raw.substring(23, 29).trim()
            val from = raw.substring(29, 32).trim()
            val to = raw.substring(32, 35).trim()
            val carrier = raw.substring(35, 38).trim()
            val flightNumber = raw.substring(38, 43).trim()
            val julianDate = raw.substring(43, 46).trim()
            val seat = raw.substring(47, 51).trim()

            return BoardingPassData(
                passengerName = passengerName,
                pnr = pnr,
                from = from,
                to = to,
                carrier = carrier,
                flightNumber = flightNumber,
                date = julianDate,
                seat = seat
            )
        } catch (e: Exception) {
            return null
        }
    }
}
