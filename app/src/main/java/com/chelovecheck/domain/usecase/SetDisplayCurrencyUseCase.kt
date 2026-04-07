package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.repository.SettingsRepository
import javax.inject.Inject

class SetDisplayCurrencyUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(currency: DisplayCurrency) {
        repository.setDisplayCurrency(currency)
    }
}
