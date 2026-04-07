package com.chelovecheck.domain.analytics

import com.chelovecheck.domain.model.RetailDisplayGroup
import com.chelovecheck.domain.model.RetailDisplayGroupsConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class RetailDisplayGroupResolverTest {

    private val resolver = RetailDisplayGroupResolver(
        RetailDisplayGroupsConfig(
            schemaVersion = 1,
            groups = listOf(
                RetailDisplayGroup(
                    id = "retail_food",
                    coicopRollupPrefixes = listOf("01.1"),
                    names = mapOf("ru" to "Еда", "en" to "Food", "kk" to "Тағам"),
                ),
                RetailDisplayGroup(
                    id = "retail_drinks",
                    coicopRollupPrefixes = listOf("01.2"),
                    names = mapOf("ru" to "Напитки", "en" to "Drinks", "kk" to "Сусын"),
                ),
                RetailDisplayGroup(
                    id = "retail_uncategorized",
                    coicopRollupPrefixes = listOf("00"),
                    names = mapOf("ru" to "Без категории", "en" to "Uncategorized", "kk" to "Санатсыз"),
                ),
            ),
        ),
    )

    @Test
    fun prefersLongestPrefix() {
        val r = RetailDisplayGroupResolver(
            RetailDisplayGroupsConfig(
                schemaVersion = 1,
                groups = listOf(
                    RetailDisplayGroup("a", listOf("01"), mapOf("en" to "A")),
                    RetailDisplayGroup("b", listOf("01.1"), mapOf("en" to "B")),
                ),
            ),
        )
        assertEquals("b", r.displayGroupForRollup("01.1"))
    }

    @Test
    fun mapsRollupToRetailGroup() {
        assertEquals("retail_food", resolver.displayGroupForRollup("01.1"))
        assertEquals("retail_drinks", resolver.displayGroupForRollup("01.2"))
        assertEquals("retail_uncategorized", resolver.displayGroupForRollup("00"))
    }

    @Test
    fun unknownRollupFallsBack() {
        assertEquals(RetailDisplayGroupResolver.FALLBACK_GROUP_ID, resolver.displayGroupForRollup("99.9"))
    }
}
