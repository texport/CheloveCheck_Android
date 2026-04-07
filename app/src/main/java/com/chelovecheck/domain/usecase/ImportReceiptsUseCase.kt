package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.ReceiptJsonCodec
import com.chelovecheck.domain.repository.ReceiptRepository
import com.chelovecheck.domain.repository.SaveManyResult
import javax.inject.Inject

class ImportReceiptsUseCase @Inject constructor(
    private val repository: ReceiptRepository,
    private val codec: ReceiptJsonCodec,
) {
    suspend operator fun invoke(json: String): SaveManyResult {
        val receipts = codec.decode(json)
        return repository.saveReceipts(receipts)
    }
}
