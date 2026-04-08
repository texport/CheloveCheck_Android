package com.chelovecheck.data.remote.ofd.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfdHttpHeadersTest {
    @Test
    fun jsonGetBrowser_includesStandardFieldsAndMergesExtra() {
        val headers = OfdHttpHeaders.jsonGetBrowser(mapOf("Referer" to "https://cabinet.wofd.kz/consumer"))
        assertEquals(OfdHttpHeaders.BROWSER_CHROME_MAC, headers["User-Agent"])
        assertEquals("application/json", headers["Accept"])
        assertEquals("https://cabinet.wofd.kz/consumer", headers["Referer"])
        assertTrue(headers.size == 3)
    }
}
