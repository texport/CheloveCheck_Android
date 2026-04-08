package com.chelovecheck.domain.model

data class AnalyticsProgress(
    val processedItems: Int = 0,
    val totalItems: Int = 0,
    val updatedAtMillis: Long = 0L,
)
