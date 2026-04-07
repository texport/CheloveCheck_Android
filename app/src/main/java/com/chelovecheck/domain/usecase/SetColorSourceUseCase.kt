package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetColorSourceUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(source: ColorSource) {
        repository.setColorSource(source)
    }
}
