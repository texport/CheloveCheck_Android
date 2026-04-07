package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.CategoryOverride
import com.chelovecheck.domain.repository.CategoryEmbeddingService
import com.chelovecheck.domain.repository.CategoryOverrideRepository
import com.chelovecheck.domain.repository.CategoryPredictionCacheInvalidator
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.repository.RetailDisplayGroupsRepository
import com.chelovecheck.domain.rollup.coicopAnalyticsBucketId
import com.chelovecheck.domain.utils.ItemNameNormalizer
import javax.inject.Inject

/**
 * Persists a user-chosen COICOP **rollup** for a normalized item name and invalidates
 * the on-device category prediction cache so analytics recomputes consistently.
 */
class SaveCategoryOverrideUseCase @Inject constructor(
    private val embeddingService: CategoryEmbeddingService,
    private val repository: CategoryOverrideRepository,
    private val categoryRepository: CategoryRepository,
    private val retailDisplayGroupsRepository: RetailDisplayGroupsRepository,
    private val predictionCacheInvalidator: CategoryPredictionCacheInvalidator,
) {
    suspend operator fun invoke(itemName: String, categoryId: String) {
        val normalized = ItemNameNormalizer.normalizeForMatch(itemName)
        val embedding = embeddingService.embed(normalized)
        val map = categoryRepository.getAllCategories().associateBy { it.id }
        val coicopRollupInput = if (categoryId.startsWith("retail_")) {
            retailDisplayGroupsRepository.canonicalCoicopRollupForDisplayGroup(categoryId) ?: categoryId
        } else {
            categoryId
        }
        val bucketId = coicopAnalyticsBucketId(coicopRollupInput, map)
        repository.saveOverride(
            CategoryOverride(
                itemName = normalized,
                categoryId = bucketId,
                embedding = embedding,
            )
        )
        predictionCacheInvalidator.invalidateAfterUserOverride(normalized)
    }
}
