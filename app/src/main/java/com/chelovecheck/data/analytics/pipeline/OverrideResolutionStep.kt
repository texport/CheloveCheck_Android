package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.RetailCategoryHint

/**
 * Step 2.2 (plan): rule-based and override-based resolution before embedding.
 */
internal class OverrideResolutionStep(
    private val ruleBased: RuleBasedPredictionStep,
) {
    suspend fun resolve(
        normalizedName: String,
        retailHint: RetailCategoryHint?,
    ): CategoryPredictionWithSource? = ruleBased.run(normalizedName, retailHint)
}
