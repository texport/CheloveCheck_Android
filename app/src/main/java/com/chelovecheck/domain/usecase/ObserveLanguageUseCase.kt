package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppLanguage> = repository.language
}
