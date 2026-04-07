package com.chelovecheck.domain.analytics

import com.chelovecheck.domain.model.RetailDisplayGroup
import com.chelovecheck.domain.model.RetailDisplayGroupsConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Keeps [analytics_retail_groups_fixture.json] aligned with `app/src/main/assets/analytics_retail_groups.json`.
 * Update the fixture when changing the production asset.
 */
class AnalyticsRetailGroupsFixtureTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun fixtureRollupMappingRegression() {
        val text = javaClass.classLoader!!.getResourceAsStream("analytics_retail_groups_fixture.json")!!.bufferedReader()
            .readText()
        val dto = json.decodeFromString(FixtureDto.serializer(), text)
        val config = RetailDisplayGroupsConfig(
            schemaVersion = dto.schemaVersion,
            groups = dto.groups.map {
                RetailDisplayGroup(it.id, it.coicopRollupPrefixes, it.names)
            },
        )
        val r = RetailDisplayGroupResolver(config)
        assertEquals(2, dto.schemaVersion)
        assertEquals("retail_food", r.displayGroupForRollup("01.1"))
        assertEquals("retail_drinks", r.displayGroupForRollup("01.2"))
        assertEquals("retail_alcohol_tobacco", r.displayGroupForRollup("02.2"))
        assertEquals("retail_health", r.displayGroupForRollup("06.2"))
        assertEquals("retail_diy_garden", r.displayGroupForRollup("05.5"))
        assertEquals("retail_fuel", r.displayGroupForRollup("07.2"))
        assertEquals("retail_transport_goods", r.displayGroupForRollup("07.1"))
        assertEquals("retail_uncategorized", r.displayGroupForRollup("00"))
        assertEquals(RetailDisplayGroupResolver.FALLBACK_GROUP_ID, r.displayGroupForRollup("99.1"))
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
