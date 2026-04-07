package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.repository.ReceiptRepository
import javax.inject.Inject

/**
 * Re-downloads receipt JSON from the stored OFD URL and replaces local line items / totals.
 */
class RefetchReceiptFromUrlUseCase @Inject constructor(
    private val repository: ReceiptRepository,
    private val fetchReceiptByUrl: FetchReceiptByUrlUseCase,
) {
    suspend operator fun invoke(fiscalSign: String) {
        val existing = repository.getReceipt(fiscalSign) ?: return
        val fresh = fetchReceiptByUrl(existing.url)
        repository.replaceReceiptFromFetch(fresh)
    }
}
