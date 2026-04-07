package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(language: AppLanguage) {
        repository.setLanguage(language)
    }
}
