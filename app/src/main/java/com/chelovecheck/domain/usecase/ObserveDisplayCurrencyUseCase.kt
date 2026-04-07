package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDisplayCurrencyUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<DisplayCurrency> = repository.displayCurrency
}
