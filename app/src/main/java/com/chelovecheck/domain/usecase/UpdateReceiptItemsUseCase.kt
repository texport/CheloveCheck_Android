package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class UpdateReceiptItemsUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(fiscalSign: String, items: List<Item>) {
        repository.updateReceiptItems(fiscalSign, items)
    }
}
