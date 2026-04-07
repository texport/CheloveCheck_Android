package com.chelovecheck.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * Result of list query: receipt row + item count without loading line items.
 */
data class ReceiptWithItemCount(
    @Embedded val receipt: ReceiptEntity,
    @ColumnInfo(name = "itemsCount") val itemsCount: Int,
)
