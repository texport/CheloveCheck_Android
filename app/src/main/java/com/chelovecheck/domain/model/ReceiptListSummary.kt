package com.chelovecheck.domain.model

import java.time.Instant

/**
 * Receipt row for list screens: no line items loaded (only [itemsCount]).
 */
data class ReceiptListSummary(
    val fiscalSign: String,
    val companyName: String,
    val companyAddress: String,
    val iinBin: String,
    val dateTime: Instant,
    /** Cursor for keyset pagination; matches [ReceiptEntity.dateTimeEpochMillis]. */
    val dateTimeEpochMillis: Long,
    val ofd: Ofd,
    val typeOperation: OperationType,
    val totalSum: Double,
    val itemsCount: Int,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
)
