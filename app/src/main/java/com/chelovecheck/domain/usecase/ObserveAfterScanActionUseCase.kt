package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAfterScanActionUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AfterScanAction> = repository.afterScanAction
}
