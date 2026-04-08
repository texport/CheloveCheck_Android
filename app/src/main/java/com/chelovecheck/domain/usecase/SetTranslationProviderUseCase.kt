package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.TranslationProvider
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetTranslationProviderUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(provider: TranslationProvider) {
        repository.setTranslationProvider(provider)
    }
}
