package org.eu.nl.syu.way2fly.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BCBPParserTest {

    @Test
    fun testParseStandardBCBP() {
        // M1DOE/JOHN            EABCDEFJFKLAXDL 00123123Y001A00001 1
        val raw = "M1DOE/JOHN            EABCDEFJFKLAXDL 00123123Y001A00001 1"
        val data = BCBPParser.parse(raw)

        assertNotNull(data)
        assertEquals("DOE/JOHN", data?.passengerName)
        assertEquals("ABCDEF", data?.pnr)
        assertEquals("JFK", data?.from)
        assertEquals("LAX", data?.to)
        assertEquals("DL", data?.carrier)
        assertEquals("00123", data?.flightNumber)
        assertEquals("123", data?.date)
        assertEquals("001A", data?.seat)
    }
}
