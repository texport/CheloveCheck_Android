package com.chelovecheck.data.remote.ofd.sanitize

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfdResponseSanitizerTest {
    @Test
    fun sanitizeUrl_stripsQueryParams() {
        val out = OfdResponseSanitizer.sanitizeUrl("https://ofd.kz/check?token=abc&f=1")
        assertEquals("https://ofd.kz/check", out)
    }

    @Test
    fun sanitizeBodyPreview_truncatesLongPayload() {
        val out = OfdResponseSanitizer.sanitizeBodyPreview("x".repeat(600))
        assertTrue(out.length < 600)
        assertTrue(out.endsWith("..."))
    }
}
