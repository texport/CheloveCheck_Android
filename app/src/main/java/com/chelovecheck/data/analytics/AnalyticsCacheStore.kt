package com.chelovecheck.data.analytics

import com.chelovecheck.domain.model.AnalyticsSummary
import com.chelovecheck.presentation.model.AnalyticsPeriod
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsCacheStore @Inject constructor() {
    private val cache = mutableMapOf<AnalyticsPeriod, CacheEntry>()

    fun get(period: AnalyticsPeriod, token: Long): AnalyticsSummary? {
        return cache[period]?.takeIf { it.token == token }?.summary
    }

    fun put(period: AnalyticsPeriod, token: Long, summary: AnalyticsSummary) {
        cache[period] = CacheEntry(token, summary)
    }

    private data class CacheEntry(
        val token: Long,
        val summary: AnalyticsSummary,
    )
}
