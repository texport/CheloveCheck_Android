package com.chelovecheck.domain.model

/**
 * One line in purchase history for a normalized product key.
 */
data class ItemPurchaseRow(
    val fiscalSign: String,
    val companyName: String,
    val dateTimeEpochMillis: Long,
    val itemName: String,
    val sum: Double,
    val quantity: Double,
)
