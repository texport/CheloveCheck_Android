package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetAnalyticsPendingPromptUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setAnalyticsPendingPromptEnabled(enabled)
    }
}
