package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class SaveReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(receipt: Receipt) {
        try {
            if (repository.getReceipt(receipt.fiscalSign) != null) {
                throw AppError.ReceiptAlreadyExists(receipt.fiscalSign)
            }
            repository.saveReceipt(receipt)
        } catch (error: Exception) {
            if (error is AppError) throw error
            throw AppError.FailedToSaveReceipt(error)
        }
    }
}
