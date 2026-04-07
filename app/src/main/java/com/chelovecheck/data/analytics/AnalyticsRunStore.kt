package com.chelovecheck.data.analytics

import com.chelovecheck.domain.model.AnalyticsSummary
import com.chelovecheck.presentation.model.AnalyticsPeriod
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class AnalyticsRunStore @Inject constructor() {
    private val _state = MutableStateFlow(AnalyticsRunState())
    val state: StateFlow<AnalyticsRunState> = _state

    fun markRunning(period: AnalyticsPeriod, token: Long) {
        _state.value = AnalyticsRunState(isRunning = true, period = period, token = token)
    }

    fun complete(period: AnalyticsPeriod, token: Long, summary: AnalyticsSummary) {
        _state.value = AnalyticsRunState(
            isRunning = false,
            period = period,
            token = token,
            summary = summary,
        )
    }

    fun fail(period: AnalyticsPeriod, token: Long, errorMessage: String?) {
        _state.value = AnalyticsRunState(
            isRunning = false,
            period = period,
            token = token,
            errorMessage = errorMessage,
        )
    }
}

data class AnalyticsRunState(
    val isRunning: Boolean = false,
    val period: AnalyticsPeriod? = null,
    val token: Long? = null,
    val summary: AnalyticsSummary? = null,
    val errorMessage: String? = null,
)
