package com.chelovecheck.data.analytics

import com.chelovecheck.domain.model.AnalyticsPeriod
import com.chelovecheck.domain.model.AnalyticsRunState
import com.chelovecheck.domain.model.AnalyticsSummary
import com.chelovecheck.domain.repository.AnalyticsForegroundRunCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class AnalyticsRunStore @Inject constructor() : AnalyticsForegroundRunCoordinator {
    private val _state = MutableStateFlow(AnalyticsRunState())
    override val state: StateFlow<AnalyticsRunState> = _state

    override fun markRunning(period: AnalyticsPeriod, token: Long) {
        _state.value = AnalyticsRunState(isRunning = true, period = period, token = token)
    }

    override fun complete(period: AnalyticsPeriod, token: Long, summary: AnalyticsSummary) {
        _state.value = AnalyticsRunState(
            isRunning = false,
            period = period,
            token = token,
            summary = summary,
        )
    }

    override fun fail(period: AnalyticsPeriod, token: Long, errorMessage: String?) {
        _state.value = AnalyticsRunState(
            isRunning = false,
            period = period,
            token = token,
            errorMessage = errorMessage,
        )
    }
}
