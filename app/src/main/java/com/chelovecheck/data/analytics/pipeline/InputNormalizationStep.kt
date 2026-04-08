package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.RetailClassificationContext
import com.chelovecheck.domain.utils.ItemNameNormalizer

internal data class ClassificationInput(
    val normalizedName: String,
    val displayName: String,
    val cacheKey: String,
)

internal class InputNormalizationStep {
    fun run(
        rawName: String,
        retailContext: RetailClassificationContext?,
    ): ClassificationInput {
        val normalized = ItemNameNormalizer.normalizeForMatch(rawName)
        val displayName = ItemNameNormalizer.cleanDisplayName(rawName)
        val key = CategoryPredictionPipeline.predictionCacheKey(normalized, retailContext)
        return ClassificationInput(
            normalizedName = normalized,
            displayName = displayName,
            cacheKey = key,
        )
    }
}
