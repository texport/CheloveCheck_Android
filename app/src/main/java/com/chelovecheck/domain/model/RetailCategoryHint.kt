package com.chelovecheck.domain.model

/**
 * Profile for biasing COICOP predictions toward typical assortments for a retail network.
 */
data class RetailCategoryHint(
    val segment: String,
    /** COICOP rollup ids (L1/L2) that receive a score bonus when [isMixedAssortment] is false. */
    val primaryRollupIds: List<String>,
    val priorWeight: Float,
    val isMixedAssortment: Boolean,
)
