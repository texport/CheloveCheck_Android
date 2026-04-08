package com.chelovecheck.domain.model

data class AnalyticsRunState(
    val isRunning: Boolean = false,
    val period: AnalyticsPeriod? = null,
    val token: Long? = null,
    val summary: AnalyticsSummary? = null,
    val errorMessage: String? = null,
)
