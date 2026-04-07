package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

class TogglePinnedReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository,
) {
    suspend operator fun invoke(fiscalSign: String, pinned: Boolean) {
        repository.setReceiptPinned(fiscalSign, pinned)
    }
}
