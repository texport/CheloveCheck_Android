package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
