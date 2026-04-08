package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CategoryPrediction

/**
 * Step 2.2 (plan): uncertainty gate / confidence policy before persistence.
 */
internal class ConfidencePolicyStep(
    private val postProcessor: CategoryPredictionPostProcessor,
) {
    fun apply(
        prediction: CategoryPrediction,
        normalizedName: String,
    ): Pair<CategoryPrediction, String?> =
        postProcessor.applyUncertaintyGateWithReason(prediction, normalizedName)
}
