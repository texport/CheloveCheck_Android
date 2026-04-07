package com.chelovecheck.domain.model

/**
 * Loaded from [com.chelovecheck.domain.repository.RetailDisplayGroupsRepository] / assets
 * `analytics_retail_groups.json`. IDs are stable public keys for analytics aggregation and UI.
 */
data class RetailDisplayGroup(
    val id: String,
    val coicopRollupPrefixes: List<String>,
    val names: Map<String, String>,
)

data class RetailDisplayGroupsConfig(
    val schemaVersion: Int,
    val groups: List<RetailDisplayGroup>,
)
