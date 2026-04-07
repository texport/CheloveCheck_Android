package com.chelovecheck.domain.rollup

import com.chelovecheck.domain.model.CoicopCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoicopRollupResolverTest {
    @Test
    fun coicopL1Id_findsLevel1() {
        val categories = mapOf(
            "01" to CoicopCategory("01", 1, null, mapOf("ru" to "Продукты"), mapOf()),
            "01.1" to CoicopCategory("01.1", 2, "01", mapOf("ru" to "Продукты"), mapOf()),
            "01.1.8" to CoicopCategory("01.1.8", 3, "01.1", mapOf("ru" to "Шоколад"), mapOf()),
            "06" to CoicopCategory("06", 1, null, mapOf("ru" to "Здоровье"), mapOf()),
        )
        assertEquals("01", coicopL1Id("01.1.8", categories))
        assertEquals("06", coicopL1Id("06", categories))
    }

    @Test
    fun coicopL1Id_unknown_returnsNull() {
        assertNull(coicopL1Id("missing", emptyMap()))
    }
}
