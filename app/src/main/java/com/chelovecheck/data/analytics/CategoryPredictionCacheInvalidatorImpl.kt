package com.chelovecheck.data.analytics

import com.chelovecheck.data.analytics.pipeline.CategoryPredictionPipeline
import com.chelovecheck.domain.repository.CategoryPredictionCacheInvalidator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryPredictionCacheInvalidatorImpl @Inject constructor(
    private val itemCategoryCacheStore: ItemCategoryCacheStore,
    private val categoryPredictionPipeline: CategoryPredictionPipeline,
) : CategoryPredictionCacheInvalidator {
    override suspend fun invalidateAfterUserOverride(normalizedItemName: String) {
        itemCategoryCacheStore.invalidateByNormalizedName(normalizedItemName)
        categoryPredictionPipeline.invalidateMemoryForNormalizedName(normalizedItemName)
    }
}
