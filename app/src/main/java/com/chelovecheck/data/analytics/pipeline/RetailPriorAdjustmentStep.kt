package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.CoicopCategory
import com.chelovecheck.domain.model.RetailClassificationContext
import com.chelovecheck.domain.model.RetailCategoryHint

/**
 * Step 2.2 (plan): apply retail-network priors when applicable.
 */
internal class RetailPriorAdjustmentStep(
    private val postProcessor: CategoryPredictionPostProcessor,
) {
    fun apply(
        clamped: CategoryPrediction,
        hint: RetailCategoryHint?,
        categories: Map<String, CoicopCategory>,
        skipRetailPrior: Boolean,
        retailContext: RetailClassificationContext?,
    ): CategoryPrediction {
        if (skipRetailPrior || retailContext == null) return clamped
        return postProcessor.applyRetailPrior(clamped, hint, categories)
    }
}
