package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.AnalyticsPeriod
import com.chelovecheck.domain.model.AnalyticsRunState
import com.chelovecheck.domain.model.AnalyticsSummary
import kotlinx.coroutines.flow.StateFlow

interface AnalyticsForegroundRunCoordinator {
    val state: StateFlow<AnalyticsRunState>

    fun markRunning(period: AnalyticsPeriod, token: Long)

    fun complete(period: AnalyticsPeriod, token: Long, summary: AnalyticsSummary)

    fun fail(period: AnalyticsPeriod, token: Long, errorMessage: String?)
}
