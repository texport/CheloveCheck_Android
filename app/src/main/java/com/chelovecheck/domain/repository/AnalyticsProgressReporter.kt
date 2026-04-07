package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.AnalyticsLoadStage

interface AnalyticsProgressReporter {
    fun report(stage: AnalyticsLoadStage)
    fun reportProgress(processedItems: Int, totalItems: Int) {}
    fun clear()
}
