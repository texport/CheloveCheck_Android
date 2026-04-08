package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CategoryCandidate
import com.chelovecheck.domain.model.CategoryIds
import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.RetailCategoryHint

internal class RuleBasedPredictionStep(
    private val overrideMatcher: CategoryOverrideMatcher,
) {
    suspend fun run(
        normalizedName: String,
        retailHint: RetailCategoryHint?,
    ): CategoryPredictionWithSource? {
        if (ReceiptLineSpecialRules.isTechnicalPlaceholder(normalizedName) ||
            ReceiptLineSpecialRules.isServiceModifier(normalizedName)
        ) {
            return CategoryPredictionWithSource(
                prediction = fixedPrediction(CategoryIds.UNCATEGORIZED, 0.95f),
                source = "special_rule_uncategorized",
                skipRetailPrior = true,
            )
        }
        if (ReceiptLineSpecialRules.isFoodServiceMenu(normalizedName)) {
            return CategoryPredictionWithSource(
                prediction = fixedPrediction("11.1", 0.82f),
                source = "special_rule_food_service",
                skipRetailPrior = true,
            )
        }
        overrideMatcher.findMatch(normalizedName)?.let { hit ->
            if (!shouldSkipOverrideForFoodService(retailHint, hit.categoryId, normalizedName)) {
                return CategoryPredictionWithSource(
                    prediction = CategoryPrediction(
                        categoryId = hit.categoryId,
                        confidence = hit.score,
                        candidates = listOf(CategoryCandidate(hit.categoryId, hit.score)),
                        isCertain = true,
                    ),
                    source = "override",
                    skipRetailPrior = true,
                )
            }
        }
        if (DisposablePackagingRule.matchesNormalized(normalizedName)) {
            return CategoryPredictionWithSource(
                prediction = CategoryPrediction(
                    categoryId = CategoryIds.UNCATEGORIZED,
                    confidence = 1f,
                    candidates = listOf(CategoryCandidate(CategoryIds.UNCATEGORIZED, 1f)),
                    isCertain = true,
                ),
                source = "packaging_rule",
                skipRetailPrior = true,
            )
        }
        return null
    }

    private fun fixedPrediction(categoryId: String, confidence: Float): CategoryPrediction {
        return CategoryPrediction(
            categoryId = categoryId,
            confidence = confidence,
            candidates = listOf(CategoryCandidate(categoryId, confidence)),
            isCertain = true,
        )
    }

    private fun shouldSkipOverrideForFoodService(
        hint: RetailCategoryHint?,
        overrideCategoryId: String,
        normalizedName: String,
    ): Boolean {
        if (hint?.segment != "food_service") return false
        if (!(overrideCategoryId == "01" || overrideCategoryId.startsWith("01."))) return false
        return CategoryPredictionPostProcessor.isFoodServiceMenuLike(normalizedName)
    }
}

internal data class CategoryPredictionWithSource(
    val prediction: CategoryPrediction,
    val source: String,
    val skipRetailPrior: Boolean,
)
