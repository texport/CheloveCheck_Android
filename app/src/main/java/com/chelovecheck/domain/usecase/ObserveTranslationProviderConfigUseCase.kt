package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.TranslationProviderConfig
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTranslationProviderConfigUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<TranslationProviderConfig> = repository.translationProviderConfig
}
