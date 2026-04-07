package com.chelovecheck.domain.analytics

/**
 * Default COICOP rollup priors by segment name from [retail_network_profiles.json].
 */
object RetailSegmentDefaults {
    fun primaryRollupIds(segment: String): List<String> = when (segment) {
        "grocery" -> listOf("01")
        "electronics" -> listOf("09", "08", "05")
        "pharmacy" -> listOf("06")
        "fuel" -> listOf("07")
        "fashion" -> listOf("03")
        "discount_mixed" -> emptyList()
        else -> emptyList()
    }
}
