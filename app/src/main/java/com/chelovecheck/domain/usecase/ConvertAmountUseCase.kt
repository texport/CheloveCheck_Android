package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.repository.ExchangeRateRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Converts a KZT amount to the selected display currency (for UI). */
class ConvertAmountUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
) {
    suspend operator fun invoke(
        amountKzt: Double,
        target: DisplayCurrency,
        atEpochMillis: Long? = null,
    ): Double {
        if (target == DisplayCurrency.KZT) return amountKzt
        val atDate = atEpochMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val rate = exchangeRateRepository.tengePerUnit(target.code, atDate)
        if (rate <= 0.0) return amountKzt
        return amountKzt / rate
    }
}
