package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetMapProviderUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(provider: MapProvider) {
        repository.setMapProvider(provider)
    }
}
