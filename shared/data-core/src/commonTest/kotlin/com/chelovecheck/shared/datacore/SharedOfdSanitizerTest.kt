package com.chelovecheck.shared.datacore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedOfdSanitizerTest {
    @Test
    fun sanitizeUrl_stripsQuery() {
        assertEquals("https://ofd.kz/check", SharedOfdSanitizer.sanitizeUrl("https://ofd.kz/check?a=1"))
    }

    @Test
    fun sanitizeBodyPreview_truncates() {
        val out = SharedOfdSanitizer.sanitizeBodyPreview("x".repeat(600))
        assertTrue(out.length < 600)
    }
}
