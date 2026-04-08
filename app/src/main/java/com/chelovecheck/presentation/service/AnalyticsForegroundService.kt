package com.chelovecheck.presentation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.chelovecheck.R
import com.chelovecheck.domain.model.AnalyticsProgress
import com.chelovecheck.domain.repository.AnalyticsForegroundProgress
import com.chelovecheck.domain.repository.AnalyticsForegroundRunCoordinator
import com.chelovecheck.domain.repository.AnalyticsPeriodSummaryCache
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.AnalyticsLoadStage
import com.chelovecheck.domain.model.AnalyticsPeriod
import com.chelovecheck.domain.usecase.GetAnalyticsUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class AnalyticsForegroundService : Service() {
    @Inject lateinit var getAnalyticsUseCase: GetAnalyticsUseCase
    @Inject lateinit var analyticsForegroundRunCoordinator: AnalyticsForegroundRunCoordinator
    @Inject lateinit var analyticsPeriodSummaryCache: AnalyticsPeriodSummaryCache
    @Inject lateinit var analyticsForegroundProgress: AnalyticsForegroundProgress
    @Inject lateinit var logger: AppLogger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var notificationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var progressStartMillis: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (job?.isActive == true) {
            return START_NOT_STICKY
        }
        val period = intent?.getStringExtra(EXTRA_PERIOD)?.let { AnalyticsPeriod.valueOf(it) } ?: AnalyticsPeriod.ALL
        val token = intent?.getLongExtra(EXTRA_TOKEN, 0L) ?: 0L
        val from = parseInstant(intent?.getLongExtra(EXTRA_FROM, -1L) ?: -1L)
        val to = parseInstant(intent?.getLongExtra(EXTRA_TO, -1L) ?: -1L)

        analyticsForegroundRunCoordinator.markRunning(period, token)
        progressStartMillis = null
        startForeground(
            NOTIFICATION_ID,
            buildNotification(analyticsForegroundProgress.stage.value, analyticsForegroundProgress.progress.value),
        )
        acquireWakeLock()

        notificationJob = serviceScope.launch {
            combine(analyticsForegroundProgress.stage, analyticsForegroundProgress.progress) { stage, progress ->
                stage to progress
            }.collect { (stage, progress) ->
                updateNotification(stage, progress)
            }
        }

        job = serviceScope.launch {
            val result = runCatching { getAnalyticsUseCase(from = from, to = to) }
            result.onSuccess { summary ->
                analyticsPeriodSummaryCache.put(period, token, summary)
                analyticsForegroundRunCoordinator.complete(period, token, summary)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                logger.error(TAG, "analytics failed: ${error.message}", error)
                analyticsForegroundRunCoordinator.fail(period, token, error.message)
            }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        notificationJob?.cancel()
        analyticsForegroundProgress.clear()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun parseInstant(epochMillis: Long): Instant? {
        return if (epochMillis >= 0L) Instant.ofEpochMilli(epochMillis) else null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:AnalyticsForegroundService",
        ).apply {
            setReferenceCounted(false)
            acquire(WAKELOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun buildNotification(stage: AnalyticsLoadStage?, progress: AnalyticsProgress): android.app.Notification {
        val stageText = when (stage) {
            AnalyticsLoadStage.LOADING_MODEL -> getString(R.string.analytics_loading_model)
            AnalyticsLoadStage.BUILDING_INDEX -> getString(R.string.analytics_loading_index)
            AnalyticsLoadStage.ANALYZING_RECEIPTS -> getString(R.string.analytics_loading_analyze)
            null -> getString(R.string.analytics_loading)
        }
        val progressText = progressText(progress)
        val contentText = if (progressText.isNotBlank()) {
            "$stageText • $progressText"
        } else {
            stageText
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.analytics_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        val total = progress.totalItems
        val processed = progress.processedItems
        if (total > 0) {
            builder.setProgress(total, processed.coerceAtMost(total), false)
        } else {
            builder.setProgress(0, 0, false)
        }
        return builder.build()
    }

    private fun updateNotification(stage: AnalyticsLoadStage?, progress: AnalyticsProgress) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(stage, progress))
    }

    private fun progressText(progress: AnalyticsProgress): String {
        val total = progress.totalItems
        val processed = progress.processedItems
        if (total <= 0) return ""
        if (processed <= 0) return "0%"
        if (progressStartMillis == null) {
            progressStartMillis = System.currentTimeMillis()
        }
        val percent = (processed.toDouble() / total.toDouble() * 100.0).toInt().coerceIn(0, 100)
        val eta = estimateRemainingMillis(processed, total)
        return if (eta != null) {
            "$percent% • ~${formatDuration(eta)}"
        } else {
            "$percent%"
        }
    }

    private fun estimateRemainingMillis(processed: Int, total: Int): Long? {
        if (processed <= 0 || total <= 0 || processed >= total) return null
        val start = progressStartMillis ?: return null
        val elapsed = System.currentTimeMillis() - start
        if (elapsed <= 0L) return null
        val rate = processed.toDouble() / elapsed.toDouble()
        if (rate <= 0.0) return null
        val remaining = (total - processed).toDouble() / rate
        return remaining.toLong().coerceAtLeast(0L)
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}m"
            minutes > 0L -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.analytics_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "AnalyticsService"
        private const val CHANNEL_ID = "analytics_background"
        private const val NOTIFICATION_ID = 9101
        private const val WAKELOCK_TIMEOUT_MS = 60 * 60 * 1000L

        private const val ACTION_START = "com.chelovecheck.analytics.START"
        private const val ACTION_STOP = "com.chelovecheck.analytics.STOP"
        private const val EXTRA_PERIOD = "extra_period"
        private const val EXTRA_FROM = "extra_from"
        private const val EXTRA_TO = "extra_to"
        private const val EXTRA_TOKEN = "extra_token"

        fun newStartIntent(
            context: Context,
            period: AnalyticsPeriod,
            from: Instant?,
            to: Instant?,
            token: Long,
        ): Intent {
            return Intent(context, AnalyticsForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PERIOD, period.name)
                putExtra(EXTRA_FROM, from?.toEpochMilli() ?: -1L)
                putExtra(EXTRA_TO, to?.toEpochMilli() ?: -1L)
                putExtra(EXTRA_TOKEN, token)
            }
        }

        fun newStopIntent(context: Context): Intent {
            return Intent(context, AnalyticsForegroundService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
