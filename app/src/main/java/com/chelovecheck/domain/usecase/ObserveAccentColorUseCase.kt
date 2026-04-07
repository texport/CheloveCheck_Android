package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAccentColorUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AccentColor> = repository.accentColor
}
