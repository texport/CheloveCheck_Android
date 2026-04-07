package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveMapProviderUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<MapProvider> = repository.mapProvider
}
