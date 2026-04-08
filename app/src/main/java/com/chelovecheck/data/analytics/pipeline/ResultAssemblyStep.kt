package com.chelovecheck.data.analytics.pipeline

import com.chelovecheck.domain.model.CategoryPrediction
import com.chelovecheck.domain.model.CoicopCategory
import com.chelovecheck.domain.model.RetailCategoryHint

/**
 * Step 2.2 (plan): clamp to rollup buckets, then domain hints (inside post-processor), then confidence gate.
 */
internal class ResultAssemblyStep(
    private val postProcessor: CategoryPredictionPostProcessor,
    private val retailPriorStep: RetailPriorAdjustmentStep,
    private val confidencePolicyStep: ConfidencePolicyStep,
) {
    data class AssemblyResult(
        val output: CategoryPrediction,
        val gateReason: String?,
        val prePrior: CategoryPrediction,
        val domainAdjusted: CategoryPrediction,
    )

    suspend fun assemble(
        raw: CategoryPrediction,
        normalizedName: String,
        retailContext: com.chelovecheck.domain.model.RetailClassificationContext?,
        hint: RetailCategoryHint?,
        skipRetailPrior: Boolean,
        categories: Map<String, CoicopCategory>,
    ): AssemblyResult {
        val clamped = postProcessor.clampPrediction(raw)
        val afterPrior = retailPriorStep.apply(clamped, hint, categories, skipRetailPrior, retailContext)
        val domainAdjusted = postProcessor.applyDomainPostFilter(afterPrior, normalizedName, hint)
        val (out, gateReason) = confidencePolicyStep.apply(domainAdjusted, normalizedName)
        return AssemblyResult(
            output = out,
            gateReason = gateReason,
            prePrior = clamped,
            domainAdjusted = domainAdjusted,
        )
    }
}
