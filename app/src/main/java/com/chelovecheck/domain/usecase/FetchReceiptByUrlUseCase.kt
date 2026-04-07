package com.chelovecheck.domain.usecase

import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.repository.ReceiptFetcher
import java.net.URL
import javax.inject.Inject

class FetchReceiptByUrlUseCase @Inject constructor(
    private val receiptFetcher: ReceiptFetcher,
) {
    suspend operator fun invoke(urlOrCode: String): Receipt {
        val normalized = if (urlOrCode.startsWith("http://")) {
            urlOrCode.replaceFirst("http://", "https://")
        } else {
            urlOrCode
        }

        val url = try {
            URL(normalized)
        } catch (error: Exception) {
            throw AppError.InvalidQrCode
        }

        return receiptFetcher.fetchReceiptByUrl(url.toString())
    }
}
