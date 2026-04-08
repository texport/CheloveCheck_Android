package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CategoryCandidate
import com.chelovecheck.domain.model.CategoryIds
import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.CoicopCategory
import com.chelovecheck.domain.model.RetailCategoryHint
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.rollup.coicopAnalyticsBucketId
import com.chelovecheck.domain.rollup.coicopL1Id
import javax.inject.Inject
import kotlin.math.min

/**
 * Post-pipeline policies: clamping to rollup buckets, retail priors, domain hints, uncertainty gating,
 * and lexical/embedding merge rules.
 */
class CategoryPredictionPostProcessor @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend fun clampPrediction(prediction: CategoryPrediction): CategoryPrediction {
        val map = categoryRepository.getAllCategories().associateBy { it.id }
        fun bucket(id: String) = coicopAnalyticsBucketId(id, map)
        val candidates = prediction.candidates
            .map { CategoryCandidate(bucket(it.categoryId), it.score) }
            .groupBy { it.categoryId }
            .map { (_, list) -> list.maxBy { it.score } }
            .sortedByDescending { it.score }
            .take(MAX_CANDIDATES)
        val categoryId = prediction.categoryId?.let { bucket(it) }
            ?: candidates.firstOrNull()?.categoryId
        return CategoryPrediction(
            categoryId = categoryId,
            confidence = prediction.confidence,
            candidates = candidates,
            isCertain = prediction.isCertain,
        )
    }

    fun mergeLexicalAndEmbedding(
        lexical: CategoryPrediction,
        embedding: CategoryPrediction,
        categories: Map<String, CoicopCategory>,
    ): CategoryPrediction {
        val lexTop = lexical.candidates.firstOrNull()?.categoryId ?: lexical.categoryId
        val embTop = embedding.candidates.firstOrNull()?.categoryId ?: embedding.categoryId
        val lexL1 = lexTop?.let { coicopL1Id(it, categories) }
        val embL1 = embTop?.let { coicopL1Id(it, categories) }
        val foodVsHarmfulEmbedding = lexL1 == "01" && (embL1 == "02" || embL1 == "06")
        val clothingVsFood = lexL1 == "03" && embL1 == "01"
        val servicesVsFood = lexL1 == "12" && embL1 == "01"
        if (foodVsHarmfulEmbedding || clothingVsFood || servicesVsFood) {
            val candidates = (lexical.candidates + embedding.candidates)
                .distinctBy { it.categoryId }
                .sortedByDescending { it.score }
                .take(MAX_CANDIDATES)
            return CategoryPrediction(
                categoryId = lexical.categoryId,
                confidence = lexical.confidence,
                candidates = candidates,
                isCertain = false,
            )
        }
        if (embedding.isCertain) return embedding
        val candidates = (embedding.candidates + lexical.candidates)
            .distinctBy { it.categoryId }
            .sortedByDescending { it.score }
            .take(MAX_CANDIDATES)
        return CategoryPrediction(
            categoryId = candidates.firstOrNull()?.categoryId,
            confidence = candidates.firstOrNull()?.score ?: 0f,
            candidates = candidates,
            isCertain = false,
        )
    }

    fun applyRetailPrior(
        prediction: CategoryPrediction,
        hint: RetailCategoryHint?,
        categories: Map<String, CoicopCategory>,
    ): CategoryPrediction {
        if (hint == null || hint.isMixedAssortment || hint.primaryRollupIds.isEmpty()) {
            return prediction
        }
        if (prediction.confidence < MIN_CONFIDENCE_FOR_RETAIL_PRIOR) {
            return prediction
        }
        val map = categories
        val bonus = hint.priorWeight
        fun boosted(candidate: CategoryCandidate): Float {
            val bucketId = coicopAnalyticsBucketId(candidate.categoryId, map)
            val match = hint.primaryRollupIds.any { pr ->
                bucketId == pr || bucketId.startsWith("$pr.")
            }
            val add = if (match) min(bonus, MAX_RETAIL_PRIOR_BONUS) else 0f
            return candidate.score + add
        }
        val fromCandidates = prediction.candidates.map { CategoryCandidate(it.categoryId, boosted(it)) }
        val newCandidates = (if (fromCandidates.isNotEmpty()) {
            fromCandidates
        } else {
            prediction.categoryId?.let { id ->
                listOf(CategoryCandidate(id, boosted(CategoryCandidate(id, prediction.confidence))))
            }.orEmpty()
        })
            .sortedByDescending { it.score }
            .take(MAX_CANDIDATES)
        if (newCandidates.isEmpty()) return prediction
        val top = newCandidates.first()
        return CategoryPrediction(
            categoryId = top.categoryId,
            confidence = top.score,
            candidates = newCandidates,
            isCertain = prediction.isCertain,
        )
    }

    fun applyDomainPostFilter(
        prediction: CategoryPrediction,
        normalizedName: String,
        hint: RetailCategoryHint?,
    ): CategoryPrediction {
        var adjusted = prediction
        if (isDrinkLike(normalizedName)) adjusted = boostBucket(adjusted, "01.2", DRINK_BONUS)
        if (isPetLike(normalizedName)) adjusted = boostBucket(adjusted, "09.3", PET_BONUS)
        if (isHouseholdLike(normalizedName)) adjusted = boostBucket(adjusted, "05.6", HOUSEHOLD_BONUS)
        if (hint?.segment == "food_service" && isFoodServiceMenuLike(normalizedName)) {
            adjusted = boostBucket(adjusted, "11.1", FOOD_SERVICE_BONUS)
        }
        return adjusted
    }

    fun applyUncertaintyGateWithReason(
        prediction: CategoryPrediction,
        normalizedName: String,
    ): Pair<CategoryPrediction, String?> {
        val top = prediction.candidates.firstOrNull() ?: return prediction to "no_candidates"
        if (top.categoryId == CategoryIds.UNCATEGORIZED) return prediction to "already_uncategorized"
        val second = prediction.candidates.getOrNull(1)
        val margin = if (second != null) top.score - second.score else 1f
        val isDomainSensitive = isDrinkLike(normalizedName) || isPetLike(normalizedName) || isHouseholdLike(normalizedName)
        val gateScore = if (isDomainSensitive) DOMAIN_MIN_GATE_SCORE else MIN_GATE_SCORE
        val gateMargin = if (isDomainSensitive) DOMAIN_MIN_GATE_MARGIN else MIN_GATE_MARGIN
        if (top.score >= gateScore || margin >= gateMargin) {
            return prediction to null
        }
        return CategoryPrediction(
            categoryId = CategoryIds.UNCATEGORIZED,
            confidence = top.score,
            candidates = prediction.candidates,
            isCertain = false,
        ) to "gate_folded(score=${top.score},margin=$margin)"
    }

    private fun boostBucket(prediction: CategoryPrediction, bucketId: String, bonus: Float): CategoryPrediction {
        val mutable = prediction.candidates.toMutableList()
        val idx = mutable.indexOfFirst { it.categoryId == bucketId }
        if (idx >= 0) {
            val current = mutable[idx]
            mutable[idx] = current.copy(score = current.score + bonus)
        } else {
            mutable.add(CategoryCandidate(bucketId, bonus))
        }
        val sorted = mutable
            .sortedByDescending { it.score }
            .take(MAX_CANDIDATES)
        val top = sorted.firstOrNull() ?: return prediction
        return prediction.copy(categoryId = top.categoryId, confidence = top.score, candidates = sorted)
    }

    companion object {
        private const val MAX_CANDIDATES = 6

        private const val MIN_CONFIDENCE_FOR_RETAIL_PRIOR = 0.45f
        private const val MAX_RETAIL_PRIOR_BONUS = 0.12f

        private const val MIN_GATE_SCORE = 0.28f
        private const val MIN_GATE_MARGIN = 0.04f
        private const val DOMAIN_MIN_GATE_SCORE = 0.24f
        private const val DOMAIN_MIN_GATE_MARGIN = 0.02f
        private const val DRINK_BONUS = 0.16f
        private const val PET_BONUS = 0.20f
        private const val HOUSEHOLD_BONUS = 0.16f
        private const val FOOD_SERVICE_BONUS = 0.12f

        private val drinkToken = Regex(
            """(?i)(cola|coca[\s-]?cola|fanta|sprite|rich|сок|шырын|нектар|чай|кофе|какао|мин\s?вода|минерал)""",
        )
        private val petToken = Regex(
            """(?i)(felix|probalance|lapka|корм\s+для\s+кош|корм\s+для\s+соб|корм\s+для\s+живот|мысық|cat\s+food|dog\s+food|pet\s+food|cat\s+litter)""",
        )
        private val householdToken = Regex(
            """(?i)(влажн\w*\s+салфет|антисеп|спрей|шампун|akmasept|palmolive|туалетн\w*\s+бумаг|чистящ\w*\s+средств|гель\s+для\s+стирк|порошок)""",
        )
        private val foodServiceMenuToken = Regex(
            """(?i)(воппер|биф\s*ролл|бургер|крылышк|обед|комбо|хачапури|цезарь)""",
        )

        internal fun isFoodServiceMenuLike(normalizedName: String): Boolean = foodServiceMenuToken.containsMatchIn(normalizedName)

        private fun isDrinkLike(normalizedName: String): Boolean = drinkToken.containsMatchIn(normalizedName)
        private fun isPetLike(normalizedName: String): Boolean = petToken.containsMatchIn(normalizedName)
        private fun isHouseholdLike(normalizedName: String): Boolean = householdToken.containsMatchIn(normalizedName)
    }
}
