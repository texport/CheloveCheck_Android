package com.chelovecheck.data.analytics

import com.chelovecheck.domain.model.AnalyticsLoadStage
import com.chelovecheck.domain.model.AnalyticsProgress
import com.chelovecheck.domain.repository.AnalyticsForegroundProgress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class AnalyticsProgressStore @Inject constructor() : AnalyticsForegroundProgress {
    private val _stage = MutableStateFlow<AnalyticsLoadStage?>(null)
    override val stage: StateFlow<AnalyticsLoadStage?> = _stage
    private val _progress = MutableStateFlow(AnalyticsProgress())
    override val progress: StateFlow<AnalyticsProgress> = _progress

    override fun report(stage: AnalyticsLoadStage) {
        val current = _stage.value
        if (current == null || stagePriority(stage) >= stagePriority(current)) {
            _stage.value = stage
        }
    }

    override fun reportProgress(processedItems: Int, totalItems: Int) {
        _progress.value = AnalyticsProgress(
            processedItems = processedItems.coerceAtLeast(0),
            totalItems = totalItems.coerceAtLeast(0),
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    override fun clear() {
        _stage.value = null
        _progress.value = AnalyticsProgress()
    }

    private fun stagePriority(stage: AnalyticsLoadStage): Int {
        return when (stage) {
            AnalyticsLoadStage.LOADING_MODEL -> 0
            AnalyticsLoadStage.BUILDING_INDEX -> 1
            AnalyticsLoadStage.ANALYZING_RECEIPTS -> 2
        }
    }
}
