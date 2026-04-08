package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptListCursor
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ReceiptListSummary
import com.chelovecheck.domain.model.ReceiptOwnershipFilter

/**
 * Persistence boundary for receipts. Implementations in `data` must:
 *
 * - Keep [Receipt.fiscalSign] unique (inserts skip or replace per product rules).
 * - Emit [ReceiptsChangeTracker] updates after any write affecting list/detail.
 * - Use keyset pagination for [getReceiptListPage] (stable ordering per [ReceiptListSortOrder]).
 *
 * Errors are surfaced as [com.chelovecheck.domain.model.AppError] (e.g. [com.chelovecheck.domain.model.AppError.DatabaseError]) where applicable.
 */
interface ReceiptRepository {
    suspend fun saveReceipt(receipt: Receipt)
    suspend fun saveReceipts(receipts: List<Receipt>): SaveManyResult
    suspend fun getReceipts(
        filter: ReceiptFilter,
        searchQuery: String?,
        offset: Int,
        limit: Int,
    ): List<Receipt>

    /**
     * Paged list without loading line items; uses keyset pagination (stable at large offsets).
     */
    suspend fun getReceiptListPage(
        filter: ReceiptFilter,
        searchQuery: String?,
        cursor: ReceiptListCursor?,
        limit: Int,
        ownership: ReceiptOwnershipFilter,
        sortOrder: ReceiptListSortOrder = ReceiptListSortOrder.DEFAULT,
    ): List<ReceiptListSummary>

    suspend fun setReceiptFavorite(fiscalSign: String, favorite: Boolean)

    suspend fun setReceiptPinned(fiscalSign: String, pinned: Boolean)

    /**
     * Replaces line items and payments with data from a freshly fetched receipt (same [Receipt.fiscalSign]).
     */
    suspend fun replaceReceiptFromFetch(receipt: Receipt)

    suspend fun getAllReceipts(): List<Receipt>
    suspend fun getReceipt(fiscalSign: String): Receipt?
    suspend fun deleteAllReceipts()
    suspend fun deleteReceipt(fiscalSign: String)
    suspend fun updateReceiptAddress(fiscalSign: String, address: String)
    suspend fun updateReceiptItems(fiscalSign: String, items: List<Item>)
    suspend fun countReceipts(): Int
}

data class SaveManyResult(
    val imported: List<Receipt>,
    val skipped: List<Receipt>,
)
