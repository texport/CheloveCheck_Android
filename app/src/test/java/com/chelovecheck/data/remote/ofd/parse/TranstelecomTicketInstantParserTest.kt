package com.chelovecheck.data.remote.ofd.parse

import com.chelovecheck.domain.model.AppError
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TranstelecomTicketInstantParserTest {
    private val zone = ZoneId.of("Asia/Almaty")

    @Test
    fun parseFromUrlParameterT_compactWithT() {
        val url = "https://ofd1.kz/t/?i=123&t=20250303T114419"
        val instant = TranstelecomTicketInstantParser.parseFromUrlParameterT(url)
        assertNotNull(instant)
        val zdt = instant!!.atZone(zone)
        assertEquals(2025, zdt.year)
        assertEquals(3, zdt.monthValue)
        assertEquals(3, zdt.dayOfMonth)
        assertEquals(11, zdt.hour)
        assertEquals(44, zdt.minute)
        assertEquals(19, zdt.second)
    }

    @Test
    fun parseFromUrlParameterT_fourteenDigitsWithoutT() {
        val url = "https://ofd1.kz/t/?t=20250303114419"
        val instant = TranstelecomTicketInstantParser.parseFromUrlParameterT(url)
        assertNotNull(instant)
        val zdt = instant!!.atZone(zone)
        assertEquals(2025, zdt.year)
        assertEquals(3, zdt.monthValue)
        assertEquals(3, zdt.dayOfMonth)
        assertEquals(11, zdt.hour)
        assertEquals(44, zdt.minute)
    }

    @Test
    fun parse_fallsBackToUrlWhenHtmlTextUnparseable() {
        val url = "https://ofd1.kz/t/?t=20250303T114419"
        val instant = TranstelecomTicketInstantParser.parse("", url)
        val zdt = instant.atZone(zone)
        assertEquals(2025, zdt.year)
        assertEquals(3, zdt.monthValue)
        assertEquals(3, zdt.dayOfMonth)
    }

    @Test
    fun parse_kazakhMonthNameInText() {
        val text = "03 наурыз 2025, 11:44"
        val url = "https://ofd1.kz/t/"
        val instant = TranstelecomTicketInstantParser.parse(text, url)
        val zdt = instant.atZone(zone)
        assertEquals(2025, zdt.year)
        assertEquals(3, zdt.monthValue)
        assertEquals(3, zdt.dayOfMonth)
        assertEquals(11, zdt.hour)
        assertEquals(44, zdt.minute)
    }

    @Test
    fun parse_throwsParsingErrorWithDetailsWhenNothingWorks() {
        try {
            TranstelecomTicketInstantParser.parse("not a date", "https://ofd1.kz/t/?x=1")
            fail("expected ParsingError")
        } catch (e: AppError.ParsingError) {
            assertNotNull(e.details)
            assertTrue(e.details!!.contains("Transtelecom date"))
        }
    }
}
