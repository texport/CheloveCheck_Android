package com.chelovecheck.domain.model

/**
 * Optional context for item classification (e.g. analytics by retail network from receipt BIN).
 */
data class RetailClassificationContext(
    val networkName: String?,
    val bin: String? = null,
)
