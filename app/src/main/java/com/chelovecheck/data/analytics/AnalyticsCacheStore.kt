package com.chelovecheck.data.analytics

import com.chelovecheck.domain.model.AnalyticsPeriod
import com.chelovecheck.domain.model.AnalyticsSummary
import com.chelovecheck.domain.repository.AnalyticsPeriodSummaryCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsCacheStore @Inject constructor() : AnalyticsPeriodSummaryCache {
    private val cache = mutableMapOf<AnalyticsPeriod, CacheEntry>()

    override fun get(period: AnalyticsPeriod, token: Long): AnalyticsSummary? {
        return cache[period]?.takeIf { it.token == token }?.summary
    }

    override fun put(period: AnalyticsPeriod, token: Long, summary: AnalyticsSummary) {
        cache[period] = CacheEntry(token, summary)
    }

    private data class CacheEntry(
        val token: Long,
        val summary: AnalyticsSummary,
    )
}
