package com.chelovecheck.data.logging

import android.util.Log
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Singleton
class LogcatLogger @Inject constructor(
    settingsRepository: SettingsRepository,
) : AppLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var level: LogLevel = LogLevel.ERROR

    init {
        settingsRepository.logLevel
            .onEach { level = it }
            .launchIn(scope)
    }

    override fun debug(tag: String, message: String, throwable: Throwable?) {
        if (level != LogLevel.DEBUG) return
        if (throwable != null) {
            Log.d(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (level == LogLevel.OFF) return
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
