package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }
}
