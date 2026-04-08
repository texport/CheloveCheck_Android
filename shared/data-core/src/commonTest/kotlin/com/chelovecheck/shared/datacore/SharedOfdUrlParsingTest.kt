package com.chelovecheck.shared.datacore

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedOfdUrlParsingTest {
    @Test
    fun parseQueryParams_splitsAmpersandPairs() {
        val m = SharedOfdUrlParsing.parseQueryParams("https://x/?a=1&b=two")
        assertEquals("1", m["a"])
        assertEquals("two", m["b"])
    }

    @Test
    fun parseQueryParams_noQuery_returnsEmpty() {
        assertEquals(emptyMap(), SharedOfdUrlParsing.parseQueryParams("https://x/path"))
    }
}
