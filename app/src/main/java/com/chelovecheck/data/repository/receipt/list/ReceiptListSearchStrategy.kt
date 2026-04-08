package com.chelovecheck.data.repository.receipt.list

import android.util.Log
import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptListCursor
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ReceiptListSummary
import com.chelovecheck.domain.model.ReceiptOwnershipFilter

internal class ReceiptListSearchStrategy(
    private val loader: suspend (
        filter: ReceiptFilter,
        searchQuery: String?,
        cursor: ReceiptListCursor?,
        limit: Int,
        ownership: ReceiptOwnershipFilter,
        useFtsForItemNames: Boolean,
        sortOrder: ReceiptListSortOrder,
    ) -> List<ReceiptListSummary>,
) {
    suspend fun load(
        filter: ReceiptFilter,
        searchQuery: String?,
        cursor: ReceiptListCursor?,
        limit: Int,
        ownership: ReceiptOwnershipFilter,
        sortOrder: ReceiptListSortOrder,
    ): List<ReceiptListSummary> {
        Log.d(
            "ChecksSearch",
            "list page request: filter=$filter query='${searchQuery?.take(80)}' cursor=${cursor?.fiscalSign ?: "null"} " +
                "limit=$limit ownership=$ownership sort=$sortOrder",
        )
        return try {
            val result = loader(
                filter,
                searchQuery,
                cursor,
                limit,
                ownership,
                true,
                sortOrder,
            )
            Log.d("ChecksSearch", "list page result (fts=true): count=${result.size}")
            result
        } catch (_: Exception) {
            Log.d("ChecksSearch", "list page fallback: fts failed, retry with LIKE")
            val result = loader(
                filter,
                searchQuery,
                cursor,
                limit,
                ownership,
                false,
                sortOrder,
            )
            Log.d("ChecksSearch", "list page result (fts=false): count=${result.size}")
            result
        }
    }
}
