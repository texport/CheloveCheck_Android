package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.ReceiptQuery
import com.chelovecheck.domain.repository.ReceiptUrlBuilder
import javax.inject.Inject

class BuildReceiptUrlUseCase @Inject constructor(
    private val receiptUrlBuilder: ReceiptUrlBuilder,
) {
    operator fun invoke(query: ReceiptQuery): String = receiptUrlBuilder.buildUrl(query)
}
