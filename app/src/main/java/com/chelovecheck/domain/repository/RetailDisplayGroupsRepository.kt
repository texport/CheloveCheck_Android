package com.chelovecheck.domain.repository

import com.chelovecheck.domain.analytics.RetailDisplayGroupResolver
import com.chelovecheck.domain.model.RetailDisplayGroupsConfig

interface RetailDisplayGroupsRepository {
    suspend fun getConfig(): RetailDisplayGroupsConfig

    suspend fun getResolver(): RetailDisplayGroupResolver

    /**
     * Short label for a [displayGroupId] (e.g. `retail_food`) or COICOP id for candidate dialogs.
     */
    fun labelForCategoryOrDisplayId(categoryOrDisplayId: String, languageTag: String): String?

    /**
     * One COICOP **rollup** id stored in overrides when the user picks this [displayGroupId]
     * (first prefix from config, or a fixed fallback for groups without prefixes).
     */
    fun canonicalCoicopRollupForDisplayGroup(displayGroupId: String): String?

    /**
     * Retail group ids shown in manual category dialogs (no COICOP subcategories). Excludes adjustments-only bucket.
     */
    fun pickerDisplayGroupIds(): List<String>

    /** Maps a COICOP rollup id to the retail display group id (for preview labels). */
    fun displayGroupIdForCoicopRollup(rollupId: String): String
}
