package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.AnalyticsLoadStage
import com.chelovecheck.domain.model.AnalyticsProgress
import kotlinx.coroutines.flow.StateFlow

interface AnalyticsForegroundProgress : AnalyticsProgressReporter {
    val stage: StateFlow<AnalyticsLoadStage?>

    val progress: StateFlow<AnalyticsProgress>
}
