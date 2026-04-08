package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ItemTranslationLanguage
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetItemTranslationLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(language: ItemTranslationLanguage) {
        repository.setItemTranslationLanguage(language)
    }
}
