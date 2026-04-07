package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetHapticsEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setHapticsEnabled(enabled)
    }
}
