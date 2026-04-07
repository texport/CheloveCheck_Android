package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class DeleteReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(fiscalSign: String) {
        repository.deleteReceipt(fiscalSign)
    }
}
