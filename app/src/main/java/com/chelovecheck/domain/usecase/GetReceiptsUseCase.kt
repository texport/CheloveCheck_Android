package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class GetReceiptsUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(
        filter: ReceiptFilter,
        searchQuery: String?,
        offset: Int,
        limit: Int,
    ): List<Receipt> {
        return repository.getReceipts(filter, searchQuery, offset, limit)
    }
}
