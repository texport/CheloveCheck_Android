package com.chelovecheck.domain.model

/**
 * Keyset cursor for paging. Fields used depend on [ReceiptListSortOrder] in the repository.
 */
data class ReceiptListCursor(
    val dateTimeEpochMillis: Long,
    val fiscalSign: String,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val totalSum: Double = 0.0,
    val companyName: String = "",
)
