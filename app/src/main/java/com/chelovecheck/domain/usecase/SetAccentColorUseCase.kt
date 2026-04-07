package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetAccentColorUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(color: AccentColor) {
        repository.setAccentColor(color)
    }
}
