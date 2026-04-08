package com.chelovecheck.data.remote.ofd.parse

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** Golden / regression strings for shared OFD parsing helpers. */
class OfdParsingCommonsGoldenTest {
    @Test
    fun parseQueryParams_preservesOrderOfLastDuplicateKey() {
        val m = OfdParsingCommons.parseQueryParams("https://h.example/t?a=1&b=two%20words&a=2")
        assertEquals("2", m["a"])
        assertEquals("two words", m["b"])
    }

    @Test
    fun convertQrDateToApiDate_golden() {
        assertEquals("2024-03-15", OfdParsingCommons.convertQrDateToApiDate("20240315T143022"))
    }

    @Test
    fun parseDateTimeOrNow_pattern_golden() {
        val instant = OfdParsingCommons.parseDateTimeOrNow("15.03.2024 14:30", "dd.MM.yyyy HH:mm", "Asia/Almaty")
        val z = instant.atZone(ZoneId.of("Asia/Almaty"))
        assertEquals(2024, z.year)
        assertEquals(3, z.monthValue)
        assertEquals(15, z.dayOfMonth)
        assertEquals(14, z.hour)
        assertEquals(30, z.minute)
    }
}
