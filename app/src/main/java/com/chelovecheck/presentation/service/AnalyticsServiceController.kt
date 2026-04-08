package com.chelovecheck.presentation.service

import android.content.Context
import androidx.core.content.ContextCompat
import com.chelovecheck.domain.model.AnalyticsPeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsServiceController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun start(period: AnalyticsPeriod, from: Instant?, to: Instant?, token: Long) {
        val intent = AnalyticsForegroundService.newStartIntent(context, period, from, to, token)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop() {
        context.stopService(AnalyticsForegroundService.newStopIntent(context))
    }
}
