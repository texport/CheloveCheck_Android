package com.chelovecheck.data.analytics

import com.chelovecheck.data.local.ItemCategoryCacheDao
import com.chelovecheck.data.local.ItemCategoryCacheEntity
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.CategoryCandidate
import com.chelovecheck.domain.model.CategoryPrediction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ItemCategoryCacheStore @Inject constructor(
    private val dao: ItemCategoryCacheDao,
    private val json: Json,
    private val logger: AppLogger,
) {
    suspend fun get(nameKey: String): CategoryPrediction? {
        val entity = dao.getByKey(nameKey) ?: return null
        if (entity.modelVersion != AnalyticsEmbeddingModelInfo.LOGICAL_MODEL_REVISION) return null
        val candidates = decodeCandidates(entity.candidatesJson)
        return CategoryPrediction(
            categoryId = entity.categoryId,
            confidence = entity.confidence,
            candidates = candidates,
            isCertain = entity.isCertain,
        )
    }

    suspend fun invalidateByNormalizedName(normalized: String) {
        if (normalized.isBlank()) return
        runCatching {
            dao.deleteByNormalizedName(normalized, "$normalized|%")
        }.onFailure { error -> logger.error(TAG, "cache invalidate failed: ${error.message}", error) }
    }

    suspend fun put(nameKey: String, prediction: CategoryPrediction) {
        val entity = ItemCategoryCacheEntity(
            nameKey = nameKey,
            categoryId = prediction.categoryId,
            confidence = prediction.confidence,
            isCertain = prediction.isCertain,
            candidatesJson = encodeCandidates(prediction.candidates),
            modelVersion = AnalyticsEmbeddingModelInfo.LOGICAL_MODEL_REVISION,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        runCatching { dao.upsert(entity) }
            .onFailure { error -> logger.error(TAG, "cache write failed: ${error.message}", error) }
    }

    private fun decodeCandidates(raw: String): List<CategoryCandidate> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<CategoryCandidateDto>>(raw).map {
                CategoryCandidate(it.categoryId, it.score)
            }
        }.getOrElse { error ->
            logger.error(TAG, "cache decode failed: ${error.message}", error)
            emptyList()
        }
    }

    private fun encodeCandidates(candidates: List<CategoryCandidate>): String {
        if (candidates.isEmpty()) return "[]"
        val payload = candidates.map { CategoryCandidateDto(it.categoryId, it.score) }
        return runCatching { json.encodeToString(payload) }.getOrElse { "[]" }
    }

    @Serializable
    private data class CategoryCandidateDto(
        @SerialName("categoryId") val categoryId: String,
        @SerialName("score") val score: Float,
    )

    companion object {
        private const val TAG = "ItemCategoryCache"
    }
}
