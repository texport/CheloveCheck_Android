package com.chelovecheck.shared.datacore

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun platformName_isNonBlank() {
        assertTrue(Platform.name.isNotBlank())
    }
}
