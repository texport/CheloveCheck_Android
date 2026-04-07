package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.ReceiptJsonCodec
import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class ExportReceiptsUseCase @Inject constructor(
    private val repository: ReceiptRepository,
    private val codec: ReceiptJsonCodec,
) {
    suspend operator fun invoke(): String {
        val receipts = repository.getAllReceipts()
        return codec.encode(receipts)
    }
}
