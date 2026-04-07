package com.chelovecheck.domain.analytics

import com.chelovecheck.domain.model.RetailDisplayGroupsConfig

/**
 * Maps COICOP rollup ids (level-2 preferred, see [com.chelovecheck.domain.rollup.coicopAnalyticsBucketId])
 * to a retail display group id. Longer [RetailDisplayGroup.coicopRollupPrefixes] win over shorter ones.
 */
class RetailDisplayGroupResolver(
    config: RetailDisplayGroupsConfig,
) {
    private val prefixRules: List<Pair<String, String>> = config.groups
        .asSequence()
        .flatMap { g -> g.coicopRollupPrefixes.asSequence().map { p -> p to g.id } }
        .sortedWith(
            compareByDescending<Pair<String, String>> { it.first.length }
                .thenBy { it.first },
        )
        .toList()

    fun displayGroupForRollup(rollupId: String): String {
        for ((prefix, groupId) in prefixRules) {
            if (rollupId == prefix || rollupId.startsWith("$prefix.")) {
                return groupId
            }
        }
        return FALLBACK_GROUP_ID
    }

    companion object {
        const val FALLBACK_GROUP_ID = "retail_uncategorized"
        const val SERVICES_GROUP_ID = "retail_services"
        const val ADJUSTMENTS_GROUP_ID = "retail_adjustments"
    }
}
