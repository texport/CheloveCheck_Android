package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.AnalyticsPeriod
import com.chelovecheck.domain.model.AnalyticsSummary

interface AnalyticsPeriodSummaryCache {
    fun get(period: AnalyticsPeriod, token: Long): AnalyticsSummary?

    fun put(period: AnalyticsPeriod, token: Long, summary: AnalyticsSummary)
}
