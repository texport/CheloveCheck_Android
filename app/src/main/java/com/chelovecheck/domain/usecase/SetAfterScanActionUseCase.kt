package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetAfterScanActionUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(action: AfterScanAction) {
        repository.setAfterScanAction(action)
    }
}
