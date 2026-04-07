package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptListCursor
import com.chelovecheck.domain.model.ReceiptListItem
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ReceiptOwnershipFilter
import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class GetReceiptListPageUseCase @Inject constructor(
    private val repository: ReceiptRepository,
    private val getNetworkNameUseCase: GetNetworkNameUseCase,
) {
    suspend operator fun invoke(
        filter: ReceiptFilter,
        searchQuery: String?,
        cursor: ReceiptListCursor?,
        limit: Int,
        ownership: ReceiptOwnershipFilter = ReceiptOwnershipFilter.All,
        sortOrder: ReceiptListSortOrder = ReceiptListSortOrder.DEFAULT,
    ): List<ReceiptListItem> {
        val rows = repository.getReceiptListPage(
            filter,
            searchQuery,
            cursor,
            limit,
            ownership,
            sortOrder,
        )
        return rows.map { summary ->
            val networkName = getNetworkNameUseCase(summary.iinBin)
            ReceiptListItem(
                summary = summary,
                displayName = networkName ?: summary.companyName,
            )
        }
    }
}
