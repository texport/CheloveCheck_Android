package com.chelovecheck.domain.model

import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorTest {
    @Test
    fun allAccentSeedsAreOpaqueArgb() {
        AccentColor.entries.forEach { accent ->
            val c = accent.seedArgb
            assertTrue((c ushr 24) == 0xFF)
        }
    }
}
