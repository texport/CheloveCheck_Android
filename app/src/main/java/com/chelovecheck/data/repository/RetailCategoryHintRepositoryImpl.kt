package com.chelovecheck.data.repository

import android.content.Context
import com.chelovecheck.domain.analytics.RetailSegmentDefaults
import com.chelovecheck.domain.model.RetailCategoryHint
import com.chelovecheck.domain.repository.RetailCategoryHintRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class RetailCategoryHintRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) : RetailCategoryHintRepository {
    private val profiles: Map<String, ProfileDto> by lazy { loadProfiles() }
    private val normalizedProfiles: Map<String, ProfileDto> by lazy {
        profiles.entries.associateBy({ normalizeNetworkName(it.key) }, { it.value })
    }

    override fun getHint(networkName: String?): RetailCategoryHint? {
        if (networkName.isNullOrBlank()) return null
        val dto = profiles[networkName]
            ?: normalizedProfiles[normalizeNetworkName(networkName)]
            ?: return null
        val primary = dto.primaryRollupIds
            ?: RetailSegmentDefaults.primaryRollupIds(dto.segment)
        return RetailCategoryHint(
            segment = dto.segment,
            primaryRollupIds = primary,
            priorWeight = dto.priorWeight,
            isMixedAssortment = dto.isMixedAssortment,
        )
    }

    private fun loadProfiles(): Map<String, ProfileDto> {
        return runCatching {
            val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val root = json.decodeFromString<RootDto>(text)
            root.profiles
        }.getOrElse { emptyMap() }
    }

    @Serializable
    private data class RootDto(
        val version: String = "1.0",
        val profiles: Map<String, ProfileDto> = emptyMap(),
    )

    @Serializable
    private data class ProfileDto(
        val segment: String,
        val primaryRollupIds: List<String>? = null,
        val priorWeight: Float = 0.1f,
        @SerialName("isMixedAssortment") val isMixedAssortment: Boolean = false,
    )

    companion object {
        private const val ASSET_NAME = "retail_network_profiles.json"

        private fun normalizeNetworkName(raw: String): String {
            return raw
                .lowercase()
                .trim()
                .replace("[\"'`]+".toRegex(), "")
                .replace("\\b(тоо|т\\.?о\\.?о\\.?|ооо|ип|и\\.?п\\.?)\\b".toRegex(), " ")
                .replace("[^\\p{L}\\p{Nd}]+".toRegex(), " ")
                .replace("\\s+".toRegex(), " ")
                .trim()
        }
    }
}
