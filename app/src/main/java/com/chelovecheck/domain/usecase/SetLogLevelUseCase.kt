package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.LogLevel
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetLogLevelUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(level: LogLevel) {
        repository.setLogLevel(level)
    }
}
