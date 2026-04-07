package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.ExchangeRatesSnapshot
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Tenge (KZT) per one unit of [currencyCode] (e.g. USD → how many ₸ per $1).
 * Stub implementation may use fixed rates; production can sync NB RK.
 */
interface ExchangeRateRepository {
    /** Returns tenge per 1 unit of foreign currency, or 1.0 for KZT. */
    suspend fun tengePerUnit(currencyCode: String, atDate: LocalDate? = null): Double

    /**
     * Fetches daily official rates from National Bank of Kazakhstan (RSS).
     * @return true if at least one rate was updated.
     */
    suspend fun refreshRatesFromNationalBank(): Boolean

    /** Persisted snapshot (last successful refresh and cached rates). */
    fun observeExchangeRatesSnapshot(): Flow<ExchangeRatesSnapshot>
}
