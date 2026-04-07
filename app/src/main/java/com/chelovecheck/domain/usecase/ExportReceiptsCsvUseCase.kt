package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.export.ReceiptCsvFormatter
import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class ExportReceiptsCsvUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(): String {
        val receipts = repository.getAllReceipts()
        return ReceiptCsvFormatter.formatAll(receipts)
    }
}
