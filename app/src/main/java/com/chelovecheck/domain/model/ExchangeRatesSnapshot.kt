package com.chelovecheck.domain.model

/**
 * Cached official rates: tenge per 1 unit of [currencyCode] (e.g. USD → KZT per $1).
 */
data class ExchangeRatesSnapshot(
    val lastUpdatedEpochMillis: Long?,
    val tengePerUnitByCode: Map<String, Double>,
)
