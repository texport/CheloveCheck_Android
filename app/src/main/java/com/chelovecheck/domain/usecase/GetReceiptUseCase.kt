package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class GetReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(fiscalSign: String): Receipt? {
        return repository.getReceipt(fiscalSign)
    }
}
