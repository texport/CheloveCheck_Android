package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

/** Replaces existing receipts with imported copies (same [Receipt.fiscalSign]). */
class ReplaceImportedDuplicatesUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(receipts: List<Receipt>) {
        receipts.forEach { repository.replaceReceiptFromFetch(it) }
    }
}
