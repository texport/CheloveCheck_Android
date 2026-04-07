package com.chelovecheck.domain.repository

/**
 * Clears on-disk and in-memory item prediction caches after a user override so the next run
 * prefers the override path over stale cached predictions.
 */
interface CategoryPredictionCacheInvalidator {
    suspend fun invalidateAfterUserOverride(normalizedItemName: String)
}
