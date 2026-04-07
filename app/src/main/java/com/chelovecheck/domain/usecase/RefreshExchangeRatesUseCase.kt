package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.ExchangeRateRepository
import javax.inject.Inject

class RefreshExchangeRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
) {
    suspend operator fun invoke(): Boolean = exchangeRateRepository.refreshRatesFromNationalBank()
}
