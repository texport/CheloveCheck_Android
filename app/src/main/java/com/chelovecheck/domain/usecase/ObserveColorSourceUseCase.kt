package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveColorSourceUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<ColorSource> = repository.colorSource
}
