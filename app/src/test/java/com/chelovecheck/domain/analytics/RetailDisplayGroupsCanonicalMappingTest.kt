package com.chelovecheck.domain.analytics

import com.chelovecheck.domain.model.CategoryIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Documents the same rules as [com.chelovecheck.data.repository.RetailDisplayGroupsRepositoryImpl.buildCanonicalRollups]
 * and picker filtering. Fixture must stay aligned with production asset.
 */
class RetailDisplayGroupsCanonicalMappingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun canonicalRollupExamples() {
        assertEquals("01.1", canonicalRollupForDisplayGroup("retail_food", listOf("01.1")))
        assertEquals("02.1", canonicalRollupForDisplayGroup("retail_alcohol_tobacco", listOf("02.1", "02.2", "02.3")))
        assertEquals("12.1", canonicalRollupForDisplayGroup(RetailDisplayGroupResolver.SERVICES_GROUP_ID, emptyList()))
        assertEquals(CategoryIds.UNCATEGORIZED, canonicalRollupForDisplayGroup(RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID, emptyList()))
        assertEquals("00", canonicalRollupForDisplayGroup("retail_uncategorized", listOf("00")))
    }

    @Test
    fun pickerExcludesAdjustments() {
        val text = javaClass.classLoader!!.getResourceAsStream("analytics_retail_groups_fixture.json")!!.bufferedReader()
            .readText()
        val dto = json.decodeFromString(FixtureDto.serializer(), text)
        val picker = dto.groups.map { it.id }.filter { it != RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID }.sorted()
        assertFalse(picker.contains(RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID))
        assertTrue(picker.contains("retail_food"))
    }

    private fun canonicalRollupForDisplayGroup(id: String, prefixes: List<String>): String {
        return when {
            prefixes.isNotEmpty() -> prefixes.minOrNull()!!
            id == RetailDisplayGroupResolver.SERVICES_GROUP_ID -> "12.1"
            id == RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID -> CategoryIds.UNCATEGORIZED
            else -> CategoryIds.UNCATEGORIZED
        }
    }

    @Serializable
    private data class FixtureDto(
        val schemaVersion: Int,
        val groups: List<FixtureGroupDto>,
    )

    @Serializable
    private data class FixtureGroupDto(
        val id: String,
        val coicopRollupPrefixes: List<String> = emptyList(),
        val names: Map<String, String>,
    )
}
