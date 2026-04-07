package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.data.analytics.OnnxSentenceEmbedderProvider
import com.chelovecheck.domain.repository.CategoryOverrideRepository
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.rollup.coicopAnalyticsBucketId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class CategoryOverrideMatcher @Inject constructor(
    private val embedderProvider: OnnxSentenceEmbedderProvider,
    private val overrideRepository: CategoryOverrideRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend fun findMatch(normalizedName: String): OverrideHit? {
        val map = categoryRepository.getAllCategories().associateBy { it.id }
        fun bucket(id: String) = coicopAnalyticsBucketId(id, map)
        val overrides = overrideRepository.getAllOverrides()
        if (overrides.isEmpty()) return null
        val direct = overrides.firstOrNull { it.itemName == normalizedName }
        if (direct != null) {
            return OverrideHit(bucket(direct.categoryId), 1f)
        }
        val embedding = embedderProvider.stage1.embed(normalizedName)
        var best: OverrideHit? = null
        for (override in overrides) {
            val score = cosine(embedding, override.embedding)
            if (score >= OVERRIDE_THRESHOLD && (best == null || score > best.score)) {
                best = OverrideHit(bucket(override.categoryId), score)
            }
        }
        return best
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val size = min(a.size, b.size)
        var sum = 0f
        for (i in 0 until size) sum += a[i] * b[i]
        return sum
    }

    data class OverrideHit(val categoryId: String, val score: Float)

    companion object {
        private const val OVERRIDE_THRESHOLD = 0.9f
    }
}
