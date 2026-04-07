package com.chelovecheck.data.repository

import android.content.Context
import com.chelovecheck.domain.analytics.RetailDisplayGroupResolver
import com.chelovecheck.domain.model.CategoryIds
import com.chelovecheck.domain.model.RetailDisplayGroup
import com.chelovecheck.domain.model.RetailDisplayGroupsConfig
import com.chelovecheck.domain.repository.ReceiptsChangeTracker
import com.chelovecheck.domain.repository.RetailDisplayGroupsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class RetailDisplayGroupsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
    private val receiptsChangeTracker: ReceiptsChangeTracker,
) : RetailDisplayGroupsRepository {
    private val lock = Any()
    @Volatile
    private var cachedConfig: RetailDisplayGroupsConfig? = null
    private var cachedResolver: RetailDisplayGroupResolver? = null
    private var idToNames: Map<String, Map<String, String>> = emptyMap()
    private var canonicalRollupByDisplayId: Map<String, String> = emptyMap()
    private var pickerIdsCache: List<String> = emptyList()

    override suspend fun getConfig(): RetailDisplayGroupsConfig {
        return withContext(Dispatchers.IO) { ensureLoaded() }
    }

    override suspend fun getResolver(): RetailDisplayGroupResolver {
        return withContext(Dispatchers.IO) {
            ensureLoaded()
            checkNotNull(cachedResolver)
        }
    }

    override fun labelForCategoryOrDisplayId(categoryOrDisplayId: String, languageTag: String): String? {
        ensureLoaded()
        val names = idToNames[categoryOrDisplayId] ?: return null
        val raw = names[languageTag]
            ?: names["en"]
            ?: names["ru"]
            ?: names.values.firstOrNull()
        return raw?.trim()?.takeIf { it.isNotEmpty() }
    }

    override fun canonicalCoicopRollupForDisplayGroup(displayGroupId: String): String? {
        ensureLoaded()
        return canonicalRollupByDisplayId[displayGroupId]
    }

    override fun pickerDisplayGroupIds(): List<String> {
        ensureLoaded()
        return pickerIdsCache
    }

    override fun displayGroupIdForCoicopRollup(rollupId: String): String {
        ensureLoaded()
        return cachedResolver!!.displayGroupForRollup(rollupId)
    }

    private fun ensureLoaded(): RetailDisplayGroupsConfig {
        cachedConfig?.let { return it }
        synchronized(lock) {
            cachedConfig?.let { return it }
            val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val dto = json.decodeFromString(RetailGroupsFileDto.serializer(), text)
            val config = RetailDisplayGroupsConfig(
                schemaVersion = dto.schemaVersion,
                groups = dto.groups.map {
                    RetailDisplayGroup(
                        id = it.id,
                        coicopRollupPrefixes = it.coicopRollupPrefixes,
                        names = it.names,
                    )
                },
            )
            cachedConfig = config
            idToNames = config.groups.associate { g -> g.id to g.names }
            cachedResolver = RetailDisplayGroupResolver(config)
            canonicalRollupByDisplayId = buildCanonicalRollups(config)
            pickerIdsCache = config.groups
                .map { it.id }
                .filter { it != RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID }
                .sorted()
            maybeNotifySchemaChange(config.schemaVersion)
            return config
        }
    }

    private fun buildCanonicalRollups(config: RetailDisplayGroupsConfig): Map<String, String> {
        return config.groups.associate { g ->
            val rollup = when {
                g.coicopRollupPrefixes.isNotEmpty() ->
                    g.coicopRollupPrefixes.minOrNull()!!
                g.id == RetailDisplayGroupResolver.SERVICES_GROUP_ID -> "12.1"
                g.id == RetailDisplayGroupResolver.ADJUSTMENTS_GROUP_ID -> CategoryIds.UNCATEGORIZED
                else -> CategoryIds.UNCATEGORIZED
            }
            g.id to rollup
        }
    }

    private fun maybeNotifySchemaChange(schemaVersion: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val last = prefs.getInt(KEY_SCHEMA, -1)
        if (last != schemaVersion) {
            prefs.edit().putInt(KEY_SCHEMA, schemaVersion).apply()
            receiptsChangeTracker.notifyChanged()
        }
    }

    companion object {
        private const val ASSET_NAME = "analytics_retail_groups.json"
        private const val PREFS_NAME = "chelovecheck_analytics"
        private const val KEY_SCHEMA = "display_groups_schema_version"
    }
}

@Serializable
private data class RetailGroupsFileDto(
    val schemaVersion: Int,
    val groups: List<RetailGroupDto>,
)

@Serializable
private data class RetailGroupDto(
    val id: String,
    val coicopRollupPrefixes: List<String> = emptyList(),
    val names: Map<String, String>,
)
